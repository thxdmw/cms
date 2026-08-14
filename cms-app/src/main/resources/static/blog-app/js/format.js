// 后端 Jackson 全局日期格式是 yyyy-MM-dd HH:mm:ss，取前 10 位即可，等价于原来的 #dates.format(...,'yyyy-MM-dd')
export function shortDate(dt) {
    return dt ? String(dt).substring(0, 10) : '';
}
