package com.byteshaft.healthvideo.serializers;

import java.io.Serializable;

/**
 * Created by s9iper1 on 8/8/17.
 */

public class NurseDetails implements Serializable{

    private String deviceId;
    private int fileId;
    private String location;
    private String timeStamp;
    private boolean isCurrentLocation;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isCurrentLocation() {
        return isCurrentLocation;
    }

    public void setCurrentLocation(boolean currentLocation) {
        isCurrentLocation = currentLocation;
    }
}
