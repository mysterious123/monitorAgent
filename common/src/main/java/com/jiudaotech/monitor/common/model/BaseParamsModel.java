package com.jiudaotech.monitor.common.model;

import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.Producer;

/**
 * @author gzy
 * @since 2018/5/7 14:36
 */
public class BaseParamsModel {

    /**
     * kafka生产端
     */
    private Producer<String, String> producer;

    /**
     * zookeeper utils
     */
    private ZkUtils zkUtils;

    /**
     * kafka推送topic
     */
    private String topic;

    /**
     * 批次间隔
     */
    private Integer threadSleep;

    public Producer<String, String> getProducer() {
        return producer;
    }

    public void setProducer(Producer<String, String> producer) {
        this.producer = producer;
    }

    public ZkUtils getZkUtils() {
        return zkUtils;
    }

    public void setZkUtils(ZkUtils zkUtils) {
        this.zkUtils = zkUtils;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getThreadSleep() {
        return threadSleep;
    }

    public void setThreadSleep(Integer threadSleep) {
        this.threadSleep = threadSleep;
    }
}
