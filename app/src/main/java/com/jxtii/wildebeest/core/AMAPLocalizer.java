package com.jxtii.wildebeest.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.jxtii.wildebeest.model.PositionRecord;
import com.jxtii.wildebeest.util.CommUtil;
import com.jxtii.wildebeest.util.DateStr;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

/**
 * Created by huangyc on 2016/3/4.
 */
public class AMAPLocalizer implements AMapLocationListener {

    String TAG = AMAPLocalizer.class.getSimpleName();
    Context ctx;
    AMapLocationClient aMapLocationClient = null;
    volatile static AMAPLocalizer instance = null;
    public String locinfo = null;
    SQLiteDatabase db = null;

    private AMAPLocalizer(Context ctx) {
        this.ctx = ctx;
        if (aMapLocationClient == null)
            aMapLocationClient = new AMapLocationClient(ctx);
        aMapLocationClient.setLocationListener(this);
        db = Connector.getDatabase();
    }

    public static AMAPLocalizer getInstance(Context ctx) {
        if (instance == null) {
            synchronized (AMAPLocalizer.class) {
                if (instance == null) {
                    instance = new AMAPLocalizer(ctx);
                }
            }
        }
        return instance;
    }

    public static void destroyInstance(Context ctx) {
        if (instance != null) {
            synchronized (AMAPLocalizer.class) {
                instance = null;
            }
        }
    }

    /**
     * 开启或关闭定位
     *
     * @param swit
     *            开启或销毁定位，单次定位无需销毁
     * @param locMode
     *            定位模式
     * @param minTime
     *            定位间隔
     */
    public void setLocationManager(Boolean swit, String locMode, long minTime) {
        if (swit && this.aMapLocationClient != null) {
            setLocationOption(locMode, minTime);
            if(!aMapLocationClient.isStarted()){
                Log.i(TAG, "AMapLocationClient start");
                aMapLocationClient.startLocation();
            }else{
                Log.w(TAG,"AMapLocationClient haved start");
            }
        } else {
            if (this.aMapLocationClient != null) {
                this.aMapLocationClient.unRegisterLocationListener(this);
                this.aMapLocationClient.stopLocation();
                this.aMapLocationClient.onDestroy();
            }
            this.aMapLocationClient = null;
        }
    }

    void setLocationOption(String locMode, long minTime) {
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        if (TextUtils.isEmpty(locMode)) {
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);
            Log.i(TAG,"Device_Sensors");
        } else {
            if ("low".equalsIgnoreCase(locMode)) {
                locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
                Log.i(TAG,"Battery_Saving");
            } else if ("gps".equalsIgnoreCase(locMode)) {
                locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);
                Log.i(TAG,"Device_Sensors");
            } else {
                //TODO 设置该选项将延长定位返回时长30s，仅在高精度模式单次定位下有效
                locationOption.setGpsFirst(true);
                locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                Log.i(TAG,"Hight_Accuracy");
            }
        }
        if (minTime < 900) {
            locationOption.setOnceLocation(true);
        } else {
            locationOption.setOnceLocation(false);
            locationOption.setInterval(minTime);
        }
        locationOption.setMockEnable(false);//default false
        locationOption.setWifiActiveScan(true);//default true
        locationOption.setHttpTimeOut(15000);
        locationOption.setKillProcess(true);//default false
        locationOption.setNeedAddress(true);//default true
        locationOption.setOnceLocation(false);//default false
        aMapLocationClient.setLocationOption(locationOption);
    }

    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null && amapLocation.getErrorCode() == 0) {
            String extra = "";
//            Log.w(TAG,
//                    "amapLocation.getProvider() = "
//                            + amapLocation.getProvider());
            if ("gps".equals(amapLocation.getProvider())) {
                extra = "定位结果来自GPS信号";
                if (amapLocation.getSatellites() != 0)
                    extra += (",连接" + amapLocation.getSatellites() + "颗卫星");
                if (amapLocation.hasAccuracy())
                    extra += (",精度：" + amapLocation.getAccuracy() + "米");
                if (amapLocation.hasSpeed())
                    extra += (",速度:"
                            + CommUtil.floatToStr(
                            amapLocation.getSpeed() * 18 / 5, 1) + "公里/时");
                if (amapLocation.hasAltitude())
                    extra += (",海拔:"
                            + CommUtil.floatToStr(
                            (float) amapLocation.getAltitude(), 1) + "米");
                if (amapLocation.hasBearing())
                    extra += (",方向:北偏东" + amapLocation.getBearing() + "度");
            } else if ("lbs".equals(amapLocation.getProvider())) {
                if (amapLocation.hasAccuracy()) {
                    Float acc = amapLocation.getAccuracy();
                    if (acc <= 100.0) {
                        extra = "定位结果来自WIFI信号";
                    } else {
                        extra = "定位结果来自基站信号";
                    }
                    extra += (",精度：" + acc + "米");
                } else {
                    extra = "定位结果来自网络定位信号";
                }
            }
            extra += ",定位通道1";
            extra += amapLocation.getLocationType();
            extra += amapLocation.getLocationDetail();
            Double geoLat = amapLocation.getLatitude();
            Double geoLng = amapLocation.getLongitude();
            locinfo = geoLat + ";" + geoLng + ";定位器不做地址解析;"
                    + amapLocation.getProvider() + ";" + extra;
            PositionRecord pr = new PositionRecord();
            pr.setLat(geoLat);
            pr.setLng(geoLng);
            pr.setDateStr(DateStr.yyyymmddHHmmssStr());
            pr.setExtra(amapLocation.toStr());
            pr.save();
//            Log.i(TAG, "locinfo = " + locinfo);
        } else if (amapLocation != null && amapLocation.getErrorCode() != 0) {
            Log.w(TAG,
                    "amapLocation.getErrorCode() = "
                            + amapLocation.getErrorCode()
                            + " amapLocation.getErrorInfo() = "
                            + amapLocation.getErrorInfo());
            // 华为8817e实测 错误12：缺少定位权限，请给app授予定位权限。判断无效，监听无返回，没有错误抛出
            locinfo = amapLocation.getErrorCode() + "_"
                    + amapLocation.getErrorInfo();
        } else {
            locinfo = "获取定位信息失败";
        }
    }
}