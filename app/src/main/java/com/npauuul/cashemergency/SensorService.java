package com.npauuul.cashemergency;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class SensorService implements SensorEventListener {
    private static final float ACCELERATION_THRESHOLD = 15.0f; // m/s²
    private static final long COOLDOWN_PERIOD = 5000; // 5 segundos

    private final Context context;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastDetectionTime = 0;
    private String emergencyNumber;
    private LocationTracker locationTracker;

    public SensorService(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    public void startMonitoring(String emergencyNumber, LocationTracker locationTracker) {
        this.emergencyNumber = emergencyNumber;
        this.locationTracker = locationTracker;

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(context, "Accelerometer not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopMonitoring() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z);

            long currentTime = System.currentTimeMillis();
            if (acceleration > ACCELERATION_THRESHOLD &&
                    (currentTime - lastDetectionTime) > COOLDOWN_PERIOD) {
                lastDetectionTime = currentTime;
                handleEmergency();
            }
        }
    }

    private void handleEmergency() {
        if (emergencyNumber == null || emergencyNumber.isEmpty()) return;

        // Obtener ubicación actual
        String location = locationTracker.getLastKnownLocation();

        // Hacer llamada
        PhoneCallHelper.makeCall(context, emergencyNumber);

        // Enviar SMS con ubicación
        String message = "Emergencia aquí. " + (location != null ? "Ubicación: " + location : "No se pudo obtener ubicación");
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(emergencyNumber, null, message, null, null);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se necesita implementar
    }
}