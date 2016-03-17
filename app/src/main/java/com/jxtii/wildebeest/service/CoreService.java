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

import com.jxtii.wildebeest.bus.GpsInfoBus;
import com.jxtii.wildebeest.model.PointRecord;
import com.jxtii.wildebeest.model.PositionRecord;
import com.jxtii.wildebeest.util.CalPointUtil;
import com.jxtii.wildebeest.util.CommUtil;
import com.jxtii.wildebeest.util.DateStr;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
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

    float[] accelerometerValues=new float[3];
    float[] magneticFieldValues=new float[3];
    float[] values=new float[3];
    float[] rotate=new float[9];
    GpsInfoBus pushBus = null;
    long gpsBearing = 60;//gps方向有效时间

    int i = 0;

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

        Sensor aSensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mSensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (aSensor != null)
            manager.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (mSensor != null)
            manager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        EventBus.getDefault().register(this);
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
                Log.e(TAG, "gAve = " + gAve);
                Log.e(TAG, "clampGAve = " + clampGAve);
//                Log.e(TAG, "event.values[0] = " + event.values[0]);
//                Log.e(TAG, "event.values[1] = " + event.values[1]);
//                Log.e(TAG, "event.values[2] = " + event.values[2]);
//                Log.e(TAG, "gValue[0] = " + gValue[0]);
//                Log.e(TAG, "gValue[1] = " + gValue[1]);
//                Log.e(TAG, "gValue[2] = " + gValue[2]);
                List<PositionRecord> list = DataSupport.order("dateStr desc").limit(2).find(PositionRecord.class);
                if(list!=null&&list.size()>1) {
                    int pointCal = CalPointUtil.calAccOrDec(gAve);
                    PointRecord pointRecord = new PointRecord();
                    pointRecord.setCreateTime(DateStr.yyyymmddHHmmssStr());
                    pointRecord.setRecord((float) gAve);
                    long timeFin = CommUtil.timeSpanSecond(list.get(0).getDateStr(), list.get(1).getDateStr());
                    Log.e(TAG, "timeFin = " + timeFin);
                    if(timeFin >= 60){
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

        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            accelerometerValues=event.values;
        }
        if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            magneticFieldValues=event.values;
        }

        SensorManager.getRotationMatrix(rotate, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(rotate, values);
        //经过SensorManager.getOrientation(rotate, values);得到的values值为弧度
        //转换为角度
        values[0]=(float)Math.toDegrees(values[0]);
        values[1]=(float)Math.toDegrees(values[1]);
        values[2]=(float)Math.toDegrees(values[2]);

        i++;
//        Log.w(TAG,">>>>" + i);
        if(i%25 == 0){
//            Log.e(TAG,">>>>" + i);
            Log.w(TAG, "values[0] = " + values[0]);
            Log.w(TAG, "values[1] = " + values[1]);
            Log.w(TAG, "values[2] = " + values[2]);

            double angleZc = Double.parseDouble(String.valueOf(-1*values[0]));
            double angleXc = Double.parseDouble(String.valueOf(-1*values[1]));
            double angleYc = Double.parseDouble(String.valueOf(values[2]));

            double[][] doubleFc = {{Math.cos(angleYc), 0, -1 * Math.sin(angleYc)}, {0, 1, 0}, {Math.sin(angleYc), 0, Math.cos(angleYc)}};
            double[][] doubleSc = {{1,0,0},{0,Math.cos(angleXc),Math.sin(angleXc)},{0,-1*Math.sin(angleXc),Math.cos(angleXc)}};
            double[][] doubleTc = {{Math.cos(angleZc),Math.sin(angleZc),0},{-1*Math.sin(angleZc),Math.cos(angleZc),0},{0,0,1}};

            Array2DRowRealMatrix matrixFc = new Array2DRowRealMatrix(doubleFc);
            Array2DRowRealMatrix matrixSc = new Array2DRowRealMatrix(doubleSc);
            Array2DRowRealMatrix matrixTc = new Array2DRowRealMatrix(doubleTc);

            Array2DRowRealMatrix transfor = matrixFc.multiply(matrixSc).multiply(matrixTc);

            if (this.pushBus != null) {
                String nowTime = DateStr.yyyymmddHHmmssStr();
                Boolean isValid = CommUtil.timeSpanSecond(this.pushBus.getCreateTime(), nowTime) > gpsBearing ? false : true;
                if (isValid) {
                    double[][] doubleEarth = {{this.pushBus.getxDrift(), this.pushBus.getyDrift(), this.pushBus.getzDrift()}};
                    Array2DRowRealMatrix earth = new Array2DRowRealMatrix(doubleEarth);
                    Array2DRowRealMatrix earth2phone = earth.multiply(transfor);
                    double[][] vDirect = earth2phone.getData();

                    double[] vDir = {vDirect[0][0], vDirect[0][1], vDirect[0][2]};
                    double[] aDir = {Double.parseDouble(String.valueOf(gValue[0])),
                            Double.parseDouble(String.valueOf(gValue[1])),
                            Double.parseDouble(String.valueOf(gValue[2]))};

                    ArrayRealVector vfc = new ArrayRealVector(vDir);
                    ArrayRealVector vSc = new ArrayRealVector(aDir);
                    double cosine = vfc.cosine(vSc);
                    Log.e(TAG, "cosine = " + cosine);
                    if(cosine == 0){
                        Log.e(TAG, "力与速度垂直");
                    }else if(cosine > 0){
                        Log.e(TAG, "加速");
                    }else{
                        Log.e(TAG, "减速");
                    }
                } else {
                    Log.w(TAG, "this.pushBus invalid");
                }
            } else {
                Log.w(TAG, "this.pushBus is null");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent(GpsInfoBus gpsInfoBus){
        Log.w(TAG, gpsInfoBus.toStr());
        this.pushBus = gpsInfoBus;

    }

    /**
     * 根据上下阀值过滤数据
     *
     * @param m
     * @return
     */
    float[] mechFilter(float m[]) {
        for (int i=0; i<3; ++i)
            if (!(m[i]>MIN_ACC || m[i]<-MIN_ACC))
                m[i]=0;
        return m;
    }

    /**
     * 根据上下阀值修正数据
     *
     * @param num
     * @param min
     * @param max
     * @return
     */
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
