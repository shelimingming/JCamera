package com.sheliming.jcamera;

import java.util.List;

public class CameraFactory {
    public static List<Camera> cameraList;

    public static List<Camera> getCameraList() {
        if(cameraList == null) {
            cameraList = CameraUtils.getAllCamera();
        }
        return cameraList;
    }

    public static void refresh() {
        cameraList = CameraUtils.getAllCamera();
    }

}
