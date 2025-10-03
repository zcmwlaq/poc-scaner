package com.pocscanner.core;

import com.pocscanner.core.model.POCConfig;
import com.pocscanner.core.model.ScanRequest;
import com.pocscanner.core.model.ScanResult;
import com.pocscanner.core.model.VulnerabilityLevel;
import com.pocscanner.http.HttpClient;
import com.pocscanner.http.HttpResponse;

import java.io.IOException;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class POCEngine {
    private HttpClient httpClient;
    private int timeout = 10000; // 默认超时时间10秒

    public POCEngine() {
        this.httpClient = new HttpClient();
    }

    public void setProxy(String host, int port) {
        httpClient.setProxy(host, port);
    }
    
    public void setProxy(String host, int port, String proxyType) {
        httpClient.setProxy(host, port, proxyType);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
        httpClient.setTimeout(timeout);
    }

    public ScanResult execute(POCConfig poc, String target) {
        ScanResult result = new ScanResult();
        result.setPocName(poc.getName());
        result.setTarget(target);
        result.setLevel(poc.getLevel() != null ? poc.getLevel().toString() : VulnerabilityLevel.Unknown.toString());

        try {
            // 构建完整URL
            String fullUrl = buildFullUrl(target, poc.getRequest().getPath(), poc.getRequest().getParams());
            
            // 准备请求头
            Map<String, String> headers = new HashMap<>();
            if (poc.getRequest().getHeaders() != null) {
                headers.putAll(poc.getRequest().getHeaders());
            }

            // 准备请求体
            String body = poc.getRequest().getBody();
            
            // 记录请求信息
            result.setRequestMethod(poc.getRequest().getMethod());
            result.setRequestUrl(fullUrl);
            result.setRequestPath(poc.getRequest().getPath());
            result.setRequestParams(poc.getRequest().getParams());
            result.setRequestHeaders(headers);
            result.setRequestBody(body);
            
            // 发送请求并获取响应
            HttpResponse response = httpClient.sendRequest(
                poc.getRequest().getMethod(), 
                fullUrl, 
                headers, 
                body
            );
            
            // 收集响应信息
            result.setResponseHeaders(convertHeaders(response.getHeaders()));
            result.setResponseBody(response.getBody());
            result.setStatusCode(String.valueOf(response.getStatusCode()));
            result.setResponseTime(response.getResponseTime());
            
            // 收集SSL/TLS信息（如果是HTTPS请求）
            if (fullUrl.startsWith("https://")) {
                result.setSslProtocol(response.getSslProtocol());
                result.setCipherSuite(response.getCipherSuite());
                result.setSslVerified(response.isSslVerified());
                result.setSslSubject(response.getSslSubject());
                result.setSslIssuer(response.getSslIssuer());
                result.setSslValidFrom(response.getSslValidFrom());
                result.setSslValidTo(response.getSslValidTo());
            }
            
            // 检查漏洞
            boolean isVulnerable = checkVulnerability(poc, response);
            result.setVulnerable(isVulnerable);
            
            if (isVulnerable) {
                result.setEvidence("Matched vulnerability pattern");
            }
            
        } catch (Exception e) {
            result.setVulnerable(false);
            result.setEvidence("Error: " + e.getMessage());
        }
        
        return result;
    }

    // 将HttpResponse的headers转换为ScanResult需要的格式
    private Map<String, String> convertHeaders(Map<String, List<String>> headers) {
        Map<String, String> result = new HashMap<>();
        if (headers == null) {
            return result;
        }

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                // 对于多值头，用逗号分隔
                result.put(entry.getKey(), String.join(", ", entry.getValue()));
            }
        }

        return result;
    }

    private String buildFullUrl(String target, String path, Map<String, String> params) {
        try {
            // 确保目标URL以http://或https://开头
            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                target = "http://" + target;
            }
            
            // 移除目标URL末尾的斜杠
            if (target.endsWith("/")) {
                target = target.substring(0, target.length() - 1);
            }
            
            // 处理路径
            if (path == null || path.isEmpty()) {
                path = "/";
            } else if (!path.startsWith("/")) {
                path = "/" + path;
            }
            
            // 构建URL
            String fullUrl = target + path;
            
            // 添加查询参数
            if (params != null && !params.isEmpty()) {
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }
                    
                    try {
                        // 对参数名和值进行URL编码
                        String encodedKey = java.net.URLEncoder.encode(entry.getKey(), "UTF-8");
                        String encodedValue = java.net.URLEncoder.encode(entry.getValue(), "UTF-8");
                        queryString.append(encodedKey).append("=").append(encodedValue);
                    } catch (Exception e) {
                        // 编码失败，使用原始值
                        queryString.append(entry.getKey()).append("=").append(entry.getValue());
                    }
                }
                fullUrl += "?" + queryString.toString();
            }
            
            return fullUrl;
        } catch (Exception e) {
            System.err.println("构建URL失败: " + e.getMessage());
            return target + (path != null ? path : "");
        }
    }

    private boolean checkVulnerability(POCConfig poc, HttpResponse response) {
    // 检查状态码
    if (poc.getResponse().getStatusCode() != null && response.getStatusCode() != poc.getResponse().getStatusCode()) {
        return false;
    }

    // 检查成功指示器
    if (poc.getResponse().getSuccessIndicators() != null && !poc.getResponse().getSuccessIndicators().isEmpty()) {
        boolean hasSuccessIndicator = false;
        for (String indicator : poc.getResponse().getSuccessIndicators()) {
            if (matchIndicator(response.getBody(), indicator, poc.getResponse().getMatchType())) {
                hasSuccessIndicator = true;
                break;
            }
        }
        if (!hasSuccessIndicator) {
            return false;
        }
    }

    // 检查错误指示器
    if (poc.getResponse().getErrorIndicators() != null && !poc.getResponse().getErrorIndicators().isEmpty()) {
        for (String indicator : poc.getResponse().getErrorIndicators()) {
            if (matchIndicator(response.getBody(), indicator, poc.getResponse().getMatchType())) {
                return false;
            }
        }
    }

    return true;
}

    private boolean matchIndicator(String responseBody, String indicator, String matchType) {
        if (responseBody == null || indicator == null) {
            return false;
        }

        switch (matchType.toLowerCase()) {
            case "equals":
                return responseBody.equals(indicator);
            case "contains":
                return responseBody.contains(indicator);
            case "regex":
                try {
                    return responseBody.matches(indicator);
                } catch (Exception e) {
                    System.err.println("正则表达式匹配失败: " + e.getMessage());
                    return false;
                }
            default:
                // 默认使用contains匹配
                return responseBody.contains(indicator);
        }
    }
}