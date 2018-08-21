package com.jiudaotech.monitor.agent.apache;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.jiudaotech.monitor.common.annotation.Service;
import com.jiudaotech.monitor.common.model.BaseParamsModel;
import kafka.admin.AdminUtils;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
/**
 * @Created with IDEA
 * @author:LiWangZhou
 * @Date:2018/7/16/016
 * @Time:10:45
 **/
@Service(type = "netdata")
public class XmlMonitorToKafka extends Thread{
    final static Logger L = LoggerFactory.getLogger(XmlMonitorToKafka.class);
    static boolean count = true;
    private BaseParamsModel baseParamsModel;
    /**
     * 默认构造函数
     */
    public XmlMonitorToKafka() throws ParseException {
    }

    public XmlMonitorToKafka(BaseParamsModel baseParamsModel) throws ParseException {

        this.baseParamsModel = baseParamsModel;
    }

    @Override
    public void run() {
        Map<String, List<String>> map = null;
        try {
            map = XmlMonitorPicker.getData();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
                // 创建一个JsonParser
                long num = 0;
                while (count) {
                    try {
                        List<String> value = map.get("data");
                        if(value ==null){
                            break;
                        }
                        System.out.println(value.size());
                        for (int m = 0; m < value.size(); m++) {
                            num++;
                            String jsonStr = gson.toJson("data");
                            String t = gson.toJson(value.get(m));
                            String jon = "{" + jsonStr + ":" + t + "}";
                            ZkUtils zkUtils = baseParamsModel.getZkUtils();
                            String topic = baseParamsModel.getTopic();
                            Producer producer = baseParamsModel.getProducer();
                            Integer threadSleep = baseParamsModel.getThreadSleep();
                            Thread.sleep(threadSleep);
                            if (AdminUtils.topicExists(zkUtils, topic)) {
                                producer.send(new ProducerRecord<>(topic, jon));
                                L.info(jon);
                            }
                            if (num ==value.size() ) {
                                break;
                            }
                        }
                        if (count) {
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("data 为空");
                    }
                }

            }
        }

