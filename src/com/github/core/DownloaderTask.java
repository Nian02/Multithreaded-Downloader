package com.github.core;

import com.github.constant.Constant;
import com.github.util.HttpUtils;
import com.github.util.LogUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * 分块下载任务
 */
public class DownloaderTask implements Callable<Boolean> {

    private String url;
    private long startPos;
    private long endPos;
    // 标识当前是哪一部分
    private int part;
    private CountDownLatch countDownLatch;

    /**
     * @param url:      下载文件地址
     * @param startPos: 下载文件起始位置
     * @param endPos:   下载文件结束位置
     * @param part:     下载文件属于哪一部分
     * @Description: 该线程运行时，获取分块下载的链接，并写入指定下载路径的分块文件里
     * @return:
     */

    public DownloaderTask(String url, long startPos, long endPos, int part, CountDownLatch countDownLatch) {
        this.url = url;
        this.startPos = startPos;
        this.endPos = endPos;
        this.part = part;
        this.countDownLatch = countDownLatch;
    }


    @Override
    public Boolean call() throws Exception {
        // 获取文件名
        String httpFileName = HttpUtils.getHttpFileName(url);
        // 分块的文件名
        httpFileName = httpFileName + ".temp" + part;
        // 下载路径
        httpFileName = Constant.PATH + httpFileName;
        // 获取分块下载的链接（此时会打印该链接的线程信息）
        HttpURLConnection httpURLConnection = HttpUtils.getHttpURLConnection(url, startPos, endPos);

        try (
                InputStream input = httpURLConnection.getInputStream(); // 输入流用于从网络获取数据
                BufferedInputStream bis = new BufferedInputStream(input); // 缓冲输入流
                RandomAccessFile accessFile = new RandomAccessFile(httpFileName, "rw"); // 实现断点下载
        ) {
            byte[] buffer = new byte[Constant.BYTE_SIZE];
            int len = -1;
            // 循环读取数据
            while ((len = bis.read(buffer)) != -1) {
                // 1s内下载数据之和，通过原子类进行操作
                DownloadInfoThread.downSize.add(len);
                accessFile.write(buffer, 0, len); // 写到文件里（硬盘）

            }
        } catch (FileNotFoundException e) {
            LogUtils.error("下载文件不存在", url);
            return false;
        } catch (Exception e) {
            LogUtils.error("下载出现异常");
            return false;
        } finally {
            httpURLConnection.disconnect();
            // 每次线程执行任务结束都会使得countDownLatch减一
            countDownLatch.countDown();
        }
        return true;
    }
}
