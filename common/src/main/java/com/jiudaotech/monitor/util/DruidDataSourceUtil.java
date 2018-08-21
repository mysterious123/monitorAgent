package com.jiudaotech.monitor.util;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * druid datasource 工具
 *
 * @author gzy
 * @since 2018/4/24 14:51
 */
public class DruidDataSourceUtil {

    private static final Logger L = LoggerFactory.getLogger(DruidDataSourceUtil.class);

    public static String getDbTypeByUrl(String url) {
        String dbType = "";
        if (StringUtils.isNotBlank(url)) {
            if (url.startsWith("jdbc:oracle")) {
                dbType = "ORACLE";
            } else if (url.startsWith("jdbc:mysql")) {
                dbType = "MYSQL";
            } else if (url.startsWith("jdbc:h2")) {
                dbType = "H2";
            } else if (url.startsWith("jdbc:sqlserver")) {
                dbType = "SQLSERVER";
            }
        }
        return dbType;
    }

    public static DruidDataSource createDataSource(String url, String username, String password) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        String dbType = getDbTypeByUrl(url);
        if ("ORACLE".equals(dbType)) {
            dataSource.setValidationQuery("select 'x'  from dual ");
        } else {
            dataSource.setValidationQuery("select 'x' ");
        }
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(true);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
        try {
            dataSource.setFilters("stat");
        } catch (SQLException e) {
            L.error("创建数据库连接池异常:{}", e.getMessage());
        }
        return dataSource;
    }

}
