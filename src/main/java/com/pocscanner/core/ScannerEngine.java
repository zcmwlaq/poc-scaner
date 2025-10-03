package com.pocscanner.core;

import com.pocscanner.core.model.POCConfig;
import com.pocscanner.core.model.ScanRequest;
import com.pocscanner.core.model.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ScannerEngine {
    private POCLoader pocLoader;
    private ExecutorService executorService;
    private POCEngine pocEngine;
    private List<ScanResult> scanResults;

    public ScannerEngine(String pocDirectory) {
        this.pocLoader = new POCLoader();
        this.pocEngine = new POCEngine();
        this.executorService = Executors.newFixedThreadPool(10); // 默认10个线程
        this.scanResults = new ArrayList<>();
    }

    public void setThreadCount(int threadCount) {
        // 重新创建线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        this.executorService = Executors.newFixedThreadPool(threadCount);
    }

    public void setProxy(String proxyHost, int proxyPort) {
        pocEngine.setProxy(proxyHost, proxyPort);
    }
    
    public void setProxy(String proxyHost, int proxyPort, String proxyType) {
        pocEngine.setProxy(proxyHost, proxyPort, proxyType);
    }

    public void setTimeout(int timeout) {
        pocEngine.setTimeout(timeout);
    }

    public List<ScanResult> scan(ScanRequest request, ScanListener listener) {
        // 清空之前的结果
        scanResults.clear();
        
        // 加载POC
        List<POCConfig> pocs = pocLoader.loadPOCsFromDirectory(request.getSelectedPOCs());
        if (pocs.isEmpty()) {
            listener.onLog("没有找到有效的POC文件");
            return scanResults;
        }
        
        listener.onLog("加载了 " + pocs.size() + " 个POC");
        
        // 创建任务列表
        List<Future<ScanResult>> futures = new ArrayList<>();
        
        // 提交扫描任务
        for (POCConfig poc : pocs) {
            Future<ScanResult> future = executorService.submit(() -> {
                listener.onLog("开始扫描: " + poc.getName());
                ScanResult result = pocEngine.execute(poc, request.getTarget());
                listener.onProgress(1);
                return result;
            });
            futures.add(future);
        }
        
        // 收集结果
        for (Future<ScanResult> future : futures) {
            try {
                ScanResult result = future.get();
                scanResults.add(result);
                listener.onResult(result);
            } catch (InterruptedException | ExecutionException e) {
                listener.onLog("扫描任务执行失败: " + e.getMessage());
            }
        }
        
        return scanResults;
    }

    public List<ScanResult> getScanResults() {
        return scanResults;
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public interface ScanListener {
        void onLog(String message);
        void onProgress(int progress);
        void onResult(com.pocscanner.core.model.ScanResult result);
    }
}