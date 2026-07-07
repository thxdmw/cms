// 少数页面专用的第三方库（Chart.js、SockJS/Stomp）按需加载，不放进全局 index.html，
// 避免每个后台页面都背上只有个别页面才用得到的脚本体积
export function loadScriptOnce(src) {
    return new Promise((resolve, reject) => {
        if (document.querySelector('script[src="' + src + '"]')) { resolve(); return; }
        const s = document.createElement('script');
        s.src = src;
        s.onload = () => resolve();
        s.onerror = () => reject(new Error('加载失败: ' + src));
        document.body.appendChild(s);
    });
}
