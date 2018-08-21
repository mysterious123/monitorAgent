package com.jiudaotech.monitor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author gzy
 * @since 2018/5/2 10:44
 */
public class FileUtils {

    private static final Logger L = LoggerFactory.getLogger(DruidDataSourceUtil.class);


    /**
     * 快速读取文件最后一行
     *
     * @param file
     * @param charset
     * @return
     */
    public static String readLastLine(File file, String charset) {
        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            L.error("文件{}读取失败", file.getName());
            return null;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            long len = raf.length();
            if (len == 0L) {
                return "";
            } else {
                long pos = len - 1;
                while (pos > 0) {
                    pos--;
                    raf.seek(pos);
                    if (raf.readByte() == '\n') {
                        break;
                    }
                }
                if (pos == 0) {
                    raf.seek(0);
                }
                byte[] bytes = new byte[(int) (len - pos)];
                raf.read(bytes);
                if (charset == null) {
                    return new String(bytes);
                } else {
                    return new String(bytes, charset);
                }
            }
        } catch (IOException e) {
            L.error("文件{}读取失败", file.getName(), e);
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception e2) {
                    L.error("RandomAccessFile close exception", e2);
                }
            }
        }
        return null;
    }

}
