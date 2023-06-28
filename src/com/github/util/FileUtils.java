package com.github.util;

import java.io.File;

/**
 * 获取本地文件的大小
 */
public class FileUtils {
    /**
     * @param path: 文件路径
     * @Description: 获取在path路径下本地文件的大小
     * @return: long
     */
    public static long getFileContentLength(String path) {
        File file = new File(path);
        return file.exists() && file.isFile() ? file.length() : 0;
    }
}
