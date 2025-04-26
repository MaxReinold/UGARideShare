package edu.uga.cs.ugarideshare;

import android.os.Bundle;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.Typeface;
import androidx.appcompat.app.ActionBar;
import androidx.cardview.widget.CardView;
import android.view.ViewGroup;
import android.view.View;

import androidx.activity.EdgeToEdge;
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

import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class ViewRequestedRides extends AppCompatActivity {

    private DatabaseReference ridesRef;
    private ValueEventListener ridesListener;
    private LinearLayout rideListLayout;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_requested_rides);
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
                    String riderUid = rideSnap.child("userRider/uid").getValue(String.class);
                    String driverUid = rideSnap.child("userDriver/uid").getValue(String.class);
                    // Only show rides where user is NOT the driver or rider, but is a requested ride (has rider, no driver)
                    if (riderUid != null && !riderUid.isEmpty()
                        && (driverUid == null || driverUid.isEmpty())
                        && !riderUid.equals(userUid)) {
                        userRides.add(rideSnap);
                    }
                }
                if (userRides.isEmpty()) {
                    TextView emptyView = new TextView(ViewRequestedRides.this);
                    emptyView.setText("No requested rides found.");
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

                        CardView cardView = new CardView(ViewRequestedRides.this);
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        cardParams.setMargins(20, 20, 20, 20);
                        cardView.setLayoutParams(cardParams);
                        cardView.setRadius(16);
                        cardView.setCardElevation(8);

                        LinearLayout cardContent = new LinearLayout(ViewRequestedRides.this);
                        cardContent.setOrientation(LinearLayout.VERTICAL);
                        cardContent.setPadding(48, 32, 48, 32);

                        TextView fromView = new TextView(ViewRequestedRides.this);
                        fromView.setText("From: " + (addressFrom != null ? addressFrom : ""));
                        fromView.setTextSize(18);
                        fromView.setTypeface(null, Typeface.BOLD);

                        TextView toView = new TextView(ViewRequestedRides.this);
                        toView.setText("To: " + (addressTo != null ? addressTo : ""));
                        toView.setTextSize(18);

                        TextView dateView = new TextView(ViewRequestedRides.this);
                        dateView.setText("Date: " + dateStr);
                        dateView.setTextSize(16);
                        dateView.setTypeface(null, Typeface.ITALIC);

                        Button detailsBtn = new Button(ViewRequestedRides.this);
                        detailsBtn.setText("View Details");
                        detailsBtn.setAllCaps(false);

                        detailsBtn.setOnClickListener(v -> {
                            Intent intent = new Intent(ViewRequestedRides.this, ViewRide.class);
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
                Toast.makeText(ViewRequestedRides.this, "Failed to load rides.", Toast.LENGTH_SHORT).show();
            }
        };
        ridesRef.addValueEventListener(ridesListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ridesListener != null) {
            ridesRef.removeEventListener(ridesListener);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}