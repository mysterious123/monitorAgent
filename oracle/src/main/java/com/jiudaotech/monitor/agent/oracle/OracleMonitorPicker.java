package com.jiudaotech.monitor.agent.oracle;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.jiudaotech.monitor.common.service.MonitorService;
import com.jiudaotech.monitor.util.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gzy
 * @since 2018/4/25 11:33
 */
@SuppressWarnings("unchecked")
public class OracleMonitorPicker {

    private static final Logger L = LoggerFactory.getLogger(OracleMonitorPicker.class);
    private static final String CRLF = "\r\n";

    public Map<String, Object> getMonitor(DruidDataSource druidDataSource) throws IOException {

        Map<String, Object> retMap = MonitorService.getRegisterHostAndTimesMap();

        Map<String, Object> propsInputMap = MonitorService.loadConfigurationFromYAMLResource("/oracle-monitor-props.yaml");

        DruidPooledConnection druidPooledConnection = null;
        PreparedStatement psta = null;
        ResultSet rs = null;

        for (String key : propsInputMap.keySet()) {
            Map<String, Object> propsGroupMap = (Map<String, Object>) propsInputMap.get(key);
            Map<String, Object> propsValMap = new HashMap<>(16);
            for (String propsKey : propsGroupMap.keySet()) {
                String[] props = propsKey.split(",");
                Map<String, Object> sqlOrCommandMap = (Map<String, Object>) propsGroupMap.get(propsKey);
                //执行sql
                if (sqlOrCommandMap.containsKey("sql")) {
                    String sql = sqlOrCommandMap.get("sql").toString();
                    L.debug("oracle执行sql:{}", sql);
                    try {
                        druidPooledConnection = druidDataSource.getConnection();
                        psta = druidPooledConnection.prepareStatement(sql);
                        rs = psta.executeQuery();
                        //目前只取第一条信息
                        if (rs.next()) {
                            for (int i = 0; i < props.length; i++) {
                                String s = rs.getString(i + 1);
                                propsValMap.put(props[i], s);
                            }
                        }
                    } catch (Exception e) {
                        L.error("oracle监控指标:执行sql语句异常", e);
                    } finally {
                        JdbcUtils.closeResultSet(rs);
                        JdbcUtils.closeStatement(psta);
                        JdbcUtils.closeConnection(druidPooledConnection);
                    }
                } else if (sqlOrCommandMap.containsKey("command")) {
                    String command = sqlOrCommandMap.get("command").toString();
                    L.debug("执行command:{}",command);
                    String osName = System.getProperty("os.name");
                    //执行command
                    Process process;
                    if (osName.toLowerCase().contains("windows")) {
                        process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/C", command});
                    } else {
                        process = Runtime.getRuntime().exec(new String[]{"ksh", "-c", command});
                    }
                    BufferedReader processOutput
                            = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
                    String line;
                    String scriptResponse = "";
                    while ((line = processOutput.readLine()) != null) {
                        if (!line.isEmpty()) {
                            scriptResponse += line + CRLF;
                        }
                    }
                    propsValMap.put(props[0], scriptResponse);
                }
                retMap.put(key, propsValMap);
            }
        }
        return retMap;
    }

}
