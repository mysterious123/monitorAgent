package com.jiudaotech.monitor.util;

import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * snmp connect and getMessage util
 *
 * @author gzy
 * @since 2018/5/17 11:17
 */
public class SnmpUtil {

    private static final Logger logger = LoggerFactory.getLogger(SnmpUtil.class);

    private static final String VERSION_1 = "1";
    private static final String VERSION_2 = "2";

    private String version;

    private String agentIP;
    private String agentPort;
    private String community;
    private int timeout;

    private Snmp snmp;

    /**
     * * @param agentIP   代理ip
     *
     * @param agentIP   代理ip
     * @param agentPort 代理端口
     * @param community 社区前缀
     * @param timeout   超时时间ms
     * @param version   snmp version 目前只支持V1 V2
     */
    public SnmpUtil(String agentIP, String agentPort, String community, int timeout, String version) throws IOException {
        this.agentIP = agentIP;
        this.agentPort = agentPort;
        this.community = community;
        this.timeout = timeout;
        this.version = version;
        start();
    }

    public void close() throws IOException {
        snmp.close();
    }

    private void start() throws IOException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        // Do not forget this line!
        transport.listen();
    }

    /**
     * * A <code>CommunityTarget</code> represents SNMP target properties for
     * community based message processing models (SNMPv1 and SNMPv2c).
     *
     * @return
     */
    private Target getCommunityTarget() {
        Address targetAddress = GenericAddress.parse("udp:" + agentIP + "/" + agentPort);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(targetAddress);
        target.setRetries(0);
        target.setTimeout(timeout);
        if (version.equals(VERSION_1)) {
            target.setVersion(SnmpConstants.version1);
        } else if (version.equals(VERSION_2)) {
            target.setVersion(SnmpConstants.version2c);
        }
        return target;
    }

    /**
     * 根据root oid获取sub tree objects
     * Gets a subtree with GETNEXT (SNMPv1) or GETBULK (SNMP2c, SNMPv3) operations from the specified target synchronously.
     * a possibly empty List of TreeEvent instances where each instance carries zero or more values (or an error condition) in depth-first-order.
     * 如果getSubtree获取的variableBinding为空，则使用GETNEXT发送请求获取数据
     *
     * @param oid
     * @return
     */
    public List<Object> getSubTreeAsListMapByRootOid(OID oid) throws IOException {
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        List<TreeEvent> list = treeUtils.getSubtree(getCommunityTarget(), oid);
        List<Object> resList = new ArrayList<>();
        for (TreeEvent treeEvent : list) {
            if (treeEvent != null) {
                VariableBinding[] variableBinding = treeEvent.getVariableBindings();
                if (variableBinding != null) {
                    for (VariableBinding var : variableBinding) {
                        resList.add(var.getVariable());
                    }
                } else {
                    //1.3.6.1.4.1.140.625.360.1.94,1.3.6.1.4.1.140.625.360.1.95,1.3.6.1.4.1.140.625.360.1.96
                    //以上几个通过getSubtree获取不到，但是getnext可以获取到
                    resList = getResponseAsListMap(oid, PDU.GETNEXT);
                }
            }
        }
        return resList;
    }

    public JsonArray getSubTreeAsJsonArrayByRootOid(OID oid) throws IOException {
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        List<TreeEvent> list = treeUtils.getSubtree(getCommunityTarget(), oid);
        JsonArray jsonArray = new JsonArray();
        for (TreeEvent treeEvent : list) {
            if (treeEvent != null) {
                VariableBinding[] variableBinding = treeEvent.getVariableBindings();
                if (variableBinding != null) {
                    for (VariableBinding var : variableBinding) {
                        jsonArray.add(var.getVariable().toString());
                    }
                } else {
                    //1.3.6.1.4.1.140.625.360.1.94,1.3.6.1.4.1.140.625.360.1.95,1.3.6.1.4.1.140.625.360.1.96
                    //以上几个通过getSubtree获取不到，但是getnext可以获取到
                    jsonArray = getResponseAsJsonArray(oid, PDU.GETNEXT);
                }
            }
        }
        return jsonArray;
    }

    /**
     * 发送PDU给agent，以获取variable
     *
     * @param oid  oid
     * @param type PDU.GET PDU.GETNEXT PDU.GETBULK ...请求类型
     * @return
     * @throws IOException
     */
    public List<Object> getResponseAsListMap(OID oid, int type) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(type);
        pdu.add(new VariableBinding(oid));
        ResponseEvent rspEvent = snmp.send(pdu, getCommunityTarget());
        PDU response = rspEvent.getResponse();
        List<Object> resList = new ArrayList<>();
        if (null != response && response.getErrorIndex() == PDU.noError && response.getErrorStatus() == PDU.noError) {
            for (VariableBinding variable : response.getVariableBindings()) {
                resList.add(variable.getVariable());
            }
        }
        return resList;
    }

    public JsonArray getResponseAsJsonArray(OID oid, int type) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(type);
        pdu.add(new VariableBinding(oid));
        ResponseEvent rspEvent = snmp.send(pdu, getCommunityTarget());
        PDU response = rspEvent.getResponse();
        JsonArray jsonArray = new JsonArray();
        if (null != response && response.getErrorIndex() == PDU.noError && response.getErrorStatus() == PDU.noError) {
            for (VariableBinding variable : response.getVariableBindings()) {
                jsonArray.add(variable.getVariable().toString());
            }
        }
        return jsonArray;
    }

    public String getVersion() {
        return version;
    }

    public String getAgentIP() {
        return agentIP;
    }

    public String getAgentPort() {
        return agentPort;
    }

    public String getCommunity() {
        return community;
    }

    public int getTimeout() {
        return timeout;
    }

    public Snmp getSnmp() {
        return snmp;
    }
}
