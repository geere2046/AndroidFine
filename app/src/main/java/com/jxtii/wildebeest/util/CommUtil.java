package com.jxtii.wildebeest.util;

import android.app.ActivityManager;
import android.content.Context;
import android.location.LocationManager;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by huangyc on 2016/3/3.
 */
public class CommUtil {

    public static final String START_INTENT = "com.jxtii.wildebeest.task_receiver";
    public static final String STOP_INTENT = "com.jxtii.wildebeest.stop_receiver";
    public static final String TASK_SERVICE = "com.jxtii.wildebeest.service.TaskService";

    /**
     * 判断GPS是否打开
     *
     * @param c
     * @return
     */
    public static boolean isOpenGPS(Context c) {
        boolean flag = false;
        LocationManager locationManager = (LocationManager) c
                .getSystemService(Context.LOCATION_SERVICE);
        if (locationManager
                .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            flag = true;
        }
        return flag;
    }

    /**
     * 判断服务是否在运行
     *
     * @param mContext
     * @param className
     * @return
     */
    public static boolean isServiceRunning(Context mContext, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(100);
        if (!(serviceList.size() > 0)) {
            return false;
        }
        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    /**
     * 从float转化为str
     * @param fl
     * @param su 保留小数点后精度位数，小于1时取整数
     * @return
     */
    public static String floatToStr(float fl, int su) {
        String str = su < 1 ? "0" : ".";
        for (int i = 0; i < su; i++) {
            str += "0";
        }
        DecimalFormat decimalFormat = new DecimalFormat(str);
        String p = decimalFormat.format(fl);
        return p;
    }
}
