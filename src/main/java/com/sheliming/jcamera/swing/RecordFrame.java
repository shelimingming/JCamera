package com.sheliming.jcamera.swing;

import com.sheliming.jcamera.Camera;
import com.sheliming.jcamera.record.CameraRecord;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

/**
 * 预览摄像头的时候
 */
public class RecordFrame extends CanvasFrame {
    private Camera camera;
    private JPanel jPanel;
    private JButton recordButton;
    private JButton cancelButton;

    private OpenCVFrameGrabber grabber;

    //标记这个界面还有没有关闭，如果关闭了，就不捕获照片了
    private boolean isClosed = false;

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public RecordFrame(Camera camera, OpenCVFrameGrabber grabber) {
        super(camera.getName());

        this.camera = camera;
        this.grabber = grabber;

        init();

        this.setSize(980, 680);
        this.setVisible(true);
    }

    private void init() {
        recordButton = new JButton("开始录制");
        recordButton.addActionListener(new RecordActionListener());

        cancelButton = new JButton("取消");
        cancelButton.addActionListener(new StopRecordActionListener());

        jPanel = new JPanel();
        jPanel.setSize(980, 50);
        jPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        jPanel.add(cancelButton);
        jPanel.add(recordButton);

        getContentPane().add(canvas, BorderLayout.CENTER);
        getContentPane().add(jPanel, BorderLayout.NORTH);
    }

    private class RecordActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                CameraRecord.record(camera, 25, grabber);
            } catch (java.lang.Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private class StopRecordActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                CameraRecord.stopRecord(camera);
            } catch (java.lang.Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 当点击了右上角关闭按钮，将isClosed设为true
     *
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
