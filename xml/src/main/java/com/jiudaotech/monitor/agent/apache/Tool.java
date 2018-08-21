package com.jiudaotech.monitor.agent.apache;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IDEA
 * author:LiWangZhou
 * Date:2018/7/16/016
 * Time:10:45
 **/
public class Tool {

    /**
     * @param parentFolder
     * @return
     */
    public static String getIp(String parentFolder) {
        String deviceAddress = "";
        StringTokenizer dirmid = new StringTokenizer(parentFolder, "_");
        String[] dirlist = parentFolder.split("[_]");
        // snmp/icmp 获得主机ip
        dirmid.nextToken();
        deviceAddress = dirmid.nextToken() + ".";
        deviceAddress = deviceAddress + dirmid.nextToken() + ".";
        deviceAddress = deviceAddress + dirmid.nextToken() + ".";
        deviceAddress = deviceAddress + dirmid.nextToken();
        return deviceAddress;
    }
    /**
     * @param filePath
     * @return
     */
    public static ArrayList<String> gainXmlPath(String filePath) throws ParseException {
        ArrayList<String> list = new ArrayList<String>();
        File file = new File(filePath);
        File[] filest = file.listFiles();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // new Date()为获取当前系统时间
        String no = df.format(new Date());
        java.util.Date now = df.parse(no);
        for (int i = 0; i < filest.length; i++) {
            if (filest[i].toString().contains("MEMORYUTILIZATION")) {
                File file2 = new File(filest[i].toString());
                File[] file1 = file2.listFiles();
            }
        }
        return list;
    }
    /**
     * 读取文件转换成字节流
     *
     * @param xmlPath
     * @return
     * @throws IOException
     */
    public static byte[] dataChange(String xmlPath) throws IOException {
        int b;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 读取xml文件
        FileInputStream in = new FileInputStream(xmlPath);
        while ((b = in.read()) != -1) {
            bos.write(b);
        }
        in.close();
        bos.close();
        byte[] readData = ((ByteArrayOutputStream) bos).toByteArray();
        return readData;
    }

    /**
     * 得到集合数据
     *
     * @param readData
     * @return
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public static Map<String, Object> getXmlDate(byte[] readData)
            throws SAXException, IOException, ParserConfigurationException {
        Map<String, Object> map = new ConcurrentHashMap<>(16);
        DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder dombuilder = domfac.newDocumentBuilder();
        InputStream ins = new ByteArrayInputStream(readData);
        // 读取xml文件
        Document doc = dombuilder.parse(ins);
        Element root = doc.getDocumentElement();
        // 子节点
        NodeList trees = root.getChildNodes();
        if (trees != null) {
            for (int i1 = 0; i1 < trees.getLength(); i1++) {
                Node book = trees.item(i1);
                if ("datalogelement".equals(book.getNodeName())) {
                    String[] dataList = book.getFirstChild().getNodeValue().split("\\|");
                    map.put("time", dataList[0].toString());
                    map.put("MemoryPoolUsed", dataList[4].toString());
                    map.put("MemoryPoolFree", dataList[5].toString());
                    map.put("SysName", dataList[6].toString());
                }
            }
        }
        return map;
    }
}
