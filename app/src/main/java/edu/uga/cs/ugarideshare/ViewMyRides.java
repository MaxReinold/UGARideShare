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
    private ValueEventListener ridesListener;
    private DatabaseReference ridesRef;
    private LinearLayout rideListLayout;
    private String userUid;

    /**
     * Called when the activity is starting.
     * @param savedInstanceState The previously saved instance state.
     */
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
        userUid = firebaseUser.getUid();

        rideListLayout = findViewById(R.id.rideListLayout);
        ridesRef = FirebaseDatabase.getInstance().getReference("rides");
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        rideListLayout.removeAllViews();
        rideKeys.clear();
        rideSummaries.clear();

        ridesListener = new ValueEventListener() {
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
                userRides.sort((a, b) -> {
                    Long aDate = 0L, bDate = 0L;
                    Object aObj = a.child("date").getValue();
                    Object bObj = b.child("date").getValue();
                    if (aObj instanceof Long) aDate = (Long) aObj;
                    else if (aObj instanceof Double) aDate = ((Double) aObj).longValue();
                    if (bObj instanceof Long) bDate = (Long) bObj;
                    else if (bObj instanceof Double) bDate = ((Double) bObj).longValue();
                    return aDate.compareTo(bDate);
                });

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

                        String driverUid = rideSnap.child("userDriver/uid").getValue(String.class);
                        String driverEmail = rideSnap.child("userDriver/email").getValue(String.class);
                        String riderUid = rideSnap.child("userRider/uid").getValue(String.class);
                        String riderEmail = rideSnap.child("userRider/email").getValue(String.class);

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

                        if (driverUid != null && userUid.equals(driverUid) && riderUid != null && userUid.equals(riderUid)) {
                            TextView roleView = new TextView(ViewMyRides.this);
                            roleView.setText("Driving & Riding");
                            roleView.setTypeface(null, Typeface.BOLD_ITALIC);
                            cardContent.addView(roleView);
                        } else if (driverUid != null && userUid.equals(driverUid) && riderUid != null && !userUid.equals(riderUid)) {
                            TextView roleView = new TextView(ViewMyRides.this);
                            roleView.setText("Driving");
                            roleView.setTypeface(null, Typeface.BOLD);
                            cardContent.addView(roleView);
                            TextView riderView = new TextView(ViewMyRides.this);
                            riderView.setText("Rider: " + (riderEmail != null ? riderEmail : ""));
                            cardContent.addView(riderView);
                        } else if (riderUid != null && userUid.equals(riderUid) && driverUid != null && !userUid.equals(driverUid)) {
                            TextView roleView = new TextView(ViewMyRides.this);
                            roleView.setText("Riding");
                            roleView.setTypeface(null, Typeface.BOLD);
                            cardContent.addView(roleView);
                            TextView driverView = new TextView(ViewMyRides.this);
                            driverView.setText("Driver: " + (driverEmail != null ? driverEmail : ""));
                            cardContent.addView(driverView);
                        } else if (driverUid != null && userUid.equals(driverUid)) {
                            TextView roleView = new TextView(ViewMyRides.this);
                            roleView.setText("Driving");
                            roleView.setTypeface(null, Typeface.BOLD);
                            cardContent.addView(roleView);
                        } else if (riderUid != null && userUid.equals(riderUid)) {
                            TextView roleView = new TextView(ViewMyRides.this);
                            roleView.setText("Riding");
                            roleView.setTypeface(null, Typeface.BOLD);
                            cardContent.addView(roleView);
                        }

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
        };
        ridesRef.addValueEventListener(ridesListener);
    }

    /**
     * Called when the activity is going into the background.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (ridesListener != null) {
            ridesRef.removeEventListener(ridesListener);
        }
    }

    /**
     * Called to save the instance state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("rideKeys", new ArrayList<>(rideKeys));
        outState.putStringArrayList("rideSummaries", new ArrayList<>(rideSummaries));
    }

    /**
     * Called to restore the instance state.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        rideKeys = savedInstanceState.getStringArrayList("rideKeys");
        rideSummaries = savedInstanceState.getStringArrayList("rideSummaries");
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