package com.jxtii.wildebeest.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jxtii.wildebeest.util.CommUtil;
import com.jxtii.wildebeest.util.DateStr;

/**
 * Created by huangyc on 2016/3/3.
 */
public class TaskReceiver extends BroadcastReceiver {

    String TAG = TaskReceiver.class.getSimpleName();
    Context ctx = null;

    public void onReceive(Context context, Intent intent) {

        ctx = context;

        if (CommUtil.START_INTENT.equals(intent.getAction())) {
//            Log.i(TAG, DateStr.HHmmssStr()+" _ receive START_INTENT");
            Boolean flag = CommUtil.isServiceRunning(context, CommUtil.TASK_SERVICE);
            if (flag) {
                Log.i(TAG, DateStr.HHmmssStr() + " _ TASK_SERVICE is alive");
            } else {
                Log.w(TAG, DateStr.HHmmssStr() + " _ TASK_SERVICE is dead");
                startTaskService();
            }
        } else if (CommUtil.STOP_INTENT.equals(intent.getAction())) {
            Log.i(TAG, DateStr.HHmmssStr()+" _ receive STOP_INTENT");
            stopTaskService();
        }
    }

    void startTaskService() {
        Intent intent = new Intent();
        intent.setClass(ctx, TaskService.class);
        intent.putExtra("interval", 2000);
        ctx.startService(intent);
    }

    void stopTaskService() {
        Intent intent = new Intent();
        intent.setClass(ctx, TaskService.class);
        ctx.stopService(intent);
    }
}
