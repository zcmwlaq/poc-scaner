package com.pocscanner.gui;

import com.pocscanner.core.ScannerEngine;
import com.pocscanner.core.model.ScanRequest;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

public class ScannerPanel extends JPanel {
    private JTextField targetField;
    private JTextField pocDirectoryField;
    private JButton browseButton;
    private JButton startButton;
    private JButton clearLogButton;
    private JProgressBar progressBar;
    private JTextArea logArea;
    private ResultPanel resultPanel;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JComboBox<String> proxyTypeComboBox; // 新增代理类型选择框
    private JCheckBox enableProxyCheckBox; // 新增代理启用复选框
    private JSlider threadCountSlider; // 新增线程数滑块
    private JLabel threadCountLabel; // 显示当前线程数的标签

    public ScannerPanel() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
    }

    private void initializeComponents() {
        targetField = new JTextField(30);
        pocDirectoryField = new JTextField(30);
        pocDirectoryField.setText("./poc"); // 设置默认POC目录为./poc
        browseButton = new JButton("浏览");
        startButton = new JButton("开始扫描");
        clearLogButton = new JButton("清除日志");
        progressBar = new JProgressBar();
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        proxyHostField = new JTextField(15);
        proxyHostField.setText("127.0.0.1"); // 设置默认代理主机
        proxyPortField = new JTextField(5);
        proxyPortField.setText("8080"); // 设置默认代理端口
        
        // 新增代理启用复选框
        enableProxyCheckBox = new JCheckBox("启用代理");
        enableProxyCheckBox.setSelected(false); // 默认不启用代理
        
        // 新增代理类型选择框
        proxyTypeComboBox = new JComboBox<>(new String[]{"HTTP", "HTTPS", "SOCKS"});
        proxyTypeComboBox.setSelectedIndex(0); // 默认选择HTTP
        
        // 初始化线程控制组件
        threadCountSlider = new JSlider(1, 50, 10); // 范围1-50，默认10
        threadCountSlider.setMajorTickSpacing(10);
        threadCountSlider.setMinorTickSpacing(1);
        threadCountSlider.setPaintTicks(true);
        threadCountSlider.setPaintLabels(true);
        threadCountLabel = new JLabel("线程数: 10");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());

        // 输入面板
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("扫描配置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 目标URL
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("目标URL:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        inputPanel.add(targetField, gbc);

        // POC目录
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("POC目录:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(pocDirectoryField, gbc);
        gbc.gridx = 2;
        inputPanel.add(browseButton, gbc);

        // 代理设置
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("代理设置:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1;
        JPanel proxyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        proxyPanel.add(enableProxyCheckBox); // 添加代理启用复选框
        proxyPanel.add(new JLabel("类型:"));
        proxyPanel.add(proxyTypeComboBox);
        proxyPanel.add(new JLabel("主机:"));
        proxyPanel.add(proxyHostField);
        proxyPanel.add(new JLabel("端口:"));
        proxyPanel.add(proxyPortField);
        inputPanel.add(proxyPanel, gbc);

        // 线程控制
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("线程控制:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JPanel threadControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        threadControlPanel.add(threadCountLabel);
        threadControlPanel.add(threadCountSlider);
        inputPanel.add(threadControlPanel, gbc);

        // 开始扫描按钮
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        inputPanel.add(startButton, gbc);

        // 进度条
        gbc.gridy = 5;
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        inputPanel.add(progressBar, gbc);

        // 日志面板
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("扫描日志"));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        logPanel.add(clearLogButton, BorderLayout.SOUTH);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(logPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.NORTH);
    }

    private void setupEventListeners() {
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int option = fileChooser.showOpenDialog(ScannerPanel.this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedDirectory = fileChooser.getSelectedFile();
                    pocDirectoryField.setText(selectedDirectory.getAbsolutePath());
                }
            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startScanning();
            }
        });

        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logArea.setText("");
            }
        });
        
        // 添加线程数滑块事件监听器
        threadCountSlider.addChangeListener(e -> {
            int threadCount = threadCountSlider.getValue();
            threadCountLabel.setText("线程数: " + threadCount);
        });
    }

    public void setResultPanel(ResultPanel resultPanel) {
        this.resultPanel = resultPanel;
    }

    private void startScanning() {
        String target = targetField.getText().trim();
        String pocDirectory = pocDirectoryField.getText().trim();
        String proxyHost = proxyHostField.getText().trim();
        String proxyPortStr = proxyPortField.getText().trim();
        String proxyType = (String) proxyTypeComboBox.getSelectedItem(); // 获取选择的代理类型
        int threadCount = threadCountSlider.getValue(); // 获取线程数

        if (target.isEmpty() || pocDirectory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写目标URL和POC目录", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 验证代理设置（如果提供了代理）
        int proxyPort = 0;
        if (enableProxyCheckBox.isSelected()) {
            if (proxyHost.isEmpty() || proxyPortStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请完整填写代理主机和端口", "代理设置错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                proxyPort = Integer.parseInt(proxyPortStr);
                if (proxyPort <= 0 || proxyPort > 65535) {
                    JOptionPane.showMessageDialog(this, "代理端口必须在1-65535之间", "代理设置错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "代理端口必须是有效的数字", "代理设置错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // 创建扫描请求
        ScanRequest request = new ScanRequest();
        request.setTarget(target);
        request.setSelectedPOCs(pocDirectory);
        request.setThreadCount(threadCount); // 设置线程数
        request.setTimeout(10000); // 默认超时时间

        // 创建扫描引擎
        ScannerEngine engine = new ScannerEngine(pocDirectory);
        engine.setThreadCount(threadCount); // 设置引擎线程数
        
        // 设置代理（如果启用了代理）
        if (enableProxyCheckBox.isSelected()) {
            engine.setProxy(proxyHost, proxyPort, proxyType.toLowerCase()); // 使用选择的代理类型
        }

        // 启动扫描任务
        startButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        logArea.append("开始扫描...\n");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                engine.scan(request, new ScannerEngine.ScanListener() {
                    @Override
                    public void onLog(String message) {
                        publish(message);
                    }

                    @Override
                    public void onProgress(int progress) {
                        // 这里可以根据需要更新进度条
                    }

                    @Override
                    public void onResult(com.pocscanner.core.model.ScanResult result) {
                        // 结果会在扫描完成后统一处理
                    }
                });
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    logArea.append(message + "\n");
                }
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }

            @Override
            protected void done() {
                logArea.append("扫描完成\n");
                progressBar.setVisible(false);
                startButton.setEnabled(true);
                
                // 显示扫描结果
                if (resultPanel != null) {
                    resultPanel.displayResults(engine.getScanResults());
                }
                
                engine.shutdown();
            }
        };

        worker.execute();
    }
}