package com.example.smarthome.ui.main;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.IdRes;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.example.smarthome.MainActivity;
import com.example.smarthome.R;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.ui.device.model.Device;
import com.example.smarthome.utils.FireBaseCallBack;
import com.example.smarthome.warning.NotificationIntentService;
import com.example.smarthome.warning.WarningService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.GenericTypeIndicator;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class TempMonitoringService extends LifecycleService implements Serializable {
    public static final String EXTRA_BUTTON_CLICKED = "EXTRA_BUTTON_CLICKED";
    public static boolean isRunning = false;
    private Timer timer;
    public int counter = 0;
    public static Vibrator v;
    public static MediaPlayer mediaPlayer;
    public static Intent warningService;

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

        isRunning = true;

        initMedia();

        if (!CommonActivity.isNullOrEmpty(intent)
                && !CommonActivity.isNullOrEmpty(intent.getExtras())
                && !CommonActivity.isNullOrEmpty(intent.getExtras().getString("idDevice"))) {
            String idDevice = Objects.requireNonNull(intent.getExtras()).getString("idDevice");
            Log.v("TempMonitoringService", "onStartCommand");
            startTimer();
            MainViewModel viewModel = new MainViewModel();
            if (!CommonActivity.isNullOrEmpty(idDevice)) {
                viewModel.getAllDevices().observe(Objects.requireNonNull(this), dataSnapshot -> {
                    getData(dataSnapshot, devices -> {
                        if (devices != null) {
                            for (int i = 0; i < devices.size(); i++) {
                                Device device = devices.get(i);
                                if (idDevice.contains(device.getId())) {
                                    if (Double.parseDouble(device.getNO()) > Double.parseDouble(device.getNG())) {
                                        createNotification(device, i);
                                    }
                                }
                            }
                        }
                    });
                });
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (v != null && v.hasVibrator()) {
            v.cancel();
        }

        isRunning = false;

        stopService(warningService);

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotification(Device device, int idNoti) {
        RemoteViews notificationLayout =
                new RemoteViews(getPackageName(), R.layout.notification_monitoring);
        notificationLayout.setTextViewText(R.id.message, device.getNO()
                + " độ trên thiết bị "
                + device.getName());

        Intent stopWarning = new Intent(this, NotificationIntentService.class);
        stopWarning.setAction("stopWarning");

        notificationLayout.setOnClickPendingIntent(R.id.removeWarning,
                PendingIntent.getService(this, 0, stopWarning, PendingIntent.FLAG_UPDATE_CURRENT));
//        notificationLayout.setString(R.id.message, null, "Nhiệt độ đo được: " + ng + " trên thiết bị: " + idDevice);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), "notify_001");
        Intent ii = new Intent(getApplicationContext(), MainActivity.class);
        ii.putExtra("menuFragment", "DetailDeviceFragment");
        ii.putExtra("idDevice", device.getId());

        mBuilder.setSmallIcon(R.drawable.ic_warning_red);
        mBuilder.setCustomContentView(notificationLayout);
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

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
        startMedia();
        vibrate();
    }

    private void initMedia() {
        mediaPlayer = MediaPlayer.create(this, R.raw.warning);
        mediaPlayer.setOnCompletionListener(mp -> stopSelf());

        warningService = new Intent(this, WarningService.class);
    }

    private void startMedia() {
        if (!WarningService.isRunning) {
            startService(warningService);
        }
//        try {
//            if (mediaPlayer == null) {
//                initMedia();
//                if (!mediaPlayer.isPlaying()) {
//                    mediaPlayer.start();
//                    Log.d("mediaPlayer", "started");
//                }
//            } else {
//                if (!mediaPlayer.isPlaying()) {
//                    mediaPlayer.start();
//                    Log.d("mediaPlayer", "started");
//                }
//            }
//        } catch (IllegalStateException e) {
//            e.printStackTrace();
//        }
    }

    public static void stopMedia() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.prepare();
                    Log.d("mediaPlayer", "stopped");
                }
                mediaPlayer.release();
            }
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    private void vibrate() {
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (!CommonActivity.isNullOrEmpty(v)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(3000);
            }
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

    private PendingIntent onButtonNotificationClick(@IdRes int id) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BUTTON_CLICKED, id);
        return PendingIntent.getBroadcast(this, id, intent, 0);
    }
}
