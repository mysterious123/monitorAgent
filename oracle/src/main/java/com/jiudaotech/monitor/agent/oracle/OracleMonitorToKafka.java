package com.jiudaotech.monitor.agent.oracle;

import com.alibaba.druid.pool.DruidDataSource;
import com.google.gson.Gson;
import com.jiudaotech.monitor.common.model.BaseParamsModel;
import com.jiudaotech.monitor.common.annotation.Service;
import kafka.admin.AdminUtils;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * oracle监控指标
 * <p>
 * 一定要加Service注解并标识该线程类的type，主类根据注解扫描并创建该类的线程放进线程池内
 * 注解的type要和conf.yaml中projects配置的相关项目名字相同
 *
 * @author gzy
 * @since 2018/4/20 14:39
 */
@Service(type = "oracle")
@SuppressWarnings("unchecked")
public class OracleMonitorToKafka extends Thread {

    private static final Logger L = LoggerFactory.getLogger(OracleMonitorToKafka.class);

    private DruidDataSource druidDataSource;

    private BaseParamsModel baseParamsModel;

    /**
     * 默认构造函数
     */
    public OracleMonitorToKafka() {
    }

    public OracleMonitorToKafka(DruidDataSource druidDataSource, BaseParamsModel baseParamsModel) {
        this.druidDataSource = druidDataSource;
        this.baseParamsModel = baseParamsModel;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Gson gson = new Gson();
                OracleMonitorPicker oracleMonitorPicker = new OracleMonitorPicker();
                Map<String, Object> retMap = oracleMonitorPicker.getMonitor(druidDataSource);
                String jsonStr = gson.toJson(retMap);
                ZkUtils zkUtils = baseParamsModel.getZkUtils();
                String topic = baseParamsModel.getTopic();
                Producer producer = baseParamsModel.getProducer();
                Integer threadSleep = baseParamsModel.getThreadSleep();
                if (AdminUtils.topicExists(zkUtils, topic)) {
                    producer.send(new ProducerRecord<>(topic, jsonStr));
                    L.info(jsonStr);
                }
                Thread.sleep(threadSleep);
            } catch (IOException | InterruptedException e) {
                L.error("oracle监控agent往kafka发送信息异常", e);
            }
        }
    }
}
