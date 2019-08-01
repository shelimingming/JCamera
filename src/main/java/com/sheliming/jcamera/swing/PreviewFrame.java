package com.sheliming.jcamera.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * 预览摄像头的时候
 */
public class PreviewFrame extends JFrame {
    private JPanel jPanel;
    private Canvas cameraCanvas;

    //标记这个界面还有没有关闭，如果关闭了，就不捕获照片了
    private boolean isClosed = false;

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public PreviewFrame(String CameraName, Canvas canvas) {

        super(CameraName);

        this.cameraCanvas = canvas;

        this.setSize(700, 500);
        setLocationByPlatform(true);

        this.setVisible(true);

        jPanel = new JPanel();
        jPanel.setLayout(null);
        jPanel.add(cameraCanvas);

        this.add(jPanel);
    }

    /**
     * 当点击了右上角关闭按钮，将isClosed设为true
     * @param e
     */
    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            isClosed = true;
            super.processWindowEvent(e);
        } else {
            super.processWindowEvent(e);
        }
    }
}
