package com.jxtii.wildebeest.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.jxtii.wildebeest.core.AMAPLocalizer;
import com.jxtii.wildebeest.model.PositionRecord;
import com.jxtii.wildebeest.util.CommUtil;

import org.litepal.crud.DataSupport;

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
                        sendAmapLocInfo();
                        releaseWakeLock();
                    }
                };
            }
            mTimer.scheduleAtFixedRate(mTimerTask, 1 * 1000,
                    interval);
        }
        return START_STICKY;
    }

    /**
     * 上报定位信息
     */
    void sendAmapLocInfo() {
        try {
            String locinfo = (amapLocalizer != null) ? amapLocalizer.locinfo : "";
            if(!TextUtils.isEmpty(locinfo)){
                Log.w(TAG, locinfo);
            }
            int prCount = DataSupport.count(PositionRecord.class);
            Log.e(TAG, ">>>>>>>>>"+prCount);
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
        if (amapLocalizer != null)
            amapLocalizer.setLocationManager(false, "", 0);
        stopTimer();
        this.stopSelf();
    }

    /**
     * 停止定时任务
     */
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
