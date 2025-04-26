package edu.uga.cs.ugarideshare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Switch;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.text.format.DateFormat;
import android.text.TextWatcher;
import android.text.Editable;
import android.graphics.Typeface;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

public class ViewRide extends AppCompatActivity {

    private String rideId;
    private boolean isOnlyUser = false;
    private boolean isDriver = false;
    private boolean isRider = false;
    private boolean isUnrelated = false;
    private String userUid;
    private String userEmail;
    private Calendar calendar = Calendar.getInstance();

    /**
     * Called when the activity is starting.
     * @param savedInstanceState The previously saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_ride);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            rideId = savedInstanceState.getString("rideId");
        } else {
            rideId = getIntent().getStringExtra("rideId");
        }

        if (rideId == null) {
            Toast.makeText(this, "No ride selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            finish();
            return;
        }
        userUid = firebaseUser.getUid();
        userEmail = firebaseUser.getEmail();

        Switch rideTypeSwitch = findViewById(R.id.rideTypeSwitch);
        TextView byLabel = findViewById(R.id.byLabel);
        EditText byEmail = findViewById(R.id.byEmail);
        EditText timeInput = findViewById(R.id.timeInput);
        EditText dateInput = findViewById(R.id.dateInput);
        EditText addressFrom = findViewById(R.id.addressFrom);
        EditText addressTo = findViewById(R.id.addressTo);
        Button actionRideBtn = findViewById(R.id.actionRideBtn);
        TextView requestLabel = findViewById(R.id.requestLabel);
        TextView offerLabel = findViewById(R.id.offerLabel);
        LinearLayout toggleRow = findViewById(R.id.toggleRow);

        rideTypeSwitch.setEnabled(false);
        toggleRow.setEnabled(false);

        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("rides").child(rideId);
        rideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            /**
             * Called when ride data is loaded.
             */
            @Override
            public void onDataChange(DataSnapshot rideSnap) {
                if (!rideSnap.exists()) {
                    Toast.makeText(ViewRide.this, "Ride not found.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String addressFromVal = rideSnap.child("addressFrom").getValue(String.class);
                String addressToVal = rideSnap.child("addressTo").getValue(String.class);
                Object dateObjRaw = rideSnap.child("date").getValue();
                String driverEmail = rideSnap.child("userDriver/email").getValue(String.class);
                String riderEmail = rideSnap.child("userRider/email").getValue(String.class);
                String driverUid = rideSnap.child("userDriver/uid").getValue(String.class);
                String riderUid = rideSnap.child("userRider/uid").getValue(String.class);

                Date rideDate = null;
                if (dateObjRaw != null) {
                    try {
                        long millis = 0;
                        if (dateObjRaw instanceof Long) {
                            millis = (Long) dateObjRaw;
                        } else if (dateObjRaw instanceof Double) {
                            millis = ((Double) dateObjRaw).longValue();
                        }
                        rideDate = new Date(millis);
                        calendar.setTime(rideDate);
                    } catch (Exception e) {
                    }
                }

                addressFrom.setText(addressFromVal != null ? addressFromVal : "");
                addressTo.setText(addressToVal != null ? addressToVal : "");
                if (rideDate != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat stf = new SimpleDateFormat("HH:mm");
                    dateInput.setText(sdf.format(rideDate));
                    timeInput.setText(stf.format(rideDate));
                } else {
                    dateInput.setText("");
                    timeInput.setText("");
                }

                boolean hasDriver = driverUid != null && !driverUid.isEmpty();
                boolean hasRider = riderUid != null && !riderUid.isEmpty();
                isDriver = hasDriver && driverUid.equals(userUid);
                isRider = hasRider && riderUid.equals(userUid);
                isOnlyUser = (isDriver && !hasRider) || (isRider && !hasDriver);
                isUnrelated = !isDriver && !isRider;

                if (hasDriver && !hasRider) {
                    rideTypeSwitch.setChecked(true);
                    offerLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                    requestLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
                    byLabel.setText("Offered by");
                    byEmail.setText(driverEmail != null ? driverEmail : "");
                } else if (!hasDriver && hasRider) {
                    rideTypeSwitch.setChecked(false);
                    requestLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                    offerLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
                    byLabel.setText("Requested by");
                    byEmail.setText(riderEmail != null ? riderEmail : "");
                } else if (hasDriver && hasRider) {
                    rideTypeSwitch.setChecked(true);
                    offerLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                    requestLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
                    byLabel.setText("Offered by");
                    byEmail.setText(driverEmail != null ? driverEmail : "");
                } else {
                    rideTypeSwitch.setChecked(false);
                    byLabel.setText("By");
                    byEmail.setText("");
                }

                boolean editable = isOnlyUser;
                addressFrom.setEnabled(editable);
                addressTo.setEnabled(editable);
                dateInput.setEnabled(editable);
                timeInput.setEnabled(editable);
                dateInput.setFocusable(editable);
                dateInput.setClickable(editable);
                timeInput.setFocusable(editable);
                timeInput.setClickable(editable);

                if (editable) {
                    dateInput.setOnClickListener(v -> {
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH);
                        int day = calendar.get(Calendar.DAY_OF_MONTH);
                        DatePickerDialog datePickerDialog = new DatePickerDialog(ViewRide.this,
                            (view, y, m, d) -> {
                                calendar.set(Calendar.YEAR, y);
                                calendar.set(Calendar.MONTH, m);
                                calendar.set(Calendar.DAY_OF_MONTH, d);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                dateInput.setText(sdf.format(calendar.getTime()));
                            }, year, month, day);
                        datePickerDialog.show();
                    });
                    timeInput.setOnClickListener(v -> {
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        int minute = calendar.get(Calendar.MINUTE);
                        TimePickerDialog timePickerDialog = new TimePickerDialog(ViewRide.this,
                            (view, hourOfDay, minute1) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute1);
                                calendar.set(Calendar.SECOND, 0);
                                String timeStr = String.format("%02d:%02d", hourOfDay, minute1);
                                timeInput.setText(timeStr);
                            }, hour, minute, DateFormat.is24HourFormat(ViewRide.this));
                        timePickerDialog.show();
                    });
                } else {
                    dateInput.setOnClickListener(null);
                    timeInput.setOnClickListener(null);
                }

