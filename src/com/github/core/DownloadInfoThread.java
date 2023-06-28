package com.github.core;

import com.github.constant.Constant;

import javax.swing.*;
import java.util.concurrent.atomic.LongAdder;

public class DownloadInfoThread implements Runnable {

    private long httpFileContentLength; // 文件总大小(单位是B)
    public static LongAdder finishedSize = new LongAdder(); // 本地已下载文件的大小
    public static volatile LongAdder downSize = new LongAdder(); // 用原子类本次累计下载的大小（本次下载到现在的内容）
    public double prevSize; // 前一次下载的大小（前一秒之前下载的内容）

    /**
     * @param httpFileContentLength: 文件总大小(单位是B)
     * @Description: 该线程运行时，打印当前文件下载信息
     * @return:
     */

    public DownloadInfoThread(long httpFileContentLength) {
        this.httpFileContentLength = httpFileContentLength;
    }

    @Override
    public void run() {
        // 计算文件总大小(单位：MB)
        String httpFileSize = String.format("%.2f", httpFileContentLength / Constant.MB);

        // 计算每秒下载速度(每秒执行一次、单位：KB)
        int speed = (int) ((downSize.doubleValue() - prevSize) / 1024d);
        prevSize = downSize.doubleValue();

        // 剩余文件的大小
        double remainingSize = httpFileContentLength - finishedSize.doubleValue() - downSize.doubleValue();

        // 计算剩余时间
        String remainTime = String.format("%.1f", remainingSize / 1024d / speed);
        if ("Infinity".equalsIgnoreCase(remainTime)) {
            remainTime = "-";
        }

        // 本次已下载大小
        String currentFileSize = String.format("%.2f", (downSize.doubleValue() - finishedSize.doubleValue()) / Constant.MB);

        // 打印
        String downInfo = String.format("已下载 %smb/%smb, 速度 %skb/s, 剩余时间 %ss",
                currentFileSize, httpFileSize, speed, remainTime);
        System.out.print("\r");
        System.out.print(downInfo);
    }
}
