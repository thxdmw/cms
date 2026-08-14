package com.thx.module.gamesave.integration;

import com.thx.module.file.storage.ObjectStorageClient;
import com.thx.module.file.storage.StoragePutResult;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 只在目标环境工作流中启用，验证 Flyway、MySQL、Redis 与 MinIO 的实际连接配置。
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "gamesave.integration", matches = "true")
class GameSaveTargetEnvironmentIntegrationTest {

    private static final String BUCKET = "game-save-private";

    @Autowired
    private ObjectStorageClient objectStorageClient;

    @Autowired
    private MinioClient minioClient;

    @Test
    void objectCanBeUploadedReadPresignedAndDeleted() throws Exception {
        byte[] content = "gamesave-target-environment".getBytes(StandardCharsets.UTF_8);
        String objectKey = "integration/" + UUID.randomUUID() + ".bin";

        try {
            StoragePutResult result = objectStorageClient.put(
                    BUCKET,
                    objectKey,
                    new ByteArrayInputStream(content),
                    content.length,
                    "application/octet-stream");

            assertEquals(BUCKET, result.getBucket());
            assertEquals(objectKey, result.getObjectKey());
            minioClient.statObject(StatObjectArgs.builder().bucket(BUCKET).object(objectKey).build());

            byte[] downloaded = new byte[content.length];
            try (GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder().bucket(BUCKET).object(objectKey).build())) {
                int offset = 0;
                while (offset < downloaded.length) {
                    int read = response.read(downloaded, offset, downloaded.length - offset);
                    if (read < 0) {
                        break;
                    }
                    offset += read;
                }
                assertEquals(content.length, offset);
            }
            assertArrayEquals(content, downloaded);
            assertFalse(objectStorageClient.presignGet(BUCKET, objectKey, 60).trim().isEmpty());
        } finally {
            objectStorageClient.delete(BUCKET, objectKey);
        }

        assertThrows(ErrorResponseException.class, () -> minioClient.statObject(
                StatObjectArgs.builder().bucket(BUCKET).object(objectKey).build()));
    }
}
