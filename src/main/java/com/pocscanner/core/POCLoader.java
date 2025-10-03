package com.pocscanner.core;

import com.pocscanner.core.model.POCConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class POCLoader {
    private final Yaml yaml;

    public POCLoader() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        this.yaml = new Yaml(new Constructor(POCConfig.class, options));
    }

    public List<POCConfig> loadPOCsFromDirectory(String directoryPath) {
        List<POCConfig> pocs = new ArrayList<>();
        File dir = new File(directoryPath);

        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("POC目录不存在: " + directoryPath);
            return pocs;
        }

        File[] yamlFiles = dir.listFiles((d, name) ->
                name.toLowerCase().endsWith(".yaml") || name.toLowerCase().endsWith(".yml"));

        if (yamlFiles != null) {
            for (File file : yamlFiles) {
                try {
                    // 尝试使用UTF-8编码读取文件
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    
                    // 如果文件包含Java对象标签，尝试清理
                    if (content.contains("!!com.pocscanner.core.model.POCConfig")) {
                        // 移除Java对象标签，使用标准YAML格式
                        content = content.replace("!!com.pocscanner.core.model.POCConfig\n", "");
                    }
                    
                    // 使用清理后的内容解析YAML
                    POCConfig poc = yaml.load(content);
                    if (poc != null) {
                        pocs.add(poc);
                        System.out.println("成功加载POC: " + poc.getName());
                    }
                } catch (Exception e) {
                    // 如果UTF-8失败，尝试其他编码
                    try {
                        // 尝试GBK编码
                        String content = new String(Files.readAllBytes(file.toPath()), "GBK");
                        if (content.contains("!!com.pocscanner.core.model.POCConfig")) {
                            content = content.replace("!!com.pocscanner.core.model.POCConfig\n", "");
                        }
                        POCConfig poc = yaml.load(content);
                        if (poc != null) {
                            pocs.add(poc);
                            System.out.println("成功加载POC (GBK编码): " + poc.getName());
                        }
                    } catch (Exception e2) {
                        // 如果所有编码都失败，记录错误
                        System.err.println("加载POC文件失败: " + file.getName() + " - " + e.getMessage());
                        System.err.println("尝试GBK编码也失败: " + e2.getMessage());
                    }
                }
            }
        }

        return pocs;
    }

    public POCConfig loadPOCFromFile(String filePath) {
        try {
            String content = new String(Files.readAllBytes(new File(filePath).toPath()), StandardCharsets.UTF_8);
            if (content.contains("!!com.pocscanner.core.model.POCConfig")) {
                content = content.replace("!!com.pocscanner.core.model.POCConfig\n", "");
            }
            return yaml.load(content);
        } catch (Exception e) {
            try {
                String content = new String(Files.readAllBytes(new File(filePath).toPath()), "GBK");
                if (content.contains("!!com.pocscanner.core.model.POCConfig")) {
                    content = content.replace("!!com.pocscanner.core.model.POCConfig\n", "");
                }
                return yaml.load(content);
            } catch (Exception e2) {
                System.err.println("加载POC文件失败: " + filePath + " - " + e.getMessage());
                return null;
            }
        }
    }

    public List<String> validatePOC(POCConfig poc) {
        List<String> errors = new ArrayList<>();

        if (poc.getName() == null || poc.getName().trim().isEmpty()) {
            errors.add("POC名称不能为空");
        }

        if (poc.getRequest() == null) {
            errors.add("请求配置不能为空");
        } else {
            if (poc.getRequest().getMethod() == null) {
                errors.add("请求方法不能为空");
            }
            // 不再强制要求路径
        }

        if (poc.getResponse() == null) {
            errors.add("响应配置不能为空");
        } else {
            if (poc.getResponse().getSuccessIndicators() == null ||
                    poc.getResponse().getSuccessIndicators().isEmpty()) {
                errors.add("成功特征指示器不能为空");
            }
        }

        return errors;
    }
}