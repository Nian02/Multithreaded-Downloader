package com.github.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Http工具包
 */
public class HttpUtils {
    /**
     * @param url: 下载文件地址
     * @Description: 获取对应链接下的文件大小
     * @return: long
     */

    public static long getHTTPFileContentLength(String url) throws IOException {
        // 获取下载的链接对象
        int contentLength;
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = getHttpURLConnection(url);
            contentLength = httpURLConnection.getContentLength();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return contentLength;
    }

    /**
     * @param url:     下载文件地址
     * @param startPos:下载文件起始位置
     * @param endPos:  下载文件结束地址
     * @Description: 根据分块下载文件的起始和结束位置将整个的下载链接改为分块的下载链接
     * @return: java.net.HttpURLConnection
     */

    public static HttpURLConnection getHttpURLConnection(String url, long startPos, long endPos) throws IOException {
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        LogUtils.info("下载的区间是：{}-{}", startPos, endPos);
        if (endPos != 0) {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-" + endPos);
        } else {
            // 下载的是最后一块区域
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-");
        }
        return httpURLConnection;
    }

    /**
     * @param url:文件的地址
     * @Description: 获取HttpURLConnection链接对象
     * @return: java.net.HttpURLConnection
     */

    public static HttpURLConnection getHttpURLConnection(String url) throws IOException {
        URL httpURL = new URL(url);
        // httpURL.openConnection()返回的是URLConnection（父类），强制类型转换为HttpURLConnection（子类）
        HttpURLConnection httpConnection = (HttpURLConnection) httpURL.openConnection(); // 向下造型，利用url创建http链接
        // 设置链接的用户标识
        httpConnection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1");
        return httpConnection;
    }

    /**
     * @param url:
     * @Description: 获取下载文件的文件名
     * @return: java.lang.String
     */

    public static String getHttpFileName(String url) {
        int index = url.lastIndexOf("/");
        return url.substring(index + 1);
    }
}
