package com.jiudaotech.monitor.agent.apache;

import com.google.gson.Gson;
import com.jiudaotech.monitor.common.model.BaseParamsModel;
import com.jiudaotech.monitor.common.annotation.Service;
import kafka.admin.AdminUtils;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * apache监控指标推送至kafka
 * <p>
 * 一定要加Service注解并标识该线程类的type，主类根据注解扫描并创建该类的线程放进线程池内
 * 注解的type要和conf.yaml中projects配置的相关项目名字相同
 *
 * @author gzy
 * @since 2018/4/20 14:39
 */
@Service(type = "apache")
public class ApacheMonitorToKafka extends Thread {

    final static Logger L = LoggerFactory.getLogger(ApacheMonitorToKafka.class);

    private String serverStatusUrl;

    private BaseParamsModel baseParamsModel;

    /**
     * 默认构造函数
     */
    public ApacheMonitorToKafka() {
    }

    public ApacheMonitorToKafka(String serverStatusUrl, BaseParamsModel baseParamsModel) {
        this.serverStatusUrl = serverStatusUrl;
        this.baseParamsModel = baseParamsModel;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Gson gson = new Gson();
                ApacheMonitorPicker apacheMonitorPicker = new ApacheMonitorPicker();
                Map<String, Object> apacheMonitorMap = apacheMonitorPicker.getMonitor(serverStatusUrl);
                String jsonStr = gson.toJson(apacheMonitorMap);
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
                L.error("apache监控agent往kafka发送信息异常", e);
            }
        }
    }
}
