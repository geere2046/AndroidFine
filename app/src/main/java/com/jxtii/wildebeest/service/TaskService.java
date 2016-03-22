package com.jxtii.wildebeest.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.amap.api.location.AMapLocation;
import com.jxtii.wildebeest.bean.PointRecordBus;
import com.jxtii.wildebeest.core.AMAPLocalizer;
import com.jxtii.wildebeest.model.CompreRecord;
import com.jxtii.wildebeest.model.PointRecord;
import com.jxtii.wildebeest.model.PositionRecord;
import com.jxtii.wildebeest.model.RouteLog;
import com.jxtii.wildebeest.util.CalPointUtil;
import com.jxtii.wildebeest.util.DateStr;
import com.jxtii.wildebeest.util.DistanceUtil;
import com.jxtii.wildebeest.util.SharedPreferences;
import com.jxtii.wildebeest.webservice.WebserviceClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.util.HashMap;
import java.util.Map;
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
    AMapLocation amapLocation;
    AMapLocation amapLocationClone;

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
                        uploadLocInfo();
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
    public void oreceiveAMapLocation(AMapLocation amapLocation){
        Log.w(TAG, "AMapLocation is " + amapLocation.toStr());
        this.amapLocation = amapLocation;
        this.amapLocationClone = amapLocation;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void receivePointRecordBus(PointRecordBus bus) {
        Log.w(TAG, "PointRecordBus is " + bus.toStr());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("sqlKey", "sql_insert_route_factor");
        params.put("sqlType", "sql");
        RouteLog log = DataSupport.findLast(RouteLog.class);
        if(log != null){
            params.put("rRouteId", log.getpRouteId());
            if (this.amapLocationClone != null) {
                params.put("rLat", this.amapLocationClone.getLatitude());
                params.put("rLon", this.amapLocationClone.getLongitude());
                params.put("rAlt", this.amapLocationClone.getAltitude());
            }else{
                params.put("rLat", 0.0);
                params.put("rLon", 0.0);
                params.put("rAlt", 0.0);
            }
            if (bus.getEventType() == 1) {
                params.put("rSpeed", Double.valueOf(String.valueOf(bus.getRecord())));
                params.put("rType", "00");
                params.put("rAccelerate",0.0);
            } else if (bus.getEventType() == 2) {
                params.put("rAccelerate", Double.valueOf(String.valueOf(bus.getRecord())));
                params.put("rType", "01");
                params.put("rSpeed",0.0);
            } else if (bus.getEventType() == 3) {
                params.put("rAccelerate", Double.valueOf(String.valueOf(bus.getRecord())));
                params.put("rType", "02");
                params.put("rSpeed",0.0);
            }
            String paramStr = JSON.toJSONString(params);
            new WebserviceClient().updateData(paramStr);
        }
    }

    void uploadLocInfo() {
        try {
            String locinfo = (amapLocalizer != null) ? amapLocalizer.locinfo : "";
            if (!TextUtils.isEmpty(locinfo)) {
                locinfo = "";
                Log.w(TAG, locinfo);
            }
            if (this.amapLocation != null) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("sqlKey", "sql_insert_location_info");
                params.put("sqlType", "sql");
                RouteLog log = DataSupport.findLast(RouteLog.class);
                if(log != null){
                    params.put("routeId", log.getpRouteId());
                    params.put("lat", this.amapLocation.getLatitude());
                    params.put("lng", this.amapLocation.getLongitude());
                    params.put("alt", this.amapLocation.getAltitude());
                    params.put("speed", this.amapLocation.getSpeed());
                    params.put("accelerate", 0);
                    params.put("addr", this.amapLocation.getAddress());
                    params.put("loctime", DateStr.yyyymmddHHmmssStr());
                    String paramStr = JSON.toJSONString(params);
                    new WebserviceClient().updateData(paramStr);
                    this.amapLocation = null;
                }
            }
        } catch (Exception e) {
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
