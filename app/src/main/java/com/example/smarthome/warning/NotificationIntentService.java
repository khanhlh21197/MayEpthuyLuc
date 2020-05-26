package com.example.smarthome.warning;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.smarthome.ui.main.TempMonitoringService;

import java.util.Objects;

public class NotificationIntentService extends IntentService {
    public NotificationIntentService() {
        super("name");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case "stopWarning":
                    Handler stopWarningHandler = new Handler(Looper.getMainLooper());
                    stopWarningHandler.post(TempMonitoringService::stopMedia);
                    break;
            }
        }
    }
}
