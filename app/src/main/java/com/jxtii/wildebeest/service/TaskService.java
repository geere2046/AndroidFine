package com.jxtii.wildebeest.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.jxtii.wildebeest.core.AMAPLocalizer;
import com.jxtii.wildebeest.util.CommUtil;

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
        String locinfo = (amapLocalizer != null) ? amapLocalizer.locinfo : "";
        Log.e(TAG,locinfo);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    void stopSelfSevice() {
        AlarmManager am = (AlarmManager) this
                .getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 3,
                new Intent(CommUtil.START_INTENT), 0);
        long triggerAtTime = SystemClock.elapsedRealtime() + 5 * 1000;
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime,
                pendingIntent);
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
        Log.i(TAG, ">>>>>>点亮屏幕");
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
        Log.i(TAG, ">>>>>>取消点亮");
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
