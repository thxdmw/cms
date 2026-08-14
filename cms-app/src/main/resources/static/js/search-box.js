function openSearchModal() {
    $("#searchModal").fadeIn(200);
    $("#searchInput").val("").focus();
    $("#searchResults").empty();
}

function closeSearchModal() {
    $("#searchModal").fadeOut(200);
}

function doSearch() {
    var keyword = $("#searchInput").val().trim();
    if (!keyword) {
        alert("请输入搜索关键词");
        return false;
    }
    // 清空旧结果
    $("#searchResults").html('<p>搜索中...</p>');

    // 模拟异步搜索请求：这里用 setTimeout 模拟，换成真实接口即可
    // AJAX 请求后端接口
    $.ajax({
        url: ctx + "blog/api/search", // 拼接接口URL
        type: "POST",                  // 如果后端要求 POST 改这里
        data: {keyword: keyword},   // 传入参数
        dataType: "json",             // 告诉 jQuery 期望 JSON 格式返回
        success: function (response) {
            // console.log("后端返回数据:", response);

            // 判断状态码
            if (response.status !== 200) {
                $("#searchResults").html('<p style="color:red;">搜索失败: ' + response.msg + '</p>');
                return;
            }

            // 取出 data 数组
            var articles = response.data;

            if (!articles || articles.length === 0) {
                $("#searchResults").html('<p style="color:#999;">没有找到相关文章</p>');
                return;
            }

            // 渲染文章列表
            var html = '<ul style="list-style:none; padding-left:0;">';
            articles.forEach(function (item) {
                html += `
                            <li style="padding:8px 0; border-bottom:1px solid #eee;">
                                <a href="${ctx}${item.skipUrl}"
                                   style="color:#337ab7; text-decoration:none; font-size:16px;">
                                    ${item.title}
                                </a>
                            </li>`;
            });
            html += '</ul>';

            $("#searchResults").html(html);
        },
        error: function (xhr, status, error) {
            console.error("请求失败:", error);
            $("#searchResults").html('<p style="color:red;">搜索请求失败，请稍后再试</p>');
        }
    });

    return false; // 阻止表单默认提交
}

// ESC关闭
document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") {
        closeSearchModal();
    }
});