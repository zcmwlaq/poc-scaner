package com.pocscanner.gui;

import com.pocscanner.core.POCLoader;
import com.pocscanner.core.model.POCConfig;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class POCManagerPanel extends JPanel {
    private JTable pocTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JButton browseDirButton;
    private JTextArea detailArea;
    private JTextField pocDirField;
    private JLabel statusLabel;
    // 新增搜索相关组件
    private JTextField searchField;
    private JButton searchButton;
    private JButton clearSearchButton;

    private List<POCConfig> pocs;
    private POCLoader pocLoader;
    private Preferences prefs;

    public POCManagerPanel() {
        this.pocLoader = new POCLoader();
        this.prefs = Preferences.userNodeForPackage(POCManagerPanel.class);
        initializeComponents();
        setupLayout();
        setupEventListeners();
        loadSavedSettings();
        refreshPOCList();
    }

    private void initializeComponents() {
        // 表格模型
        String[] columns = {"名称", "描述", "危险等级", "作者"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        pocTable = new JTable(tableModel);
        pocTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pocTable.getTableHeader().setReorderingAllowed(false);
        pocTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        pocTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        pocTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        pocTable.getColumnModel().getColumn(3).setPreferredWidth(120);

        // 按钮
        addButton = new JButton("添加POC");
        editButton = new JButton("编辑POC");
        deleteButton = new JButton("删除POC");
        refreshButton = new JButton("刷新");
        browseDirButton = new JButton("浏览");

        // 目录选择
        pocDirField = new JTextField(30);
        pocDirField.setText("./poc");

        // 详情区域
        detailArea = new JTextArea(15, 50);
        detailArea.setEditable(false);
        
        // 状态栏
        statusLabel = new JLabel("就绪");
        
        // 搜索相关组件
        searchField = new JTextField(20);
        searchButton = new JButton("搜索");
        clearSearchButton = new JButton("清除");
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部配置面板
        JPanel configPanel = new JPanel(new BorderLayout(5, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("POC目录配置"));
        
        JPanel dirSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dirSelectPanel.add(new JLabel("POC目录: "));
        dirSelectPanel.add(pocDirField);
        dirSelectPanel.add(browseDirButton);
        
        configPanel.add(dirSelectPanel, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        
        // 添加搜索面板
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        searchPanel.add(new JLabel("搜索POC:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearSearchButton);
        
        // 将搜索面板添加到按钮面板的右侧
        JPanel topButtonPanel = new JPanel(new BorderLayout());
        topButtonPanel.add(buttonPanel, BorderLayout.WEST);
        topButtonPanel.add(searchPanel, BorderLayout.EAST);
        
        configPanel.add(topButtonPanel, BorderLayout.SOUTH);

        // 主内容面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);

        // 上部：表格
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JScrollPane(pocTable), BorderLayout.CENTER);

        // 下部：详情
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("POC详情"));
        bottomPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);

        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        splitPane.setResizeWeight(0.6);
        splitPane.setOneTouchExpandable(true);

        // 状态栏
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // 添加到主面板
        add(configPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupEventListeners() {
        refreshButton.addActionListener(e -> refreshPOCList());

        browseDirButton.addActionListener(e -> browsePOCDirectory());

        addButton.addActionListener(e -> showAddPOCDialog());

        editButton.addActionListener(e -> {
            int selectedRow = pocTable.getSelectedRow();
            if (selectedRow >= 0) {
                showEditPOCDialog(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "请选择一个POC进行编辑");
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = pocTable.getSelectedRow();
            if (selectedRow >= 0) {
                deletePOC(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "请选择一个POC进行删除");
            }
        });

        pocTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = pocTable.getSelectedRow();
                if (selectedRow >= 0) {
                    showPOCDetails(selectedRow);
                }
            }
        });
        
        // 添加搜索功能事件监听器
        searchButton.addActionListener(e -> performSearch());
        clearSearchButton.addActionListener(e -> clearSearch());
        searchField.addActionListener(e -> performSearch());
        
        // 添加表格右键菜单
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openDirItem = new JMenuItem("打开POC文件目录");
        openDirItem.addActionListener(e -> openPOCDirectory());
        popupMenu.add(openDirItem);
        
        pocTable.setComponentPopupMenu(popupMenu);
        pocTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = pocTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < pocTable.getRowCount()) {
                        pocTable.setRowSelectionInterval(row, row);
                    }
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void loadSavedSettings() {
        // 从偏好设置中加载上次使用的POC目录，如果没有保存的设置则使用默认值./poc
        String savedDir = prefs.get("pocDirectory", null);
        if (savedDir == null || savedDir.trim().isEmpty()) {
            savedDir = "./poc"; // 确保默认值为./poc
        }
        pocDirField.setText(savedDir);
    }

    private void saveSettings() {
        // 保存当前的POC目录设置
        prefs.put("pocDirectory", pocDirField.getText());
    }

    private void browsePOCDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        // 设置当前目录为初始目录
        String currentDir = pocDirField.getText();
        if (!currentDir.isEmpty()) {
            File dir = new File(currentDir);
            if (dir.exists() && dir.isDirectory()) {
                fileChooser.setCurrentDirectory(dir);
            }
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            pocDirField.setText(selectedDir.getAbsolutePath());
            saveSettings();
            refreshPOCList();
        }
    }

    private void refreshPOCList() {
        String directoryPath = pocDirField.getText().trim();
        
        if (directoryPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入有效的POC目录路径");
            return;
        }

        // 检查目录是否存在
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "POC目录不存在: " + directoryPath,
                    "错误", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("错误: POC目录不存在");
            return;
        }

        try {
            pocs = pocLoader.loadPOCsFromDirectory(directoryPath);
            tableModel.setRowCount(0);

            for (POCConfig poc : pocs) {
                tableModel.addRow(new Object[]{
                        poc.getName(),
                        poc.getDescription() != null ? poc.getDescription() : "",
                        poc.getLevel() != null ? poc.getLevel() : "",
                        poc.getAuthor() != null ? poc.getAuthor() : ""
                });
            }

            statusLabel.setText("已加载 " + pocs.size() + " 个POC文件，目录: " + directoryPath);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载POC文件出错: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("加载错误: " + e.getMessage());
        }
    }

    private void showPOCDetails(int rowIndex) {
        if (rowIndex < pocs.size()) {
            POCConfig poc = pocs.get(rowIndex);
            StringBuilder details = new StringBuilder();

            details.append("名称: ").append(poc.getName()).append("\n");
            details.append("描述: ").append(poc.getDescription() != null ? poc.getDescription() : "").append("\n");
            details.append("危险等级: ").append(poc.getLevel() != null ? poc.getLevel() : "").append("\n");
            details.append("作者: ").append(poc.getAuthor() != null ? poc.getAuthor() : "").append("\n\n");

            details.append("请求配置:\n");
            if (poc.getRequest() != null) {
                details.append("  方法: ").append(poc.getRequest().getMethod() != null ? poc.getRequest().getMethod() : "").append("\n");
                details.append("  路径: ").append(poc.getRequest().getPath() != null ? poc.getRequest().getPath() : "").append("\n");
                
                if (poc.getRequest().getParams() != null && !poc.getRequest().getParams().isEmpty()) {
                    details.append("  参数: \n");
                    for (Map.Entry<String, String> entry : poc.getRequest().getParams().entrySet()) {
                        details.append("    " + entry.getKey() + ": " + entry.getValue()).append("\n");
                    }
                }

                if (poc.getRequest().getHeaders() != null && !poc.getRequest().getHeaders().isEmpty()) {
                    details.append("  请求头: \n");
                    for (Map.Entry<String, String> entry : poc.getRequest().getHeaders().entrySet()) {
                        details.append("    " + entry.getKey() + ": " + entry.getValue()).append("\n");
                    }
                }
                
                if (poc.getRequest().getBody() != null) {
                    details.append("  请求体: " + poc.getRequest().getBody()).append("\n");
                }
            }

            details.append("\n响应配置:\n");
            if (poc.getResponse() != null) {
                if (poc.getResponse().getSuccessIndicators() != null && !poc.getResponse().getSuccessIndicators().isEmpty()) {
                    details.append("  成功特征: \n");
                    for (String indicator : poc.getResponse().getSuccessIndicators()) {
                        details.append("    " + indicator).append("\n");
                    }
                }
                if (poc.getResponse().getStatusCode() != null) {
                    details.append("  状态码: " + poc.getResponse().getStatusCode()).append("\n");
                }
            }

            detailArea.setText(details.toString());
        }
    }

    private void showAddPOCDialog() {
        // 创建添加POC的对话框
        final JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "添加POC", true);
        dialog.setSize(600, 700);  // 增加对话框大小
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        // 创建表单面板
        final JPanel formPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // 名称
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("名称: *"), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField nameField = new JTextField(30);
        formPanel.add(nameField, gbc);
        row++;
        
        // 描述
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("描述: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField descField = new JTextField(30);
        formPanel.add(descField, gbc);
        row++;
        
        // 危险等级
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("危险等级: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        String[] levels = {"", "低", "中", "高", "严重"};
        final JComboBox<String> levelComboBox = new JComboBox<>(levels);
        formPanel.add(levelComboBox, gbc);
        row++;
        
        // 作者
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("作者: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField authorField = new JTextField(30);
        formPanel.add(authorField, gbc);
        row++;
        
        // 请求方法
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("请求方法: *"), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        String[] methods = {"GET", "POST", "PUT", "DELETE", "HEAD"};
        final JComboBox<String> methodComboBox = new JComboBox<>(methods);
        formPanel.add(methodComboBox, gbc);
        row++;
        
        // 请求路径
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("请求路径: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField pathField = new JTextField("", 30);
        formPanel.add(pathField, gbc);
        row++;
        
        // 参数
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("参数: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField paramsField = new JTextField(300);
        paramsField.setToolTipText("格式: key1:value1,key2:value2");
        paramsField.setText("");
        formPanel.add(paramsField, gbc);
        row++;
        
        // User-Agent
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("User-Agent: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField headersField = new JTextField(300);
        headersField.setToolTipText("直接填写User-Agent的值");
        formPanel.add(headersField, gbc);
        row++;
        
        // 请求体
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("请求体: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField bodyField = new JTextField(30);
        formPanel.add(bodyField, gbc);
        row++;
        
        // 响应状态码（两列）
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("响应状态码: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField statusCodeField = new JTextField(10);
        statusCodeField.setToolTipText("可选，期望的HTTP状态码");
        formPanel.add(statusCodeField, gbc);
        row++;
        
        // 匹配规则类型
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("匹配规则: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        String[] matchTypes = {"包含匹配", "完全等于", "正则表达式匹配"};
        final JComboBox<String> matchTypeComboBox = new JComboBox<>(matchTypes);
        matchTypeComboBox.setToolTipText("选择特征匹配方式");
        matchTypeComboBox.setSelectedIndex(0);
        formPanel.add(matchTypeComboBox, gbc);
        row++;
        
        // 成功特征
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("成功特征: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        final JTextArea successArea = new JTextArea(3, 30);
        successArea.setBorder(BorderFactory.createEtchedBorder());
        successArea.setToolTipText("每行一个特征，支持包含匹配");
        formPanel.add(new JScrollPane(successArea), gbc);
        row++;
        
        // 错误特征
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("错误特征: "), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        final JTextArea errorArea = new JTextArea(3, 30);
        errorArea.setBorder(BorderFactory.createEtchedBorder());
        errorArea.setToolTipText("每行一个特征，存在这些特征表示无漏洞");
        errorArea.setText("");
        formPanel.add(new JScrollPane(errorArea), gbc);
        
        // 按钮面板
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton saveButton = new JButton("保存");
        final JButton cancelButton = new JButton("取消");
        
        saveButton.addActionListener(e -> {
            // 验证必填字段
            String name = nameField.getText().trim();
            String method = (String) methodComboBox.getSelectedItem();
            // 不再强制要求路径
            
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "POC名称不能为空");
                return;
            }
            
            // 创建新的POC配置
            POCConfig newPoc = new POCConfig();
            
            // 更新POC配置（使用表单中的值）
            newPoc.setName(name);
            newPoc.setDescription(descField.getText().trim());
            newPoc.setLevel((String) levelComboBox.getSelectedItem());
            newPoc.setAuthor(authorField.getText().trim());
            
            if (newPoc.getRequest() == null) {
                newPoc.setRequest(new POCConfig.Request());
            }
            newPoc.getRequest().setMethod(method);
            
            // 设置路径（可以为空）
            String path = pathField.getText().trim();
            if (!path.isEmpty()) {
                newPoc.getRequest().setPath(path);
            }
            
            // 解析参数
            String paramsText = paramsField.getText().trim();
            if (!paramsText.isEmpty()) {
                try {
                    Map<String, String> params = new HashMap<>();
                    String[] pairs = paramsText.split(",");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split(":", 2);
                        if (keyValue.length == 2) {
                            params.put(keyValue[0].trim(), keyValue[1].trim());
                        }
                    }
                    newPoc.getRequest().setParams(params);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "参数格式错误");
                    return;
                }
            } else {
                newPoc.getRequest().setParams(null);
            }
            
            // 解析请求头
            String userAgent = headersField.getText().trim();
            if (!userAgent.isEmpty()) {
                try {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", userAgent);
                    newPoc.getRequest().setHeaders(headers);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "请求头格式错误");
                    return;
                }
            } else {
                newPoc.getRequest().setHeaders(null);
            }
            
            // 请求体
            String bodyText = bodyField.getText().trim();
            if (!bodyText.isEmpty()) {
                newPoc.getRequest().setBody(bodyText);
            } else {
                newPoc.getRequest().setBody(null);
            }
            
            if (newPoc.getResponse() == null) {
                newPoc.setResponse(new POCConfig.Response());
            }
            
            POCConfig.Response response = newPoc.getResponse();
            
            // 状态码
            String statusCodeText = statusCodeField.getText().trim();
            if (!statusCodeText.isEmpty()) {
                try {
                    response.setStatusCode(Integer.parseInt(statusCodeText));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "状态码必须是数字");
                    return;
                }
            } else {
                response.setStatusCode(null);
            }
            
            // 匹配规则类型
            String matchType;
            if (matchTypeComboBox.getSelectedIndex() == 0) {
                matchType = "contains";
            } else if (matchTypeComboBox.getSelectedIndex() == 1) {
                matchType = "equals";
            } else {
                matchType = "regex";
            }
            response.setMatchType(matchType);
            
            // 成功特征
            String successText = successArea.getText().trim();
            if (!successText.isEmpty()) {
                List<String> successIndicators = new ArrayList<>();
                for (String line : successText.split("\\n")) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        successIndicators.add(line);
                    }
                }
                if (!successIndicators.isEmpty()) {
                    response.setSuccessIndicators(successIndicators);
                } else {
                    response.setSuccessIndicators(null);
                }
            } else {
                response.setSuccessIndicators(null);
            }
            
            // 错误特征
            String errorText = errorArea.getText().trim();
            if (!errorText.isEmpty()) {
                List<String> errorIndicators = new ArrayList<>();
                for (String line : errorText.split("\\n")) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        errorIndicators.add(line);
                    }
                }
                if (!errorIndicators.isEmpty()) {
                    response.setErrorIndicators(errorIndicators);
                } else {
                    response.setErrorIndicators(null);
                }
            } else {
                response.setErrorIndicators(null);
            }
            
            // 保存到文件
            if (savePOCToFile(newPoc, null)) {
                dialog.dispose();
                refreshPOCList();
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }

    private void showEditPOCDialog(int rowIndex) {
        if (rowIndex < pocs.size()) {
            final POCConfig originalPoc = pocs.get(rowIndex);
            
            // 创建编辑POC的对话框
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "编辑POC", true);
            dialog.setSize(1000, 900);  // 增加对话框大小
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());
            
            // 创建表单面板
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            
            int row = 0;
            
            // 添加选项：在原基础上修改还是新生成
            gbc.gridx = 0; gbc.gridy = row;
            formPanel.add(new JLabel("编辑方式:"), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            String[] editOptions = {"在原基础上修改", "新生成"};
            final JComboBox<String> editOptionComboBox = new JComboBox<>(editOptions);
            editOptionComboBox.setSelectedIndex(0); // 默认选择在原基础上修改
            formPanel.add(editOptionComboBox, gbc);
            row++;
            
            // 名称
            gbc.gridx = 0; gbc.gridy = row;
            formPanel.add(new JLabel("名称: *"), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            final JTextField nameField = new JTextField(originalPoc.getName(), 300);
            formPanel.add(nameField, gbc);
            row++;
            
            // 描述
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("描述: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            final JTextField descField = new JTextField(originalPoc.getDescription() != null ? originalPoc.getDescription() : "", 300);
            formPanel.add(descField, gbc);
            row++;
            
            // 危险等级
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("危险等级: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            String[] levels = {"", "低", "中", "高", "严重"};
            final JComboBox<String> levelComboBox = new JComboBox<>(levels);
            if (originalPoc.getLevel() != null) {
                levelComboBox.setSelectedItem(originalPoc.getLevel());
            }
            formPanel.add(levelComboBox, gbc);
            row++;
            
            // 作者
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("作者: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            final JTextField authorField = new JTextField(originalPoc.getAuthor() != null ? originalPoc.getAuthor() : "", 30);
            formPanel.add(authorField, gbc);
            row++;
            
            // 请求方法
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("请求方法: *"), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            String[] methods = {"GET", "POST", "PUT", "DELETE", "HEAD"};
            final JComboBox<String> methodComboBox = new JComboBox<>(methods);
            if (originalPoc.getRequest() != null && originalPoc.getRequest().getMethod() != null) {
                methodComboBox.setSelectedItem(originalPoc.getRequest().getMethod());
            }
            formPanel.add(methodComboBox, gbc);
            row++;
            
            // 请求路径
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("请求路径: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            final JTextField pathField = new JTextField(originalPoc.getRequest() != null && originalPoc.getRequest().getPath() != null ? originalPoc.getRequest().getPath() : "", 30);
            formPanel.add(pathField, gbc);
            row++;
            
            // 参数
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("参数: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            final JTextField paramsField = new JTextField(300);
            paramsField.setToolTipText("格式: key1:value1,key2:value2");
            if (originalPoc.getRequest() != null && originalPoc.getRequest().getParams() != null) {
                StringBuilder paramsBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : originalPoc.getRequest().getParams().entrySet()) {
                    if (paramsBuilder.length() > 0) paramsBuilder.append(",");
                    paramsBuilder.append(entry.getKey()).append(":").append(entry.getValue());
                }
                paramsField.setText(paramsBuilder.toString());
            }
            formPanel.add(paramsField, gbc);
            row++;
            
            // 请求头
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("请求头: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            final JTextField headersField = new JTextField(30);
            headersField.setToolTipText("格式: key1:value1\nkey2:value2");
            if (originalPoc.getRequest() != null && originalPoc.getRequest().getHeaders() != null) {
                StringBuilder headersBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : originalPoc.getRequest().getHeaders().entrySet()) {
                    if (headersBuilder.length() > 0) headersBuilder.append(",");
                    headersBuilder.append(entry.getKey()).append(":").append(entry.getValue());
                }
                headersField.setText(headersBuilder.toString());
            }
            formPanel.add(headersField, gbc);
            row++;
            
            // 请求体
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("请求体: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            final JTextField bodyField = new JTextField(30);
            if (originalPoc.getRequest() != null && originalPoc.getRequest().getBody() != null) {
                bodyField.setText(originalPoc.getRequest().getBody());
            }
            formPanel.add(bodyField, gbc);
            row++;
            
            // 响应状态码
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("响应状态码: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            final JTextField statusCodeField = new JTextField(30);
            statusCodeField.setToolTipText("可选，期望的HTTP状态码");
            if (originalPoc.getResponse() != null && originalPoc.getResponse().getStatusCode() != null) {
                statusCodeField.setText(String.valueOf(originalPoc.getResponse().getStatusCode()));
            }
            formPanel.add(statusCodeField, gbc);
            row++;
            
            // 成功特征
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            formPanel.add(new JLabel("成功特征: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            final JTextArea successArea = new JTextArea(3, 30);
            successArea.setBorder(BorderFactory.createEtchedBorder());
            successArea.setToolTipText("每行一个特征，支持包含匹配");
            if (originalPoc.getResponse() != null && originalPoc.getResponse().getSuccessIndicators() != null) {
                StringBuilder successBuilder = new StringBuilder();
                for (String indicator : originalPoc.getResponse().getSuccessIndicators()) {
                    if (successBuilder.length() > 0) successBuilder.append("\n");
                    successBuilder.append(indicator);
                }
                successArea.setText(successBuilder.toString());
            }
            formPanel.add(new JScrollPane(successArea), gbc);
            row++;
            
            // 匹配规则类型
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(new JLabel("匹配规则: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            String[] matchTypes = {"包含匹配", "完全等于", "正则表达式匹配"};
            final JComboBox<String> matchTypeComboBox = new JComboBox<>(matchTypes);
            matchTypeComboBox.setToolTipText("选择特征匹配方式");
            if (originalPoc.getResponse() != null && "equals".equals(originalPoc.getResponse().getMatchType())) {
                matchTypeComboBox.setSelectedIndex(1);
            } else if (originalPoc.getResponse() != null && "regex".equals(originalPoc.getResponse().getMatchType())) {
                matchTypeComboBox.setSelectedIndex(2);
            } else {
                matchTypeComboBox.setSelectedIndex(0);
            }
            formPanel.add(matchTypeComboBox, gbc);
            row++;
            
            // 错误特征
            gbc.gridx = 0; gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            formPanel.add(new JLabel("错误特征: "), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            final JTextArea errorArea = new JTextArea(3, 30);
            errorArea.setBorder(BorderFactory.createEtchedBorder());
            errorArea.setToolTipText("每行一个特征，存在这些特征表示无漏洞");
            if (originalPoc.getResponse() != null && originalPoc.getResponse().getErrorIndicators() != null) {
                StringBuilder errorBuilder = new StringBuilder();
                for (String indicator : originalPoc.getResponse().getErrorIndicators()) {
                    if (errorBuilder.length() > 0) errorBuilder.append("\n");
                    errorBuilder.append(indicator);
                }
                errorArea.setText(errorBuilder.toString());
            }
            formPanel.add(new JScrollPane(errorArea), gbc);
            
            // 按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveButton = new JButton("保存");
            JButton cancelButton = new JButton("取消");
            
            saveButton.addActionListener(e -> {
                // 验证必填字段
                String name = nameField.getText().trim();
                String method = (String) methodComboBox.getSelectedItem();
                // 不再强制要求路径
                
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "POC名称不能为空");
                    return;
                }
                
                // 根据用户选择的编辑方式创建POC配置
                POCConfig newPoc;
                String originalName = originalPoc.getName(); // 保存原始名称
                
                if (editOptionComboBox.getSelectedIndex() == 0) {
                    // 在原基础上修改 - 直接使用原POC对象
                    newPoc = originalPoc;
                } else {
                    // 新生成 - 创建全新的POC
                    newPoc = new POCConfig();
                    
                    // 备份原POC文件
                    String pocDir = pocDirField.getText().trim();
                    String oldFileName = originalName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + ".yaml";
                    File oldFile = new File(pocDir, oldFileName);
                    
                    if (oldFile.exists()) {
                        
                        String backupFileName = originalName + ".bak";
                        File backupFile = new File(pocDir, backupFileName);
                        
                        try {
                            Files.copy(oldFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            // 同时将原文件重命名为.bak
                            File bakFile = new File(pocDir, originalName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + ".bak");
                            oldFile.renameTo(bakFile);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(dialog, "备份原POC文件失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                
                // 更新POC配置（使用表单中的值）
                newPoc.setName(name);
                newPoc.setDescription(descField.getText().trim());
                newPoc.setLevel((String) levelComboBox.getSelectedItem());
                newPoc.setAuthor(authorField.getText().trim());
                
                if (newPoc.getRequest() == null) {
                    newPoc.setRequest(new POCConfig.Request());
                }
                newPoc.getRequest().setMethod(method);
                
                // 设置路径（可以为空）
                String pathValue = pathField.getText().trim();
                if (!pathValue.isEmpty()) {
                    newPoc.getRequest().setPath(pathValue);
                }
                
                // 解析参数
                String paramsText = paramsField.getText().trim();
                if (!paramsText.isEmpty()) {
                    try {
                        Map<String, String> params = new HashMap<>();
                        String[] pairs = paramsText.split(",");
                        for (String pair : pairs) {
                            String[] keyValue = pair.split(":", 2);
                            if (keyValue.length == 2) {
                                params.put(keyValue[0].trim(), keyValue[1].trim());
                            }
                        }
                        newPoc.getRequest().setParams(params);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "参数格式错误");
                        return;
                    }
                } else {
                    newPoc.getRequest().setParams(null);
                }
                
                // 解析请求头
                String userAgent = headersField.getText().trim();
                if (!userAgent.isEmpty()) {
                    try {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("User-Agent", userAgent);
                        newPoc.getRequest().setHeaders(headers);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "请求头格式错误");
                        return;
                    }
                } else {
                    newPoc.getRequest().setHeaders(null);
                }
                
                // 请求体
                String bodyText = bodyField.getText().trim();
                if (!bodyText.isEmpty()) {
                    newPoc.getRequest().setBody(bodyText);
                } else {
                    newPoc.getRequest().setBody(null);
                }
                
                if (newPoc.getResponse() == null) {
                    newPoc.setResponse(new POCConfig.Response());
                }
                
                POCConfig.Response response = newPoc.getResponse();
                
                // 状态码
                String statusCodeText = statusCodeField.getText().trim();
                if (!statusCodeText.isEmpty()) {
                    try {
                        response.setStatusCode(Integer.parseInt(statusCodeText));
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "状态码必须是数字");
                        return;
                    }
                } else {
                    response.setStatusCode(null);
                }
                
                // 匹配规则类型
                String matchType;
                if (matchTypeComboBox.getSelectedIndex() == 0) {
                    matchType = "contains";
                } else if (matchTypeComboBox.getSelectedIndex() == 1) {
                    matchType = "equals";
                } else {
                    matchType = "regex";
                }
                response.setMatchType(matchType);
                
                // 成功特征
                String successText = successArea.getText().trim();
                if (!successText.isEmpty()) {
                    List<String> successIndicators = new ArrayList<>();
                    for (String line : successText.split("\\n")) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            successIndicators.add(line);
                        }
                    }
                    if (!successIndicators.isEmpty()) {
                        response.setSuccessIndicators(successIndicators);
                    } else {
                        response.setSuccessIndicators(null);
                    }
                } else {
                    response.setSuccessIndicators(null);
                }
                
                // 错误特征
                String errorText = errorArea.getText().trim();
                if (!errorText.isEmpty()) {
                    List<String> errorIndicators = new ArrayList<>();
                    for (String line : errorText.split("\\n")) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            errorIndicators.add(line);
                        }
                    }
                    if (!errorIndicators.isEmpty()) {
                        response.setErrorIndicators(errorIndicators);
                    } else {
                        response.setErrorIndicators(null);
                    }
                } else {
                    response.setErrorIndicators(null);
                }
                
                // 保存到文件
                if (savePOCToFile(newPoc, originalName)) {
                    dialog.dispose();
                    refreshPOCList();
                }
            });
            
            cancelButton.addActionListener(e -> dialog.dispose());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            
            dialog.setVisible(true);
        }
    }

    private void deletePOC(int rowIndex) {
        if (rowIndex < pocs.size()) {
            POCConfig poc = pocs.get(rowIndex);
            String pocDir = pocDirField.getText().trim();
            
            // 构建POC文件路径
            String fileName = poc.getName().replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "_") + ".yaml";
            File pocFile = new File(pocDir, fileName);
            
            int confirm = JOptionPane.showConfirmDialog(this,
                    "确定要删除POC: " + poc.getName() + " 吗？" + 
                    (pocFile.exists() ? "\n此操作将删除文件: " + pocFile.getAbsolutePath() : ""),
                    "确认删除", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    // 删除文件
                    if (pocFile.exists() && pocFile.delete()) {
                        JOptionPane.showMessageDialog(this, "POC删除成功: " + poc.getName());
                        refreshPOCList();
                    } else {
                        JOptionPane.showMessageDialog(this, "POC文件删除失败或文件不存在",
                                "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "删除POC出错: " + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private boolean savePOCToFile(POCConfig poc, String originalName) {
        try {
            String pocDir = pocDirField.getText().trim();
            
            // 确保目录存在
            File dir = new File(pocDir);
            if (!dir.exists() && !dir.mkdirs()) {
                JOptionPane.showMessageDialog(this, "无法创建POC目录: " + pocDir,
                        "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // 生成文件名并保存
            String fileName = poc.getName().replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "_") + ".yaml";
            File file = new File(dir, fileName);
            
            // 如果POC名称发生变化，删除旧文件
            if (originalName != null && !originalName.equals(poc.getName())) {
                String oldFileName = originalName.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "_") + ".yaml";
                File oldFile = new File(dir, oldFileName);
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }
            
            // 如果文件已存在，提示用户是否覆盖
            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(this, 
                        "POC文件 " + fileName + " 已存在，是否覆盖？",
                        "确认覆盖", JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return false; // 用户选择不覆盖
                }
                
                // 创建备份
                File backupFile = new File(file.getParent(), fileName + ".bak");
                try {
                    Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "创建备份文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            
            // 设置UTF-8编码写入文件，解决乱码问题
            Yaml yaml = new Yaml();
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                yaml.dump(poc, writer);
            }
            
            JOptionPane.showMessageDialog(this, "POC保存成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
            refreshPOCList(); // 刷新POC列表
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存POC文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    // 重载方法，保持向后兼容
    private boolean savePOCToFile(POCConfig poc) {
        return savePOCToFile(poc, null);
    }

    private void performSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        
        if (searchTerm.isEmpty()) {
            // 如果搜索词为空，显示所有POC
            refreshPOCList();
            return;
        }
        
        // 过滤POC列表
        tableModel.setRowCount(0);
        int count = 0;
        
        for (POCConfig poc : pocs) {
            // 检查名称是否包含搜索词
            if (poc.getName() != null && poc.getName().toLowerCase().contains(searchTerm)) {
                tableModel.addRow(new Object[]{
                        poc.getName(),
                        poc.getDescription() != null ? poc.getDescription() : "",
                        poc.getLevel() != null ? poc.getLevel() : "",
                        poc.getAuthor() != null ? poc.getAuthor() : ""
                });
                count++;
            }
        }
        
        statusLabel.setText("搜索完成，找到 " + count + " 个匹配的POC");
    }
    
    private void clearSearch() {
        searchField.setText("");
        refreshPOCList();
    }
    
    private void openPOCDirectory() {
        String directoryPath = pocDirField.getText().trim();
        
        if (directoryPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "POC目录路径为空");
            return;
        }

        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "POC目录不存在: " + directoryPath,
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // 使用系统默认方式打开目录
            Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "无法打开目录: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}