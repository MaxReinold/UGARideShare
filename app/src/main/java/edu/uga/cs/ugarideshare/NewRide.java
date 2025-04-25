package edu.uga.cs.ugarideshare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.text.format.DateFormat;
import android.widget.LinearLayout;
import android.util.Log;
import android.widget.DatePicker;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

public class NewRide extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_ride);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // --- New code for ride creation UI ---
        Switch rideTypeSwitch = findViewById(R.id.rideTypeSwitch);
        TextView byLabel = findViewById(R.id.byLabel);
        EditText byEmail = findViewById(R.id.byEmail);
        EditText timeInput = findViewById(R.id.timeInput);
        EditText dateInput = findViewById(R.id.dateInput);
        EditText addressFrom = findViewById(R.id.addressFrom);
        EditText addressTo = findViewById(R.id.addressTo);
        Button submitRideBtn = findViewById(R.id.submitRideBtn);

        TextView requestLabel = findViewById(R.id.requestLabel);
        TextView offerLabel = findViewById(R.id.offerLabel);
        LinearLayout toggleRow = findViewById(R.id.toggleRow);

        // Get current user from FirebaseAuth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        final Calendar calendar = Calendar.getInstance();
        String userUid;
        String userEmail;
        if (firebaseUser != null) {
            userUid = firebaseUser.getUid();
            userEmail = firebaseUser.getEmail();
        } else {
            userEmail = "";
            userUid = "";
        }
        byEmail.setText(userEmail);

        // Helper to update bolding and label
        Runnable updateToggleUI = () -> {
            if (rideTypeSwitch.isChecked()) {
                // Offer selected
                offerLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                requestLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
                byLabel.setText("Offered by");
            } else {
                // Request selected
                requestLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                offerLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
                byLabel.setText("Requested by");
            }
        };

        // Set up toggle logic
        rideTypeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateToggleUI.run());

        // Make the whole row clickable to toggle
        toggleRow.setOnClickListener(v -> {
            rideTypeSwitch.setChecked(!rideTypeSwitch.isChecked());
            // updateToggleUI will be called by the switch listener
        });

        // Set initial state
        updateToggleUI.run();

        // Disable submit button by default
        submitRideBtn.setEnabled(false);

        // Helper to check if all fields are filled and date/time is valid
        Runnable checkFieldsAndDate = () -> {
            String addrFrom = addressFrom.getText().toString().trim();
            String addrTo = addressTo.getText().toString().trim();
            String dateStr = dateInput.getText().toString().trim();
            String timeStr = timeInput.getText().toString().trim();

            boolean allFilled = !addrFrom.isEmpty() && !addrTo.isEmpty() && !dateStr.isEmpty() && !timeStr.isEmpty();

            // Check if date/time is in the future
            boolean dateValid = false;
            if (allFilled) {
                Date now = new Date();
                Date rideDate = calendar.getTime();
                dateValid = rideDate.after(now);
            }

            submitRideBtn.setEnabled(allFilled && dateValid);
        };

        // Add text watchers to all relevant fields
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkFieldsAndDate.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        addressFrom.addTextChangedListener(watcher);
        addressTo.addTextChangedListener(watcher);
        dateInput.addTextChangedListener(watcher);
        timeInput.addTextChangedListener(watcher);

        // Date picker dialog
        dateInput.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, y, m, d) -> {
                    calendar.set(Calendar.YEAR, y);
                    calendar.set(Calendar.MONTH, m);
                    calendar.set(Calendar.DAY_OF_MONTH, d);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    dateInput.setText(sdf.format(calendar.getTime()));

                    // After picking date, check if date+time is in the future
                    Date now = new Date();
                    Date rideDate = calendar.getTime();
                    // Only check if time is also set
                    if (!timeInput.getText().toString().trim().isEmpty() && !rideDate.after(now)) {
                        Toast.makeText(this, "Please enter a valid future date and time.", Toast.LENGTH_SHORT).show();
                    }
                    checkFieldsAndDate.run();
                }, year, month, day);
            datePickerDialog.show();
        });

        // Time picker dialog (update calendar object)
        timeInput.setOnClickListener(v -> {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute1);
                    calendar.set(Calendar.SECOND, 0);
                    String timeStr = String.format("%02d:%02d", hourOfDay, minute1);
                    timeInput.setText(timeStr);

                    // After picking time, check if date+time is in the future
                    Date now = new Date();
                    Date rideDate = calendar.getTime();
                    // Only check if date is also set
                    if (!dateInput.getText().toString().trim().isEmpty() && !rideDate.after(now)) {
                        Toast.makeText(this, "Please enter a valid future date and time.", Toast.LENGTH_SHORT).show();
                    }
                    checkFieldsAndDate.run();
                }, hour, minute, DateFormat.is24HourFormat(this));
            timePickerDialog.show();
        });

        // Submit button logic
        submitRideBtn.setOnClickListener(v -> {
            // Only create the Ride object here
            String addrFrom = addressFrom.getText().toString().trim();
            String addrTo = addressTo.getText().toString().trim();

            // Compose Date object from calendar (date+time)
            Date rideDate = calendar.getTime();

            User userDriver = null;
            User userRider = null;
            if (rideTypeSwitch.isChecked()) {
                userDriver = new User(userUid, userEmail);
            } else {
                userRider = new User(userUid, userEmail);
            }

            Ride ride = new Ride(rideDate, addrTo, addrFrom, userDriver, userRider);

            // Print to Logcat
            Log.d("NewRide", "Ride object: " + ride.toString());

            // Add ride to Firebase Database
            DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("rides");
            String rideId = ridesRef.push().getKey();
            if (rideId != null) {
                ridesRef.child(rideId).setValue(ride);
            }

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        return true;
    }
}