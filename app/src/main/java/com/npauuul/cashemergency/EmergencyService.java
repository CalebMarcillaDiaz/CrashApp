package com.npauuul.cashemergency;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class EmergencyService extends Service {
    private static final String CHANNEL_ID = "EmergencyServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private SensorService sensorService;
    private LocationTracker locationTracker;
    private String emergencyNumber;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(),
                    FOREGROUND_SERVICE_TYPE_LOCATION | FOREGROUND_SERVICE_TYPE_PHONE_CALL);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        locationTracker = new LocationTracker(this);
        sensorService = new SensorService(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            emergencyNumber = intent.getStringExtra("emergency_number");
            if (emergencyNumber != null) {
                sensorService.startMonitoring(emergencyNumber, locationTracker);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorService != null) {
            sensorService.stopMonitoring();
        }
        if (locationTracker != null) {
            locationTracker.stopLocationUpdates();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Emergency Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        // Para Android Q (10) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Emergency Service")
                    .setContentText("Monitoring for potential accidents")
                    .setSmallIcon(R.drawable.baseline_announcement_24)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .build();
        } else {
            // Para versiones anteriores
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Emergency Service")
                    .setContentText("Monitoring for potential accidents")
                    .setSmallIcon(R.drawable.baseline_announcement_24)
                    .build();
        }
    }
}