package com.jxtii.wildebeest.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.jxtii.wildebeest.core.AMAPLocalizer;
import com.jxtii.wildebeest.model.CompreRecord;
import com.jxtii.wildebeest.model.PointRecord;
import com.jxtii.wildebeest.model.PositionRecord;
import com.jxtii.wildebeest.util.CalPointUtil;
import com.jxtii.wildebeest.util.DateStr;
import com.jxtii.wildebeest.util.DistanceUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by huangyc on 2016/3/4.
 */
public class TaskService extends Service {

    String TAG = TaskService.class.getSimpleName();
    Context ctx;
    AMAPLocalizer amapLocalizer;
    Timer mTimer;
    TimerTask mTimerTask;
    int interval = 900;
    PowerManager.WakeLock m_wakeLockObj;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, ">>>>>>>onCreate service");
        ctx = TaskService.this;
        amapLocalizer = AMAPLocalizer.getInstance(ctx);
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w(TAG, ">>>>>>>onStartCommand intent is null");
            stopSelfSevice();
        } else {
            interval = intent.getIntExtra("interval", 900);
            Log.w(TAG, ">>>>>>>onStartCommand interval = " + interval);
            if (amapLocalizer != null)
                amapLocalizer.setLocationManager(true, "gps", interval);
            stopTimer();
            if (mTimer == null)
                mTimer = new Timer();
            if (mTimerTask == null) {
                mTimerTask = new TimerTask() {
                    public void run() {
                        acquireWakeLock(ctx);
//                        sendAmapLocInfo();
                        releaseWakeLock();
                    }
                };
            }
            mTimer.scheduleAtFixedRate(mTimerTask, 1 * 1000,
                    interval);
        }
        return START_STICKY;
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent(AMapLocation amapLocation){
        Log.w(TAG, amapLocation.toStr());
    }

    void sendAmapLocInfo() {
        try {
            String locinfo = (amapLocalizer != null) ? amapLocalizer.locinfo : "";
            if(!TextUtils.isEmpty(locinfo)){
//                Log.w(TAG, locinfo);
                locinfo ="";
            }
            int prCount = DataSupport.count(PositionRecord.class);
            Log.w(TAG, ">>>>>>>>> prCount = "+prCount);
            /******************模拟GPS数据******************************/
            /*double geoLat = 28.677822 + new Random().nextFloat()/2000;
            double geoLng = 115.90674 + new Random().nextFloat()/2000;
            float curSpeed = new Random().nextFloat() * 130;
            PositionRecord pr = new PositionRecord();
            pr.setLat(geoLat);
            pr.setLng(geoLng);
            pr.setDateStr(DateStr.yyyymmddHHmmssStr());
            pr.setSpeed(curSpeed);
            pr.save();
            int crCount = DataSupport.count(CompreRecord.class);
            Log.e(TAG, ">>>>>>>>>>>>> crCount + " + crCount);
            if(crCount == 0){
                CompreRecord cr = new CompreRecord();
                cr.setBeginTime(DateStr.yyyymmddHHmmssStr());
                cr.setCurrentTime(DateStr.yyyymmddHHmmssStr());
                cr.setMaxSpeed(curSpeed);
                cr.setTravelMeter(0);
                cr.setSaveLat(geoLat);
                cr.setSaveLng(geoLng);
                cr.save();
            }else{
                CompreRecord cr = new CompreRecord();
                cr.setCurrentTime(DateStr.yyyymmddHHmmssStr());
                CompreRecord lastCr = DataSupport.findLast(CompreRecord.class);
                if(lastCr != null){
                    float lastSpeed = lastCr.getMaxSpeed();
                    if(curSpeed > lastSpeed){
                        cr.setMaxSpeed(curSpeed);
                    }
                    float lastDis = lastCr.getTravelMeter();
                    float curDistance = (float)DistanceUtil.distance(geoLng,geoLat,lastCr.getSaveLng(),lastCr.getSaveLat());
                    cr.setTravelMeter(lastDis + curDistance);
                    cr.update(lastCr.getId());
                }
            }

            int pointSpeed = CalPointUtil.calSpeeding(curSpeed);
            if(pointSpeed > 0){
                PointRecord pointRecord = new PointRecord();
                pointRecord.setCreateTime(DateStr.yyyymmddHHmmssStr());
                pointRecord.setEventType(1);
                pointRecord.setRecord(curSpeed);
                pointRecord.setPoint(pointSpeed);
                pointRecord.save();
            }*/
            /******************模拟GPS数据******************************/

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.w(TAG,">>>>>>>>  onDestroy");
        super.onDestroy();
        stopSelfSevice();
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG,">>>>>>>>  onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.w(TAG,">>>>>>>>  onTrimMemory");
        super.onTrimMemory(level);
    }

    void stopSelfSevice() {
        Log.w(TAG,">>>>>>>>  stopSelfSevice");
//        AlarmManager am = (AlarmManager) this
//                .getSystemService(Context.ALARM_SERVICE);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 3,
//                new Intent(CommUtil.START_INTENT), 0);
//        long triggerAtTime = SystemClock.elapsedRealtime() + 5 * 1000;
//        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime,
//                pendingIntent);
        //暂时屏蔽停止定位服务
//        if (amapLocalizer != null)
//            amapLocalizer.setLocationManager(false, "", 0);
        stopTimer();
        EventBus.getDefault().unregister(this);
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
