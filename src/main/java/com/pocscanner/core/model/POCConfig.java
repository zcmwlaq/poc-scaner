package com.pocscanner.core.model;

import java.util.List;
import java.util.Map;

public class POCConfig {
    private String name;
    private String description;
    private String level;
    private String author;
    private Request request;
    private Response response;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Request getRequest() { return request; }
    public void setRequest(Request request) { this.request = request; }

    public Response getResponse() { return response; }
    public void setResponse(Response response) { this.response = response; }

    public static class Request {
        private String method;
        private String path;
        private Map<String, String> headers;
        private String body;
        private Map<String, String> params;

        // Getters and Setters
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }

        public Map<String, String> getParams() { return params; }
        public void setParams(Map<String, String> params) { this.params = params; }
    }

    public static class Response {
        private List<String> successIndicators;
        private List<String> errorIndicators;
        private Integer statusCode;
        private String matchType;  // "contains"、"equals" 或 "regex"

        // Getters and Setters
        public List<String> getSuccessIndicators() { return successIndicators; }
        public void setSuccessIndicators(List<String> successIndicators) { this.successIndicators = successIndicators; }

        public List<String> getErrorIndicators() { return errorIndicators; }
        public void setErrorIndicators(List<String> errorIndicators) { this.errorIndicators = errorIndicators; }

        public Integer getStatusCode() { return statusCode; }
        public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

        public String getMatchType() { return matchType != null ? matchType : "contains"; }
        public void setMatchType(String matchType) { this.matchType = matchType; }
    }
}