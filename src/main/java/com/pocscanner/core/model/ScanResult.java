package com.pocscanner.core.model;

import java.util.Map;

public class ScanResult {
    private String pocName;
    private boolean vulnerable;
    private long responseTime;
    private String statusCode;
    private String evidence;
    private String target;
    private String level;
    private String requestMethod;
    private String requestUrl;
    private String requestPath;
    private Map<String, String> requestParams;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private Map<String, String> responseHeaders;
    private String responseBody;
    // SSL/TLS相关信息
    private String sslProtocol;
    private String cipherSuite;
    private boolean sslVerified;
    private String sslSubject;
    private String sslIssuer;
    private String sslValidFrom;
    private String sslValidTo;

    public ScanResult() {
    }

    public ScanResult(String pocName, boolean vulnerable, long responseTime, String statusCode, 
                      Map<String, String> responseHeaders, String responseBody) {
        this.pocName = pocName;
        this.vulnerable = vulnerable;
        this.responseTime = responseTime;
        this.statusCode = statusCode;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    // Getters and Setters
    public String getPocName() {
        return pocName;
    }

    public void setPocName(String pocName) {
        this.pocName = pocName;
    }

    public boolean isVulnerable() {
        return vulnerable;
    }

    public void setVulnerable(boolean vulnerable) {
        this.vulnerable = vulnerable;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(Map<String, String> requestParams) {
        this.requestParams = requestParams;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    // SSL/TLS相关信息的Getter和Setter
    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public String getCipherSuite() {
        return cipherSuite;
    }

    public void setCipherSuite(String cipherSuite) {
        this.cipherSuite = cipherSuite;
    }

    public boolean isSslVerified() {
        return sslVerified;
    }

    public void setSslVerified(boolean sslVerified) {
        this.sslVerified = sslVerified;
    }

    public String getSslSubject() {
        return sslSubject;
    }

    public void setSslSubject(String sslSubject) {
        this.sslSubject = sslSubject;
    }

    public String getSslIssuer() {
        return sslIssuer;
    }

    public void setSslIssuer(String sslIssuer) {
        this.sslIssuer = sslIssuer;
    }

    public String getSslValidFrom() {
        return sslValidFrom;
    }

    public void setSslValidFrom(String sslValidFrom) {
        this.sslValidFrom = sslValidFrom;
    }

    public String getSslValidTo() {
        return sslValidTo;
    }

    public void setSslValidTo(String sslValidTo) {
        this.sslValidTo = sslValidTo;
    }
}