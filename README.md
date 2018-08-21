## 项目介绍

监控agent

## 支持监控主体

* windows
* Oracle
* Sqlserver
* Tomcat
* Apache
* Weblogic

## 本地开发

   1、环境依赖:
   * kafka 0.10.2.1
   * zookeeper
   * java 1.8
   
   2、运行方式
   core/com.jiudaotech.monitor.agent  MonitorAgent.java为主类入口,直接运行该类main方法,
   默认读取core/resources下的conf.yaml配置文件

## 打包
core/build.gradle dependencies中按需配置依赖监控子项目
```gradle
  dependencies {
      //需要监控的类型场景，按需依赖
      compile project(":windows")
      compile project(":oracle")
  }
```

配置完成后，执行命令，两种模式可选择
- 命令行模式
```
/monitorAgent文件夹下执行
#gradle shadowJar
```
- IDEA开发环境
```
:monitorAgent/Tasks/shadow
#shadowJar
```

## 运行

环境依赖:
* kafka 0.10.2.1
* zookeeper
* java 1.8

### 执行命令
```
java -jar core.jar d:\conf.yaml
```
其中conf.yaml为项目运行时必须的配置文件，具体配置规则见配置文件内注释