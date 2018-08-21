package com.jiudaotech.monitor.agent.apache;

import com.jiudaotech.monitor.common.service.MonitorService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * @author gzy
 * @since 2018/5/4 10:20
 */
@SuppressWarnings("unchecked")
public class ApacheMonitorPicker {

    public Map<String, Object> getMonitor(String serverStatusUrl) throws IOException {

        Map<String, Object> retMap = MonitorService.getRegisterHostAndTimesMap();
        Map<String, Object> apacheMonitorPropMap = MonitorService.loadConfigurationFromYAMLResource("/apache-monitor-props.yaml");

        URL url = new URL(serverStatusUrl);
        URLConnection conn = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        List<String> propsInYaml = (List<String>) apacheMonitorPropMap.get("ServerStatus");
        while ((inputLine = br.readLine()) != null) {
            String[] props = inputLine.split(":");
            if (props.length > 1) {
                String propKey = props[0];
                String propVal = props[1];
                if (propsInYaml.contains(propKey)) {
                    retMap.put(propKey, propVal);
                }
            }
        }

        br.close();
        return retMap;
    }
}
