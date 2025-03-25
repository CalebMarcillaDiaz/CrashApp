package com.npauuul.cashemergency;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class LocationTracker {
    private static final long MIN_TIME_BETWEEN_UPDATES = 10000; // 10 segundos
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 metros

    private final Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location lastKnownLocation;

    public LocationTracker(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        initLocationListener();
        startLocationUpdates();
    }

    private void initLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastKnownLocation = location;
                Log.d("LocationTracker", "Nueva ubicación: " + location.getLatitude() + ", " + location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    public void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener,
                        Looper.getMainLooper());

                // Obtener última ubicación conocida
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (Exception e) {
                Log.e("LocationTracker", "Error al iniciar actualizaciones de ubicación", e);
            }
        }
    }

    public void stopLocationUpdates() {
        locationManager.removeUpdates(locationListener);
    }

    public String getLastKnownLocation() {
        if (lastKnownLocation != null) {
            return "https://maps.google.com/?q=" + lastKnownLocation.getLatitude() +
                    "," + lastKnownLocation.getLongitude();
        }
        return null;
    }
}