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
import androidx.cardview.widget.CardView;

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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        LinearLayout rideListLayout = findViewById(R.id.rideListLayout);

        DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("rides");
        ridesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Remove all views except the example card (index 0)
                while (rideListLayout.getChildCount() > 1) {
                    rideListLayout.removeViewAt(1);
                }
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
                        Object dateObjRaw = rideSnap.child("date").getValue();
                        String dateStr = "";
                        if (dateObjRaw != null) {
                            try {
                                long millis = 0;
                                if (dateObjRaw instanceof Long) {
                                    millis = (Long) dateObjRaw;
                                } else if (dateObjRaw instanceof Double) {
                                    millis = ((Double) dateObjRaw).longValue();
                                }
                                dateStr = DateFormat.getDateTimeInstance().format(new Date(millis));
                            } catch (Exception e) {
                                dateStr = String.valueOf(dateObjRaw);
                            }
                        }

                        CardView cardView = new CardView(ViewMyRides.this);
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        cardParams.setMargins(20, 20, 20, 20);
                        cardView.setLayoutParams(cardParams);
                        cardView.setRadius(16);
                        cardView.setCardElevation(8);

                        LinearLayout cardContent = new LinearLayout(ViewMyRides.this);
                        cardContent.setOrientation(LinearLayout.VERTICAL);
                        cardContent.setPadding(48, 32, 48, 32);

                        TextView fromView = new TextView(ViewMyRides.this);
                        fromView.setText("From: " + (addressFrom != null ? addressFrom : ""));
                        fromView.setTextSize(18);
                        fromView.setTypeface(null, Typeface.BOLD);

                        TextView toView = new TextView(ViewMyRides.this);
                        toView.setText("To: " + (addressTo != null ? addressTo : ""));
                        toView.setTextSize(18);

                        TextView dateView = new TextView(ViewMyRides.this);
                        dateView.setText("Date: " + dateStr);
                        dateView.setTextSize(16);
                        dateView.setTypeface(null, Typeface.ITALIC);

                        Button detailsBtn = new Button(ViewMyRides.this);
                        detailsBtn.setText("View Details");
                        detailsBtn.setAllCaps(false);

                        rideKeys.add(rideId);
                        rideSummaries.add(addressFrom + " - " + addressTo + " - " + dateStr);

                        detailsBtn.setOnClickListener(v -> {
                            Intent intent = new Intent(ViewMyRides.this, ViewRide.class);
                            intent.putExtra("rideId", rideId);
                            startActivity(intent);
                        });

                        cardContent.addView(fromView);
                        cardContent.addView(toView);
                        cardContent.addView(dateView);
                        cardContent.addView(detailsBtn);

                        cardView.addView(cardContent);
                        rideListLayout.addView(cardView);
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
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}