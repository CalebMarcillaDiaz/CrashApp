package com.npauuul.cashemergency;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.Set;

public class ContactPickerActivity extends AppCompatActivity {

    private static final int CONTACT_PERMISSION_CODE = 101;
    private ListView lvContacts;
    private EditText etSearch;
    private SimpleCursorAdapter adapter;
    private Cursor cursor;
    private Set<String> loadedContactIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_picker);

        lvContacts = findViewById(R.id.lv_contacts);
        etSearch = findViewById(R.id.et_search);

        setupSearchFunction();
        checkContactsPermission();
    }

    private void setupSearchFunction() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterContacts(String query) {
        if (cursor == null || cursor.isClosed()) return;

        Cursor filteredCursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone._ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                },
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? OR " +
                        ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (filteredCursor != null) {
            Cursor uniqueFilteredCursor = removeDuplicateContacts(filteredCursor);
            adapter.changeCursor(uniqueFilteredCursor);
        }
    }

    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                Snackbar.make(lvContacts, "Se necesita permiso para acceder a tus contactos",
                                Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", v -> ActivityCompat.requestPermissions(
                                ContactPickerActivity.this,
                                new String[]{Manifest.permission.READ_CONTACTS},
                                CONTACT_PERMISSION_CODE))
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        CONTACT_PERMISSION_CODE);
            }
        } else {
            loadContacts();
        }
    }

    private void loadContacts() {
        Uri contactsUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
        };

        String selection = ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + "=1";
        String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC";

        CursorLoader cursorLoader = new CursorLoader(
                this,
                contactsUri,
                projection,
                selection,
                null,
                sortOrder
        );

        cursor = cursorLoader.loadInBackground();
        Cursor uniqueContactsCursor = removeDuplicateContacts(cursor);

        String[] fromColumns = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        };

        int[] toViews = {R.id.tv_contact_name, R.id.tv_contact_number, R.id.iv_contact_photo};

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_contact,
                uniqueContactsCursor,
                fromColumns,
                toViews,
                0
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.iv_contact_photo) {
                    String imageUri = cursor.getString(columnIndex);
                    ImageView imageView = (ImageView) view;
                    if (imageUri != null) {
                        Glide.with(ContactPickerActivity.this)
                                .load(Uri.parse(imageUri))
                                .circleCrop()
                                .placeholder(R.drawable.ic_person)
                                .into(imageView);
                    } else {
                        imageView.setImageResource(R.drawable.ic_person);
                    }
                    return true;
                }
                return false;
            }
        });

        lvContacts.setAdapter(adapter);

        lvContacts.setOnItemClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            String contactName = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String contactNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER));

            contactNumber = contactNumber.replaceAll("[^0-9+]", "");

            Intent resultIntent = new Intent();
            resultIntent.putExtra("contact_name", contactName);
            resultIntent.putExtra("contact_number", contactNumber);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private Cursor removeDuplicateContacts(Cursor originalCursor) {
        if (originalCursor == null) return null;

        // Usamos un MatrixCursor para almacenar los resultados únicos
        android.database.MatrixCursor uniqueCursor = new android.database.MatrixCursor(
                new String[]{
                        ContactsContract.CommonDataKinds.Phone._ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                }
        );

        Set<String> uniqueContactIds = new HashSet<>();
        loadedContactIds.clear();

        originalCursor.moveToPosition(-1);
        while (originalCursor.moveToNext()) {
            String contactId = originalCursor.getString(
                    originalCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

            if (!uniqueContactIds.contains(contactId)) {
                uniqueContactIds.add(contactId);
                loadedContactIds.add(contactId);

                String id = originalCursor.getString(
                        originalCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID));
                String name = originalCursor.getString(
                        originalCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = originalCursor.getString(
                        originalCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String photoUri = originalCursor.getString(
                        originalCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));

                uniqueCursor.addRow(new Object[]{id, name, number, photoUri});
            }
        }

        return uniqueCursor;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CONTACT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(this, "Permiso denegado para leer contactos", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        if (adapter != null && adapter.getCursor() != null && !adapter.getCursor().isClosed()) {
            adapter.getCursor().close();
        }
    }
}