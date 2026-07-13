-- cms内容管理系统初始化表

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 系统表 --
-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission`
(
    `id`            char(36) CHARACTER SET utf8 COLLATE utf8_general_ci     NOT NULL,
    `parent_id`     char(36) CHARACTER SET utf8 COLLATE utf8_general_ci     NULL DEFAULT NULL,
    `permission_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '权限id',
    `name`          varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '权限名称',
    `description`   varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限描述',
    `url`           varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限访问路径',
    `perms`         varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限标识',
    `type`          int(1)                                                  NULL DEFAULT NULL COMMENT '类型   0：目录   1：菜单   2：按钮',
    `order_num`     int(3)                                                  NULL DEFAULT 0 COMMENT '排序',
    `icon`          varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '图标',
    `status`        int(1)                                                  NOT NULL COMMENT '状态：1有效；2删除',
    `create_time`   datetime                                                NULL DEFAULT NULL,
    `update_time`   datetime                                                NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '权限表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of permission
-- ----------------------------
INSERT INTO `permission`
VALUES ('01ec9876365b40618c5646d431d27040', 'fea8292b32214cb2a7300282298d35aa', '1000000804049447', '编辑', '编辑文章',
        '/article/edit', 'article:edit', 2, 0, '', 1, '2018-07-29 20:21:50', '2018-07-29 20:21:50');
INSERT INTO `permission`
VALUES ('0285ec4086614046b768a13cc85cfed2', 'fea8292b32214cb2a7300282298d35aa', '1000000265030829', '文章下载',
        '文章下载', '', 'article:download', 2, 0, '', 1, '2025-07-13 19:18:51', '2025-07-13 19:18:51');
INSERT INTO `permission`
VALUES ('032de1be33fe4cdf897b4ac80f387a75', '578cff30350a4a5aaa59ccb6493d310e', '1000001305005793', '新增', '友链-新增',
        '/link/add', 'link:add', 2, 0, '', 1, '2018-07-25 11:07:46', '2018-07-25 11:07:46');
INSERT INTO `permission`
VALUES ('0a42e90c6d1e44e2baaabe19d9b8a2db', '1841b4b12f3a48738f0eb3318541ffd9', '1000000919723998', '主题管理',
        '主题管理', '/themes', 'themes', 1, 2, 'fas fa-chess-queen', 1, '2018-09-19 15:43:50', '2018-09-19 15:43:50');
INSERT INTO `permission`
VALUES ('14763d0d5db24b68ae89e82636c6a5af', '7aa603d3538c456ab0a40a0ef5490dfc', '1000001530229707', '批量删除',
        '批量删除公告', '/notify/batch/delete', 'notify:batchDelete', 2, 0, '', 0, '2018-07-24 23:46:25',
        '2018-09-13 12:34:02');
INSERT INTO `permission`
VALUES ('1841b4b12f3a48738f0eb3318541ffd9', '0', '1000000863853891', '网站管理', '网站管理', NULL, NULL, 0, 3,
        'fas fa-columns', 1, '2018-07-24 15:44:23', '2020-04-19 19:08:46');
INSERT INTO `permission`
VALUES ('1b6dfe45fa8e48cc839f0939b734aac1', 'fea8292b32214cb2a7300282298d35aa', '1000001971628142', '下载文章',
        '下载文章', '', 'article:download', 2, 0, '', 1, '2025-07-13 18:38:06', '2025-07-13 18:38:06');
INSERT INTO `permission`
VALUES ('1caad1b06f954ef5b91423af8e294c52', '7857c80405654aecb247562b4be38d60', '5363468278683993', '批量删除',
        '批量删除服务器文件', '/serverFile/batch/delete', 'serverFile:batchDelete', 2, 0, '', 1, '2026-07-06 20:44:47',
        '2026-07-06 20:44:47');
INSERT INTO `permission`
VALUES ('1e71cb20b9ad4bd3a1f33a4846088a4e', '7857c80405654aecb247562b4be38d60', '2377565638880373', '查询',
        '查询服务器文件列表', '/serverFile/list', 'serverFile:list', 2, 0, '', 1, '2026-07-06 20:44:47',
        '2026-07-06 20:44:47');
INSERT INTO `permission`
VALUES ('1f97dc73931b43309ac168ddbf197020', '578cff30350a4a5aaa59ccb6493d310e', '1000001011730177', '删除', '友链-删除',
        '/link/delete', 'link:delete', 2, 0, '', 1, '2018-07-25 11:08:53', '2018-07-25 11:08:53');
INSERT INTO `permission`
VALUES ('22785a835e3841a9bfdac9c86be7ccf6', '3679f1959904486c8b84395a9e0fead9', '1000002127467055', '查询',
        '查询标签列表', '/tag/list', 'tag:list', 2, 0, '', 1, '2018-07-25 18:51:20', '2018-07-25 18:51:20');
INSERT INTO `permission`
VALUES ('2b710f56a4ec4471959386e78e40c7ce', 'd673556295b74677808648e8ca655062', '301', '数据监控', '数据监控',
        '/database/monitoring', 'database', 1, 1, 'fas fa-chess-queen', 1, '2018-07-06 15:19:55',
        '2018-07-06 15:19:55');
INSERT INTO `permission`
VALUES ('33244a8403a44de4ad4fe126d7f0c04a', '0', '2', '权限管理', '权限管理', NULL, NULL, 0, 4, 'fas fa-user-cog', 1,
        '2017-07-13 15:04:42', '2020-04-19 19:09:22');
INSERT INTO `permission`
VALUES ('3679f1959904486c8b84395a9e0fead9', '835f9751138e46b3a19e2709b91f3c94', '1000000976625379', '标签管理',
        '标签管理', '/tags', 'tags', 1, 4, 'fas fa-chess-queen', 1, '2018-07-25 18:50:47', '2020-04-19 20:33:35');
INSERT INTO `permission`
VALUES ('3876fa2dc84b4ffd85bee626a085d29a', '0a42e90c6d1e44e2baaabe19d9b8a2db', '1000002051091207', '编辑', '编辑主题',
        '/theme/edit', 'theme:edit', 2, 0, '', 1, '2018-09-19 15:54:34', '2018-09-19 15:54:34');
INSERT INTO `permission`
VALUES ('3ae5936f2cd04755b98f9d2a7b8d7560', '0a42e90c6d1e44e2baaabe19d9b8a2db', '1000000215201942', '新增', '新增主题',
        '/theme/add', 'theme:add', 2, 0, '', 1, '2018-09-19 15:45:34', '2018-09-19 15:45:34');
INSERT INTO `permission`
VALUES ('3ce4cce0bd1548c69bea24adaeee6648', '7aa603d3538c456ab0a40a0ef5490dfc', '1000001531648485', '编辑',
        '系统公告-编辑', '/notify/edit', 'notify:edit', 2, 0, '', 0, '2018-07-24 23:44:39', '2018-09-13 12:33:52');
