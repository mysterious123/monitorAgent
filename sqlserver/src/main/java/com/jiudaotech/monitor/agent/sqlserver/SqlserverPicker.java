package com.jiudaotech.monitor.agent.sqlserver;

import com.jiudaotech.monitor.common.service.MonitorService;
import com.jiudaotech.monitor.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;

/**
 * @author gzy
 * @since 2018/5/2 10:01
 */
public class SqlserverPicker {

    final static Logger L = LoggerFactory.getLogger(SqlserverPicker.class);

    public Map<String, Object> getMonitor(String monitorFilePath) throws FileNotFoundException {
        Map<String, Object> retMap = MonitorService.getRegisterHostAndTimesMap();
        BufferedReader reader = new BufferedReader(new FileReader(monitorFilePath));
        try {
            //第一行信息，为监控类型信息
            String headerLine = reader.readLine();
            //读取最后一行监控数据
            String lastLine = FileUtils.readLastLine(new File(monitorFilePath), null);
            if (StringUtils.isNotEmpty(headerLine) && StringUtils.isNotEmpty(lastLine)) {
                String[] headers = headerLine.split(",");
                String[] lastLines = lastLine.split(",");
                headers[0] = "time";
                for (int i = 0; i < headers.length; i++) {
                    retMap.put(headers[i], lastLines[i]);
                }

            }
        } catch (IOException e) {
            L.error("SqlserverPicker.getMonitor exception",e);
        }
        return retMap;
    }
}
