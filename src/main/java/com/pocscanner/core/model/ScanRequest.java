package com.pocscanner.core.model;

public class ScanRequest {
    private String target;
    private String selectedPOCs;
    private int threadCount;
    private int timeout;

    public ScanRequest() {
        this.threadCount = 10;
        this.timeout = 10000;
    }

    // Getters and Setters
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getSelectedPOCs() { return selectedPOCs; }
    public void setSelectedPOCs(String selectedPOCs) { this.selectedPOCs = selectedPOCs; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}