import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import zhCn from '../libs/element-plus/locale/zh-cn.js';
import App from './components/App.js';
import router from './router.js';

const app = createApp(App);
app.use(ElementPlus, { locale: zhCn });
app.use(router);
app.mount('#app');
