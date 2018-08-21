package com.jiudaotech.monitor.agent.apache;


import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;
    /**
     * @Created with IDEA
     * @author:LiWangZhou
     * @Date:2018/7/16/016
     * @Time:10:45
     **/
public class XmlMonitorPicker {
    public  static String  path=null;
    public  static String   time=null;
    public  static List<String> list = new ArrayList<String>();
        public static Map<String, Object> getMonitor(String path) throws IOException, ParserConfigurationException, SAXException {
            return Tool.getXmlDate(Tool.dataChange(path));
        }

    public static java.util.Map<String, List<String>> getData()
            throws ParseException, ParserConfigurationException, SAXException, IOException {
        InputStream inputStream = XmlMonitorPicker.class.getResourceAsStream("/xml-monitor-props.yaml");
        Yaml yaml = new Yaml();
        Map<String, String> targetInputMap = yaml.load(inputStream);
        for (String key : targetInputMap.keySet()) {
            if ("path".equals(key.toString())) {
                String value = targetInputMap.get(key.toString());
                path = value;
            }
            if ("time".equals(key.toString())) {
                String value = targetInputMap.get(key.toString());
                time = value;
            }
        }
        DocumentBuilderFactory documentBuilder = DocumentBuilderFactory.newInstance();
        Map<String, List<String>> timeMap = new HashMap<String, List<String>>(16);
        List<String> timeList = new ArrayList<String>();
        DocumentBuilder document = documentBuilder.newDocumentBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowDateFormat = dateFormat.format(new Date());
        java.util.Date now = dateFormat.parse(nowDateFormat);
        File file = new File(path);
        File[] filesList = file.listFiles();
        for (File lastFile : filesList) {
            if (lastFile.toString().contains("MEMORYUTILIZATION")) {
                File memoryFile = new File(lastFile.toString());
                File[] memoryFileXml = memoryFile.listFiles();
                for (File fileText : memoryFileXml) {
                    long nowTimesTamp = now.getTime()
                            - dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(fileText.lastModified())).getTime();
                    if (nowTimesTamp > 3000000) {
                        list.add(fileText.toString());
                    }
                }
            }

        }
        for (String fileText : list) {
            Document documents = document.parse(fileText.toString());
            NodeList node = documents.getElementsByTagName("datalogelement");
            for (int i = 0; i < node.getLength() - 1; i++) {
                String ip = Tool.getIp(fileText.toString());
                Node node1 = node.item(i);
                node1.getTextContent();
                String[] dataList = node1.getTextContent().split("\\|");
                timeList.add(dataList[0].toString() + ";" + dataList[4].toString()
                        + ";" + dataList[5].toString() + ";"
                        + dataList[6].toString() + ";" + ip);
                timeMap.put("data", timeList);
            }
        }
            return timeMap;
        }

}