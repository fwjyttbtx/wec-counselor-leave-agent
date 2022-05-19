### 请假的代理工具  
使用`Gradle`命令打包  
`.\gradlew distTar`  

### 软件环境
1. `Linux`发行版`x64`（推荐`Ubuntu 20.04`或`CentOS 8.5`）  
2. `Java8`运行时环境，即`jre`  

### 部署说明  
1. 解压`tar.gz`到**指定目录**  
`tar -zxvf wec-counselor-leave-agent.tar.gz -C /opt`  
2. `cd`到`agent`的目录  
`cd /opt/leave-agent`  
3. 配置信息  
配置信息位于`/opt/leave-agent/conf/application.properties`  
配置内容如下：  
    ```properties
    # 申请open-api的学校code一般为国标的学校代码
    agent.openapi.school-code = 20180611
    # 申请的openapi的appid
    agent.openapi.app-id = 162129959205702212
    # 申请的openapi的secret
    agent.openapi.secret = rStA8NC3vYjjEKVSQyZDyv+xy+lih8YjeukHg8ncOIiUhoc3cki6EDYxRz9PBTp2R0TCEXm7wajAMKE0LrVO1osYbrr7dKnT
    # 获取openapi的区域openapi domain的智校云地址
    agent.openapi.domain-url = https://wecmpapi.wisedu.com/devopsConfig/getOpenApiDomain
    # 如果学校服务器8080端口被占用可以更改此端口号
    server.port=8080
    ```

4. 启动  
启动脚本`./start.sh`  
如果启动有报错或者`java`的进程没有拉起, 则需要先执行一下停止服务的脚本`./stop.sh`再重新执行`./start.sh`脚本  

5. 停止  
执行脚本`./stop.sh`  

6. 查看启动日志  
`tail -f /opt/logs/wec-counselor-leave-agent/wec-counselor-leave-agent.log`  
日志输出如下信息则启动成功：  
   ```log
   2022-05-19 12:46:09.323 [main] INFO  [org.apache.catalina.core.StandardService:173] : Starting service [Tomcat]
   2022-05-19 12:46:09.324 [main] INFO  [org.apache.catalina.core.StandardEngine:173] : Starting Servlet engine: [Apache Tomcat/9.0.62]
   2022-05-19 12:46:09.536 [main] INFO  [o.a.catalina.core.ContainerBase.[Tomcat].[localhost].[/]:173] : Initializing Spring embedded WebApplicationContext
   2022-05-19 12:46:09.537 [main] INFO  [o.s.b.w.s.context.ServletWebServerApplicationContext:290] : Root WebApplicationContext: initialization completed in 3442 ms
   2022-05-19 12:46:10.554 [main] INFO  [io.ktor.client.HttpClient:14] : REQUEST: https://wecmpapi.wisedu.com/devopsConfig/getOpenApiDomain
   METHOD: HttpMethod(value=POST)
   2022-05-19 12:46:11.552 [main] INFO  [io.ktor.client.HttpClient:14] : RESPONSE: 200 OK
   METHOD: HttpMethod(value=POST)
   FROM: https://wecmpapi.wisedu.com/devopsConfig/getOpenApiDomain
   2022-05-19 12:46:11.690 [main] INFO  [c.w.w.weccounselorleaveagent.controller.AgentController:72] : 获取请求区域的API域
   名信息：
   {
     "domain" : "https://openapiv2.wisedu.com",
     "errCode" : "0",
     "errMsg" : "调用成功"
   }
   2022-05-19 12:46:12.728 [main] INFO  [o.s.boot.web.embedded.tomcat.TomcatWebServer:220] : Tomcat started on port(s): 8080 (http) with context path ''
   2022-05-19 12:46:12.753 [main] INFO  [c.w.w.w.WecCounselorLeaveAgentApplicationKt:61] : Started WecCounselorLeaveAgentApplicationKt in 8.15 seconds (JVM running for 9.412)
   ```


