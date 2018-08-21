package com.jiudaotech.monitor.agent.weblogic;

import com.google.gson.JsonObject;
import com.jiudaotech.monitor.common.annotation.Service;
import com.jiudaotech.monitor.common.model.BaseParamsModel;
import kafka.admin.AdminUtils;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * weblogic监控指标
 * <p>
 * 一定要加Service注解并标识该线程类的type，主类根据注解扫描并创建该类的线程放进线程池内
 * 注解的type要和conf.yaml中projects配置的相关项目名字相同
 *
 * @author gzy
 * @since 2018/4/20 14:39
 */
@Service(type = "weblogic")
@SuppressWarnings("unchecked")
public class WeblogicMonitorToKafka extends Thread {

    private static final Logger L = LoggerFactory.getLogger(WeblogicMonitorToKafka.class);

    private String snmpIp;
    private String snmpPortNumber;
    private String snmpCommunity;
    private Integer timeout;
    private String snmpVersion;

    private BaseParamsModel baseParamsModel;

    /**
     * 默认构造函数
     */
    public WeblogicMonitorToKafka() {
    }

    public WeblogicMonitorToKafka(String snmpIp, String snmpPortNumber, String snmpCommunity, Integer timeout, String snmpVersion, BaseParamsModel baseParamsModel) {
        this.snmpIp = snmpIp;
        this.snmpPortNumber = snmpPortNumber;
        this.snmpCommunity = snmpCommunity;
        this.timeout = timeout;
        this.snmpVersion = snmpVersion;
        this.baseParamsModel = baseParamsModel;
    }

    @Override
    public void run() {
        while (true) {
            try {
                WeblogicMonitorPicker weblogicMonitorPicker = new WeblogicMonitorPicker();
                JsonObject jsonObject = weblogicMonitorPicker.getMonitor(snmpIp, snmpPortNumber, snmpCommunity, timeout, snmpVersion);
                String jsonStr = jsonObject.toString();
                ZkUtils zkUtils = baseParamsModel.getZkUtils();
                String topic = baseParamsModel.getTopic();
                Producer producer = baseParamsModel.getProducer();
                Integer threadSleep = baseParamsModel.getThreadSleep();
                if (AdminUtils.topicExists(zkUtils, topic)) {
                    producer.send(new ProducerRecord<>(topic, jsonStr));
                    L.info(jsonStr);
                }
                Thread.sleep(threadSleep);
            } catch (IOException | InterruptedException | URISyntaxException e) {
                L.error("weblogic监控agent往kafka发送信息异常", e);
            }
        }
    }
}
