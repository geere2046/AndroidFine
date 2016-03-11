package com.jxtii.wildebeest.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jxtii.wildebeest.model.PointRecord;
import com.jxtii.wildebeest.model.PositionRecord;
import com.jxtii.wildebeest.util.CalPointUtil;
import com.jxtii.wildebeest.util.DateStr;

import org.litepal.crud.DataSupport;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by huangyc on 2016/3/10.
 */
public class CoreService extends Service implements SensorEventListener{

    String TAG = CoreService.class.getSimpleName();
    Context ctx = null;
    Timer mTimer;
    TimerTask mTimerTask;
    PowerManager.WakeLock m_wakeLockObj;
    Boolean haveSensor =false;
    float[] gValue = new float[3];
    float MIN_ACC = 0.01f;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, ">>>>>>>onCreate service");
        ctx = CoreService.this;
        SensorManager manager = (SensorManager) ctx.getSystemService(ctx.SENSOR_SERVICE);
        List<Sensor> list = manager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
        if(list !=null && list.size() >0){
            haveSensor= true;
            //SENSOR_DELAY_UI 70ms
            //SENSOR_DELAY_NORMAL 200ms
            //SENSOR_DELAY_FASTEST 20ms
            //SENSOR_DELAY_GAME 20ms
            manager.registerListener(this,list.get(0),SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w(TAG, ">>>>>>>onStartCommand intent is null");
            stopSelfSevice();
        } else {
            stopTimer();
            if (mTimer == null)
                mTimer = new Timer();
            if (mTimerTask == null) {
                mTimerTask = new TimerTask() {
                    public void run() {
                        acquireWakeLock(ctx);
                        getInfo();
                        releaseWakeLock();
                    }
                };
            }
//            mTimer.scheduleAtFixedRate(mTimerTask, 1 * 1000,
//                    60 * 1000);
        }
        return START_STICKY;
    }

    void getInfo() {
        try {

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, ">>>>>>>>  onDestroy");
        super.onDestroy();
        stopSelfSevice();
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, ">>>>>>>>  onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.w(TAG, ">>>>>>>>  onTrimMemory");
        super.onTrimMemory(level);
    }

    void stopSelfSevice() {
        Log.w(TAG,">>>>>>>>  stopSelfSevice");
        stopTimer();
        this.stopSelf();
    }

    void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onSensorChanged(SensorEvent event) {
        if (Sensor.TYPE_LINEAR_ACCELERATION == event.sensor.getType()) {

            gValue[0] = m2g(event.values[0]);
            gValue[1] = m2g(event.values[1]);
            gValue[2] = m2g(event.values[2]);
            gValue = mechFilter(gValue);

            double gAve = Math.abs(Math.sqrt(gValue[0] * gValue[0] + gValue[1] * gValue[1] + gValue[2] * gValue[2]));
            double clampGAve = clamp(gAve,0.0,1.0);

            if (gAve > 0.3) {
//                Log.e(TAG, "gAve = " + gAve);
//                Log.e(TAG, "clampGAve = " + clampGAve);
//                Log.e(TAG, "event.values[0] = " + event.values[0]);
//                Log.e(TAG, "event.values[1] = " + event.values[1]);
//                Log.e(TAG, "event.values[2] = " + event.values[2]);
//                Log.e(TAG, "gValue[0] = " + gValue[0]);
//                Log.e(TAG, "gValue[1] = " + gValue[1]);
//                Log.e(TAG, "gValue[2] = " + gValue[2]);
                List<PositionRecord> list = DataSupport.order("id desc").limit(2).find(PositionRecord.class);
                if(list!=null&&list.size()>1) {
                    int pointCal = CalPointUtil.calAccOrDec(gAve);
                    PointRecord pointRecord = new PointRecord();
                    pointRecord.setCreateTime(DateStr.yyyymmddHHmmssStr());
                    pointRecord.setRecord((float) gAve);
                    if (list.get(0).getSpeed() > list.get(1).getSpeed()) {
                        //加速状态
                        pointRecord.setEventType(2);
                        pointRecord.setPoint(pointCal+5);
                        pointRecord.save();
                    } else if (list.get(0).getSpeed() < list.get(1).getSpeed()) {
                        //减速状态
                        pointRecord.setEventType(3);
                        pointRecord.setPoint(pointCal+10);
                        pointRecord.save();
                    } else {
                        //无法判断加、减速
                    }
                }
            }
        }
    }

    float[] mechFilter(float m[]) {
        for (int i=0; i<3; ++i)
            if (!(m[i]>MIN_ACC || m[i]<-MIN_ACC))
                m[i]=0;
        return m;
    }

    double clamp(double num, double min, double max){
        return num < min ? min : (num > max ? max : num);
    }

    /**
     * 加速度单位由m/s2转为g
     *
     * @param m
     * @return
     */
    float m2g(float m){
        float re = m * 5 /49;
        return re;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void acquireWakeLock(Context cxt) {
        Log.d(TAG, ">>>>>>点亮屏幕");
        if (m_wakeLockObj == null) {
            PowerManager pm = (PowerManager) cxt
                    .getSystemService(Context.POWER_SERVICE);
            m_wakeLockObj = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, TAG);
            m_wakeLockObj.acquire();
        }
    }

    public void releaseWakeLock() {
        Log.d(TAG, ">>>>>>取消点亮");
        if (m_wakeLockObj != null && m_wakeLockObj.isHeld()) {
            m_wakeLockObj.setReferenceCounted(false);// 处理RuntimeException:
            // WakeLock
            // under-locked
            // BaiDuLocReceiver
            m_wakeLockObj.release();
            m_wakeLockObj = null;
        }
    }
}
