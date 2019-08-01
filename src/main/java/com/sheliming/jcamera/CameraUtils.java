package com.sheliming.jcamera;

import org.bytedeco.javacv.*;

import javax.swing.*;

public class CameraUtils {
    /**
     * 获取所有摄像头名称
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
    public static void preview(int deviceId) throws FrameGrabber.Exception, InterruptedException {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(deviceId);
        grabber.start();   //开始获取摄像头数据
        CanvasFrame canvas = new CanvasFrame("摄像头");//新建一个窗口
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);
        while (true) {
            if (!canvas.isDisplayable()) {//窗口是否关闭
                grabber.stop();//停止抓取
                System.exit(-1);//退出
            }

            Frame frame = grabber.grab();

            canvas.showImage(frame);//获取摄像头图像并放到窗口上显示， 这里的Frame frame=grabber.grab(); frame是一帧视频图像
            Thread.sleep(50);//50毫秒刷新一次图像
        }

    }

    public static void main(String[] args) {
        String[] allCameraName = getAllCameraName();
        for (String cameraName : allCameraName) {
            System.out.println(cameraName);
        }

        try {
            preview(0);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
