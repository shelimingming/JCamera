package com.sheliming.jcamera;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.VideoInputFrameGrabber;

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

    public void preview() {

    }

    public static void main(String[] args) {
        String[] allCameraName = getAllCameraName();
        for (String cameraName : allCameraName) {
            System.out.println(cameraName);
        }
    }
}
