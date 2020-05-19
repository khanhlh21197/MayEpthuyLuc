package com.example.smarthome.ui.main;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.example.smarthome.MainActivity;
import com.example.smarthome.R;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.ui.device.model.Device;
import com.example.smarthome.utils.FireBaseCallBack;
import com.example.smarthome.utils.StringUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class TempMonitoringService extends LifecycleService {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference deviceRef = database.getReference("devices");
    private Timer timer;
    public int counter = 0;

    public TempMonitoringService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    public void startTimer() {
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            public void run() {
                Log.i("Count", "=========  " + (counter++));
            }
        };
        timer.schedule(timerTask, 1000, 1000); //
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (!CommonActivity.isNullOrEmpty(intent)
                && !CommonActivity.isNullOrEmpty(intent.getExtras())
                && !CommonActivity.isNullOrEmpty(intent.getExtras().getString("idDevice"))) {
            String idDevice = Objects.requireNonNull(intent.getExtras()).getString("idDevice");
            Log.v("TempMonitoringService", "onStartCommand");
            startTimer();
            MainViewModel viewModel = new MainViewModel();
            if (!CommonActivity.isNullOrEmpty(idDevice)){
                viewModel.getAllDevices().observe(Objects.requireNonNull(this), dataSnapshot -> {
                    getData(dataSnapshot, devices -> {
                        if (devices != null) {
                            for (int i= 0; i< devices.size(); i++) {
                                Device device = devices.get(i);
                                if (idDevice.contains(device.getId())) {
                                    if (Double.parseDouble(device.getNO()) > Double.parseDouble(device.getNG())) {
                                        createNotification(device.getNO(), device.getId(), i);
                                    }
                                }
                            }
                        }
                    });
                });
            }
//            viewModel.getDevicesOfUser(idDevice).observe(this, dataSnapshot -> {
//                GenericTypeIndicator<ArrayList<Device>> t = new GenericTypeIndicator<ArrayList<Device>>() {
//                };
//                ArrayList<Device> devices = dataSnapshot.getValue(t);
//                if (devices != null) {
//                    for (int i = 0; i < devices.size(); i++) {
//                        try {
//                            Device device = devices.get(i);
//                            if (Double.parseDouble(device.getNO()) > Double.parseDouble(device.getNG())) {
//                                createNotification(device.getNO(), device.getId(), i);
//                            }
//                        } catch (NumberFormatException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            });
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v("TempMonitoringService", "onDestroy");
        super.onDestroy();

        stoptimertask();
//auto restart service
//        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction("restartservice");
//        broadcastIntent.setClass(this, Restarter.class);
//        this.sendBroadcast(broadcastIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getData(DataSnapshot dataSnapshot, FireBaseCallBack<ArrayList<Device>> callBack) {
        GenericTypeIndicator<ArrayList<Device>> t = new GenericTypeIndicator<ArrayList<Device>>() {
        };
        ArrayList<Device> devices = dataSnapshot.getValue(t);
        callBack.afterDataChanged(devices);
    }

    private void createNotification(String ng, String idDevice, int idNoti) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), "notify_001");
        Intent ii = new Intent(getApplicationContext(), MainActivity.class);
        ii.putExtra("menuFragment", "DetailDeviceFragment");
        ii.putExtra("idDevice", idDevice);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, ii, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText("Nhiệt độ đo được: " + ng + " trên thiết bị: " + idDevice);
        bigText.setBigContentTitle("Nhiệt độ vượt ngưỡng !");
        bigText.setSummaryText("Cảnh báo");

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.drawable.ic_warning_red);
        mBuilder.setContentTitle(getString(R.string.app_name));
        mBuilder.setContentText("Nhiệt độ vượt ngưỡng !");
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setStyle(bigText);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "Your_channel_id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            mBuilder.setChannelId(channelId);
        }

        if (mNotificationManager != null) {
            mNotificationManager.notify(idNoti, mBuilder.build());
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "example.smarthome";
        String channelName = "Background TempMonitoring Service";

        //get Bundle Data

        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

//        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
//        bigText.bigText("Đang theo dõi nhiệt độ");

        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_warning_red)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Đang theo dõi nhiệt độ")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
//                .setStyle(bigText)
                .build();
        startForeground(9, notification);
    }
}
