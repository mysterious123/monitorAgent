package com.jiudaotech.monitor.agent;


import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jiudaotech.monitor.common.annotation.Service;
import com.jiudaotech.monitor.common.model.BaseParamsModel;
import com.jiudaotech.monitor.common.service.MonitorService;
import com.jiudaotech.monitor.util.DruidDataSourceUtil;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.Producer;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 监控主类
 *
 * @author gzy
 * @since 2018/4/17 16:14
 */
@SuppressWarnings("unchecked")
public class MonitorAgent {

    private static final Logger L = LoggerFactory.getLogger(MonitorAgent.class);

    /**
     * 监控主函数入口
     *
     * @param args args[0] 配置文件路径
     * @throws InterruptedException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static void main(String[] args) throws InterruptedException, FileNotFoundException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        Map<String, Object> confMap;
        if (args.length == 0) {
            L.info("本地开发，读取资源文件中路径");
            confMap = MonitorService.loadConfigurationFromYAMLResource("/conf.yaml");
        } else {
            confMap = MonitorService.loadConfigurationFromYAMLFile(args[0]);
        }
        //kafka producer 是线程安全的，通常在所有线程中共享以达到最佳表现
       Producer<String, String> producer = MonitorService.getKafkaProducerByConf(confMap);
        ZkUtils zkUtils = MonitorService.getZKUtilsByConf(confMap);

        //根据注解扫描得到特定的包下的Class
        Reflections reflections = new Reflections("com.jiudaotech.monitor.agent");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Service.class);
        List<String> projects = (List<String>) confMap.get("projects");
        for(int i = 0 ; i <projects.size() ; i++) {
            System.out.println("list 集合遍历"+projects.get(i));
        }
        //手动创建线程池
        ThreadFactory threadFactory = new ThreadFactoryBuilder().build();
        //Common Thread Pool
        ExecutorService pool = new ThreadPoolExecutor(projects.size(), 50, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1024), threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
        //根据配置的监控项目，扫描对应的线程类，并按照构造函数约定实例化线程类，然后放到线程池中执行
        //这样做的目的是可以分模块进行打包，而不必把所有的子监控项目全部打包
        for (String project : projects) {
            //监控子项目配置map
            Map<String, Object> projectConfMap = (Map<String, Object>) confMap.get(project);
            System.out.println("配置集合数据"+projectConfMap);
            String projectTopic = projectConfMap.get("kafka.topic").toString();
            int threadSleepMs = Integer.parseInt(projectConfMap.get("thread.sleep").toString());
            //项目topic不存在的情况，先根据项目创建topic
            MonitorService.createTopicIfNotCreateByConf(confMap, projectTopic);
            BaseParamsModel baseParamsModel = new BaseParamsModel();
            baseParamsModel.setProducer(producer);
            baseParamsModel.setZkUtils(zkUtils);
            baseParamsModel.setTopic(projectTopic);
            baseParamsModel.setThreadSleep(threadSleepMs);

            //根据注解扫描需要启动的线程并创建实例
            for (Class clz : annotated) {
                Service annotation = (Service) clz.getAnnotation(Service.class);
                Thread projectThread;
                if (annotation.type().equals(project)) {
                    if ("oracle".equals(project)) {
                        String jdbcUrl = projectConfMap.get("jdbc.url").toString();
                        String jdbcUsername = projectConfMap.get("jdbc.username").toString();
                        String jdbcPassword = projectConfMap.get("jdbc.password").toString();
                        DruidDataSource druidDataSource = DruidDataSourceUtil.createDataSource(jdbcUrl, jdbcUsername, jdbcPassword);
                        Constructor constructor = clz.getConstructor(DruidDataSource.class, BaseParamsModel.class);
                        projectThread = (Thread) constructor.newInstance(druidDataSource, baseParamsModel);
                    } else if ("sqlserver".equals(project)) {
                        String perfmonPath = projectConfMap.get("perfmon.path").toString();
                        Constructor constructor = clz.getConstructor(String.class, BaseParamsModel.class);
                        projectThread = (Thread) constructor.newInstance(perfmonPath, baseParamsModel);
                    } else if ("apache".equals(project)) {
                        String serverStatusUrl = projectConfMap.get("serverstatus.url").toString();
                        Constructor constructor = clz.getConstructor(String.class, BaseParamsModel.class);
                        projectThread = (Thread) constructor.newInstance(serverStatusUrl, baseParamsModel);
                    } else if ("tomcat".equals(project)) {
                        String jmxUrl = projectConfMap.get("jmx.url").toString();
                        Boolean jmxAuthenticate = (Boolean) projectConfMap.get("jmx.authenticate");
                        //需要认证
                        if (jmxAuthenticate) {
                            String jmxUsername = projectConfMap.get("jmx.username").toString();
                            String jmxPassword = projectConfMap.get("jmx.password").toString();
                            Constructor constructor = clz.getConstructor(String.class, String.class, String.class, BaseParamsModel.class);
                            projectThread = (Thread) constructor.newInstance(jmxUrl, jmxUsername, jmxPassword, baseParamsModel);
                        } else {
                            //不需要认证
                            Constructor constructor = clz.getConstructor(String.class, BaseParamsModel.class);
                            projectThread = (Thread) constructor.newInstance(jmxUrl, baseParamsModel);
                        }

                    } else if ("weblogic".equals(project)) {
                        String snmpIp = projectConfMap.get("snmp.ip").toString();
                        String snmpPortNumber = projectConfMap.get("snmp.portNumber").toString();
                        String snmpCommunity = projectConfMap.get("snmp.Community").toString();
                        Integer snmpTimeout = Integer.parseInt(projectConfMap.get("snmp.timeout").toString());
                        String snmpVersion = projectConfMap.get("snmp.Version").toString();
                        Constructor constructor = clz.getConstructor(String.class, String.class, String.class, Integer.class, String.class, BaseParamsModel.class);
                        projectThread = (Thread) constructor.newInstance(snmpIp, snmpPortNumber, snmpCommunity, snmpTimeout, snmpVersion, baseParamsModel);
                    } else if ("windows".equals(project)) {
                        Constructor constructor = clz.getConstructor(BaseParamsModel.class);
                        projectThread = (Thread) constructor.newInstance( baseParamsModel);
                    }else {
                        Constructor constructor = clz.getConstructor(BaseParamsModel.class);
                        projectThread = (Thread) constructor.newInstance(baseParamsModel);
                    }
                    pool.execute(projectThread);
                }
            }
        }
        pool.shutdown();
    }
}