INSERT INTO `permission`
VALUES ('3f36ace16c564810ba0b31d723c275e5', '0a42e90c6d1e44e2baaabe19d9b8a2db', '1000000784272506', '查询', '主题列表',
        '/theme/list', 'theme:list', 2, 0, '', 1, '2018-09-19 15:44:50', '2018-09-19 15:44:50');
INSERT INTO `permission`
VALUES ('47ef361f98c147cca0e362281ccfb192', '3679f1959904486c8b84395a9e0fead9', '1000001832967209', '编辑', '编辑标签',
        '/tag/edit', 'tag:edit', 2, 0, '', 1, '2018-07-25 18:52:17', '2018-07-25 18:52:17');
INSERT INTO `permission`
VALUES ('48b9f74897d54fa0900413e6c422534d', '0', '4907585561005516', '文件管理', '文件管理', NULL, NULL, 0, 8,
        'fas fa-folder-open', 1, '2026-07-06 20:44:47', '2026-07-06 20:44:47');
INSERT INTO `permission`
VALUES ('48d8366fd8bc4b0c8668fccefe691e18', '86ce6dde2fde44c29fb58ab159c253a8', '1000002075182223', '批量删除',
        '批量删除评论', '/comment/batch/delete', 'comment:batchDelete', 2, 0, '', 1, '2018-08-10 10:07:57',
        '2018-08-10 10:07:57');
