package com.jiudaotech.monitor.agent.service;

import com.google.gson.Gson;
import com.jiudaotech.monitor.common.annotation.Service;
import com.jiudaotech.monitor.common.model.BaseParamsModel;
import com.jiudaotech.monitor.common.service.MonitorService;
import kafka.admin.AdminUtils;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 心跳线程
 * * 一定要加Service注解并标识该线程类的type，主类根据注解扫描并创建该类的线程放进线程池内
 * 注解的type要和conf.yaml中projects配置的相关项目名字相同
 *
 * @author gzy
 * @since 2018/4/20 15:26
 */
@Service(type = "heartbeat")
public class AgentHeartBeatRunnableToKafka extends Thread {

    final static Logger L = LoggerFactory.getLogger(AgentHeartBeatRunnableToKafka.class);

    private BaseParamsModel baseParamsModel;

    /**
     * 默认构造函数
     */
    public AgentHeartBeatRunnableToKafka() {
    }

    public AgentHeartBeatRunnableToKafka(BaseParamsModel baseParamsModel) {
        this.baseParamsModel = baseParamsModel;
    }

    @Override
    public void run() {

        try {
            while (true) {
                Map<String, Object> heartBeatMap = MonitorService.getRegisterHostAndTimesMap();
                heartBeatMap.put("type", "heartbeat");
                Gson gson = new Gson();
                String jsonStr = gson.toJson(heartBeatMap);
                ZkUtils zkUtils = baseParamsModel.getZkUtils();
                String topic = baseParamsModel.getTopic();
                Producer producer = baseParamsModel.getProducer();
                Integer threadSleep = baseParamsModel.getThreadSleep();
                if (AdminUtils.topicExists(zkUtils, topic)) {
                    producer.send(new ProducerRecord<>(topic, jsonStr));
                    L.info(jsonStr);
                }
                Thread.sleep(threadSleep);
            }
        } catch (InterruptedException e) {
            L.error("监控agent心跳往kafka发送信息异常", e);
        }
    }
}
