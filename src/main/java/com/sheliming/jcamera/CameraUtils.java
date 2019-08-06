package com.sheliming.jcamera;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.VideoInputFrameGrabber;

import java.util.ArrayList;
import java.util.List;

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
     * 获取所有的摄像头
     * @return
     */
    public static List<Camera> getAllCamera() {
        List<Camera> cameraList = new ArrayList<Camera>();

        String[] allCameraName = getAllCameraName();

        for (int i = 0; i < allCameraName.length; i++) {
            Camera camera = new Camera(i, allCameraName[i]);
            cameraList.add(camera);
        }

        return cameraList;

    }

}
