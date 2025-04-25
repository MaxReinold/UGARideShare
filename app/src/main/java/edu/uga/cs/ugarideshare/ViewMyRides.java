package edu.uga.cs.ugarideshare;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.view.ViewGroup;
import android.view.View;
import android.graphics.Typeface;
import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.List;

public class ViewMyRides extends AppCompatActivity {

    private List<String> rideKeys = new ArrayList<>();
    private List<String> rideSummaries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_my_rides);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            finish();
            return;
        }
        String userUid = firebaseUser.getUid();

        LinearLayout rideListLayout = new LinearLayout(this);
        rideListLayout.setOrientation(LinearLayout.VERTICAL);
        rideListLayout.setPadding(24, 24, 24, 24);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(rideListLayout);

        ViewGroup root = findViewById(R.id.main);
        root.addView(scrollView);

        DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("rides");
        ridesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                rideListLayout.removeAllViews();
                rideKeys.clear();
                rideSummaries.clear();
                List<DataSnapshot> userRides = new ArrayList<>();
                for (DataSnapshot rideSnap : snapshot.getChildren()) {
                    Object userDriverObj = rideSnap.child("userDriver").getValue();
                    Object userRiderObj = rideSnap.child("userRider").getValue();
                    boolean isDriver = false, isRider = false;
                    if (userDriverObj != null && rideSnap.child("userDriver/uid").getValue() != null) {
                        String driverUid = String.valueOf(rideSnap.child("userDriver/uid").getValue());
                        isDriver = userUid.equals(driverUid);
                    }
                    if (userRiderObj != null && rideSnap.child("userRider/uid").getValue() != null) {
                        String riderUid = String.valueOf(rideSnap.child("userRider/uid").getValue());
                        isRider = userUid.equals(riderUid);
                    }
                    if (isDriver || isRider) {
                        userRides.add(rideSnap);
                    }
                }

                if (userRides.isEmpty()) {
                    TextView emptyView = new TextView(ViewMyRides.this);
                    emptyView.setText("No rides found.");
                    emptyView.setTypeface(null, Typeface.ITALIC);
                    rideListLayout.addView(emptyView);
                } else {
                    for (DataSnapshot rideSnap : userRides) {
                        String rideId = rideSnap.getKey();
                        String addressFrom = rideSnap.child("addressFrom").getValue(String.class);
                        String addressTo = rideSnap.child("addressTo").getValue(String.class);
                        String dateStr = String.valueOf(rideSnap.child("date").getValue());
                        String driverEmail = rideSnap.child("userDriver/email").getValue(String.class);
                        String riderEmail = rideSnap.child("userRider/email").getValue(String.class);

                        // Compose a summary for the button
                        String summary = "From: " + (addressFrom != null ? addressFrom : "") +
                                " | To: " + (addressTo != null ? addressTo : "") +
                                " | Date: " + dateStr;

                        Button rideBtn = new Button(ViewMyRides.this);
                        rideBtn.setText(summary);
                        rideBtn.setAllCaps(false);

                        // Save for state restoration
                        rideKeys.add(rideId);
                        rideSummaries.add(summary);

                        rideBtn.setOnClickListener(v -> {
                            Intent intent = new Intent(ViewMyRides.this, ViewRide.class);
                            intent.putExtra("rideId", rideId);
                            startActivity(intent);
                        });

                        rideListLayout.addView(rideBtn);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ViewMyRides.this, "Failed to load rides.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("rideKeys", new ArrayList<>(rideKeys));
        outState.putStringArrayList("rideSummaries", new ArrayList<>(rideSummaries));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        rideKeys = savedInstanceState.getStringArrayList("rideKeys");
        rideSummaries = savedInstanceState.getStringArrayList("rideSummaries");
        // UI will be rebuilt by Firebase listener, so nothing else needed here
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        return true;
    }
}