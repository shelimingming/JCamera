package com.sheliming.jcamera;

/**
 * 摄像头
 */
public class Camera {
    private int deviceId;
    private String name;
    private CamerStatus state = CamerStatus.close;

    public Camera(int deviceId, String name) {
        this.deviceId = deviceId;
        this.name = name;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CamerStatus getState() {
        return state;
    }

    public void setState(CamerStatus state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "deviceId=" + deviceId +
                ", name='" + name + '\'' +
                ", state=" + state +
                '}';
    }
}
