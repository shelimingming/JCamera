package com.sheliming.jcamera.demo;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacpp.opencv_videoio.*;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.awt.*;
import java.text.DecimalFormat;

public class OpenCVDemo2 {
    public static void main(String[] args) {
        String msg="fps:";//水印文字
        // 水印文字位置
        Point point = new Point(10, 50);
        // 颜色，使用黄色
        Scalar scalar = new Scalar(0, 255, 255, 0);
        DecimalFormat df=new DecimalFormat(".##");//数字格式化
        VideoCapture vc=null;
        //遍历查找摄像头
        int index=-1;
        for(;index<2;index++){
            vc=new opencv_videoio.VideoCapture(index);
            if(vc.grab()){
                //找到摄像头设备，退出遍历
                System.err.println("做好自己！--eguid温馨提示，获取本机当前摄像头序号："+index);
                break;
            }
            vc.close();//没找到设备，释放资源
        }
        //vc为null，并且设备没正常开启，说明没找到设备
        if(vc!=null&&!vc.isOpened()){
            System.err.println("无法找到摄像头，请检查是否存在摄像头设备");
            return;
        }
        //使用java的JFrame显示图像
        CanvasFrame cFrame = new CanvasFrame("做好自己！--eguid！http://www.eguid.cc",CanvasFrame.getDefaultGamma()/2.2);
        //javacv提供的转换器，方便mat转换为Frame
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        Mat mat=new Mat();
        double start=System.currentTimeMillis();
        double end;
        for(int i=0;;i++){
            vc.retrieve(mat);//重新获取mat
            if(vc.grab()){//是否采集到摄像头数据
                if(vc.read(mat)){//读取一帧mat图像
                    end=System.currentTimeMillis();
                    if(mat!=null){
                        opencv_imgproc.putText(mat,msg+df.format((1000.0/(end-start))), point, opencv_imgproc.CV_FONT_VECTOR0, 1.2, scalar, 1, 20, false);
                    }
//				opencv_highgui.imshow("eguid", mat);该opencv方法windows下会无响应
                    cFrame.showImage(converter.convert(mat));
                    System.err.println(i);
                    start=end;
                }
                mat.release();//释放mat
            }
        }
    }
}
