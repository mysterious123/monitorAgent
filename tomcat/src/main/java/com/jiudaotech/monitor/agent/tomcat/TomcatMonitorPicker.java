package com.jiudaotech.monitor.agent.tomcat;

import com.jiudaotech.monitor.common.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * tomccat监控指标获取
 *
 * @author gzy
 * @since 2018/5/3 10:56
 */
@SuppressWarnings("unchecked")
public class TomcatMonitorPicker {

    final static Logger L = LoggerFactory.getLogger(TomcatMonitorPicker.class);

    /**
     * 获取tomcat监控
     * 需要认证
     *
     * @param jmxURL
     * @param monitorUser
     * @param password
     * @return
     * @throws IOException
     * @throws MalformedObjectNameException
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     * @throws InstanceNotFoundException
     */
    public Map<String, Object> getMonitor(String jmxURL, String monitorUser, String password) throws IOException, MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException {
        JMXServiceURL serviceURL = new JMXServiceURL(jmxURL);
        Map map = new HashMap(16);
        String[] credentials = new String[]{monitorUser, password};
        map.put("jmx.remote.credentials", credentials);
        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, map);
        MBeanServerConnection mbsc = connector.getMBeanServerConnection();
        return getHttpAprCatalinaAttribute(mbsc);
    }

    /**
     * 获取tomcat监控
     * 不需要认证获
     *
     * @param jmxURL
     * @return
     * @throws IOException
     * @throws MalformedObjectNameException
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     * @throws InstanceNotFoundException
     */
    public Map<String, Object> getMonitor(String jmxURL) throws IOException, MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException {
        JMXServiceURL serviceURL = new JMXServiceURL(jmxURL);
        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, null);
        MBeanServerConnection mbsc = connector.getMBeanServerConnection();
        return getHttpAprCatalinaAttribute(mbsc);
    }

    /**
     * 根据连接获取http-apr catalina 值
     *
     * @param mbsc
     * @return
     * @throws IOException
     * @throws MalformedObjectNameException
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     * @throws InstanceNotFoundException
     */
    public Map<String, Object> getHttpAprCatalinaAttribute(MBeanServerConnection mbsc) throws IOException, MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException {
        Map<String, Object> retMap = MonitorService.getRegisterHostAndTimesMap();
        Map<String, Object> tomcatMonitorPropMap = MonitorService.loadConfigurationFromYAMLResource("/tomcat-monitor-props.yaml");
        for (String catalinaType : tomcatMonitorPropMap.keySet()) {
            Map<String, Object> catalinaPropsMap = new HashMap<>(16);
            ObjectName objectName = new ObjectName("Catalina:type=" + catalinaType + ",*");
            Set<ObjectName> grps = mbsc.queryNames(objectName, null);
            for (ObjectName catalina : grps) {
                ObjectName catalinaObject = new ObjectName(catalina.getCanonicalName());
                List<String> catalinaProps = (List<String>) tomcatMonitorPropMap.get(catalinaType);
                String keyProperty = catalinaObject.getKeyProperty("name");
                //目前程序只监控http-apr-{port}
                if (keyProperty.contains("http")) {
                    for (String catalinaProp : catalinaProps) {
                        catalinaPropsMap.put(catalinaProp, mbsc.getAttribute(catalinaObject, catalinaProp));
                    }
                }
            }
            retMap.put(catalinaType, catalinaPropsMap);
        }
        return retMap;
    }

}
