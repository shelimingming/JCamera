package com.sheliming.jcamera.demo;

import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacpp.opencv_core.*;

import static org.bytedeco.javacpp.opencv_core.addWeighted;

public class WaterMarkDemo {
    public static void main(String[] args) throws Exception {
        // 转换器，用于Frame/Mat/IplImage相互转换
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        // 使用OpenCV抓取本机摄像头，摄像头设备号默认0
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        // 开启抓取器
        grabber.start();
        //做好自己 - - eguid!,转载请注明出处
        CanvasFrame cFrame = new CanvasFrame("做好自己！--eguid！", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        cFrame.setAlwaysOnTop(true);
        cFrame.setVisible(true);
        // 水印文字位置
        Point point = new Point(10, 50);
        // 颜色，使用黄色
        Scalar scalar = new Scalar(0, 255, 255, 0);
        Frame frame = null;
        int index = 0;

        Mat logo = opencv_imgcodecs.imread("4ycfb.png");
        Mat mask = opencv_imgcodecs.imread("4ycfb.png", 0);

        opencv_imgproc.threshold(mask,mask,254,255,opencv_imgcodecs.IMWRITE_PNG_BILEVEL);

        double alpha = 0.5;// 图像透明权重值,0-1之间
        while (cFrame.isShowing()) {
            if ((frame = grabber.grabFrame()) != null) {
                // 取一帧视频（图像），并转换为Mat
                Mat mat = converter.convertToMat(grabber.grabFrame());

                // 加文字水印，opencv_imgproc.putText（图片，水印文字，文字位置，字体，字体大小，字体颜色，字体粗度，平滑字体，是否翻转文字）
                opencv_imgproc.putText(mat, "eguid!", point, opencv_imgproc.CV_FONT_VECTOR0, 1.2, scalar, 1, 20, false);
                // 定义感兴趣区域(位置，logo图像大小)
                Mat ROI = mat.apply(new Rect(400, 350, logo.cols(), logo.rows()));

                addWeighted(ROI, alpha, logo, 1.0 - alpha, 0.0, ROI);
                // 把logo图像复制到感兴趣区域
//				 logo.copyTo(ROI, mask);
                // 显示图像到窗口
                cFrame.showImage(converter.convert(mat));
                if (index == 0) {
                    // 保存第一帧图片到本地
                    opencv_imgcodecs.imwrite("eguid.jpg", mat);
                }
                // 释放Mat资源
                ROI.release();
                ROI.close();
                mat.release();
                mat.close();
                Thread.sleep(40);
                index++;
            }

            index++;
        }
        // 关闭窗口
        cFrame.dispose();
        // 停止抓取器
        grabber.stop();
        // 释放资源
        logo.release();
        logo.close();
        mask.release();
        mask.close();
//        scalar.close();
//        point.close();
    }
}
