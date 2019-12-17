package com.example.dcl.safetravel;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class singleHistoryView extends AppCompatActivity implements OnMapReadyCallback,RoutingListener {

    private String  rideId,currentUser,customerId,driverId,userDriverOrCustomer;

    private TextView location;
    private TextView distance;
    private TextView date;
    private TextView name;
    private TextView phone;

    private String ridedistance;
    private double rideprice;

    private ImageView userPic;
    private RatingBar ratingBar;

    private GoogleMap nmap;
    private SupportMapFragment mapFragment;

    private DatabaseReference rideHistoryInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_history_view);

        rideId = getIntent().getExtras().getString("rideId");

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        
        location = findViewById(R.id.rideLocation);
        distance = findViewById(R.id.distanceTV);
        date = findViewById(R.id.rideDateTV);
        name = findViewById(R.id.nameTV);
        phone = findViewById(R.id.PhoneTV);
        userPic = findViewById(R.id.userImage);
        ratingBar = findViewById(R.id.ratingBar);

        currentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        rideHistoryInfo = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        
        getRideInfo();

        
    }

    private void getRideInfo() {
        rideHistoryInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                 if(dataSnapshot.exists()){
                     for(DataSnapshot child : dataSnapshot.getChildren()){
                         if(child.getKey().equals("customer")){
                             customerId = child.getValue().toString();
                             if(!customerId.equals(currentUser)){
                                 userDriverOrCustomer="driver";
                                 getUserInfo("Customers",customerId);
                             }
                         }

                         if(child.getKey().equals("driver")){
                             driverId = child.getValue().toString();
                             if(!driverId.equals(currentUser)){
                                 userDriverOrCustomer="customer";
                                 getUserInfo("Riders",driverId);
                                 getCustomerRelatedObject();
                             }
                         }

                         if(child.getKey().equals("date")){
                             date.setText(getDate(Long.valueOf(child.getValue().toString())));
                         }

                         if(child.getKey().equals("destination")){
                             location.setText(child.getValue().toString());
                         }
                         if(child.getKey().equals("date")){
                             date.setText(getDate(Long.valueOf(child.getValue().toString())));
                         }
                         if(child.getKey().equals("Rating")){
                             ratingBar.setRating(Float.valueOf(child.getValue().toString()));
                         }
                         if(child.getKey().equals("distance")){
                             ridedistance = child.getValue().toString();
                             distance.setText(ridedistance.substring(0,Math.min(ridedistance.length(),5))+"Km");
                             rideprice = Double.valueOf(ridedistance)*20;
                         }
                     }
                 }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getCustomerRelatedObject() {
        ratingBar.setVisibility(View.VISIBLE);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                rideHistoryInfo.child("rating").setValue(v);

                DatabaseReference driverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverId).child("Rating");
                driverRatingDb.child(rideId).setValue(v);
            }
        });
    }

    private void getUserInfo(String otherCustomerOrDriver, String otherUserId) {

        DatabaseReference mOtherUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(otherCustomerOrDriver).child(otherUserId);
        mOtherUserDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        name.setText(map.get("name").toString());
                    }

                    if(map.get("phone")!=null){
                        phone.setText(map.get("phone").toString());
                    }
                    if(map.get("profilePic")!=null){
                        Glide.with(getApplication()).load(map.get("profieImage").toString()).into(userPic);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public String getDate(Long time){
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(time*1000);
        String  datentime = android.text.format.DateFormat.format("dd-MM-yyyy hh:mm",cal).toString();
        return datentime;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        nmap = googleMap;
    }

    @Override
    public void onRoutingFailure(RouteException e) {

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> arrayList, int i) {

    }

    @Override
    public void onRoutingCancelled() {

    }
}
