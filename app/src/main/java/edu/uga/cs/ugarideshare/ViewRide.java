package edu.uga.cs.ugarideshare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
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

public class ViewRide extends AppCompatActivity {

    private String rideId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        setContentView(layout);

        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, insets) -> {
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
        String userUid = firebaseUser.getUid();

        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("rides").child(rideId);
        rideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot rideSnap) {
                if (!rideSnap.exists()) {
                    Toast.makeText(ViewRide.this, "Ride not found.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String addressFrom = rideSnap.child("addressFrom").getValue(String.class);
                String addressTo = rideSnap.child("addressTo").getValue(String.class);
                String dateStr = String.valueOf(rideSnap.child("date").getValue());
                String driverEmail = rideSnap.child("userDriver/email").getValue(String.class);
                String riderEmail = rideSnap.child("userRider/email").getValue(String.class);
                String driverUid = rideSnap.child("userDriver/uid").getValue(String.class);
                String riderUid = rideSnap.child("userRider/uid").getValue(String.class);

                // Display ride info
                TextView title = new TextView(ViewRide.this);
                title.setText("Ride Details");
                title.setTextSize(22);
                title.setTypeface(null, Typeface.BOLD);
                title.setPadding(0, 0, 0, 24);

                TextView info = new TextView(ViewRide.this);
                info.setText(
                        "From: " + (addressFrom != null ? addressFrom : "") + "\n" +
                        "To: " + (addressTo != null ? addressTo : "") + "\n" +
                        "Date: " + dateStr + "\n" +
                        "Driver: " + (driverEmail != null ? driverEmail : "None") + "\n" +
                        "Rider: " + (riderEmail != null ? riderEmail : "None")
                );
                info.setTextSize(18);
                info.setPadding(0, 0, 0, 32);

                Button cancelBtn = new Button(ViewRide.this);
                cancelBtn.setText("Cancel");
                cancelBtn.setAllCaps(false);

                // Determine if user is only one in ride
                boolean isOnlyUser;
                boolean isDriver, isRider;
                if (driverUid != null && driverUid.equals(userUid) && (riderUid == null || riderUid.isEmpty())) {
                    isRider = false;
                    isOnlyUser = true;
                    isDriver = true;
                } else if (riderUid != null && riderUid.equals(userUid) && (driverUid == null || driverUid.isEmpty())) {
                    isDriver = false;
                    isOnlyUser = true;
                    isRider = true;
                } else {
                    isOnlyUser = false;
                    if (driverUid != null && driverUid.equals(userUid)) isDriver = true;
                    else {
                        isDriver = false;
                    }
                    if (riderUid != null && riderUid.equals(userUid)) isRider = true;
                    else {
                        isRider = false;
                    }
                }

                cancelBtn.setOnClickListener(v -> {
                    if (isOnlyUser) {
                        rideRef.removeValue();
                        Toast.makeText(ViewRide.this, "Ride canceled.", Toast.LENGTH_SHORT).show();
                    } else {
                        if (isDriver) {
                            rideRef.child("userDriver").removeValue();
                        }
                        if (isRider) {
                            rideRef.child("userRider").removeValue();
                        }
                        Toast.makeText(ViewRide.this, "You have been removed from this ride.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                });

                layout.removeAllViews();
                layout.addView(title);
                layout.addView(info);
                layout.addView(cancelBtn);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ViewRide.this, "Failed to load ride.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("rideId", rideId);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(this, ViewMyRides.class);
        startActivity(intent);
        finish();
        return true;
    }
}