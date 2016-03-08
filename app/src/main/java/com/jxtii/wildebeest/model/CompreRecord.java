package com.jxtii.wildebeest.model;

import org.litepal.crud.DataSupport;

/**
 * Created by huangyc on 2016/3/8.
 */
public class CompreRecord extends DataSupport{

    private String beginTime;

    private String currentTime;

    private float maxSpeed;

    private float travelMeter;

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public float getTravelMeter() {
        return travelMeter;
    }

    public void setTravelMeter(float travelMeter) {
        this.travelMeter = travelMeter;
    }
}
