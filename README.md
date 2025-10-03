# Java POC扫描器

一个功能强大的基于Java的图形化POC（概念验证）扫描工具，用于检测目标URL是否存在特定漏洞。该工具通过加载YAML格式的POC文件，对目标系统发送定制化HTTP请求，并根据响应结果判断是否存在漏洞。

## 功能特点

- **直观的图形化界面**：提供三个主要标签页（扫描器、扫描结果、POC管理），操作简便
- **POC管理功能**：支持从本地目录加载、添加、编辑和删除YAML格式的POC文件
- **多线程扫描**：可配置线程数量（1-50），提高批量扫描效率
- **代理支持**：支持HTTP、HTTPS和SOCKS代理，可按需启用
- **灵活的匹配规则**：支持"contains"（包含）、"equals"（等于）和"regex"（正则表达式）三种匹配方式
- **详细的结果展示**：显示请求/响应信息、漏洞状态、响应时间、SSL/TLS信息等关键数据
- **可配置参数**：支持自定义超时时间、代理设置和线程数

## 技术栈

- **开发语言**：Java 8+
- **构建工具**：Maven
- **GUI框架**：Swing
- **YAML解析**：SnakeYAML
- **HTTP客户端**：OkHttp

## 项目结构

```
poc-scanner-gui/
├── src/main/java/com/pocscanner/
│   ├── Application.java           # 应用程序入口类
│   ├── core/                      # 核心功能模块
│   │   ├── POCEngine.java         # POC执行引擎，处理单个POC的执行逻辑
│   │   ├── POCLoader.java         # POC加载器，负责从YAML文件加载POC配置
│   │   ├── ScannerEngine.java     # 扫描引擎，管理多线程扫描任务
│   │   └── model/                 # 数据模型定义
│   │       ├── POCConfig.java     # POC配置模型
│   │       ├── ScanRequest.java   # 扫描请求模型
│   │       └── ScanResult.java    # 扫描结果模型
│   ├── gui/                       # 图形界面模块
│   │   ├── MainFrame.java         # 主窗口，包含三个功能标签页
│   │   ├── POCManagerPanel.java   # POC管理面板，用于POC文件的管理
│   │   ├── ResultPanel.java       # 结果展示面板，显示扫描结果详情
│   │   └── ScannerPanel.java      # 扫描器面板，配置和执行扫描
│   └── http/                      # HTTP请求模块
│       ├── HttpClient.java        # HTTP客户端，基于OkHttp封装
│       └── HttpResponse.java      # HTTP响应模型
├── pocs/                          # 默认POC文件存储目录
│   ├── test-contains-matching.yaml
│   ├── test-equals-matching.yaml
│   └── Test_Contains_Matching.yaml
├── pom.xml                        # Maven项目配置文件
└── README.md                      # 项目说明文档
```

## 安装与运行

### 前提条件

- JDK 8或更高版本
- Maven 3.6.0或更高版本

### 构建项目

1. 克隆或下载项目代码到本地
2. 打开命令行工具，进入项目根目录
3. 执行以下命令构建项目：

```bash
mvn clean package
```

### 运行应用程序

构建成功后，可以通过以下两种方式运行应用程序：

**方式1：使用Maven**

```bash
mvn exec:java
```

**方式2：直接运行JAR文件**

```bash
java -jar target/poc-scanner-gui-1.0-SNAPSHOT.jar
```

## 使用说明

### 1. 加载POC文件

- 切换到"POC管理"标签页
- 默认会加载`./pocs`目录下的POC文件
- 如需更改POC目录，点击"浏览"按钮选择新的目录，然后点击"刷新"按钮
- 加载完成后，POC列表将显示在表格中

### 2. 配置扫描参数

- 切换到"扫描器"标签页
- 在"目标URL"输入框中输入要扫描的目标URL
- 在"POC目录"输入框中确认POC文件目录
- 可选配置：
  - 设置代理服务器（类型、主机、端口）
  - 调整线程数（使用滑块控制，范围1-50）

### 3. 执行扫描

- 点击"开始扫描"按钮启动扫描
- 扫描过程中，日志区域将显示实时扫描状态
- 进度条显示当前扫描进度

### 4. 查看扫描结果

- 切换到"扫描结果"标签页
- 表格中显示所有POC的扫描结果概览
- 点击表格中的任意一行，下方将显示该次扫描的详细信息，包括：
  - 请求信息（URL、方法、请求头、请求体）
  - 响应信息（状态码、响应头、响应体）
  - 漏洞状态和匹配证据


## POC文件格式

POC文件采用YAML格式，遵循以下结构：

```yaml
name: POC名称                     # POC的唯一标识符
description: POC描述              # 对该POC的详细描述
level: 漏洞级别                   # 如：低/中/高/严重
author: 作者                      # POC作者信息
request:
  method: HTTP方法                # 如：GET/POST/PUT/DELETE等
  path: 请求路径                  # 目标URL的路径部分
  headers:                        # 可选，HTTP请求头
    User-Agent: Mozilla/5.0 ...
    Content-Type: application/json
  body: 请求体                    # 可选，适用于POST等方法
  params:                         # 可选，URL查询参数
    id: 1
    type: test
response:
  matchType: 匹配类型             # contains（包含）、equals（等于）或regex（正则表达式）
  successIndicators:              # 成功匹配的字符串列表
    - "关键字1"
    - "关键字2"
  errorIndicators:                # 失败匹配的字符串列表
    - "错误信息1"
  statusCode: 期望的HTTP状态码    # 如：200/404/500等
```

### 示例POC

```yaml
name: Test Contains Matching
description: 测试包含匹配规则的POC示例
level: 中
author: Test
request:
  method: GET
  path: /test-contains
response:
  matchType: contains
  successIndicators:
    - "partial"
    - "content"
  errorIndicators:
    - "not found"
  statusCode: 200
```

## 工作原理

1. **POC加载**：通过`POCLoader`类从YAML文件中加载POC配置，支持UTF-8和GBK编码
2. **扫描准备**：在`ScannerPanel`中配置扫描参数，创建`ScanRequest`对象
3. **任务分发**：`ScannerEngine`创建线程池，为每个POC分配扫描任务
4. **POC执行**：`POCEngine`根据POC配置构建HTTP请求，通过`HttpClient`发送请求
5. **结果匹配**：根据POC中定义的匹配规则（contains/equals/regex），检查响应内容是否符合漏洞特征
6. **结果展示**：将扫描结果显示在`ResultPanel`中，包括漏洞状态、请求/响应详情和SSL/TLS信息

## 注意事项

1. **合法性**：本工具仅用于合法的安全测试和授权的漏洞评估，请勿用于未授权的系统
2. **性能**：扫描大量目标或使用多个POC时，建议合理设置线程数以避免系统资源耗尽
3. **准确性**：扫描结果仅供参考，建议对阳性结果进行人工验证
4. **目录配置**：默认POC目录为`./pocs`，如需要修改请在扫描器页面进行配置

## 开发说明

### 添加新功能

1. 核心功能扩展：在`core`包中实现新的功能逻辑
2. 界面组件添加：在`gui`包中创建或修改界面组件
3. HTTP请求增强：在`http`包中扩展HTTP请求相关功能
4. 数据模型更新：在`model`包中添加新的数据结构

### 构建与测试

```bash
mvn clean test       # 运行单元测试
mvn clean package    # 构建项目
```

## 待改进功能

1. SSL证书验证开关（目前需通过代码修改）
2. 批量目标扫描功能
3. 扫描结果导出功能
4. 定时扫描任务
5. 更多匹配规则的支持
