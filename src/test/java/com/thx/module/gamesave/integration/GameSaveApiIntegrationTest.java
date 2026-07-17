package com.thx.module.gamesave.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.gamesave.service.GameCleanupService;
import com.thx.module.gamesave.service.GameObjectCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 在隔离的 MySQL、Redis、MinIO 环境中验证 GameSave 的完整用户业务闭环。 */
@SpringBootTest(properties = {
        "gamesave.login-user-ip-failures=2",
        "gamesave.login-ip-failures=2"
})
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "gamesave.integration", matches = "true")
class GameSaveApiIntegrationTest {

    private static final String API = "/api/game-save/v1";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private GameObjectCleanupService objectCleanupService;
    @Autowired private GameCleanupService gameCleanupService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearRateLimitKeys() {
        Set<String> keys = redisTemplate.keys("gamesave:auth:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
    }

    @Test
    void registerLoginSyncDownloadDeleteAndCleanupRoundTrip() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "gamesave_" + suffix;
        String password = "Safe-pass-" + suffix;
        String deviceId = "device-" + suffix;

        JsonNode registered = json(mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest(username, password, deviceId, "集成测试设备"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String token = registered.at("/data/deviceToken").asText();
        assertFalse(token.isEmpty());
        assertNotNull(redisTemplate.opsForValue().get("gamesave:auth:register:ip:127.0.0.1"));

        JsonNode loggedIn = json(mockMvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest(username, password, deviceId, "集成测试设备"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        token = loggedIn.at("/data/deviceToken").asText();

        JsonNode game = authorized(post(API + "/games"), token,
                "{\"name\":\"集成游戏-" + suffix + "\",\"provider\":\"CUSTOM\"}", 201);
        String gameId = game.at("/data/gameId").asText();

        byte[] content = ("save-content-" + suffix).getBytes(StandardCharsets.UTF_8);
        String hash = sha256(content);
        String checkBody = "{\"objects\":[{\"sha256\":\"" + hash + "\",\"size\":" + content.length + "}]}";
        assertEquals(1, authorized(post(API + "/objects/check"), token, checkBody, 200).path("data").size());

        MockMultipartFile file = new MockMultipartFile(
                "file", "save.dat", "application/octet-stream", content);
        JsonNode uploaded = json(mockMvc.perform(multipart(API + "/objects")
                        .file(file).param("sha256", hash).param("size", String.valueOf(content.length))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String objectId = uploaded.at("/data/objectId").asText();
        assertFalse(objectId.isEmpty());
        assertEquals(0, authorized(post(API + "/objects/check"), token, checkBody, 200).path("data").size());

        String firstCommit = "{\"expectedHeadSnapshotId\":null,\"triggerType\":\"MANUAL\","
                + "\"description\":\"integration\",\"files\":[{\"path\":\"root1/save.dat\","
                + "\"sha256\":\"" + hash + "\",\"size\":" + content.length + "}]}";
        JsonNode first = authorized(post(API + "/games/" + gameId + "/snapshots"), token, firstCommit, 201);
        String firstSnapshot = first.at("/data/snapshotId").asText();
        JsonNode head = authorized(get(API + "/games/" + gameId + "/head"), token, null, 200);
        assertEquals(firstSnapshot, head.at("/data/headSnapshotId").asText());

        JsonNode download = authorized(get(API + "/objects/" + objectId + "/download-url"), token, null, 200);
        assertArrayEquals(content, readAll(new URL(download.path("data").asText()).openStream()));

        String secondCommit = "{\"expectedHeadSnapshotId\":\"" + firstSnapshot
                + "\",\"triggerType\":\"MANUAL\",\"description\":\"empty\",\"files\":[]}";
        JsonNode second = authorized(post(API + "/games/" + gameId + "/snapshots"), token, secondCommit, 201);
        String secondSnapshot = second.at("/data/snapshotId").asText();
        authorized(delete(API + "/games/" + gameId + "/snapshots/" + firstSnapshot), token, null, 200);
        assertEquals(content.length, authorized(get(API + "/account/quota"), token, null, 200)
                .at("/data/usedBytes").asLong());
        assertEquals(1, objectCleanupService.cleanupDeletingObjects());
        assertEquals(0L, authorized(get(API + "/account/quota"), token, null, 200)
                .at("/data/usedBytes").asLong());

        MockMultipartFile reuploadedFile = new MockMultipartFile(
                "file", "save.dat", "application/octet-stream", content);
        JsonNode reuploaded = json(mockMvc.perform(multipart(API + "/objects")
                        .file(reuploadedFile).param("sha256", hash).param("size", String.valueOf(content.length))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertEquals(objectId, reuploaded.at("/data/objectId").asText());

        String secondDeviceId = "device-second-" + suffix;
        JsonNode secondDeviceLogin = json(mockMvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest(username, password, secondDeviceId, "第二台集成设备"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String secondToken = secondDeviceLogin.at("/data/deviceToken").asText();

        String winningCommit = "{\"expectedHeadSnapshotId\":\"" + secondSnapshot
                + "\",\"triggerType\":\"MANUAL\",\"description\":\"device-one\",\"files\":[{"
                + "\"path\":\"root1/save.dat\",\"sha256\":\"" + hash + "\",\"size\":"
                + content.length + "}]}";
        authorized(post(API + "/games/" + gameId + "/snapshots"), token, winningCommit, 201);
        String losingCommit = "{\"expectedHeadSnapshotId\":\"" + secondSnapshot
                + "\",\"triggerType\":\"MANUAL\",\"description\":\"device-two\",\"files\":[]}";
        JsonNode conflict = authorized(post(API + "/games/" + gameId + "/snapshots"),
                secondToken, losingCommit, 409);
        assertEquals("SYNC_CONFLICT", conflict.path("code").asText());

        authorized(delete(API + "/devices/" + secondDeviceId), token, null, 200);
        mockMvc.perform(get(API + "/games").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isUnauthorized());

        authorized(delete(API + "/games/" + gameId), token, null, 200);
        for (int attempt = 0; attempt < 10 && gameCleanupService.cleanupRunnableTasks() > 0; attempt++) {
            // 每批推进一次游标，直到任务完成。
        }
        assertTrue(authorized(get(API + "/games"), token, null, 200).path("data").isEmpty());
        assertEquals(1, objectCleanupService.cleanupDeletingObjects());
        assertEquals(0L, authorized(get(API + "/account/quota"), token, null, 200)
                .at("/data/usedBytes").asLong());

        jdbcTemplate.update("UPDATE game_device SET token_expire_time=DATE_SUB(NOW(),INTERVAL 1 MINUTE) "
                + "WHERE device_id=?", deviceId);
        mockMvc.perform(get(API + "/games")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-ID", "integration-expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("X-Request-ID", "integration-expired-token"));
    }

    @Test
    void repeatedLoginFailuresAreLimitedThroughRedis() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        AuthRequest invalid = new AuthRequest(
                "missing_" + suffix, "Wrong-pass-" + suffix, "device-" + suffix, "限流测试设备");
        String body = objectMapper.writeValueAsString(invalid);

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post(API + "/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());

        assertEquals("2", redisTemplate.opsForValue().get(
                "gamesave:auth:login:user-ip:missing_" + suffix + ":127.0.0.1"));
    }

    private JsonNode authorized(MockHttpServletRequestBuilder request,
                                String token,
                                String body,
                                int expectedStatus) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return json(mockMvc.perform(request).andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }

    private byte[] readAll(InputStream input) throws Exception {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            for (int read; (read = stream.read(buffer)) >= 0; ) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }

    private String sha256(byte[] content) throws Exception {
        StringBuilder value = new StringBuilder();
        for (byte item : MessageDigest.getInstance("SHA-256").digest(content))
            value.append(String.format("%02x", item & 0xff));
        return value.toString();
    }

    private static final class AuthRequest {
        public final String username;
        public final String password;
        public final String deviceId;
        public final String deviceName;

        private AuthRequest(String username, String password, String deviceId, String deviceName) {
            this.username = username;
            this.password = password;
            this.deviceId = deviceId;
            this.deviceName = deviceName;
        }
    }
}
