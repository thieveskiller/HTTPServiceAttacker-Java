# HTTPServiceAttacker-Java

开发版构建: https://ci.ishland.com/job/ThievesKiller/job/HTTPServiceAttacker-Java/job/master/

English document: https://github.com/thieveskiller/HTTPServiceAttacker-Java/blob/master/README.md

# 用法
- 下载最新的构建或自己构建
- 使用 ``` java -jar yourbuild.jar ``` 启动
- 编辑 ``` config.yml ``` 配置文件
- 再次启动
- Enjoy

# 配置文件
- showExceptions: 是否显示异常的详细信息
- targets: 一个存放目标的数组
- - addr: 目标URL
- - threads: 线程数量
- - mode: 使用 ``` GET ``` 或者 ``` POST ``` 进行请求
- - data: 当使用  ``` POST ```, 请求体
- - referer: Header ``` Referer ``` 的自定义值

# 占位符
- ``` [QQ] ``` QQ 号
- ``` [86Phone] ``` 中国手机号
- ``` [Ascii_x] ``` 随机可打印字符组成的字符串, ``` x ``` 为长度
- ``` [Number_x] ``` 随机数字组成的字符串, ``` x ``` 为长度
- ``` [Alpha_x] ``` 随机字母组成的字符串, ``` x ``` 为长度
- ``` [NumAlp_x] ``` 随机字母和数字组成的字符串, ``` x ``` 为长度
- ``` x ``` 的范围是  1-32
