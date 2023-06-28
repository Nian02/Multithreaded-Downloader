package com.github.core;

import com.github.constant.Constant;
import com.github.util.FileUtils;
import com.github.util.HttpUtils;
import com.github.util.LogUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * 下载器
 */
public class Downloader {
    // 创建一个线程池，用于定时打印线程信息
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    // 创建一个线程池对象，用于执行多线程下载
    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(Constant.THREAD_NUM, Constant.THREAD_NUM, 0,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(5));

    // 创建CountDownLatch对象，确保所有线程执行完毕
    private CountDownLatch countDownLatch = new CountDownLatch(Constant.THREAD_NUM);

    /**
     * @param url: 下载文件地址
     * @Description: 根据url地址进行文件的下载
     * @return: void
     */

    public void download(String url) {

        // 获取下载文件的文件名
        String httpFileName = HttpUtils.getHttpFileName(url);

        // 获取下载地址
        httpFileName = Constant.PATH + httpFileName;

        // 获取已下载文件的大小
        long localContentLength = FileUtils.getFileContentLength(httpFileName);

        // 获取链接对象
        HttpURLConnection httpURLConnection = null;

        // 创建获取下载信息的线程
        DownloadInfoThread downloadInfoThread;
        try {
            httpURLConnection = HttpUtils.getHttpURLConnection(url);

            // 获取要下载的文件的总大小
            int contentLength = httpURLConnection.getContentLength();

            // 判断文件是否下载过
            if (localContentLength >= contentLength) {
                LogUtils.info("{}已经下载完毕，无需重新下载", httpFileName);
                return;
            }

            // 打印的线程信息，包括已下载文件大小、速度、剩余时间等
            downloadInfoThread = new DownloadInfoThread(contentLength);
            // 将任务交给线程执行，每隔1s执行一次
            scheduledExecutorService.scheduleAtFixedRate(downloadInfoThread, 2, 1, TimeUnit.SECONDS);

            // 切分任务
            ArrayList<Future> list = new ArrayList<>();
            // 切分的过程中会用DownloaderTask创建线程，此时会打印线程的名称和下载区间
            spilt(url, list);

            // 确保所有线程执行完毕（等到计数器归零，每个线程执行完任务会使得计数器减一）
            countDownLatch.await();

            // 合并文件
            if (merge(httpFileName)) {
                // 清除临时文件
                clearTemp(httpFileName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.print("\r");
            System.out.println("下载完成");

            // 关闭连接对象
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            // 关闭打印线程信息的线程池
            scheduledExecutorService.shutdownNow();

            // 关闭分块下载的线程池对象
            poolExecutor.shutdown();
        }
    }

    /**
     * @param url:        下载文件地址
     * @param futureList: Future类型且返回类型是Boolean的集合存储任务对象
     * @Description: 根据下载文件的大小进行文件切分
     * @return: void
     */

    public void spilt(String url, ArrayList<Future> futureList) {

        try {
            // 获取下载文件的大小
            long contentLength = HttpUtils.getHTTPFileContentLength(url);
            // 计算切分后的文件大小
            long size = contentLength / Constant.THREAD_NUM;
            // 计算分块个数
            for (int i = 0; i < Constant.THREAD_NUM; i++) {
                // 计算下载的起始位置
                long startPos = i * size;
                long endPos;
                if (i == Constant.THREAD_NUM - 1) {
                    // 下载最后一块，下载剩余部分
                    endPos = 0;
                } else {
                    endPos = startPos + size;
                }

                // 如果不是第一块，起始位置要+1
                if (startPos != 0) {
                    startPos++;
                }

                // 创建任务对象，每一次会创建一个线程用分块链接写入分块文件中
                DownloaderTask downloaderTask = new DownloaderTask(url, startPos, endPos, i, countDownLatch);

                // 将任务提交到线程池中，返回类型为Boolean
                Future<Boolean> future = poolExecutor.submit(downloaderTask);

                // 对象添加到集合中
                futureList.add(future);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param fileName: 总的文件名
     * @Description: 将临时的分块文件合并
     * @return: boolean
     */

    public boolean merge(String fileName) {
        LogUtils.info("开始合并文件{}", fileName);
        byte[] buffer = new byte[Constant.BYTE_SIZE];
        int len = -1;
        try (RandomAccessFile accessFile = new RandomAccessFile(fileName, "rw")) {
            for (int i = 0; i < Constant.THREAD_NUM; i++) {
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName + ".temp" + i))) {
                    while ((len = bis.read(buffer)) != -1) {
                        accessFile.write(buffer, 0, len);
                    }
                }
            }
            LogUtils.info("文件合并完毕{}", fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @param fileName: 总的文件名
     * @Description: 将临时的分块文件清空
     * @return: boolean
     */

    public boolean clearTemp(String fileName) {
        for (int i = 0; i < Constant.THREAD_NUM; i++) {
            File file = new File(fileName + ".temp" + i);
            file.delete();
        }
        return true;
    }
}
