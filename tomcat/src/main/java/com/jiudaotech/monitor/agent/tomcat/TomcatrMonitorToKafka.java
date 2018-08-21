package com.jiudaotech.monitor.agent.tomcat;

import com.google.gson.Gson;
import com.jiudaotech.monitor.common.annotation.Service;
import com.jiudaotech.monitor.common.model.BaseParamsModel;
import kafka.admin.AdminUtils;
import kafka.utils.ZkUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * tomcat监控指标推送至kafka
 * <p>
 * 一定要加Service注解并标识该线程类的type，主类根据注解扫描并创建该类的线程放进线程池内
 * 注解的type要和conf.yaml中projects配置的相关项目名字相同
 *
 * @author gzy
 * @since 2018/4/20 14:39
 */
@Service(type = "tomcat")
public class TomcatrMonitorToKafka extends Thread {

    final static Logger L = LoggerFactory.getLogger(TomcatrMonitorToKafka.class);

    private String jmxURL;

    private String jmxUsername;

    private String jmxPassword;

    private BaseParamsModel baseParamsModel;

    /**
     * 默认构造函数
     */
    public TomcatrMonitorToKafka() {
    }

    public TomcatrMonitorToKafka(String jmxURL, BaseParamsModel baseParamsModel) {
        this.jmxURL = jmxURL;
        this.baseParamsModel = baseParamsModel;
    }

    public TomcatrMonitorToKafka(String jmxURL, String jmxUsername, String jmxPassword, BaseParamsModel baseParamsModel) {
        this.jmxURL = jmxURL;
        this.jmxUsername = jmxUsername;
        this.jmxPassword = jmxPassword;
        this.baseParamsModel = baseParamsModel;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Gson gson = new Gson();
                TomcatMonitorPicker tomcatMonitorPicker = new TomcatMonitorPicker();
                Map<String, Object> tomcatMonitorMap;
                if (StringUtils.isNotEmpty(jmxUsername)) {
                    tomcatMonitorMap = tomcatMonitorPicker.getMonitor(jmxURL, jmxUsername, jmxPassword);
                } else {
                    tomcatMonitorMap = tomcatMonitorPicker.getMonitor(jmxURL);
                }
                String jsonStr = gson.toJson(tomcatMonitorMap);
                ZkUtils zkUtils = baseParamsModel.getZkUtils();
                String topic = baseParamsModel.getTopic();
                Producer producer = baseParamsModel.getProducer();
                Integer threadSleep = baseParamsModel.getThreadSleep();
                if (AdminUtils.topicExists(zkUtils, topic)) {
                    producer.send(new ProducerRecord<>(topic, jsonStr));
                    L.info(jsonStr);
                }
                Thread.sleep(threadSleep);
            } catch (Exception e) {
                L.error("tomcat监控agent往kafka发送信息异常", e);
            }
        }
    }
}
