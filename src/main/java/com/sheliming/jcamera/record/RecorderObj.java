package com.sheliming.jcamera.record;

import com.sheliming.jcamera.Camera;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameGrabber;

/**
 * 录像所需要的对象
 */
public class RecorderObj {
    private Camera camera;
    private String  saveFileName;
    private OpenCVFrameGrabber grabber;
    private FFmpegFrameRecorder recorder;

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public String getSaveFileName() {
        return saveFileName;
    }

    public void setSaveFileName(String saveFileName) {
        this.saveFileName = saveFileName;
    }

    public OpenCVFrameGrabber getGrabber() {
        return grabber;
    }

    public void setGrabber(OpenCVFrameGrabber grabber) {
        this.grabber = grabber;
    }

    public FFmpegFrameRecorder getRecorder() {
        return recorder;
    }

    public void setRecorder(FFmpegFrameRecorder recorder) {
        this.recorder = recorder;
    }
}
