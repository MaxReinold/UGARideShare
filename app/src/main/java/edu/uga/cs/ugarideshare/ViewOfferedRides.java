package edu.uga.cs.ugarideshare;

import android.os.Bundle;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Typeface;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.ViewGroup;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class ViewOfferedRides extends AppCompatActivity {

    private DatabaseReference ridesRef;
    private ValueEventListener ridesListener;
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
        setContentView(R.layout.activity_view_offered_rides);
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

        ridesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                rideListLayout.removeAllViews();
                List<DataSnapshot> userRides = new ArrayList<>();
                for (DataSnapshot rideSnap : snapshot.getChildren()) {
                    String driverUid = rideSnap.child("userDriver/uid").getValue(String.class);
                    String riderUid = rideSnap.child("userRider/uid").getValue(String.class);
                    if (driverUid != null && !driverUid.isEmpty()
                        && (riderUid == null || riderUid.isEmpty())
                        && !driverUid.equals(userUid)) {
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
                    TextView emptyView = new TextView(ViewOfferedRides.this);
                    emptyView.setText("No offered rides found.");
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

                        CardView cardView = new CardView(ViewOfferedRides.this);
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        cardParams.setMargins(20, 20, 20, 20);
                        cardView.setLayoutParams(cardParams);
                        cardView.setRadius(16);
                        cardView.setCardElevation(8);

                        LinearLayout cardContent = new LinearLayout(ViewOfferedRides.this);
                        cardContent.setOrientation(LinearLayout.VERTICAL);
                        cardContent.setPadding(48, 32, 48, 32);

                        if (driverUid != null && !driverUid.isEmpty() && riderUid != null && !riderUid.isEmpty()) {
                            TextView driverView = new TextView(ViewOfferedRides.this);
                            driverView.setText("Driver: " + (driverEmail != null ? driverEmail : ""));
                            cardContent.addView(driverView);
                            TextView riderView = new TextView(ViewOfferedRides.this);
                            riderView.setText("Rider: " + (riderEmail != null ? riderEmail : ""));
                            cardContent.addView(riderView);
                        } else if (driverUid != null && !driverUid.isEmpty()) {
                            TextView driverView = new TextView(ViewOfferedRides.this);
                            driverView.setText("Driver: " + (driverEmail != null ? driverEmail : ""));
                            cardContent.addView(driverView);
                        }

                        TextView fromView = new TextView(ViewOfferedRides.this);
                        fromView.setText("From: " + (addressFrom != null ? addressFrom : ""));
                        fromView.setTextSize(18);
                        fromView.setTypeface(null, Typeface.BOLD);

                        TextView toView = new TextView(ViewOfferedRides.this);
                        toView.setText("To: " + (addressTo != null ? addressTo : ""));
                        toView.setTextSize(18);

                        TextView dateView = new TextView(ViewOfferedRides.this);
                        dateView.setText("Date: " + dateStr);
                        dateView.setTextSize(16);
                        dateView.setTypeface(null, Typeface.ITALIC);

                        Button detailsBtn = new Button(ViewOfferedRides.this);
                        detailsBtn.setText("View Details");
                        detailsBtn.setAllCaps(false);

                        detailsBtn.setOnClickListener(v -> {
                            Intent intent = new Intent(ViewOfferedRides.this, ViewRide.class);
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
                android.widget.Toast.makeText(ViewOfferedRides.this, "Failed to load rides.", android.widget.Toast.LENGTH_SHORT).show();
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
     * Called when the user presses the Up button.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}