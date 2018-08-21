package com.jiudaotech.monitor.agent.windows;

import com.jiudaotech.monitor.common.service.MonitorService;
import org.yaml.snakeyaml.Yaml;
import wmi4java.WMI4Java;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * windows 监控
 *
 * @author gzy
 * @since 2018/4/18 9:50
 */
public class WindowsMonitorPicker {

    public Map<String, Object> getMonitor() {
        Map<String, Object> retMap = MonitorService.getRegisterHostAndTimesMap();

        InputStream inputStream = WindowsMonitorPicker.class.getResourceAsStream("/windows-monitor-props.yaml");
        Yaml yaml = new Yaml();
        Map<String, Map<String, List<String>>> targetInputMap = yaml.load(inputStream);
        //告警指标组
        for (String key : targetInputMap.keySet()) {
            Map<String, Object> wmiClassMap = new HashMap<>(16);
            Map<String, List<String>> targetWmiClassMap = targetInputMap.get(key);
            //所配置的WMI class
            for (String wmiClass : targetWmiClassMap.keySet()) {
                Map<String, String> wmiWin32ClassProperties = WMI4Java.get().VBSEngine().getWMIObject(wmiClass);
                Map<String, Object> wmiParamMap = new HashMap<>(16);
                //所配置的wmi 参数
                List<String> wmiParams = targetWmiClassMap.get(wmiClass);
                for (String wmiParam : wmiParams) {
                    wmiParamMap.put(wmiParam, wmiWin32ClassProperties.get(wmiParam));
                }
                wmiClassMap.put(wmiClass, wmiParamMap);
            }
            retMap.put(key, wmiClassMap);
        }
        return retMap;
    }

}
