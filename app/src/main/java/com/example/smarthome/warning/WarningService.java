package com.example.smarthome.warning;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.smarthome.R;

public class WarningService extends Service implements MediaPlayer.OnCompletionListener {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MediaPlayer mediaPlayer;
    private String fname = "warning.mp3";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
//        int resID = getResources().getIdentifier(fname, "raw", getPackageName());
        mediaPlayer = MediaPlayer.create(this, R.raw.warning);
        mediaPlayer.setOnCompletionListener(this);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Log.d("mediaPlayer", "started");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            Log.d("mediaPlayer", "stopped");
        }
        mediaPlayer.release();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopSelf();
    }
}
