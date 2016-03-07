package com.yuzhi.fine.fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jxtii.wildebeest.model.PositionRecord;
import com.jxtii.wildebeest.service.TaskService;
import com.jxtii.wildebeest.util.CommUtil;
import com.yuzhi.fine.R;

import org.litepal.crud.DataSupport;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;

public class WildebeestFragment extends Fragment implements View.OnClickListener {

    Context context;
    static final int GPS_OPEN_STATUS = 1;
    Timer timer;
    TimerTask timerTask;
    Handler handler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    tv5.setText("急加速：" + (new Random().nextInt(100) % (100 - 10 + 1) + 10));
                    tv6.setText("急减速：" + (new Random().nextInt(100) % (100 - 10 + 1) + 10));
                    tv7.setText("急转弯：" + (new Random().nextInt(100) % (100 - 10 + 1) + 10));
                    tv8.setText("疲劳：" + (new Random().nextInt(100) % (100 - 10 + 1) + 10));
                    tv9.setText("超速：" + (new Random().nextInt(100) % (100 - 10 + 1) + 10));
                    tv10.setText("路况：" + (new Random().nextInt(100) % (100 - 10 + 1) + 10));
                    break;
            }
        }
    };

    @Bind(R.id.aaa)
    TextView aaa;
    @Bind(R.id.vw_title)
    RelativeLayout vwTitle;
    @Bind(R.id.tv_1)
    TextView tv1;
    @Bind(R.id.tv_2)
    TextView tv2;
    @Bind(R.id.tv_3)
    TextView tv3;
    @Bind(R.id.tv_4)
    TextView tv4;
    @Bind(R.id.tv_5)
    TextView tv5;
    @Bind(R.id.tv_6)
    TextView tv6;
    @Bind(R.id.tv_7)
    TextView tv7;
    @Bind(R.id.tv_8)
    TextView tv8;
    @Bind(R.id.tv_9)
    TextView tv9;
    @Bind(R.id.tv_10)
    TextView tv10;
    @Bind(R.id.vw_scroll)
    ScrollView vwScroll;
    @Bind(R.id.btn_finish)
    Button btnFinish;
    @Bind(R.id.btn_start)
    Button btnStart;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wildebeest, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getActivity();
        initData();
        initView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    void initData() {

    }

    void initView() {
        btnStart.setOnClickListener(this);
        btnFinish.setOnClickListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                showToast("start");
                initLocService();
                break;
            case R.id.btn_finish:
                showToast("end");
                stopLocService();
                break;
        }
    }

    void initLocService() {
        if (CommUtil.isOpenGPS(context)) {
            showToast("已开启GPS！");
            startLocService();
        } else {
            showToast("请开启GPS！");
            Intent intent = new Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, GPS_OPEN_STATUS);
        }

    }

    void stopLocService() {
        stopReflash();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent();
        intent.setAction(CommUtil.START_INTENT);
        intent.setPackage("com.yuzhi.fine");
        intent.putExtra("interval", 2000);
        //Implicit intents with startService are not safe
//        Intent intent = new Intent(CommUtil.START_INTENT);
        PendingIntent pt = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.cancel(pt);

        intent = new Intent();
        intent.setAction(CommUtil.STOP_INTENT);
        intent.setPackage("com.yuzhi.fine");
        intent.putExtra("interval", 2000);
        //Implicit intents with startService are not safe
//        intent = new Intent(CommUtil.STOP_INTENT);
        pt = PendingIntent.getBroadcast(context, 0, intent, 0);
        long triggerAtTime = SystemClock.elapsedRealtime() + 5 * 1000;
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pt);
    }

    void startLocService() {
        DataSupport.deleteAll(PositionRecord.class);
        startReflash();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAtTime = System.currentTimeMillis() + 5 * 1000;
        long interval = 15 * 60 * 1000;
        Intent intent = new Intent();
        intent.setAction(CommUtil.START_INTENT);
        intent.setPackage("com.yuzhi.fine");
        intent.putExtra("interval", 2000);
        //Implicit intents with startService are not safe
//        Intent intent = new Intent(CommUtil.START_INTENT);
        PendingIntent pt = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTime, interval, pt);

        Intent ii = new Intent();
        ii.setAction("com.jxtii.wildebeest.task_service");
        ii.setPackage("com.yuzhi.fine");
        //Implicit intents with startService are not safe
//        ii.setClass(getActivity(), TaskService.class);
        ii.putExtra("interval", 2000);
        getActivity().startService(ii);
    }

    void startReflash(){
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                handler.sendEmptyMessage(0);
            }
        };
        timer.scheduleAtFixedRate(timerTask, 10*1000, 5*1000);
    }

    void stopReflash() {
        if (timer != null)
            timer.cancel();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GPS_OPEN_STATUS:
                Boolean isOpen = CommUtil.isOpenGPS(context);
                if (isOpen) {
                    showToast("已开启GPS！");
                    startLocService();
                } else {
                    showToast("请开启GPS！");
                    Intent intent = new Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, GPS_OPEN_STATUS);
                }
                break;
        }
    }

    void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
