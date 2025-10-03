package com.pocscanner.gui;

import com.pocscanner.core.model.ScanResult;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class ResultPanel extends JPanel {
    private JTable resultTable;
    private ResultTableModel tableModel;
    private JTabbedPane detailTabbedPane;
    private JTextArea requestHeadersArea;
    private JTextArea responseHeadersArea;
    private JTextArea pocArea;

    public ResultPanel() {
        setLayout(new BorderLayout());
        
        // 初始化表格模型
        tableModel = new ResultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 设置表格列宽
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(150); // POC名称
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // 漏洞
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(120); // 响应时间
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 状态码
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(300); // POC请求/响应
        
        // 添加表格选择监听器
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = resultTable.getSelectedRow();
                if (selectedRow >= 0) {
                    ScanResult result = tableModel.getResultAt(selectedRow);
                    displayResultDetails(result);
                }
            }
        });
        
        // 添加鼠标悬停事件监听器
        resultTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = resultTable.rowAtPoint(e.getPoint());
                int col = resultTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    ScanResult result = tableModel.getResultAt(row);
                    // 创建工具提示信息
                    StringBuilder tooltip = new StringBuilder();
                    tooltip.append("<html><b>POC名称:</b> ").append(result.getPocName()).append("<br>");
                    tooltip.append("<b>目标:</b> ").append(result.getTarget()).append("<br>");
                    tooltip.append("<b>漏洞:</b> ").append(result.isVulnerable() ? "是" : "否").append("<br>");
                    tooltip.append("<b>响应时间:</b> ").append(result.getResponseTime()).append(" ms<br>");
                    tooltip.append("<b>状态码:</b> ").append(result.getStatusCode()).append("<br>");
                    tooltip.append("<b>等级:</b> ").append(result.getLevel() != null ? result.getLevel() : "未知");
                    tooltip.append("</html>");
                    
                    resultTable.setToolTipText(tooltip.toString());
                } else {
                    resultTable.setToolTipText(null);
                }
            }
        });
        
        // 初始化详细信息面板
        initDetailPanel();
        
        // 创建上下分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(new JScrollPane(resultTable));
        splitPane.setBottomComponent(detailTabbedPane);
        splitPane.setDividerLocation(300);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    private void initDetailPanel() {
        // 创建左右分割面板
        JSplitPane requestResponseSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // 创建左侧请求面板
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder("请求"));
        
        // 创建右侧响应面板
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("响应"));
        
        // 初始化请求相关组件
        requestHeadersArea = createTextArea();
        
        requestPanel.add(new JScrollPane(requestHeadersArea), BorderLayout.CENTER);
        
        // 初始化响应相关组件
        responseHeadersArea = createTextArea();
        
        responsePanel.add(new JScrollPane(responseHeadersArea), BorderLayout.CENTER);
        
        // 设置左右分割面板
        requestResponseSplitPane.setLeftComponent(requestPanel);
        requestResponseSplitPane.setRightComponent(responsePanel);
        requestResponseSplitPane.setDividerLocation(400);
        
        // 创建POC面板
        JPanel pocPanel = new JPanel(new BorderLayout());
        pocPanel.setBorder(BorderFactory.createTitledBorder("POC详细信息"));
        
        // 初始化POC相关组件
        pocArea = createTextArea();
        
        pocPanel.add(new JScrollPane(pocArea), BorderLayout.CENTER);
        
        // 创建底部选项卡面板
        detailTabbedPane = new JTabbedPane();
        detailTabbedPane.addTab("请求/响应", requestResponseSplitPane);
        detailTabbedPane.addTab("POC", pocPanel);
    }
    
    private JTextArea createTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setComponentPopupMenu(createCopyPopupMenu());
        
        return textArea;
    }

    public void displayResults(java.util.List<ScanResult> results) {
        tableModel.setResults(results);
    }

    private void displayResultDetails(ScanResult result) {
        // 构建请求信息
        StringBuilder requestInfo = new StringBuilder();
        String requestMethod = result.getRequestMethod() != null ? result.getRequestMethod() : "GET";
        String targetUrl = result.getTarget() != null ? result.getTarget() : "";
        String requestPath = result.getRequestPath() != null ? result.getRequestPath() : "";
        
        // 添加请求行（按照浏览器F12格式）
        requestInfo.append(requestMethod).append(" ").append(requestPath).append(" HTTP/1.1\n");
        
        // 添加Host头
        if (result.getRequestHeaders() != null && result.getRequestHeaders().containsKey("Host")) {
            requestInfo.append("Host: ").append(result.getRequestHeaders().get("Host")).append("\n");
        } else {
            // 从URL中提取主机名
            String host = extractHostFromUrl(targetUrl);
            requestInfo.append("Host: ").append(host).append("\n");
        }
        
        // 添加其他请求头
        if (result.getRequestHeaders() != null && !result.getRequestHeaders().isEmpty()) {
            for (Map.Entry<String, String> entry : result.getRequestHeaders().entrySet()) {
                if (!"Host".equals(entry.getKey())) {
                    requestInfo.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
        }
        
        // 添加空行分隔请求头和请求体
        requestInfo.append("\n");
        
        // 显示请求体
        String requestBody = result.getRequestBody();
        if (requestBody != null && !requestBody.isEmpty()) {
            if (isJsonContent(requestBody)) {
                requestInfo.append(formatJson(requestBody));
            } else if (isHtmlContent(requestBody)) {
                requestInfo.append(formatHtml(requestBody));
            } else {
                requestInfo.append(requestBody);
            }
        }
        
        requestHeadersArea.setText(requestInfo.toString());
        
        // 构建响应信息
        StringBuilder responseInfo = new StringBuilder();
        String statusCodeStr = result.getStatusCode();
        String statusCode = statusCodeStr != null && !statusCodeStr.isEmpty() ? statusCodeStr : "";
        int statusCodeInt = 0;
        try {
            statusCodeInt = Integer.parseInt(statusCodeStr);
        } catch (NumberFormatException e) {
            // 解析失败，使用默认值0
        }
        String statusText = getStatusText(statusCodeInt);
        
        // 添加响应行
        responseInfo.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\n");
        
        // 显示响应头
        if (result.getResponseHeaders() != null && !result.getResponseHeaders().isEmpty()) {
            for (Map.Entry<String, String> entry : result.getResponseHeaders().entrySet()) {
                responseInfo.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        // 添加空行分隔响应头和响应体
        responseInfo.append("\n");
        
        // 显示响应体
        String responseBody = result.getResponseBody();
        if (responseBody != null && !responseBody.isEmpty()) {
            if (isJsonContent(responseBody)) {
                responseInfo.append(formatJson(responseBody));
            } else if (isHtmlContent(responseBody)) {
                responseInfo.append(formatHtml(responseBody));
            } else {
                responseInfo.append(responseBody);
            }
        }
        
        responseHeadersArea.setText(responseInfo.toString());
        
        // 构建POC信息
        StringBuilder pocInfo = new StringBuilder();
        
        // 添加POC名称
        pocInfo.append("POC名称: ").append(result.getPocName() != null ? result.getPocName() : "未知").append("\n");
        
        // 添加漏洞等级
        pocInfo.append("漏洞等级: ").append(result.getLevel() != null ? result.getLevel() : "未知").append("\n");
        
        // 添加漏洞状态
        pocInfo.append("漏洞状态: ").append(result.isVulnerable() ? "存在漏洞" : "无漏洞").append("\n");
        
        // 添加证据
        if (result.getEvidence() != null && !result.getEvidence().isEmpty()) {
            pocInfo.append("证据: ").append(result.getEvidence()).append("\n");
        }
        
        // 添加响应时间
        pocInfo.append("响应时间: ").append(result.getResponseTime()).append(" ms\n");
        
        // 添加请求摘要
        pocInfo.append("\n请求摘要:\n");
        pocInfo.append("  ").append(requestMethod).append(" ").append(requestPath).append("\n");
        
        // 添加请求参数
        if (result.getRequestParams() != null && !result.getRequestParams().isEmpty()) {
            pocInfo.append("  参数: ");
            boolean first = true;
            for (Map.Entry<String, String> entry : result.getRequestParams().entrySet()) {
                if (!first) {
                    pocInfo.append(", ");
                }
                pocInfo.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            pocInfo.append("\n");
        }
        
        // 添加响应摘要
        pocInfo.append("\n响应摘要:\n");
        pocInfo.append("  状态码: ").append(result.getStatusCode() != null ? result.getStatusCode() : "N/A").append("\n");
        
        // 添加响应内容摘要
        if (responseBody != null && !responseBody.isEmpty()) {
            // 截取前100个字符作为摘要
            String summary = responseBody.length() > 100 ? responseBody.substring(0, 100) + "..." : responseBody;
            pocInfo.append("  响应内容: ").append(summary).append("\n");
        }
        
        pocArea.setText(pocInfo.toString());
    }
    
    // 从URL中提取主机名
    private String extractHostFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "www.deepseek.com";
        }
        
        try {
            // 移除协议部分
            String host = url;
            if (host.startsWith("http://")) {
                host = host.substring(7);
            } else if (host.startsWith("https://")) {
                host = host.substring(8);
            }
            
            // 移除路径部分
            int pathIndex = host.indexOf('/');
            if (pathIndex > 0) {
                host = host.substring(0, pathIndex);
            }
            
            // 移除端口部分
            int portIndex = host.indexOf(':');
            if (portIndex > 0) {
                host = host.substring(0, portIndex);
            }
            
            return host;
        } catch (Exception e) {
            return "www.deepseek.com";
        }
    }
    
    // 根据状态码获取状态文本
    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 100: return "Continue";
            case 101: return "Switching Protocols";
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 203: return "Non-Authoritative Information";
            case 204: return "No Content";
            case 205: return "Reset Content";
            case 206: return "Partial Content";
            case 300: return "Multiple Choices";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 303: return "See Other";
            case 304: return "Not Modified";
            case 305: return "Use Proxy";
            case 307: return "Temporary Redirect";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 407: return "Proxy Authentication Required";
            case 408: return "Request Timeout";
            case 409: return "Conflict";
            case 410: return "Gone";
            case 411: return "Length Required";
            case 412: return "Precondition Failed";
            case 413: return "Request Entity Too Large";
            case 414: return "Request-URI Too Long";
            case 415: return "Unsupported Media Type";
            case 416: return "Requested Range Not Satisfiable";
            case 417: return "Expectation Failed";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "HTTP Version Not Supported";
            default: return "Unknown Status";
        }
    }
    
    // 判断内容是否为JSON
    private boolean isJsonContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || 
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
    
    // 判断内容是否为HTML
    private boolean isHtmlContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        String trimmed = content.trim().toLowerCase();
        return trimmed.startsWith("<html") || trimmed.startsWith("<!doctype html") || 
               trimmed.startsWith("<!doctype") || trimmed.startsWith("<");
    }
    
    // 简单的JSON格式化
    private String formatJson(String json) {
        try {
            StringBuilder formatted = new StringBuilder();
            int indentLevel = 0;
            boolean inString = false;
            
            for (char c : json.toCharArray()) {
                switch (c) {
                    case '{':
                    case '[':
                        formatted.append(c);
                        if (!inString) {
                            indentLevel++;
                            formatted.append('\n');
                            appendIndent(formatted, indentLevel);
                        }
                        break;
                    case '}':
                    case ']':
                        if (!inString) {
                            indentLevel--;
                            formatted.append('\n');
                            appendIndent(formatted, indentLevel);
                        }
                        formatted.append(c);
                        break;
                    case ',':
                        formatted.append(c);
                        if (!inString) {
                            formatted.append('\n');
                            appendIndent(formatted, indentLevel);
                        }
                        break;
                    case '"':
                        formatted.append(c);
                        inString = !inString;
                        break;
                    case '\\':
                        formatted.append(c);
                        if (inString) {
                            // 处理转义字符
                            if (json.indexOf(c) < json.length() - 1) {
                                formatted.append(json.charAt(json.indexOf(c) + 1));
                            }
                        }
                        break;
                    default:
                        formatted.append(c);
                        break;
                }
            }
            
            return formatted.toString();
        } catch (Exception e) {
            // 格式化失败，返回原始内容
            return json;
        }
    }
    
    // 添加缩进
    private void appendIndent(StringBuilder sb, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            sb.append("  ");
        }
    }
    
    // 简单的HTML格式化
    private String formatHtml(String html) {
        try {
            StringBuilder formatted = new StringBuilder();
            int indentLevel = 0;
            boolean inTag = false;
            boolean inComment = false;
            
            for (int i = 0; i < html.length(); i++) {
                char c = html.charAt(i);
                
                // 处理注释
                if (!inComment && i + 3 < html.length() && c == '<' && 
                    html.charAt(i + 1) == '!' && html.charAt(i + 2) == '-' && html.charAt(i + 3) == '-') {
                    inComment = true;
                    formatted.append("<!--");
                    i += 3;
                    continue;
                }
                
                if (inComment && i + 2 < html.length() && c == '-' && 
                    html.charAt(i + 1) == '-' && html.charAt(i + 2) == '>') {
                    inComment = false;
                    formatted.append("-->");
                    i += 2;
                    continue;
                }
                
                if (inComment) {
                    formatted.append(c);
                    continue;
                }
                
                // 处理标签
                if (c == '<') {
                    inTag = true;
                    // 检查是否是结束标签
                    if (i + 1 < html.length() && html.charAt(i + 1) == '/') {
                        // 结束标签，减少缩进
                        if (indentLevel > 0) {
                            indentLevel--;
                        }
                        formatted.append("\n");
                        appendIndent(formatted, indentLevel);
                    } else if (indentLevel > 0) {
                        // 开始标签，增加缩进（除了自闭合标签）
                        formatted.append("\n");
                        appendIndent(formatted, indentLevel);
                    }
                    formatted.append(c);
                } else if (c == '>') {
                    formatted.append(c);
                    inTag = false;
                    // 检查是否是自闭合标签
                    if (i > 0 && html.charAt(i - 1) == '/') {
                        // 自闭合标签，不增加缩进
                    } else if (i > 0 && !Character.isWhitespace(html.charAt(i - 1))) {
                        // 不是自闭合标签，增加缩进
                        indentLevel++;
                    }
                } else {
                    formatted.append(c);
                }
            }
            
            return formatted.toString();
        } catch (Exception e) {
            // 格式化失败，返回原始内容
            return html;
        }
    }
    
    private JPopupMenu createCopyPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem copyItem = new JMenuItem("复制");
        copyItem.addActionListener(e -> {
            JTextArea textArea = (JTextArea) menu.getInvoker();
            String selectedText = textArea.getSelectedText();
            if (selectedText != null && !selectedText.isEmpty()) {
                // 复制选中的文本
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(selectedText), null);
            } else {
                // 复制全部文本
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(textArea.getText()), null);
            }
        });
        menu.add(copyItem);
        
        return menu;
    }
    
    // 自定义表格模型
    private static class ResultTableModel extends DefaultTableModel {
        private java.util.List<ScanResult> results = new java.util.ArrayList<>();
        private String[] columnNames = {"POC名称", "漏洞", "响应时间(ms)", "状态码", "POC请求/响应"};
        
        public ResultTableModel() {
            // 使用空的构造函数
            super();
            // 手动设置列名
            setColumnIdentifiers(columnNames);
        }
        
        public void setResults(java.util.List<ScanResult> results) {
            this.results = results;
            fireTableDataChanged();
        }
        
        public ScanResult getResultAt(int row) {
            return results.get(row);
        }
        
        @Override
        public int getRowCount() {
            return results == null ? 0 : results.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            ScanResult result = results.get(row);
            switch (column) {
                case 0: return result.getPocName();
                case 1: return result.isVulnerable() ? "是" : "否";
                case 2: return result.getResponseTime();
                case 3: return result.getStatusCode();
                case 4: return generatePocSummary(result);
                default: return "";
            }
        }
        
        // 生成POC请求/响应摘要
        private String generatePocSummary(ScanResult result) {
            StringBuilder summary = new StringBuilder();
            
            // 添加请求方法
            String requestMethod = result.getRequestMethod() != null ? result.getRequestMethod() : "GET";
            summary.append(requestMethod).append(" ");
            
            // 添加请求路径
            String requestPath = result.getRequestPath() != null ? result.getRequestPath() : "";
            summary.append(requestPath).append(" ");
            
            // 添加状态码
            summary.append("[").append(result.getStatusCode() != null ? result.getStatusCode() : "N/A").append("]");
            
            return summary.toString();
        }
        
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // 表格不可编辑
        }
    }
}