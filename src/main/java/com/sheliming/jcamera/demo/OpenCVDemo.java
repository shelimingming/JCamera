package com.sheliming.jcamera.demo;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

public class OpenCVDemo {
    public static void main(String[] args) {
        opencv_videoio.VideoCapture vc = null;
        //遍历查找摄像头
        int index = -1;
        for (; index < 2; index++) {
            vc = new opencv_videoio.VideoCapture(index);
            if (vc.grab()) {
                //找到摄像头设备，退出遍历
                System.err.println("当前摄像头：" + index);
                break;
            }
            vc.close();//没找到设备，释放资源
        }
        //vc为null，并且设备没正常开启，说明没找到设备
        if (vc != null && !vc.isOpened()) {
            System.err.println("无法找到摄像头，请检查是否存在摄像头设备");
            return;
        }
        //使用java的JFrame显示图像
        CanvasFrame cFrame = new CanvasFrame("做好自己！--eguid！http://www.eguid.cc/", CanvasFrame.getDefaultGamma() / 2.2);
        //javacv提供的转换器，方便mat转换为Frame
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        opencv_core.Mat mat = new opencv_core.Mat();
        for (; ; ) {
            vc.retrieve(mat);//重新获取mat
            if (vc.grab()) {//是否采集到摄像头数据
                if (vc.read(mat)) {//读取一帧mat图像
//				opencv_highgui.imshow("eguid", mat);该opencv方法windows下会无响应
                    cFrame.showImage(converter.convert(mat));
                }
                mat.release();//释放mat
            }

            try {
                Thread.sleep(45);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
