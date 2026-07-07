import { onUnmounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../../api.js';
import { loadScriptOnce } from '../../loadScript.js';

function formatDateTime(date) {
    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

const ESCAPE_MAP = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
function escapeHtml(str) {
    return String(str == null ? '' : str).replace(/[&<>"']/g, (c) => ESCAPE_MAP[c]);
}
function escapeRegExp(str) {
    return String(str).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

const LEVEL_CLASS = { INFO: 'log-info', WARN: 'log-warn', ERROR: 'log-error' };

export default {
    setup() {
        const activeTab = ref('realtime');
        const realtimeLogs = ref([]);
        const connected = ref(false);
        const realtimeContainer = ref(null);
        let stompClient = null;
        let remainingRetries = 3;

        async function connect() {
            if (remainingRetries <= 0) return;
            await loadScriptOnce('/admin-app/libs/sockjs/sockjs.min.js');
            await loadScriptOnce('/admin-app/libs/stomp/stomp.min.js');
            const socket = new window.SockJS('/websocket-logs', null, { transports: ['websocket'] });
            stompClient = window.Stomp.over(socket);
            stompClient.connect({}, () => {
                remainingRetries = 3;
                connected.value = true;
                ElMessage.success('实时日志连接成功');
                stompClient.subscribe('/topic/logs', (message) => {
                    realtimeLogs.value.push(JSON.parse(message.body));
                    requestAnimationFrame(() => {
                        if (realtimeContainer.value) realtimeContainer.value.scrollTop = realtimeContainer.value.scrollHeight;
                    });
                });
            }, () => {
                remainingRetries--;
                setTimeout(connect, 5000);
            });
        }

        function disconnect() {
            if (stompClient) stompClient.disconnect();
            connected.value = false;
        }

        function switchTab(tab) {
            activeTab.value = tab;
            if (tab === 'query') {
                disconnect();
                realtimeLogs.value = [];
                search();
            } else {
                queryLogs.value = [];
            }
        }

        // 查询时间默认最近 30 分钟
        const defaultNow = new Date();
        const startTime = ref(formatDateTime(new Date(defaultNow.getTime() - 30 * 60 * 1000)));
        const endTime = ref(formatDateTime(defaultNow));
        const logLevel = ref('');
        const keyword = ref('');
        const pageSize = ref(10);
        const queryLogs = ref([]);
        const searching = ref(false);

        async function search() {
            searching.value = true;
            try {
                const res = await api.searchLogs({
                    startTime: startTime.value || undefined,
                    endTime: endTime.value || undefined,
                    logLevel: logLevel.value || undefined,
                    keyword: keyword.value || undefined,
                    size: pageSize.value
                });
                queryLogs.value = (res && res.content) || [];
            } finally {
                searching.value = false;
            }
        }

        function highlightedMessage(message) {
            const escaped = escapeHtml(message);
            if (!keyword.value || !keyword.value.trim()) return escaped;
            const regex = new RegExp(escapeRegExp(escapeHtml(keyword.value)), 'gi');
            return escaped.replace(regex, (match) => '<span class="keyword-highlight">' + match + '</span>');
        }

        function levelClass(level) { return LEVEL_CLASS[level] || ''; }
        function formatTimestamp(ts) { return new Date(ts).toLocaleString(); }

        onUnmounted(disconnect);

        return {
            activeTab, switchTab, realtimeLogs, connected, realtimeContainer, connect, disconnect,
            startTime, endTime, logLevel, keyword, pageSize, queryLogs, searching, search,
            highlightedMessage, levelClass, formatTimestamp
        };
    },
    template: `
    <div>
        <el-card>
            <el-tabs :model-value="activeTab" @tab-change="switchTab">
                <el-tab-pane label="实时日志" name="realtime">
                    <div style="margin-bottom:12px;">
                        <el-button type="primary" :disabled="connected" @click="connect">连接</el-button>
                        <el-button type="danger" :disabled="!connected" @click="disconnect">断开</el-button>
                    </div>
                    <div ref="realtimeContainer" style="height:600px; overflow-y:auto; background:#f8f9fa; border:1px solid #dee2e6; border-radius:4px; padding:10px; font-family:monospace;">
                        <div v-for="(log, i) in realtimeLogs" :key="i" :class="levelClass(log.level)">
                            <span style="color:#6c757d; font-size:0.85em;">[{{ new Date(log.timestamp).toLocaleTimeString() }}]</span>
                            <strong>{{ log.level }}</strong> - {{ log.message }}
                        </div>
                    </div>
                </el-tab-pane>
                <el-tab-pane label="日志查询" name="query">
                    <el-form :inline="true" @submit.prevent="search" style="margin-bottom:12px;">
                        <el-form-item label="开始时间">
                            <el-date-picker v-model="startTime" type="datetime" placeholder="选择开始时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm"></el-date-picker>
                        </el-form-item>
                        <el-form-item label="结束时间">
                            <el-date-picker v-model="endTime" type="datetime" placeholder="选择结束时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm"></el-date-picker>
                        </el-form-item>
                        <el-form-item label="日志级别">
                            <el-select v-model="logLevel" placeholder="全部" clearable style="width:120px">
                                <el-option label="INFO" value="INFO"></el-option>
                                <el-option label="WARN" value="WARN"></el-option>
                                <el-option label="ERROR" value="ERROR"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="每页条数">
                            <el-select v-model="pageSize" style="width:100px">
                                <el-option v-for="n in [10,20,50,100,200,500]" :key="n" :label="n + '条'" :value="n"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="关键字">
                            <el-input v-model="keyword" placeholder="输入关键字" clearable></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" :loading="searching" @click="search">查询</el-button>
                        </el-form-item>
                    </el-form>
                    <div style="height:600px; overflow-y:auto; background:#f8f9fa; border:1px solid #dee2e6; border-radius:4px; padding:10px; font-family:monospace;">
                        <div v-if="!queryLogs.length" style="color:#909399;">没有找到匹配的日志记录。</div>
                        <div v-for="(log, i) in queryLogs" :key="i" :class="levelClass(log.level)">
                            <span style="color:#6c757d; font-size:0.85em;">[{{ formatTimestamp(log.timestamp) }}]</span>
                            <strong>{{ log.level }}</strong> - <span v-html="highlightedMessage(log.message)"></span>
                        </div>
                    </div>
                </el-tab-pane>
            </el-tabs>
        </el-card>
    </div>
    `
};
