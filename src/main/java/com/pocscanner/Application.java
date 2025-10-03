package com.pocscanner;

import com.pocscanner.gui.MainFrame;

import javax.swing.*;

import static javax.swing.UIManager.*;

public class Application {
    public static void main(String[] args) {
        // 故意抛出异常，使程序启动时报错

        
        // 原有代码被注释掉

        // 设置系统外观
        try {
            setLookAndFeel(getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 创建并显示GUI
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });

    }
}