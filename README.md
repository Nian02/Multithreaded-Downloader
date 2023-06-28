# Multithreaded-Downloader

这是一个对动力节点的多线程下载器的学习项目，使用Java语言开发，知识点设计Java IO、集合、异常等Java se的知识，并可以更进一步了解多线程的知识，适合刚学习完Java se的群体学习使用。

## 上手指南

### 安装环境

1. 开发工具：IntelliJ IDEA
2. JDK版本：17
3. 项目编码：UTF-8

### 安装步骤

从GitHub中部署项目到本地

1. 在IDEA中选中file-->new--> Project from Version Conrtrol
2. 填写需要导入的github链接

## 运行

在Main方法中直接运行，在控制台中输入想要下载的文件链接（比如.exe文件），此时下载器会将待下载的文件拆分为多个文件（本项目设置为5，可以在Constant类下自行修改），并通过线程池启用多个线程进行下载。同时控制台会通过定时线程池打印下载文件的信息。

## 项目概述

在此对项目的结构以及采用的多线程的知识作一个简要的介绍：

- Main：主类，传入url启动多线程下载器Downloader类

- constant：存放常量类的包
	- Constant：存放文件下载路径、启用线程数等常量

- core：存放了下载器核心类的包
	- Downloader：Downloader会根据url地址进行文件的下载。
		1. 在Downloader类中首先定义了两个线程池
		2. 判断文件是否下载过（比较已下载文件的大小和要下载的文件的总大小）
		3. 定长打印下载文件信息（线程池：`ScheduledExecutorService` + 线程：`DownloadInfoThread`）
		4. 文件切分下载spilt（线程池：`ThreadPoolExecutor` + 线程：`DownloaderTask`）
		5. 合并文件merge：`RandomAccessFile`：支持以随机访问的方式读取或写入文件中的数据。
	- DownloadInfoThread：实现`Runnable`类型的接口，该线程运行时，打印当前文件下载信息
	- DownloaderTask：实现`Callable<Boolean>`类型的接口，该线程运行时，获取分块下载的链接（`getHttpURLConnection(String url, long startPos, long endPos)`），并写入指定下载路径的分块文件里。

- util：存放工具类的包
	- FileUtils：文件工具类
		- `getFileContentLength(String path)`：传入Path地址，获取在path路径下本地文件的大小
	- HttpUtils：网络工具类
		- `getHttpFileName(String url)`：获取下载文件的文件名
		- `getHttpURLConnection(String url)`：获取HttpURLConnection链接对象
		- `getHTTPFileContentLength(String url)`：获取对应链接下的文件大小（调用http链接的方法`getContentLength()`）
		- `getHttpURLConnection(String url, long startPos, long endPos)`：根据传入的文件起始位置和结束位置获取分块的下载链接（调用http链接的方法`setRequestProperty`），**并打印当前下载的区间**
	- LogUtils：打印工具类，包括普通输出info和异常输出error
		- 输出当前时间、线程名、实现输入字符串的拼接（对”{}“的替换）

### 线程池

- `ScheduledExecutorService`：创建一个容量为1的定长线程池，支持定时及周期性任务执行，用于定时打印文件下载信息。
- `ThreadPoolExecutor`：创建一个容量为5个核心线程的线程池，用于执行多线程下载，采用ArrayBlockingQueue数组阻塞队列，阻塞队列的容量为5。

>当有核心线程空闲时，任务交给核心线程执行，若核心线程已满则任务进入阻塞队列进行等待，等到核心线程空闲时再取出。

### 同步机制

`CountDownLatch.await()`方法使线程等待直到计数达到零。

### Http链接

使用`java.net.HttpURLConnection`类进行Http链接。

```java
public static HttpURLConnection getHttpURLConnection(String url) throws IOException {  
	URL httpURL = new URL(url);  
	// httpURL.openConnection()返回的是URLConnection（父类），强制类型转换为HttpURLConnection（子类）  
	HttpURLConnection httpConnection = (HttpURLConnection) httpURL.openConnection(); // 向下造型，利用url创建http链接  
	// 设置链接的用户标识  
	httpConnection.setRequestProperty("User-Agent",  
"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1");  
	return httpConnection;  
}
```

### 原子类LongAdder

在打印文件下载信息时，因为采用了多线程的方式进行下载，所以在统计1s内下载数据之和的时候需要使用到LongAdder类。

LongAdder类提供了一种高效的方式来进行**多线程并发计数**，特别适用于高度竞争的计数场景。以下是`LongAdder`的一些主要方法：

- `void add(long x)`：将给定的值`x`添加到计数器中。
-  `void increment()`：将计数器的值增加1。
-  `void decrement()`：将计数器的值减少1。
-  `long sum()`：返回当前计数器的总和。
-  `void reset()`：将计数器的值重置为0。

`LongAdder`的设计允许多个线程同时更新不同的内部计数器，从而减少了线程之间的争用。在**计算总和时，它会将所有内部计数器的值累加起来，提供一个准确的总和**。在本项目中的DownloadTask类中，运行该线程时会获取分块下载的链接并进行分块下载，分块下载的过程如下：

```java
// 循环读取数据  
while ((len = bis.read(buffer)) != -1) {  
	// 1s内下载数据之和，通过原子类进行操作  
	DownloadInfoThread.downSize.add(len);  
	accessFile.write(buffer, 0, len); // 写到文件里（硬盘）  
  
}
```