INSERT INTO `permission`
VALUES ('4a780946321c4e448f17bbae24563880', 'af903c0ba66a44bf994a9da625f1f865', '20201', '列表查询', '角色列表查询',
        '/role/list', 'role:list', 2, 0, NULL, 1, '2017-10-10 15:31:36', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('4aa7712387414645ad5308131bb835fe', 'eed589d6a14a4cf7ae4a3146ad1c2efd', '1000000015836901', '新增', '新增分类',
        '/category/add', 'category:add', 2, 0, '', 1, '2018-07-25 17:44:28', '2018-07-25 17:44:28');
INSERT INTO `permission`
VALUES ('50676e72e3964ef6a8b11609b0111e2a', '3679f1959904486c8b84395a9e0fead9', '1000000759248744', '批量删除',
        '批量删除标签', '/tag/batch/delete', 'tag:batchDelete', 2, 0, '', 1, '2018-07-25 18:53:14',
        '2018-07-25 18:53:14');
INSERT INTO `permission`
VALUES ('52e521c311a3426396d58425af2639b2', '578cff30350a4a5aaa59ccb6493d310e', '1000001312374743', '批量删除',
        '友链-批量删除', '/link/batch/delete', 'link:batchDelete', 2, 0, '', 1, '2018-07-25 11:09:40',
        '2018-07-25 11:09:40');
INSERT INTO `permission`
VALUES ('56453efe2ccf4f978e976a571fab37ae', '7857c80405654aecb247562b4be38d60', '8910016920024060', '上传',
        '上传服务器文件', '/serverFile/upload', 'serverFile:upload', 2, 0, '', 1, '2026-07-06 20:44:47',
        '2026-07-06 20:44:47');
INSERT INTO `permission`
VALUES ('578cff30350a4a5aaa59ccb6493d310e', '1841b4b12f3a48738f0eb3318541ffd9', '1000000237721285', '友链管理',
        '友情链接', '/links', 'links', 1, 3, 'fas fa-chess-queen', 1, '2018-07-25 11:05:49', '2018-07-25 11:07:03');
INSERT INTO `permission`
VALUES ('57e38f7f6f08401fa362b1c9c10f68d6', '7857c80405654aecb247562b4be38d60', '7902260530491681', '下载',
        '下载服务器文件', '/serverFile/download', 'serverFile:download', 2, 0, '', 1, '2026-07-06 20:44:47',
        '2026-07-06 20:44:47');
INSERT INTO `permission`
VALUES ('64af47ca515b46d896f537eb429b0545', 'fea8292b32214cb2a7300282298d35aa', '1000000686545782', '查询', '查询文章',
        '/article/list', 'article:list', 2, 0, '', 1, '2018-07-29 20:20:54', '2018-07-29 20:20:54');
INSERT INTO `permission`
VALUES ('697af658eaaf4b7c833acf6deaa60e7d', 'ecdec8ec8cc84c3bba32fcad37a30455', '20102', '新增', '新增用户',
        '/user/add', 'user:add', 2, 0, NULL, 1, '2017-07-13 15:06:50', '2018-02-28 17:58:46');
INSERT INTO `permission`
VALUES ('6e59a51dfd5147a6ae811700654ffda5', 'd673556295b74677808648e8ca655062', '1000001566487351', '系统日志',
        '系统日志', '/log/page', 'log', 1, 2, 'fas fa-book', 1, '2025-03-20 02:25:55', '2025-03-22 00:46:36');
INSERT INTO `permission`
VALUES ('7629aa3093cb4bfe88928a98cd3e8d8a', '7aa603d3538c456ab0a40a0ef5490dfc', '1000001548165826', '删除',
        '系统公告-删除', '/notify/delete', 'notify:delete', 2, 0, '', 0, '2018-07-24 23:45:27', '2018-09-13 12:33:57');
INSERT INTO `permission`
VALUES ('76dc847d416e476c8d673f45134ef3ad', '7857c80405654aecb247562b4be38d60', '9542011192685452', '删除',
        '删除服务器文件', '/serverFile/delete', 'serverFile:delete', 2, 0, '', 1, '2026-07-06 20:44:47',
        '2026-07-06 20:44:47');
INSERT INTO `permission`
VALUES ('7857c80405654aecb247562b4be38d60', '48b9f74897d54fa0900413e6c422534d', '1138387567097145', '服务器文件',
        '服务器文件管理', '/serverFile', 'serverFile', 1, 1, 'fas fa-hdd', 1, '2026-07-06 20:44:47',
        '2026-07-06 20:44:47');
INSERT INTO `permission`
VALUES ('798fc9824e2a4c4d99273d9577e5b2fe', '801ced29a7a94ccdaa1a41d3362ae64e', '20301', '列表查询', '资源列表',
        '/permission/list', 'permission:list', 2, 0, NULL, 1, '2018-07-12 16:25:28', '2018-07-12 16:25:33');
INSERT INTO `permission`
VALUES ('7a1595ecb0884f1d9947d97ee7947146', 'ecdec8ec8cc84c3bba32fcad37a30455', '20104', '删除', '删除用户',
        '/user/delete', 'user:delete', 2, 0, NULL, 1, '2017-07-13 15:08:42', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('7aa603d3538c456ab0a40a0ef5490dfc', '1841b4b12f3a48738f0eb3318541ffd9', '1000001792841328', '系统公告',
        '系统公告', '/notifies', 'notifies', 1, 2, 'fas fa-chess-queen', 0, '2018-07-24 23:40:45',
        '2018-09-13 12:34:18');
INSERT INTO `permission`
VALUES ('7cef9dc19f81486ab82d4fc254207bd1', '0', '4', '系统管理', '系统管理', NULL, NULL, 0, 5, 'fas fa-cog', 1,
        '2018-07-06 15:20:38', '2020-04-19 19:08:58');
INSERT INTO `permission`
VALUES ('7d5b3a8b56084e6a8f032b5f172a453e', 'af903c0ba66a44bf994a9da625f1f865', '20206', '分配权限', '分配权限',
        '/role/assign/permission', 'role:assignPerms', 2, 0, NULL, 1, '2017-09-26 07:33:05', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('7d9b2f8510234af09170072336f6e6c1', '7cef9dc19f81486ab82d4fc254207bd1', '401', '在线用户', '在线用户',
        '/online/users', 'onlineUsers', 1, 1, 'fas fa-chess-queen', 1, '2018-07-06 15:21:00', '2018-07-24 14:58:22');
INSERT INTO `permission`
VALUES ('801ced29a7a94ccdaa1a41d3362ae64e', '33244a8403a44de4ad4fe126d7f0c04a', '203', '资源管理', '资源管理',
        '/permissions', 'permissions', 1, 3, 'fas fa-chess-queen', 1, '2017-09-26 07:33:51', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('835f9751138e46b3a19e2709b91f3c94', '0', '1000000602555213', '文章管理', '文章管理', NULL, NULL, 0, 2,
        'fas fa-newspaper', 1, '2018-07-25 17:43:12', '2020-04-19 19:04:49');
INSERT INTO `permission`
VALUES ('86ce6dde2fde44c29fb58ab159c253a8', '1841b4b12f3a48738f0eb3318541ffd9', '1000000224901858', '评论管理',
        '评论管理', '/comments', 'comments', 1, 4, 'fas fa-chess-queen', 1, '2018-08-10 09:44:41',
        '2018-09-19 15:44:13');
INSERT INTO `permission`
VALUES ('88251fe161ea468dbfba875a2a22be8b', '3679f1959904486c8b84395a9e0fead9', '1000001458372033', '新增', '新增标签',
        '/tag/add', 'tag:add', 2, 0, '', 1, '2018-07-25 18:51:42', '2018-07-25 18:51:42');
INSERT INTO `permission`
VALUES ('88373ad833054bdab2247e4b13fa0190', '86ce6dde2fde44c29fb58ab159c253a8', '1000000663968031', '审核', '审核评论',
        '/comment/audit', 'comment:audit', 2, 0, '', 1, '2018-08-10 09:57:11', '2018-08-10 09:57:11');
INSERT INTO `permission`
VALUES ('8afeed38e1d443439f96822950058285', 'ecdec8ec8cc84c3bba32fcad37a30455', '20101', '列表查询', '用户列表查询',
        '/user/list', 'user:list', 2, 0, NULL, 1, '2017-07-13 15:09:24', '2017-10-09 05:38:29');
INSERT INTO `permission`
VALUES ('8c27e0a621fb4355a32f858014481e5f', 'fea8292b32214cb2a7300282298d35aa', '1000000488864959', '删除', '删除文章',
        '/article/delete', 'article:delete', 2, 0, '', 1, '2018-07-29 20:23:27', '2018-07-29 20:23:27');
INSERT INTO `permission`
VALUES ('8f98dcf96e6c43d8b751c5884d02c914', 'fea8292b32214cb2a7300282298d35aa', '1000001642272578', '新增', '新增文章',
        '/article/add', 'article:add', 2, 0, '', 1, '2018-07-29 20:21:21', '2018-07-29 20:21:21');
INSERT INTO `permission`
VALUES ('9143bc44dbe34a758a37804e8c40fbcd', 'e255f37c51294bb8867e20553b601ba2', '1000000432183856', '保存',
        '基础设置-保存', '/siteinfo/save', 'siteinfo:save', 2, 0, '', 1, '2018-07-24 15:49:12', '2018-07-24 15:49:12');
INSERT INTO `permission`
VALUES ('93a32f253ce546439ab27d70d9886886', '801ced29a7a94ccdaa1a41d3362ae64e', '20303', '编辑', '编辑资源',
        '/permission/edit', 'permission:edit', 2, 0, NULL, 1, '2017-09-27 21:29:04', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('991071581ca04c3eaca3cd56e6ef8064', '835f9751138e46b3a19e2709b91f3c94', '1000000899091444', '发布文章',
        '写文章', '/article/add', 'article:add', 1, 1, 'fas fa-chess-queen', 1, '2018-07-29 20:39:49',
        '2020-04-19 19:16:06');
INSERT INTO `permission`
VALUES ('9a6e3be7f9ca474fbc570c35edf36c1b', '7d9b2f8510234af09170072336f6e6c1', '1000000171409776', '批量踢出',
        '批量踢出', '/online/user/batch/kickout', 'onlineUser:batchKickout', 2, 0, '', 1, '2018-07-24 15:04:09',
        '2018-07-24 15:04:09');
INSERT INTO `permission`
VALUES ('a05eb5181d1d4dafa8c9361cfab87eea', '7d9b2f8510234af09170072336f6e6c1', '1000001992372345', '在线用户查询',
        '在线用户查询', '/online/user/list', 'onlineUser:list', 2, 0, '', 1, '2018-07-24 15:02:23',
        '2018-07-24 15:02:23');
INSERT INTO `permission`
VALUES ('a1de9be294bb45b1832af96fb93c400c', '7d9b2f8510234af09170072336f6e6c1', '1000002083579304', '踢出用户',
        '踢出用户', '/online/user/kickout', 'onlineUser:kickout', 2, 0, '', 1, '2018-07-24 15:03:16',
        '2018-07-24 15:03:16');
INSERT INTO `permission`
VALUES ('a401320e9d8e4b3789686bbad8143136', '0', '1', '工作台', '工作台', '/workdest', 'workdest', 1, 1, 'fas fa-home',
        1, '2017-09-27 21:22:02', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('a7ac6cf0617f4bda9e012658a2ebc92c', '86ce6dde2fde44c29fb58ab159c253a8', '1000000322655547', '回复', '回复评论',
        '/comment/reply', 'comment:audit', 2, 0, '', 1, '2018-08-10 10:04:28', '2018-08-10 10:04:28');
INSERT INTO `permission`
VALUES ('a9166fc1a1e945638220750435da93b9', '0a42e90c6d1e44e2baaabe19d9b8a2db', '1000000431577803', '删除', '删除主题',
        '/theme/delete', 'theme:delete', 2, 0, '', 1, '2018-09-19 15:48:06', '2018-09-19 15:48:06');
INSERT INTO `permission`
VALUES ('aaf45edf22b24c288ee0f1fcda8cb9b0', '3679f1959904486c8b84395a9e0fead9', '1000000754923037', '删除', '删除标签',
        '/tag/delete', 'tag:delete', 2, 0, '', 1, '2018-07-25 18:52:40', '2018-07-25 18:52:40');
INSERT INTO `permission`
VALUES ('ab928dabf76744c294dfa5ba3557eb2e', 'ecdec8ec8cc84c3bba32fcad37a30455', '20103', '编辑', '编辑用户',
        '/user/edit', 'user:edit', 2, 0, NULL, 1, '2017-07-13 15:08:03', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('ae80c519f63a4fc0b485db5a05f53d05', 'eed589d6a14a4cf7ae4a3146ad1c2efd', '1000001439189167', '编辑', '编辑分类',
        '/category/edit', 'category:edit', 2, 0, '', 1, '2018-07-25 17:44:52', '2018-07-25 17:44:52');
INSERT INTO `permission`
VALUES ('af903c0ba66a44bf994a9da625f1f865', '33244a8403a44de4ad4fe126d7f0c04a', '202', '角色管理', '角色管理', '/roles',
        'roles', 1, 2, 'fas fa-chess-queen', 1, '2017-07-17 14:39:09', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('b3657d113a9b4ea5ba54a292d75228b6', '86ce6dde2fde44c29fb58ab159c253a8', '1000001419287014', '删除', '删除评论',
        '/comment/delete', 'comment:delete', 2, 0, '', 1, '2018-08-10 10:06:27', '2018-08-10 10:06:27');
INSERT INTO `permission`
VALUES ('bcc039d30dc94974be8fd8d34d6adbc0', 'af903c0ba66a44bf994a9da625f1f865', '20205', '批量删除', '批量删除角色',
        '/role/batch/delete', 'role:batchDelete', 2, 0, '', 1, '2018-07-10 22:20:43', '2018-07-10 22:20:43');
INSERT INTO `permission`
VALUES ('bd6d5b435ca94c97b4474bec7f6a30b9', 'eed589d6a14a4cf7ae4a3146ad1c2efd', '1000001647995753', '删除', '删除分类',
        '/category/delete', 'category:delete', 2, 0, '', 1, '2018-07-25 17:45:28', '2018-07-25 17:45:28');
INSERT INTO `permission`
VALUES ('c0bec8f28f7b4bd5a4d7e9d598efb663', '0a42e90c6d1e44e2baaabe19d9b8a2db', '1000001065007557', '启用', '启用主题',
        '/theme/use', 'theme:use', 2, 0, '', 1, '2018-09-19 15:46:28', '2018-09-19 15:46:28');
INSERT INTO `permission`
VALUES ('c7a8596d1a494d39b2bb248109e07ca1', '578cff30350a4a5aaa59ccb6493d310e', '1000001507480127', '审核', '友链-审核',
        '/link/audit', 'link:audit', 2, 0, '', 1, '2018-07-25 11:42:28', '2018-07-25 11:42:28');
INSERT INTO `permission`
VALUES ('d09b5cf32d874546b9143cceaf2c58d4', '578cff30350a4a5aaa59ccb6493d310e', '1000001679037501', '编辑', '友链-编辑',
        '/link/edit', 'link:edit', 2, 0, '', 1, '2018-07-25 11:08:21', '2018-07-25 11:08:21');
INSERT INTO `permission`
VALUES ('d0bc49591d6b441686c48c32d7881be9', 'fea8292b32214cb2a7300282298d35aa', '1000000512435306', '批量删除',
        '批量删除文章', '/article/batch/delete', 'article:batchDelete', 2, 0, '', 1, '2018-07-29 20:23:49',
        '2018-07-29 20:23:49');
INSERT INTO `permission`
VALUES ('d3aba30e3d6b4524b7a4cc489ff87322', '801ced29a7a94ccdaa1a41d3362ae64e', '20302', '新增', '新增资源',
        '/permission/add', 'permission:add', 2, 0, NULL, 1, '2017-09-26 08:06:58', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('d5db7859e08744e79a51a0940b283332', '0a42e90c6d1e44e2baaabe19d9b8a2db', '1000000207002458', '批量删除',
        '批量删除主题', 'theme/batch/delete', 'theme:batchDelete', 2, 0, '', 1, '2018-09-19 15:48:39',
        '2018-09-19 15:48:39');
INSERT INTO `permission`
VALUES ('d673556295b74677808648e8ca655062', '0', '3', '运维管理', '运维管理', NULL, NULL, 0, 7, 'fas fa-people-carry',
        1, '2018-07-06 15:19:26', '2020-04-19 19:09:59');
INSERT INTO `permission`
VALUES ('d76513ed92974f27aa1d6caa14e17106', '86ce6dde2fde44c29fb58ab159c253a8', '1000001579533936', '查询', '查询',
        '/comment/list', 'comment:list', 2, 0, '', 1, '2018-08-10 09:46:54', '2018-08-10 09:46:54');
INSERT INTO `permission`
VALUES ('d8b24e57cf2c4d379f3111ed6cbeb380', '7aa603d3538c456ab0a40a0ef5490dfc', '1000000791685519', '新增',
        '系统公告-新增', '/notify/add', 'notify:add', 2, 0, '', 0, '2018-07-24 23:42:20', '2018-09-13 12:33:26');
INSERT INTO `permission`
VALUES ('dbeb1b365edc4604a619da3d857ecf02', 'af903c0ba66a44bf994a9da625f1f865', '20203', '编辑', '编辑角色',
        '/role/edit', 'role:edit', 2, 0, NULL, 1, '2017-07-17 14:40:15', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('de10dfe31d8a4022ac7c57156af45e27', 'ecdec8ec8cc84c3bba32fcad37a30455', '20106', '分配角色', '分配角色',
        '/user/assign/role', 'user:assignRole', 2, 0, NULL, 1, '2017-07-13 15:09:24', '2017-10-09 05:38:29');
INSERT INTO `permission`
VALUES ('dfed8ce6bcf14ca09cc54c413cf4b7cf', 'eed589d6a14a4cf7ae4a3146ad1c2efd', '1000000841419865', '查询', '分类查询',
        '/category/list', 'category:list', 2, 0, '', 1, '2018-07-25 17:49:43', '2018-07-25 17:49:43');
INSERT INTO `permission`
VALUES ('e255f37c51294bb8867e20553b601ba2', '1841b4b12f3a48738f0eb3318541ffd9', '1000001264798222', '基础信息',
        '基础设置', '/siteinfo', 'siteinfo', 1, 1, 'fas fa-chess-queen', 1, '2018-07-24 15:48:13',
        '2018-07-24 17:43:39');
INSERT INTO `permission`
VALUES ('e409dd5dfd434a278ef6ff1c2dfe6ffd', 'af903c0ba66a44bf994a9da625f1f865', '20204', '删除', '删除角色',
        '/role/delete', 'role:delete', 2, 0, NULL, 1, '2017-07-17 14:40:57', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('e9c21f6d7972430f96d0287090cee852', '7857c80405654aecb247562b4be38d60', '2102804487773613', '编辑',
        '在线编辑服务器文件', '/serverFile/save', 'serverFile:edit', 2, 0, '', 1, '2026-07-06 20:44:47',
        '2026-07-06 20:44:47');
INSERT INTO `permission`
VALUES ('ecdec8ec8cc84c3bba32fcad37a30455', '33244a8403a44de4ad4fe126d7f0c04a', '201', '用户管理', '用户管理', '/users',
        'users', 1, 1, 'fas fa-chess-queen', 1, '2017-07-13 15:05:47', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('eed589d6a14a4cf7ae4a3146ad1c2efd', '835f9751138e46b3a19e2709b91f3c94', '1000001729104792', '分类管理',
        '分类管理', '/categories', 'categories', 1, 3, 'fas fa-chess-queen', 1, '2018-07-25 17:43:50',
        '2020-04-19 20:33:27');
INSERT INTO `permission`
VALUES ('f5115534473349e1b6027fdbf81d645b', 'af903c0ba66a44bf994a9da625f1f865', '20202', '新增', '新增角色',
        '/role/add', 'role:add', 2, 0, NULL, 1, '2017-07-17 14:39:46', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('f8fe1f067400450cba0e5373e433a570', '801ced29a7a94ccdaa1a41d3362ae64e', '20304', '删除', '删除资源',
        '/permission/delete', 'permission:delete', 2, 0, NULL, 1, '2017-09-27 21:29:50', '2018-02-27 10:53:14');
INSERT INTO `permission`
VALUES ('fc11e9c9fd8f4b229e79ba7610702d38', '578cff30350a4a5aaa59ccb6493d310e', '1000001238193773', '查询', '友链-查询',
        '/link/list', 'link:list', 2, 0, '', 1, '2018-07-25 11:06:44', '2018-07-25 11:06:44');
INSERT INTO `permission`
VALUES ('fcf7daf17f764efca4c7a5134a5ed6fb', '7aa603d3538c456ab0a40a0ef5490dfc', '1000001351219537', '查询',
        '系统公告-查询', '/notify/list', 'notify:list', 2, 0, '', 0, '2018-07-24 23:41:30', '2018-09-13 12:33:19');
INSERT INTO `permission`
VALUES ('fd527c024c844b32b65f9ada7f4545d2', 'ecdec8ec8cc84c3bba32fcad37a30455', '20105', '批量删除', '批量删除用户',
        '/user/batch/delete', 'user:batchDelete', 2, 0, '', 1, '2018-07-11 01:53:09', '2018-07-11 01:53:09');
INSERT INTO `permission`
VALUES ('fea8292b32214cb2a7300282298d35aa', '835f9751138e46b3a19e2709b91f3c94', '1000001038456544', '文章列表',
        '文章列表', '/articles', 'articles', 1, 2, 'fas fa-chess-queen', 1, '2018-07-29 20:20:23',
        '2020-04-19 19:23:06');
INSERT INTO `permission`
VALUES ('ffe2b79265a04759b5306fd8447386b5', 'fea8292b32214cb2a7300282298d35aa', '5011629010561508', '批量推送',
        '批量推送百度', '/article/batch/push', 'article:batchPush', 2, 0, '', 1, '2018-10-28 15:15:00',
        '2018-10-28 15:15:00');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`
(
    `id`          varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键id（UUID）',
    `role_id`     varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '角色id',
    `name`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '角色名称',
    `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色描述',
    `status`      int(1)                                                  NOT NULL COMMENT '状态：1有效；2删除',
    `create_time` datetime                                                NULL DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime                                                NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '角色表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role`
VALUES ('3f6d7b04b7094d1da47dbaf0d8497464', '4', '数据库管理员', '数据库管理员', 1, '2017-07-12 11:50:22',
        '2017-10-09 17:38:02');
INSERT INTO `role`
VALUES ('746476300b7a4c0d882d931a7d087968', '1', '超级管理员', '超级管理员', 1, '2017-06-28 20:30:05',
        '2017-06-28 20:30:10');
INSERT INTO `role`
VALUES ('adaf1fca836a4ce098affef95198a050', '3', '普通用户', '普通用户', 1, '2017-06-30 23:35:44',
        '2018-07-13 11:44:06');
INSERT INTO `role`
VALUES ('cf97cbeae410476fb37b52ce0ec59b5d', '2', '管理员', '管理员', 1, '2017-06-30 23:35:19', '2017-10-11 09:32:33');

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`
(
    `id`            varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id（UUID）',
    `role_id`       varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '角色id',
    `permission_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '权限id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '角色权限表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_permission
-- ----------------------------
INSERT INTO `role_permission`
VALUES ('00bd1f1b909142bca66b44b39fc23db4', '1', '1000000863853891');
INSERT INTO `role_permission`
VALUES ('01a241a5edbc4c109120e94fbd6afd69', '1', '1000000015836901');
INSERT INTO `role_permission`
VALUES ('02e223db26eb495da41a37f83bb024b2', '1', '1000000224901858');
INSERT INTO `role_permission`
VALUES ('07699e6430314f91acf30e2d888512fb', '1', '20301');
INSERT INTO `role_permission`
VALUES ('0b081aa546644e74b1a71434cfe8b080', '1', '1000000512435306');
INSERT INTO `role_permission`
VALUES ('0ba8deeb68a0427e835b82dd7890e891', '1', '1');
INSERT INTO `role_permission`
VALUES ('1142f91c56ef4fa6aaf490e43b5b97d5', '1', '1000000171409776');
INSERT INTO `role_permission`
VALUES ('1220953eb84f4ced9b7735ff2ebf2889', '1', '1000000215201942');
INSERT INTO `role_permission`
VALUES ('185ce1724f1a43b19315d81c44d31eec', '1', '1000001971628142');
INSERT INTO `role_permission`
VALUES ('1baaa600cd4b483690fb4a5981176f37', '1', '1000002015173332');
INSERT INTO `role_permission`
VALUES ('1e13f2fb43774807a6c1b2441fd2be52', '1', '1000000237721285');
INSERT INTO `role_permission`
VALUES ('24e4683fa93a4ea7a8a5dd8bf318c58a', '1', '1000000432183856');
INSERT INTO `role_permission`
VALUES ('2c498504e8b74c6486e14c3219fe3aca', '1', '1000001729104792');
INSERT INTO `role_permission`
VALUES ('33657c94a3eb4107b2cd09d1925d3d7f', '1', '1000000322655547');
INSERT INTO `role_permission`
VALUES ('483956a6063b42209c75338704cbb52d', '1', '1000000663968031');
INSERT INTO `role_permission`
VALUES ('4c8a5addadd3444fab2d140be85b010c', '1', '20101');
INSERT INTO `role_permission`
VALUES ('4ff5cd9037794d458dc8ad8bc915e940', '1', '1000001439189167');
INSERT INTO `role_permission`
VALUES ('5a74f54f8dc44fd9b097646ebd73cfcd', '1', '20104');
INSERT INTO `role_permission`
VALUES ('5c552b36ea0546eabf2c5b101061d31f', '1', '20102');
INSERT INTO `role_permission`
VALUES ('5f8fbe3e93644fa89191bfbcc5e4b77d', '1', '1000001305005793');
INSERT INTO `role_permission`
VALUES ('60628975063048d0909ee49910186382', '1', '1000001679037501');
INSERT INTO `role_permission`
VALUES ('610297cd3c574e01ae5b0d617a09899d', '1', '1000000265030829');
INSERT INTO `role_permission`
VALUES ('61fc764685c947bfa57ebadfddcc6baf', '1', '301');
INSERT INTO `role_permission`
VALUES ('64c6b72ecc7945b38893d4f788637909', '1', '1000000841419865');
INSERT INTO `role_permission`
VALUES ('6873be9d552b4198a6fe42ee847acace', '1', '1000000602555213');
INSERT INTO `role_permission`
VALUES ('6e14797ecfa048f09607284342b37983', '1', '20202');
INSERT INTO `role_permission`
VALUES ('6f5eff8a390f4399a1f610f0840a2dbe', '1', '7902260530491681');
INSERT INTO `role_permission`
VALUES ('729cb6be1461425aa30fb11dd80173d3', '1', '1000002083579304');
INSERT INTO `role_permission`
VALUES ('73171b67aee24603b2622d63cc9d23c0', '1', '20105');
INSERT INTO `role_permission`
VALUES ('734c3fd3909346d5b961394258dbb6f0', '1', '1000001566487351');
INSERT INTO `role_permission`
VALUES ('743514eaa4694bdc9cc71f112e1f1543', '1', '203');
INSERT INTO `role_permission`
VALUES ('76ca015a75f543a5a9348f6efa477bb0', '1', '1000000919723998');
INSERT INTO `role_permission`
VALUES ('777675905fca4c6183907f6fb6bc4b30', '1', '1000000899091444');
INSERT INTO `role_permission`
VALUES ('783968f8c95f49a0b6b4c55cc32a817f', '1', '20201');
INSERT INTO `role_permission`
VALUES ('79d3d8a97f344a9597179f0022ed072d', '1', '1000000431577803');
INSERT INTO `role_permission`
VALUES ('7a2af1ec3f2742cf88acf0269a23d792', '1', '20106');
INSERT INTO `role_permission`
VALUES ('7bbb3d001bdf4912b51abc3d1e1471dc', '1', '20205');
INSERT INTO `role_permission`
VALUES ('7d21e7043cbc4e2b8c794909160b433a', '1', '1000001419287014');
INSERT INTO `role_permission`
VALUES ('7d97b4fb92a94daeb9da455a9ff95573', '1', '1000001038456544');
INSERT INTO `role_permission`
VALUES ('8201e30506b541e8bbafe029383f503d', '1', '1000001642272578');
INSERT INTO `role_permission`
VALUES ('82a8e317f16f4180aa11b8cc19e5b375', '1', '1000000759248744');
INSERT INTO `role_permission`
VALUES ('854d4dc421ba46bd926231cdeea56e2a', '1', '4');
INSERT INTO `role_permission`
VALUES ('88bedb1ba5d14341a5635a6fcf7fe956', '1', '2377565638880373');
INSERT INTO `role_permission`
VALUES ('890183cf0dbf436ea2bfdcf468b70d4d', '1', '2102804487773613');
INSERT INTO `role_permission`
VALUES ('8aa67b5ffa0b408c83e8e94a98f8f838', '1', '5011629010561508');
INSERT INTO `role_permission`
VALUES ('8cae4456d620434a989baee859ca3246', '1', '1000001238193773');
INSERT INTO `role_permission`
VALUES ('96553e4176b94a0295540d99345339e8', '1', '1000002051091207');
INSERT INTO `role_permission`
VALUES ('9a0b1cdb10eb48568c0503e871ad7473', '1', '1000000686545782');
INSERT INTO `role_permission`
VALUES ('9c57c36d953748c09b8b1894d62a5e12', '1', '1000000976625379');
INSERT INTO `role_permission`
VALUES ('9c91dfdea77640a4bb2bf0c55ca3d7b3', '1', '1000001507480127');
INSERT INTO `role_permission`
VALUES ('9f200c246a6b4f51a98a1aa92997c19b', '1', '1000001011730177');
INSERT INTO `role_permission`
VALUES ('a04f0667a055482a88fd181ef3381447', '1', '8910016920024060');
INSERT INTO `role_permission`
VALUES ('a815b4e7737b478caccde34ac1302d76', '1', '1138387567097145');
INSERT INTO `role_permission`
VALUES ('a82ddd88bf9642b0bece8c2475383d98', '1', '1000001264798222');
INSERT INTO `role_permission`
VALUES ('a9c04a059be74388bc734a77b1e99c9a', '1', '20304');
INSERT INTO `role_permission`
VALUES ('a9dabedf4fea4ab7b9d1b808df74931c', '1', '1000002075182223');
INSERT INTO `role_permission`
VALUES ('aca5fbf1115847db81ada588c78e7a99', '1', '1000000207002458');
INSERT INTO `role_permission`
VALUES ('ade316a1354d4239b9be2f335711bcb2', '1', '20204');
INSERT INTO `role_permission`
VALUES ('ae529b34a14c482d906cb5c5b7f3d9ac', '1', '20302');
INSERT INTO `role_permission`
VALUES ('b4050d253a77408da2088e9cdfe2a2db', '1', '1000000488864959');
INSERT INTO `role_permission`
VALUES ('b95590a11af641a488c4d4931068fea8', '1', '4907585561005516');
INSERT INTO `role_permission`
VALUES ('ba4748574f684b769fad3c989b91fd36', '1', '1000002127467055');
INSERT INTO `role_permission`
VALUES ('bbdcec9380bd4a78b5f88238cbc4c173', '1', '201');
INSERT INTO `role_permission`
VALUES ('c87a16aa1bd24d0fb71d4ebbbd4d24ae', '1', '5363468278683993');
INSERT INTO `role_permission`
VALUES ('c9e0404ae5744a21a24b63502254c5da', '1', '1000001832967209');
INSERT INTO `role_permission`
VALUES ('cefacf33c98546e8a5c49e542b90e1cd', '1', '1000001312374743');
INSERT INTO `role_permission`
VALUES ('d0bba2b138d24d4886c9128115320174', '1', '20203');
INSERT INTO `role_permission`
VALUES ('d0bdc81d0e49414c825f31202507cab8', '1', '1000001647995753');
INSERT INTO `role_permission`
VALUES ('d0cb87a8060448bdab0a859cffc0abc4', '1', '1000000754923037');
INSERT INTO `role_permission`
VALUES ('d20095e48c16497d91683eedce379cc1', '1', '2');
INSERT INTO `role_permission`
VALUES ('d7c3adeca0de4f44aaf0c9ab65e46b1f', '1', '1000000784272506');
INSERT INTO `role_permission`
VALUES ('dc069041e31f4a948b8dc3ccc6d4c00b', '1', '20103');
INSERT INTO `role_permission`
VALUES ('ddfcefd2993742758405cacaf2952cb5', '1', '1000001065007557');
INSERT INTO `role_permission`
VALUES ('de82370dee8f4d5498fa8bd31d9c8e14', '1', '9542011192685452');
INSERT INTO `role_permission`
VALUES ('debe13489302422a8fe36b6f20c6e1b4', '1', '1000001458372033');
INSERT INTO `role_permission`
VALUES ('e12fcc479b8449a790cea6a3263cc439', '1', '1000000804049447');
INSERT INTO `role_permission`
VALUES ('e4516b9d6d1f4a2dad17109b0385f4d3', '1', '1000001537148226');
INSERT INTO `role_permission`
VALUES ('f4cf5ab8c73d4edea3408b238d2590d5', '1', '202');
INSERT INTO `role_permission`
VALUES ('f5814505aa4e47089849d597f1878bce', '1', '1000001579533936');
INSERT INTO `role_permission`
VALUES ('f5e000fdd12f4aa28fd302826b1631b2', '1', '401');
INSERT INTO `role_permission`
VALUES ('f9650156342149c0aa158e24858c8aad', '1', '1000001992372345');
INSERT INTO `role_permission`
VALUES ('fafc5a2888c74cab9445a22372b0d21d', '1', '3');
INSERT INTO `role_permission`
VALUES ('fb36fb2faa8c43d7b6c1825b14aacf59', '1', '20303');
INSERT INTO `role_permission`
VALUES ('fd73bdfc65ca4531bf11fd4234ca4797', '1', '20206');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`
(
    `id`        bigint(20)                                                     NOT NULL AUTO_INCREMENT,
    `sys_key`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL DEFAULT NULL COMMENT 'key',
    `sys_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'value',
    `status`    tinyint(4)                                                     NULL DEFAULT 1 COMMENT '状态   0：隐藏   1：显示',
    `remark`    varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `key` (`sys_key`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 11
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '系统配置信息表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config`
VALUES (2, 'SITE_NAME', '技术栈', 1, '网站名称');
INSERT INTO `sys_config`
VALUES (3, 'SITE_KWD', 'Java', 1, '网站关键字');
INSERT INTO `sys_config`
VALUES (4, 'SITE_DESC', '个人网站', 1, '网站描述');
INSERT INTO `sys_config`
VALUES (5, 'SITE_LOGO', '', 1, '站点logo');
INSERT INTO `sys_config`
VALUES (6, 'SITE_PERSON_PIC', '', 1, '站长头像');
INSERT INTO `sys_config`
VALUES (7, 'SITE_PERSON_NAME', '👑特昂糖', 1, '站长名称');
INSERT INTO `sys_config`
VALUES (8, 'SITE_PERSON_DESC', '有 bug？没关系，debug 是艺术。选择困难？不，我选择 if else。', 1, '站长描述');
INSERT INTO `sys_config`
VALUES (9, 'BAIDU_PUSH_URL', 'test', 1, '百度推送地址');
INSERT INTO `sys_config`
VALUES (10, 'SITE_STATIC', 'off', 1, '是否静态网站');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `user_id`         varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '用户id',
    `username`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '用户名',
    `password`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `salt`            varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '加密盐值',
    `nickname`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '昵称',
    `email`           varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '邮箱',
    `phone`           varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '联系方式',
    `sex`             int(255)                                                NULL DEFAULT NULL COMMENT '年龄：1男2女',
    `age`             int(3)                                                  NULL DEFAULT NULL COMMENT '年龄',
    `img`             varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像地址',
    `status`          int(1)                                                  NOT NULL COMMENT '用户状态：1有效; 2删除',
    `create_time`     datetime                                                NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`     datetime                                                NULL DEFAULT NULL COMMENT '更新时间',
    `last_login_time` datetime                                                NULL DEFAULT NULL COMMENT '最后登录时间',
    UNIQUE INDEX `user_user_id_uindex` (`user_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user`
VALUES ('1', 'admin', '5a6aab8b14da6a972b8a132f51ecc863', '4f1e6f284a64d13e6c773247a69410ca', '站长',
        '1742354728@qq.com', '15680457556', 1, 24,
        'https://tse1-mm.cn.bing.net/th/id/OIP.Ups1Z8igjNjLuDfO38XhTgHaHa?pid=Api&rs=1', 1, '2018-05-23 21:22:06',
        '2025-03-17 22:49:02', '2026-07-06 20:05:54');

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`
(
    `id`      varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id（UUID）',
    `user_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户id',
    `role_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '角色id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户角色表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_role
-- ----------------------------
INSERT INTO `user_role`
VALUES ('6aa0a1992e854d37ab9e684dea412749', '1', '1');

-- 其他表 --
-- ----------------------------
-- Table structure for app_desktop_data
-- ----------------------------
DROP TABLE IF EXISTS `app_desktop_data`;
CREATE TABLE `app_desktop_data`
(
    `id`          int(10) UNSIGNED                                              NOT NULL AUTO_INCREMENT,
    `title`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci       NULL DEFAULT NULL COMMENT '应用名称',
    `url`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用地址',
    `icon`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用图标',
    `create_time` datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 7
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_article
-- ----------------------------
DROP TABLE IF EXISTS `biz_article`;
CREATE TABLE `biz_article`
(
    `id`          int(11) UNSIGNED                                          NOT NULL AUTO_INCREMENT,
    `title`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '文章标题',
    `user_id`     varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci    NULL DEFAULT NULL COMMENT '用户ID',
    `author`      varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci    NULL DEFAULT NULL COMMENT '作者',
    `cover_image` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '文章封面图片',
    `qrcode_path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '文章专属二维码地址',
    `is_markdown` tinyint(1) UNSIGNED                                       NULL DEFAULT 1,
    `content`     longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '文章内容',
    `content_md`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'markdown版的文章内容',
    `top`         tinyint(1) UNSIGNED                                       NULL DEFAULT 0 COMMENT '是否置顶',
    `category_id` int(11) UNSIGNED                                          NULL DEFAULT NULL COMMENT '类型',
    `status`      tinyint(1) UNSIGNED                                       NULL DEFAULT NULL COMMENT '状态',
    `recommended` tinyint(1) UNSIGNED                                       NULL DEFAULT 0 COMMENT '是否推荐',
    `slider`      tinyint(1) UNSIGNED                                       NULL DEFAULT 0 COMMENT '是否轮播',
    `slider_img`  varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '轮播图地址',
    `original`    tinyint(1) UNSIGNED                                       NULL DEFAULT 1 COMMENT '是否原创',
    `description` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '文章简介，最多200字',
    `keywords`    varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '文章关键字，优化搜索',
    `comment`     tinyint(1) UNSIGNED                                       NULL DEFAULT 1 COMMENT '是否开启评论',
    `create_time` datetime                                                  NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` datetime                                                  NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 62
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '文章表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_article_look
-- ----------------------------
DROP TABLE IF EXISTS `biz_article_look`;
CREATE TABLE `biz_article_look`
(
    `id`          int(20) UNSIGNED                                       NOT NULL AUTO_INCREMENT,
    `article_id`  int(20) UNSIGNED                                       NOT NULL COMMENT '文章ID',
    `user_id`     varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '已登录用户ID',
    `user_ip`     varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户IP',
    `look_time`   datetime                                               NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    `create_time` datetime                                               NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` datetime                                               NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 912
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '文章浏览表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_article_tags
-- ----------------------------
DROP TABLE IF EXISTS `biz_article_tags`;
CREATE TABLE `biz_article_tags`
(
    `id`          int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
    `tag_id`      int(11) UNSIGNED NOT NULL COMMENT '标签表主键',
    `article_id`  int(11) UNSIGNED NOT NULL COMMENT '文章ID',
    `create_time` datetime         NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` datetime         NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 221
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '文章标签关联表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_category
-- ----------------------------
DROP TABLE IF EXISTS `biz_category`;
CREATE TABLE `biz_category`
(
    `id`          int(11) UNSIGNED                                        NOT NULL AUTO_INCREMENT,
    `pid`         int(11) UNSIGNED                                        NULL DEFAULT NULL,
    `name`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '文章类型名',
    `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型介绍',
    `sort`        int(10)                                                 NULL DEFAULT NULL COMMENT '排序',
    `icon`        varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图标',
    `status`      tinyint(1) UNSIGNED                                     NULL DEFAULT 1 COMMENT '是否可用',
    `create_time` datetime                                                NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` datetime                                                NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 9
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '文章类型表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_comment
-- ----------------------------
DROP TABLE IF EXISTS `biz_comment`;
CREATE TABLE `biz_comment`
(
    `id`                 int(20) UNSIGNED                                         NOT NULL AUTO_INCREMENT,
    `sid`                int(20)                                                  NULL DEFAULT NULL COMMENT '被评论的文章或者页面的ID(-1:留言板)',
    `user_id`            varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '评论人的ID',
    `pid`                int(20) UNSIGNED                                         NULL DEFAULT NULL COMMENT '父级评论的id',
    `qq`                 varchar(13) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '评论人的QQ（未登录用户）',
    `nickname`           varchar(13) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '评论人的昵称（未登录用户）',
    `avatar`             varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '评论人的头像地址',
    `email`              varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '评论人的邮箱地址（未登录用户）',
    `url`                varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '评论人的网站地址（未登录用户）',
    `status`             tinyint(1)                                               NULL DEFAULT 0 COMMENT '评论的状态',
    `ip`                 varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '评论时的ip',
    `lng`                varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '经度',
    `lat`                varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '纬度',
    `address`            varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '评论时的地址',
    `os`                 varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '评论时的系统类型',
    `os_short_name`      varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '评论时的系统的简称',
    `browser`            varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '评论时的浏览器类型',
    `browser_short_name` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '评论时的浏览器的简称',
    `content`            varchar(2000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '评论的内容',
    `remark`             varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '备注（审核不通过时添加）',
    `support`            int(10) UNSIGNED                                         NULL DEFAULT 0 COMMENT '支持（赞）',
    `oppose`             int(10) UNSIGNED                                         NULL DEFAULT 0 COMMENT '反对（踩）',
    `create_time`        datetime                                                 NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time`        datetime                                                 NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 5
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '评论表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_link
-- ----------------------------
DROP TABLE IF EXISTS `biz_link`;
CREATE TABLE `biz_link`
(
    `id`          int(11) UNSIGNED                                        NOT NULL AUTO_INCREMENT,
    `name`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '链接名',
    `url`         varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '链接地址',
    `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '链接介绍',
    `img`         varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '友链图片地址',
    `email`       varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '友链站长邮箱',
    `qq`          varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '友链站长qq',
    `status`      int(1) UNSIGNED                                         NULL DEFAULT NULL COMMENT '状态',
    `origin`      int(1)                                                  NULL DEFAULT NULL COMMENT '1-管理员添加 2-自助申请',
    `remark`      varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
    `create_time` datetime                                                NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` datetime                                                NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '友链表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_love
-- ----------------------------
DROP TABLE IF EXISTS `biz_love`;
CREATE TABLE `biz_love`
(
    `id`          int(11) UNSIGNED                                       NOT NULL AUTO_INCREMENT,
    `biz_id`      int(11) UNSIGNED                                       NOT NULL COMMENT '业务ID',
    `biz_type`    tinyint(1)                                             NULL DEFAULT NULL COMMENT '业务类型：1.文章，2.评论',
    `user_id`     varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '已登录用户ID',
    `user_ip`     varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户IP',
    `status`      tinyint(1)                                             NULL DEFAULT NULL COMMENT '状态',
    `create_time` datetime                                               NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` datetime                                               NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 59
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '点赞表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_tags
-- ----------------------------
DROP TABLE IF EXISTS `biz_tags`;
CREATE TABLE `biz_tags`
(
    `id`          int(11) UNSIGNED                                        NOT NULL AUTO_INCREMENT,
    `name`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '书签名',
    `description` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
    `create_time` datetime                                                NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` datetime                                                NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 9
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '标签表'
  ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for biz_theme
-- ----------------------------
DROP TABLE IF EXISTS `biz_theme`;
CREATE TABLE `biz_theme`
(
    `id`          int(11)                                                 NOT NULL AUTO_INCREMENT COMMENT 'id主键',
    `name`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '主题名（路径前缀）',
    `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主题描述',
    `img`         varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主题预览图url',
    `status`      tinyint(1)                                              NULL DEFAULT NULL COMMENT '0-未启用 1-启用',
    `create_time` datetime                                                NULL DEFAULT NULL,
    `update_time` datetime                                                NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '主题表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_server_file
-- 服务器文件管理：admin 模块自己的业务表，只记录文件名/备注/是否可编辑等元数据，
-- 文件内容本身通过 FileSystemService 托管在 file 模块（file_asset 表），
-- file_id 是逻辑外键，指向 file_asset.file_id，不建真实外键约束（与项目其它表一致）
-- ----------------------------
DROP TABLE IF EXISTS `biz_server_file`;
CREATE TABLE `biz_server_file`
(
    `id`             int(11)      NOT NULL AUTO_INCREMENT COMMENT 'id主键',
    `file_id`        varchar(64)  NOT NULL COMMENT '关联 file 模块的 file_id',
    `original_name`  varchar(255) NOT NULL COMMENT '文件名（含扩展名，可重命名）',
    `extension`      varchar(32)           DEFAULT NULL COMMENT '扩展名（小写，不含点）',
    `content_type`   varchar(128)          DEFAULT NULL COMMENT '文件 MIME 类型',
    `size`           bigint(20)            DEFAULT NULL COMMENT '文件大小（字节）',
    `editable`       tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否可在线预览/编辑的文本类型：1是 0否',
    `uploader`       varchar(50)           DEFAULT NULL COMMENT '上传人昵称（冗余展示）',
    `upload_user_id` varchar(32)           DEFAULT NULL COMMENT '上传人用户id',
    `remark`         varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`    datetime              DEFAULT NULL,
    `update_time`    datetime              DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '服务器文件管理表（业务元数据，文件内容托管在 file 模块）'
  ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
