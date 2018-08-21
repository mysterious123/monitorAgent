package com.jiudaotech.monitor.agent.weblogic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jiudaotech.monitor.util.SnmpUtil;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.snmp4j.smi.OID;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Iterator;

/**
 * weblogic监控指标获取
 * 使用snmp
 *
 * @author gzy
 * @since 2018/5/18 11:38
 */
public class WeblogicMonitorPicker {

    public JsonObject getMonitor(String snmpIp, String snmpPortNumber, String snmpCommunity, int timeout, String snmpVersion) throws IOException, URISyntaxException {

        SnmpUtil snmpUtil = new SnmpUtil(snmpIp, snmpPortNumber, snmpCommunity, timeout, snmpVersion);
        InputStream inputStream = WeblogicMonitorPicker.class.getClassLoader().getResourceAsStream("k00.ref");
        File tempFile = File.createTempFile("k00", "");
        FileUtils.copyInputStreamToFile(inputStream, tempFile);
        Document document = Jsoup.parse(tempFile, "UTF-8");
        Elements elements = document.getElementsByTag("snmp");
        Iterator<Element> elementIterator = elements.iterator();
        JsonObject agJsonObject = new JsonObject();
        while (elementIterator.hasNext()) {
            Element snmpElement = elementIterator.next();
            String ag = snmpElement.attr("ag");
            Elements snmpChildren = snmpElement.children();
            Iterator<Element> metricElements = snmpChildren.iterator();
            JsonArray metricJsonArray = new JsonArray();
            while (metricElements.hasNext()) {
                JsonObject metricJsonObject = new JsonObject();
                Element metricElement = metricElements.next();
                String oid = metricElement.attr("oid");
                String metricName = metricElement.attr("name");
                JsonArray resJsonArray = snmpUtil.getSubTreeAsJsonArrayByRootOid(new OID(oid));
                metricJsonObject.add(metricName, resJsonArray);
                metricJsonArray.add(metricJsonObject);
            }
            agJsonObject.add(ag, metricJsonArray);
            //附加snmp相关信息
            agJsonObject.addProperty("snmpPortNumber", snmpUtil.getAgentPort());
            agJsonObject.addProperty("snmpIp", snmpUtil.getAgentIP());
            agJsonObject.addProperty("snmpCommunity", snmpUtil.getCommunity());
            agJsonObject.addProperty("snmpVersion", snmpUtil.getVersion());
        }
        //关闭snmp监听
        snmpUtil.close();
        return agJsonObject;
    }

}
