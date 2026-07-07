mysql配置：
docker pull mysql:5.7
docker run -d --name mysql-5.7 \
  -p 3306:3306 \
  -v /app/cms/mysql-5.7/data:/var/lib/mysql \
  -v /app/cms/mysql-5.7/logs:/var/log/mysql \
  -v /app/cms/mysql-5.7/conf:/etc/mysql/conf.d \
  -e MYSQL_ROOT_PASSWORD=密码 \
  mysql:5.7

mysql配置文件:
```config
[client]
# 端口号
port=3306
 
[mysql]
no-beep
default-character-set=utf8mb4
 
[mysqld]
# 端口号
port=3306
bind-address = 0.0.0.0
# 数据目录
datadir=/var/lib/mysql
# 新模式或表时将使用的默认字符集
character-set-server=utf8mb4
# 默认存储引擎
default-storage-engine=INNODB
# 将 SQL 模式设置为严格
sql-mode="STRICT_TRANS_TABLES,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION"
#  最大连接数
max_connections=1024
# 表缓存
table_open_cache=2000
# 表内存
tmp_table_size=16M
# 线程缓存
thread_cache_size=10
# 设置大小写不敏感
lower_case_table_names=1
 
# myisam设置
myisam_max_sort_file_size=10G
myisam_sort_buffer_size=8M
key_buffer_size=8M
read_buffer_size=0
read_rnd_buffer_size=0
 
# innodb设置
innodb_flush_log_at_trx_commit=1
innodb_log_buffer_size=1M
innodb_buffer_pool_size=8M
innodb_log_file_size=48M
innodb_thread_concurrency=33
innodb_autoextend_increment=64
innodb_buffer_pool_instances=8
innodb_concurrency_tickets=5000
innodb_old_blocks_time=1000
innodb_open_files=300
innodb_stats_on_metadata=0
innodb_file_per_table=1
innodb_checksum_algorithm=0
 
# 其他设置
back_log=80
flush_time=0
join_buffer_size=256K
max_allowed_packet=4M
max_connect_errors=100
open_files_limit=4161
sort_buffer_size=256K
table_definition_cache=1400
binlog_row_event_max_size=8K
sync_master_info=10000
sync_relay_log=10000
sync_relay_log_info=10000
```

redis配置：
docker pull redis:6.0
docker run -d --name redis-6.0 \
  -p 6379:6379 \
  -v /app/cms/redis-6.0/conf/redis.conf:/usr/local/etc/redis/redis.conf \
  -v /app/cms/redis-6.0/data:/data \
  redis:6.0 \
  redis-server /usr/local/etc/redis/redis.conf

redis 配置文件:
```config
# Redis 默认端口
port 6379

# 是否作为守护进程运行
daemonize no

# 绑定地址
#bind 127.0.0.1
bind 0.0.0.0
# 工作目录
dir /data

# 设置密码（注释则为无密码）
# 原文档提交的真实密码已泄漏并被移除，部署时请替换为新生成的强密码
requirepass CHANGE_ME_STRONG_PASSWORD

# 最大客户端连接数
maxclients 10000

# RDB 持久化配置
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb

# AOF 持久化配置
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
```

jar 包启动(安装jdk环境)
sudo apt-get install openjdk-8-jdk
优先级： 命令行参数 > 系统属性参数 > properties参数 > yml参数 > yaml参数

[shell脚本]
#!/bin/bash
nohup java -jar /app/cms/cms.jar --server.port=8080 --spring.profiles.active=prd > app.log 2>&1 &
