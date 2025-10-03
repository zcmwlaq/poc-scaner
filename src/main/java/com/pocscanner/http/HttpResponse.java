package com.pocscanner.http;

import java.util.List;
import java.util.Map;

public class HttpResponse {
    private int statusCode;
    private String body;
    private Map<String, List<String>> headers;
    private long responseTime;
    // SSL/TLS相关信息
    private String sslProtocol;
    private String cipherSuite;
    private boolean sslVerified;
    private String sslSubject;
    private String sslIssuer;
    private String sslValidFrom;
    private String sslValidTo;

    public HttpResponse(int statusCode, String body, Map<String, List<String>> headers, long responseTime) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
        this.responseTime = responseTime;
    }

    // Getters and Setters
    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
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

    @Override
    public String toString() {
        return String.format("Status: %d, Body Length: %d, Response Time: %d ms, SSL: %s", 
                statusCode, body.length(), responseTime, sslProtocol != null ? sslProtocol : "No SSL");
    }
}