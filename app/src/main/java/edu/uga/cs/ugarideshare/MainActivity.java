package edu.uga.cs.ugarideshare;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Button;
import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ScrollView;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import android.text.format.DateFormat;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "UGARideShare";
    private FirebaseAuth mAuth;
    private FirebaseDatabase dbRef;
    FirebaseUser user;
    TextView emailDisplay;
    TextView pointsDisplay;
    Button btnRequestRide, btnViewMyRides, btnViewOfferedRides, btnViewRequestedRides;

    /**
     * Called when the activity is starting.
     * @param savedInstanceState The previously saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance();
        emailDisplay = findViewById(R.id.user_email);
        pointsDisplay = findViewById(R.id.points_display);
        user = mAuth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
        } else {
            emailDisplay.setText(getResources().getString(R.string.welcome, user.getEmail()));
            DatabaseReference userRef = dbRef.getReference("users").child(user.getUid());
            userRef.child("points").addValueEventListener(new ValueEventListener() {
                @SuppressLint("DefaultLocale")
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Long numberValue = dataSnapshot.getValue(Long.class);
                        if (numberValue != null) {
                            pointsDisplay.setText(String.format("%,d", numberValue));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, databaseError.toString());
                }
            });
        }

        btnRequestRide = findViewById(R.id.btn_new_ride);
        btnViewMyRides = findViewById(R.id.btn_view_rides);
        btnViewOfferedRides = findViewById(R.id.btn_view_offered_rides);
        btnViewRequestedRides = findViewById(R.id.btn_view_requested_rides);

        btnRequestRide.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewRide.class);
            startActivity(intent);
        });

        btnViewMyRides.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewMyRides.class);
            startActivity(intent);
        });

        btnViewOfferedRides.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewOfferedRides.class);
            startActivity(intent);
        });

        btnViewRequestedRides.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewRequestedRides.class);
            startActivity(intent);
        });
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkPastRides();
    }

    /**
     * Checks for past rides and prompts the user if needed.
     */
    private void checkPastRides() {
        if (user == null) return;
        DatabaseReference ridesRef = dbRef.getReference("rides");
        ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                for (DataSnapshot rideSnap : snapshot.getChildren()) {
                    Object dateObj = rideSnap.child("date").getValue();
                    long rideTime = 0;
                    if (dateObj instanceof Long) {
                        rideTime = (Long) dateObj;
                    } else if (dateObj instanceof Double) {
                        rideTime = ((Double) dateObj).longValue();
                    }
                    if (rideTime == 0 || rideTime > now) continue;

                    String driverUid = rideSnap.child("userDriver/uid").getValue(String.class);
                    String driverEmail = rideSnap.child("userDriver/email").getValue(String.class);
                    String riderUid = rideSnap.child("userRider/uid").getValue(String.class);
                    String riderEmail = rideSnap.child("userRider/email").getValue(String.class);

                    boolean isDriver = driverUid != null && driverUid.equals(user.getUid());
                    boolean isRider = riderUid != null && riderUid.equals(user.getUid());
                    if (!isDriver && !isRider) continue;

                    String addressFrom = rideSnap.child("addressFrom").getValue(String.class);
                    String addressTo = rideSnap.child("addressTo").getValue(String.class);

                    Boolean driverConfirm = rideSnap.child("driverConfirm").getValue(Boolean.class);
                    Boolean riderConfirm = rideSnap.child("riderConfirm").getValue(Boolean.class);

                    boolean showPrompt = (isDriver && driverConfirm == null) || (isRider && riderConfirm == null);

                    if (!showPrompt) continue;

                    showPastRideDialog(rideSnap.getKey(), addressFrom, addressTo, rideTime,
                        driverEmail, riderEmail, driverUid, riderUid,
                        isDriver, isRider, driverConfirm, riderConfirm);
                    break;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Shows a dialog for a past ride.
     */
    private void showPastRideDialog(String rideId, String addressFrom, String addressTo, long rideTime,
                                   String driverEmail, String riderEmail, String driverUid, String riderUid,
                                   boolean isDriver, boolean isRider, Boolean driverConfirm, Boolean riderConfirm) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("Past Ride");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 32);

        TextView info = new TextView(this);
        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(addressFrom != null ? addressFrom : "").append("\n");
        sb.append("To: ").append(addressTo != null ? addressTo : "").append("\n");
        String dateStr;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat stf = new SimpleDateFormat("HH:mm");
            Date rideDateObj = new Date(rideTime);
            dateStr = sdf.format(rideDateObj) + " " + stf.format(rideDateObj);
        } catch (Exception e) {
            dateStr = new Date(rideTime).toString();
        }
        sb.append("Date: ").append(dateStr).append("\n");
        sb.append("Driver: ").append(driverEmail != null ? driverEmail : "None").append("\n");
        sb.append("Rider: ").append(riderEmail != null ? riderEmail : "None").append("\n");
        info.setText(sb.toString());
        info.setTextSize(18);
        info.setPadding(0, 0, 0, 32);

        layout.addView(title);
        layout.addView(info);

        DatabaseReference rideRef = dbRef.getReference("rides").child(rideId);

        if ((driverUid == null || driverUid.isEmpty()) || (riderUid == null || riderUid.isEmpty())) {
            Button removeBtn = new Button(this);
            removeBtn.setText("Remove Ride");
            removeBtn.setAllCaps(false);
            removeBtn.setOnClickListener(v -> {
                rideRef.removeValue();
                dialog.dismiss();
                checkPastRides();
            });
            layout.addView(removeBtn);
        } else {
            boolean showConfirm = (isDriver && driverConfirm == null) || (isRider && riderConfirm == null);
            if (showConfirm) {
                Button confirmBtn = new Button(this);
                confirmBtn.setText("Confirm Ride Took Place");
                confirmBtn.setAllCaps(false);
                Button denyBtn = new Button(this);
                denyBtn.setText("Deny Ride Took Place");
                denyBtn.setAllCaps(false);

                confirmBtn.setOnClickListener(v -> {
                    if (isDriver) {
                        rideRef.child("driverConfirm").setValue(true);
                    }
                    if (isRider) {
                        rideRef.child("riderConfirm").setValue(true);
                    }
                    rideRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            Boolean dConf = snap.child("driverConfirm").getValue(Boolean.class);
                            Boolean rConf = snap.child("riderConfirm").getValue(Boolean.class);
                            if (Boolean.TRUE.equals(dConf) && Boolean.TRUE.equals(rConf)) {
                                DatabaseReference usersRef = dbRef.getReference("users");
                                if (driverUid != null && riderUid != null) {
                                    usersRef.child(driverUid).child("points").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dSnap) {
                                            Long dPoints = dSnap.getValue(Long.class);
                                            if (dPoints == null) dPoints = 0L;
                                            usersRef.child(driverUid).child("points").setValue(dPoints + 50);
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                                    usersRef.child(riderUid).child("points").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot rSnap) {
                                            Long rPoints = rSnap.getValue(Long.class);
                                            if (rPoints == null) rPoints = 0L;
                                            usersRef.child(riderUid).child("points").setValue(rPoints - 50);
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                                }
                                rideRef.removeValue();
                            }
                            dialog.dismiss();
                            checkPastRides();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                });

                denyBtn.setOnClickListener(v -> {
                    if (isDriver) {
                        rideRef.child("driverConfirm").setValue(false);
                    }
                    if (isRider) {
                        rideRef.child("riderConfirm").setValue(false);
                    }
                    dialog.dismiss();
                    checkPastRides();
                });

                layout.addView(confirmBtn);
                layout.addView(denyBtn);
            }
        }

        scrollView.addView(layout);
        dialog.setContentView(scrollView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.CENTER);
        }
        dialog.show();
    }

    /**
     * Called to initialize the contents of the Activity's standard options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Called whenever an item in your options menu is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}