package com.pocscanner.http;

import okhttp3.*;
import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class HttpClient {
    private int timeout = 10000; // 默认超时时间10秒
    private Proxy proxy = Proxy.NO_PROXY; // 默认不使用代理
    private Proxy.Type proxyType = Proxy.Type.HTTP; // 默认代理类型
    private boolean ignoreSSL = true; // 是否忽略SSL证书验证
    private OkHttpClient client;

    public HttpClient() {
        buildClient();
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
        buildClient();
    }

    public void setProxy(String proxyHost, int proxyPort) {
        setProxy(proxyHost, proxyPort, "http"); // 默认使用HTTP代理
    }
    
    public void setProxy(String proxyHost, int proxyPort, String proxyType) {
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            Proxy.Type type;
            switch (proxyType.toLowerCase()) {
                case "https":
                    type = Proxy.Type.HTTP; // HTTPS代理也使用HTTP类型
                    break;
                case "socket":
                case "socks":
                    type = Proxy.Type.SOCKS;
                    break;
                case "http":
                default:
                    type = Proxy.Type.HTTP;
                    break;
            }
            this.proxyType = type;
            this.proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
        } else {
            this.proxy = Proxy.NO_PROXY;
            this.proxyType = Proxy.Type.HTTP;
        }
        buildClient();
    }

    public void setIgnoreSSL(boolean ignoreSSL) {
        this.ignoreSSL = ignoreSSL;
        buildClient();
    }
    
    public String getProxyType() {
        if (proxy == Proxy.NO_PROXY) {
            return "none";
        }
        switch (proxyType) {
            case HTTP: return "http";
            case SOCKS: return "socks";
            default: return "unknown";
        }
    }

    private void buildClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS);

        // 设置代理
        if (proxy != Proxy.NO_PROXY) {
            builder.proxy(proxy);
        }

        // 配置SSL证书验证（如果需要忽略SSL验证）
        if (ignoreSSL) {
            configureIgnoreSSL(builder);
        }

        this.client = builder.build();
    }

    public HttpResponse sendRequest(String method, String url, Map<String, String> headers, String body) throws IOException {
        long startTime = System.currentTimeMillis();

        // 构建请求
        Request.Builder requestBuilder = new Request.Builder().url(url);

        // 设置请求方法
        switch (method.toUpperCase()) {
            case "POST":
                if (body != null && !body.isEmpty()) {
                    // 尝试从请求头中确定Content-Type
                    String contentType = "application/x-www-form-urlencoded"; // 默认值
                    if (headers != null && headers.containsKey("Content-Type")) {
                        contentType = headers.get("Content-Type");
                    }
                    RequestBody requestBody = RequestBody.create(body, MediaType.parse(contentType));
                    requestBuilder.post(requestBody);
                } else {
                    requestBuilder.post(RequestBody.create("", null));
                }
                break;
            case "PUT":
                if (body != null && !body.isEmpty()) {
                    // 尝试从请求头中确定Content-Type
                    String contentType = "application/x-www-form-urlencoded"; // 默认值
                    if (headers != null && headers.containsKey("Content-Type")) {
                        contentType = headers.get("Content-Type");
                    }
                    RequestBody requestBody = RequestBody.create(body, MediaType.parse(contentType));
                    requestBuilder.put(requestBody);
                } else {
                    requestBuilder.put(RequestBody.create("", null));
                }
                break;
            case "DELETE":
                requestBuilder.delete();
                break;
            case "GET":
            default:
                requestBuilder.get();
                break;
        }

        // 按照HTTP标准顺序设置请求头
        Headers.Builder headersBuilder = new Headers.Builder();

        // 1. 首先设置Host头（HTTP标准要求Host在最前面）
        java.net.URL urlObj = new java.net.URL(url);
        String host = urlObj.getHost();
        if (urlObj.getPort() != -1) {
            host += ":" + urlObj.getPort();
        }
        headersBuilder.add("Host", host);

        // 2. 设置User-Agent（如果用户没有提供自定义的）
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        if (headers == null || !headers.containsKey("User-Agent")) {
            headersBuilder.add("User-Agent", userAgent);
        }
        
        // 3. 设置其他标准请求头（如果用户没有提供）
        if (headers == null || !headers.containsKey("Accept")) {
            headersBuilder.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        }
        if (headers == null || !headers.containsKey("Accept-Language")) {
            headersBuilder.add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        }
        if (headers == null || !headers.containsKey("Accept-Charset")) {
            headersBuilder.add("Accept-Charset", "UTF-8,GBK,GB2312,ISO-8859-1;q=0.7,*;q=0.5");
        }
        if (headers == null || !headers.containsKey("Accept-Encoding")) {
            headersBuilder.add("Accept-Encoding", "gzip, deflate");
        }
        if (headers == null || !headers.containsKey("Connection")) {
            headersBuilder.add("Connection", "close");
        }

        // 4. 最后设置用户自定义请求头（覆盖默认设置）
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                headersBuilder.add(entry.getKey(), entry.getValue());
            }
        }

        requestBuilder.headers(headersBuilder.build());

        // 发送请求
        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 构建响应
            Map<String, List<String>> responseHeaders = response.headers().toMultimap();
            String responseBody = response.body() != null ? getResponseBodyWithCorrectEncoding(response) : "";
            
            // 创建HttpResponse对象
            HttpResponse httpResponse = new HttpResponse(response.code(), responseBody, responseHeaders, responseTime);
            
            // 如果是HTTPS请求，收集SSL/TLS信息
            if (url.startsWith("https://")) {
                try {
                    // 获取连接信息（OkHttp 4.x的新方式）
                    okhttp3.Handshake handshake = response.handshake();

                    if (handshake != null) {
                        // 设置SSL/TLS协议版本
                        httpResponse.setSslProtocol(handshake.tlsVersion().javaName());
                        // 设置加密套件
                        httpResponse.setCipherSuite(handshake.cipherSuite().javaName());
                        // 设置SSL验证状态
                        httpResponse.setSslVerified(!ignoreSSL);
                        
                        // 获取证书信息（第一个证书）
                        if (!handshake.peerCertificates().isEmpty()) {
                            java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) handshake.peerCertificates().get(0);
                            
                            // 设置证书主题
                            httpResponse.setSslSubject(cert.getSubjectDN().getName());
                            // 设置证书颁发者
                            httpResponse.setSslIssuer(cert.getIssuerDN().getName());
                            // 设置证书有效期
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            httpResponse.setSslValidFrom(sdf.format(cert.getNotBefore()));
                            httpResponse.setSslValidTo(sdf.format(cert.getNotAfter()));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("收集SSL信息失败: " + e.getMessage());
                }
            }
            
            return httpResponse;
        }
    }
    
    // 根据Content-Type或HTML meta标签获取正确的字符编码
    private String getResponseBodyWithCorrectEncoding(Response response) throws IOException {
        // 获取原始字节
        byte[] responseBodyBytes = getUncompressedResponseBody(response);
        if (responseBodyBytes == null || responseBodyBytes.length == 0) {
            return "";
        }
        
        // 尝试从Content-Type头获取字符编码
        String contentType = response.header("Content-Type");
        String charset = "UTF-8"; // 默认使用UTF-8
        
        if (contentType != null) {
            // 从Content-Type中提取字符编码
            Pattern pattern = Pattern.compile("charset=([^;]+)");
            Matcher matcher = pattern.matcher(contentType);
            if (matcher.find()) {
                charset = matcher.group(1).trim().toUpperCase();
            }
        }
        
        // 如果是HTML内容，尝试从HTML meta标签获取字符编码
        if (contentType != null && (contentType.contains("text/html") || contentType.contains("application/xhtml+xml"))) {
            try {
                // 先用UTF-8尝试解析HTML
                String tempHtml = new String(responseBodyBytes, StandardCharsets.UTF_8);
                
                // 从HTML meta标签中提取字符编码（多种格式）
                Pattern[] metaPatterns = {
                    Pattern.compile("<meta[^>]*charset=[\"']([^\"'>]+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("<meta[^>]*content=[\"'][^\"']*charset=([^\"'>]+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("<meta\\s+http-equiv=[\"']Content-Type[\"'][^>]*content=[\"'][^\"']*charset=([^\"'>]+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE)
                };
                
                for (Pattern metaPattern : metaPatterns) {
                    Matcher metaMatcher = metaPattern.matcher(tempHtml);
                    if (metaMatcher.find()) {
                        String detectedCharset = metaMatcher.group(1).trim().toUpperCase();
                        if (isValidCharset(detectedCharset)) {
                            charset = detectedCharset;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // 解析失败，使用默认编码
                System.err.println("解析HTML编码失败: " + e.getMessage());
            }
        }
        
        // 对于XML内容，尝试从XML声明中获取编码
        if (contentType != null && (contentType.contains("text/xml") || contentType.contains("application/xml"))) {
            try {
                // 尝试用UTF-8解析XML声明
                String tempXml = new String(responseBodyBytes, StandardCharsets.UTF_8);
                Pattern xmlPattern = Pattern.compile("<\\?xml[^>]*encoding=[\"']([^\"'>]+)[\"'][^>]*\\?>");
                Matcher xmlMatcher = xmlPattern.matcher(tempXml);
                if (xmlMatcher.find()) {
                    String detectedCharset = xmlMatcher.group(1).trim().toUpperCase();
                    if (isValidCharset(detectedCharset)) {
                        charset = detectedCharset;
                    }
                }
            } catch (Exception e) {
                // 解析失败，使用默认编码
                System.err.println("解析XML编码失败: " + e.getMessage());
            }
        }
        
        // 如果没有检测到编码或检测到的编码无效，尝试自动检测编码
        if (!isValidCharset(charset)) {
            charset = detectCharset(responseBodyBytes);
        }
        
        // 使用检测到的字符编码解码响应体
        try {
            return new String(responseBodyBytes, charset);
        } catch (Exception e) {
            System.err.println("使用编码 " + charset + " 解码失败: " + e.getMessage());
            
            // 如果指定的字符编码无效，尝试常见编码（优先UTF-8）
            String[] fallbackCharsets = {"UTF-8", "GBK", "GB2312", "ISO-8859-1", "WINDOWS-1252"};
            for (String fallbackCharset : fallbackCharsets) {
                try {
                    return new String(responseBodyBytes, fallbackCharset);
                } catch (Exception ex) {
                    // 继续尝试下一个编码
                }
            }
            
            // 如果所有编码都失败，使用系统默认编码
            return new String(responseBodyBytes);
        }
    }
    
    // 获取解压缩后的响应体字节数组
    private byte[] getUncompressedResponseBody(Response response) throws IOException {
        byte[] compressedBytes = response.body().bytes();
        if (compressedBytes == null || compressedBytes.length == 0) {
            return compressedBytes;
        }
        
        // 检查Content-Encoding头
        String contentEncoding = response.header("Content-Encoding");
        if (contentEncoding == null) {
            // 没有压缩，直接返回原始字节
            return compressedBytes;
        }
        
        // 根据压缩类型进行解压缩
        try (InputStream inputStream = new ByteArrayInputStream(compressedBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            InputStream decompressedStream;
            
            switch (contentEncoding.toLowerCase()) {
                case "gzip":
                    decompressedStream = new GZIPInputStream(inputStream);
                    break;
                case "deflate":
                    decompressedStream = new InflaterInputStream(inputStream);
                    break;
                default:
                    // 不支持的压缩类型，直接返回原始字节
                    return compressedBytes;
            }
            
            // 读取解压缩后的数据
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = decompressedStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            System.err.println("解压缩响应体失败: " + e.getMessage());
            // 解压缩失败，返回原始字节
            return compressedBytes;
        }
    }
    
    // 验证字符编码是否有效
    private boolean isValidCharset(String charsetName) {
        if (charsetName == null || charsetName.trim().isEmpty()) {
            return false;
        }
        
        try {
            return Charset.isSupported(charsetName.trim());
        } catch (Exception e) {
            return false;
        }
    }
    
    // 简单的字符编码检测
    private String detectCharset(byte[] bytes) {
        // 检查UTF-8 BOM
        if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
            return "UTF-8";
        }
        
        // 检查UTF-16 BE BOM
        if (bytes.length >= 2 && bytes[0] == (byte)0xFE && bytes[1] == (byte)0xFF) {
            return "UTF-16BE";
        }
        
        // 检查UTF-16 LE BOM
        if (bytes.length >= 2 && bytes[0] == (byte)0xFF && bytes[1] == (byte)0xFE) {
            return "UTF-16LE";
        }
        
        // 默认使用UTF-8（最通用的编码）
        return "UTF-8";
    }
    
    /**
     * 配置忽略SSL证书验证
     */
    private void configureIgnoreSSL(OkHttpClient.Builder builder) {
        try {
            // 创建信任所有证书的TrustManager
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{trustManager}, new java.security.SecureRandom());

            // 创建SSL套接字工厂
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, trustManager)
                   .hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            System.err.println("配置SSL忽略失败: " + e.getMessage());
        }
    }
}