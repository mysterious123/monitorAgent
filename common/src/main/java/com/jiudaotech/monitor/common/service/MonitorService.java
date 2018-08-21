package com.jiudaotech.monitor.common.service;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 监控服务类
 * 主要是获取配置文件或者一些中间件客户端
 *
 * @author gzy
 * @since 2018/4/20 14:06
 */
@SuppressWarnings("unchecked")
public class MonitorService {

    private static final Logger L = LoggerFactory.getLogger(MonitorService.class);

    /**
     * 从YAML配置文件读取配置
     *
     * @param yamlFilePath 资源文件路径
     * @return
     * @throws FileNotFoundException
     */
    public static Map<String, Object> loadConfigurationFromYAMLFile(String yamlFilePath) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(yamlFilePath);
        Yaml yaml = new Yaml();
        return yaml.load(fileInputStream);
    }

    /**
     * 从系统resource文件夹下获取配置
     *
     * @param yamlResourcePath 资源文件在resource下路径
     * @return
     * @throws FileNotFoundException
     */
    public static Map<String, Object> loadConfigurationFromYAMLResource(String yamlResourcePath) throws FileNotFoundException {
        InputStream inputStream = MonitorService.class.getResourceAsStream(yamlResourcePath);
        Yaml yaml = new Yaml();
        return yaml.load(inputStream);
    }

    /**
     * 根据配置信息返回ZKUtils
     *
     * @param confMap
     * @return
     */
    public static ZkUtils getZKUtilsByConf(Map<String, Object> confMap) {

        Map<String, Object> zookeeperConfMap = (Map<String, Object>) confMap.get("zookeeper");
        String zookeeperHosts = zookeeperConfMap.get("hosts").toString();
        int zookeeperSessionTimeOutMs = Integer.parseInt(zookeeperConfMap.get("sessionTimeOutMs").toString());
        int zookeeperConnectTimeOutMs = Integer.parseInt(zookeeperConfMap.get("connectionTimeOutMs").toString());

        ZkClient zkClient = new ZkClient(zookeeperHosts, zookeeperSessionTimeOutMs, zookeeperConnectTimeOutMs, ZKStringSerializer$.MODULE$);
        return new ZkUtils(zkClient, new ZkConnection(zookeeperHosts), false);

    }

    /**
     * 如果kafka topic 不存在，则创建
     *
     * @param confMap
     * @param topic
     */
    public static void createTopicIfNotCreateByConf(Map<String, Object> confMap, String topic) {
        Map<String, Object> kafkaConfMap = (Map<String, Object>) confMap.get("kafka");
        Map<String, Object> kafkaTopicConfMap = (Map<String, Object>) kafkaConfMap.get("topic");
        //分片数
        int numberOfPartitions = Integer.parseInt(kafkaTopicConfMap.get("numberOfPartitions").toString());
        //副本数
        int replication = Integer.parseInt(kafkaTopicConfMap.get("replication").toString());
        ZkUtils zkUtils = getZKUtilsByConf(confMap);
        if (!AdminUtils.topicExists(zkUtils, topic)) {
            AdminUtils.createTopic(zkUtils, topic, numberOfPartitions, replication, new Properties(), RackAwareMode.Enforced$.MODULE$);
        }
    }

    /**
     * 根据配置获取kafka Producer
     *
     * @param confMap
     * @return
     */
    public static Producer<String, String> getKafkaProducerByConf(Map<String, Object> confMap) {
        Map<String, Object> kafkaConfMap = (Map<String, Object>) confMap.get("kafka");
        Properties properties = new Properties();
        Map<String, Object> kafkaProducerConfMap = (Map<String, Object>) kafkaConfMap.get("producer");
        for (String kafkaProperties : kafkaProducerConfMap.keySet()) {
            properties.put(kafkaProperties, kafkaProducerConfMap.get(kafkaProperties));
        }
        return new KafkaProducer<>(properties);
    }

    /**
     * 获取注册了host以及时间戳的map
     *
     * @return
     */
    public static Map<String, Object> getRegisterHostAndTimesMap() {
        Map<String, Object> retMap = new HashMap<>(16);
        try {
            Date date = new Date();
            SimpleDateFormat sdfTimetamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdfTimetamp.setTimeZone(TimeZone.getTimeZone("GMT+8"));//时间戳采用东八时区
            InetAddress inetAddress = InetAddress.getLocalHost();
            retMap.put("host", inetAddress.getHostAddress());
            retMap.put("hostname", inetAddress.getHostName());
            retMap.put("timestamp", sdfTimetamp.format(date));
        } catch (UnknownHostException e) {
            L.error("获取机器host，hostname失败", e);
        }
        return retMap;
    }

}
