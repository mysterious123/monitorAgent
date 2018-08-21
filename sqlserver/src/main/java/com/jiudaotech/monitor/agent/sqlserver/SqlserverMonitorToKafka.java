package com.jiudaotech.monitor.agent.sqlserver;

import com.google.gson.Gson;
import com.jiudaotech.monitor.common.model.BaseParamsModel;
import com.jiudaotech.monitor.common.annotation.Service;
import kafka.admin.AdminUtils;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * sqlserver监控指标推送至kafka
 * <p>
 * 一定要加Service注解并标识该线程类的type，主类根据注解扫描并创建该类的线程放进线程池内
 * 注解的type要和conf.yaml中projects配置的相关项目名字相同
 *
 * @author gzy
 * @since 2018/4/20 14:39
 */
@Service(type = "sqlserver")
public class SqlserverMonitorToKafka extends Thread {

    final static Logger L = LoggerFactory.getLogger(SqlserverMonitorToKafka.class);

    private String monitorFilePath;

    private BaseParamsModel baseParamsModel;

    private Map<String, Object> retMap = new ConcurrentHashMap<>();

    /**
     * 默认构造函数
     */
    public SqlserverMonitorToKafka() {
    }

    public SqlserverMonitorToKafka(String monitorFilePath, BaseParamsModel baseParamsModel) {
        this.monitorFilePath = monitorFilePath;
        this.baseParamsModel = baseParamsModel;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Gson gson = new Gson();
                SqlserverPicker sqlserverPicker = new SqlserverPicker();
                Map<String, Object> sqlserverMonitorMap = sqlserverPicker.getMonitor(monitorFilePath);
                String jsonStr = gson.toJson(sqlserverMonitorMap);
                ZkUtils zkUtils = baseParamsModel.getZkUtils();
                String topic = baseParamsModel.getTopic();
                Producer producer = baseParamsModel.getProducer();
                Integer threadSleep = baseParamsModel.getThreadSleep();
                if (retMap.get("time") != null && sqlserverMonitorMap.get("time").equals(retMap.get("time"))) {
                    L.info("==============csv文件未更新，请检查是否启动性能计数器==========================");
                } else {
                    if (AdminUtils.topicExists(zkUtils, topic)) {
                        producer.send(new ProducerRecord<>(topic, jsonStr));
                        L.info(jsonStr);
                    }
                }
                retMap.put("time", sqlserverMonitorMap.get("time"));
                Thread.sleep(threadSleep);
            } catch (InterruptedException | FileNotFoundException e) {
                L.error("sqlserver监控agent往kafka发送信息异常", e);
            }
        }
    }
}
