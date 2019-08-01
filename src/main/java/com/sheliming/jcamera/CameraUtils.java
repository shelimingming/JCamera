package com.sheliming.jcamera;

import com.sheliming.jcamera.swing.PreviewFrame;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacv.*;

import javax.swing.*;
import java.awt.*;

public class CameraUtils {
    /**
     * 获取所有摄像头名称
     *
     * @return
     */
    public static String[] getAllCameraName() {
        String[] deviceDescriptions = null;
        try {
            deviceDescriptions = VideoInputFrameGrabber.getDeviceDescriptions();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        return deviceDescriptions;
    }

    /**
     * 预览摄像头
     */
    //TODO 对象有没有释放！！
    public static void preview(int deviceId) {
        opencv_videoio.VideoCapture vc = new opencv_videoio.VideoCapture(deviceId);
        //使用java的JFrame显示图像
        CanvasFrame cFrame = new CanvasFrame("opencv自带", CanvasFrame.getDefaultGamma() / 2.2);
        cFrame.setVisible(false);

        Canvas canvas = cFrame.getCanvas();
        PreviewFrame previewFrame = new PreviewFrame("预览摄像头",canvas);

        //javacv提供的转换器，方便mat转换为Frame
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        opencv_core.Mat mat = new opencv_core.Mat();

        while (true) {
            if (previewFrame.isClosed()) {//窗口是否关闭
                vc.release();//停止抓取
                return;
            }
            vc.retrieve(mat);//重新获取mat
            if (vc.grab()) {//是否采集到摄像头数据
                if (vc.read(mat)) {//读取一帧mat图像
                    cFrame.showImage(converter.convert(mat));
                }
                mat.release();//释放mat
            }

            //每45毫秒捕获一帧
            try {
                Thread.sleep(45);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    public static void main(String[] args) {
        String[] allCameraName = getAllCameraName();
        for(int i=0;i<allCameraName.length;i++){
            System.out.println(allCameraName[i]);
            preview(i);
        }

    }
}
