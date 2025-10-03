package com.pocscanner.gui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private ScannerPanel scannerPanel;
    private ResultPanel resultPanel;
    private POCManagerPanel pocManagerPanel;

    public MainFrame() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Java POC扫描器 v1.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 创建选项卡
        JTabbedPane tabbedPane = new JTabbedPane();

        scannerPanel = new ScannerPanel();
        resultPanel = new ResultPanel();
        pocManagerPanel = new POCManagerPanel();

        tabbedPane.addTab("扫描器", scannerPanel);
        tabbedPane.addTab("扫描结果", resultPanel);
        tabbedPane.addTab("POC管理", pocManagerPanel);

        add(tabbedPane);

        // 连接面板间的事件
        scannerPanel.setResultPanel(resultPanel);
    }
}