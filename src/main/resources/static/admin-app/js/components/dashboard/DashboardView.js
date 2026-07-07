import { nextTick, onMounted, ref } from 'vue';
import { api } from '../../api.js';
import { loadScriptOnce } from '../../loadScript.js';

export default {
    setup() {
        const statistic = ref({ articleCount: '0', commentCount: '0', lookCount: '0', userCount: '0' });
        const chartCanvas = ref(null);

        async function drawChart(lookCountByDay, userCountByDay) {
            await loadScriptOnce('/admin-app/libs/chartjs/Chart.min.js');
            await nextTick();
            if (!chartCanvas.value) return;
            new window.Chart(chartCanvas.value.getContext('2d'), {
                type: 'line',
                data: {
                    labels: Object.keys(lookCountByDay || {}),
                    datasets: [
                        {
                            label: '近7日访问量',
                            backgroundColor: 'rgba(60,141,188,0.9)',
                            borderColor: 'rgba(60,141,188,0.8)',
                            pointRadius: false,
                            data: Object.values(lookCountByDay || {})
                        },
                        {
                            label: '近7日用户量',
                            backgroundColor: 'rgba(210, 214, 222, 1)',
                            borderColor: 'rgba(210, 214, 222, 1)',
                            pointRadius: false,
                            data: Object.values(userCountByDay || {})
                        }
                    ]
                },
                options: {
                    maintainAspectRatio: false,
                    responsive: true,
                    legend: { display: false },
                    scales: {
                        xAxes: [{ gridLines: { display: false } }],
                        yAxes: [{ gridLines: { display: false } }]
                    }
                }
            });
        }

        onMounted(async () => {
            const res = await api.getDashboardStatistic();
            const data = (res && res.data) || {};
            statistic.value = data;
            drawChart(data.lookCountByDay, data.userCountByDay);
        });

        return { statistic, chartCanvas };
    },
    template: `
    <div>
        <el-row :gutter="16" style="margin-bottom:16px;">
            <el-col :span="6">
                <el-card style="background:#17a2b8; color:#fff;">
                    <div style="font-size:28px; font-weight:600;">{{ statistic.articleCount }}</div>
                    <div>文章数量</div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card style="background:#28a745; color:#fff;">
                    <div style="font-size:28px; font-weight:600;">{{ statistic.commentCount }}</div>
                    <div>评论数量</div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card style="background:#ffc107; color:#fff;">
                    <div style="font-size:28px; font-weight:600;">{{ statistic.lookCount }}</div>
                    <div>访问次数</div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card style="background:#dc3545; color:#fff;">
                    <div style="font-size:28px; font-weight:600;">{{ statistic.userCount }}</div>
                    <div>访问人数</div>
                </el-card>
            </el-col>
        </el-row>
        <el-card>
            <template #header>
                <i class="fas fa-chart-pie" style="margin-right:6px;"></i>访问统计
            </template>
            <div style="position:relative; height:400px;">
                <canvas ref="chartCanvas"></canvas>
            </div>
        </el-card>
    </div>
    `
};
