package com.npauuul.cashemergency;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CONTACT_PICKER_REQUEST = 200;

    private TextView tvEmergencyContact;
    private Switch swEmergencyToggle;
    private Button btnSelectContact;

    private SharedPreferences sharedPreferences;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEmergencyContact = findViewById(R.id.tv_emergency_contact);
        swEmergencyToggle = findViewById(R.id.sw_emergency_toggle);
        btnSelectContact = findViewById(R.id.btn_select_contact);

        sharedPreferences = getSharedPreferences("EmergencyAppPrefs", MODE_PRIVATE);

        // Verificar y solicitar permisos
        checkAndRequestPermissions();

        // Cargar contacto guardado
        loadSavedContact();

        // Configurar listeners
        btnSelectContact.setOnClickListener(v -> openContactPicker());
        swEmergencyToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startEmergencyService();
            } else {
                stopEmergencyService();
            }
        });
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void loadSavedContact() {
        String contactName = sharedPreferences.getString("emergency_contact_name", null);
        String contactNumber = sharedPreferences.getString("emergency_contact_number", null);

        if (contactName != null && contactNumber != null) {
            tvEmergencyContact.setText(String.format("%s (%s)", contactName, contactNumber));
        } else {
            tvEmergencyContact.setText("No contact selected");
        }
    }

    private void openContactPicker() {
        Intent intent = new Intent(this, ContactPickerActivity.class);
        startActivityForResult(intent, CONTACT_PICKER_REQUEST);
    }

    private void startEmergencyService() {
        String contactNumber = sharedPreferences.getString("emergency_contact_number", null);
        if (contactNumber == null || contactNumber.isEmpty()) {
            Toast.makeText(this, "Please select an emergency contact first", Toast.LENGTH_SHORT).show();
            swEmergencyToggle.setChecked(false);
            return;
        }

        Intent serviceIntent = new Intent(this, EmergencyService.class);
        serviceIntent.putExtra("emergency_number", contactNumber);
        ContextCompat.startForegroundService(this, serviceIntent);
        isServiceRunning = true;
    }

    private void stopEmergencyService() {
        Intent serviceIntent = new Intent(this, EmergencyService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACT_PICKER_REQUEST && resultCode == RESULT_OK) {
            String contactName = data.getStringExtra("contact_name");
            String contactNumber = data.getStringExtra("contact_number");

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("emergency_contact_name", contactName);
            editor.putString("emergency_contact_number", contactNumber);
            editor.apply();

            tvEmergencyContact.setText(String.format("%s (%s)", contactName, contactNumber));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                Toast.makeText(this, "Some permissions were denied. The app may not work properly.", Toast.LENGTH_LONG).show();
                // Opcional: abrir configuración para que el usuario otorgue los permisos manualmente
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceRunning) {
            stopEmergencyService();
        }
    }
}