                if (isOnlyUser) {
                    actionRideBtn.setText("Save Changes");
                    actionRideBtn.setEnabled(true);

                    Button cancelBtn = new Button(ViewRide.this);
                    cancelBtn.setText("Cancel Ride");
                    cancelBtn.setAllCaps(false);
                    ((LinearLayout) findViewById(R.id.main)).addView(cancelBtn);

                    actionRideBtn.setOnClickListener(v -> {
                        String addrFrom = addressFrom.getText().toString().trim();
                        String addrTo = addressTo.getText().toString().trim();
                        String dateStr = dateInput.getText().toString().trim();
                        String timeStr = timeInput.getText().toString().trim();
                        if (addrFrom.isEmpty() || addrTo.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty()) {
                            Toast.makeText(ViewRide.this, "All fields required.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                            Date newDate = sdf.parse(dateStr + " " + timeStr);
                            rideRef.child("addressFrom").setValue(addrFrom);
                            rideRef.child("addressTo").setValue(addrTo);
                            rideRef.child("date").setValue(newDate.getTime());
                            Toast.makeText(ViewRide.this, "Ride updated.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(ViewRide.this, "Invalid date/time.", Toast.LENGTH_SHORT).show();
                        }
                    });

                    cancelBtn.setOnClickListener(v -> {
                        rideRef.removeValue();
                        Toast.makeText(ViewRide.this, "Ride canceled.", Toast.LENGTH_SHORT).show();
                        finish();
                    });

                } else if (isDriver || isRider) {
                    actionRideBtn.setText("Remove Me From Ride");
                    actionRideBtn.setEnabled(true);
                    actionRideBtn.setOnClickListener(v -> {
                        if (isDriver) {
                            rideRef.child("userDriver").removeValue();
                        }
                        if (isRider) {
                            rideRef.child("userRider").removeValue();
                        }
                        Toast.makeText(ViewRide.this, "You have been removed from this ride.", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else if (isUnrelated) {
                    if (hasDriver && !hasRider) {
                        actionRideBtn.setText("Accept as Rider");
                        actionRideBtn.setEnabled(true);
                        actionRideBtn.setOnClickListener(v -> {
                            rideRef.child("userRider").child("uid").setValue(userUid);
                            rideRef.child("userRider").child("email").setValue(userEmail);
                            Toast.makeText(ViewRide.this, "You are now the rider.", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else if (!hasDriver && hasRider) {
                        actionRideBtn.setText("Accept as Driver");
                        actionRideBtn.setEnabled(true);
                        actionRideBtn.setOnClickListener(v -> {
                            rideRef.child("userDriver").child("uid").setValue(userUid);
                            rideRef.child("userDriver").child("email").setValue(userEmail);
                            Toast.makeText(ViewRide.this, "You are now the driver.", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else {
                        actionRideBtn.setText("Ride Full");
                        actionRideBtn.setEnabled(false);
                    }
                } else {
                    actionRideBtn.setText("N/A");
                    actionRideBtn.setEnabled(false);
                }
            }

            /**
             * Called if loading ride data fails.
             */
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ViewRide.this, "Failed to load ride.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Called to save the instance state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("rideId", rideId);
    }

    /**
     * Called when the user presses the Up button.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}