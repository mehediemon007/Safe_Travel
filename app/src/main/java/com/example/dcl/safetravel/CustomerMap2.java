package com.example.dcl.safetravel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMap2 extends AppCompatActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
                     GoogleApiClient.OnConnectionFailedListener,
                     com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;

    GoogleApiClient mgoogleappclient;
    Location mlastlocation;
    LocationRequest mlocationrequest;
    private static final int request_code=101;
    private int radius =1;
    LatLng pickupLocation;
    Marker pickupMarker;
    private  GeoQuery geoQuery;
    Marker driverMarkerOpt;
    private DatabaseReference  driverLocation;
    private ValueEventListener driverEventListener;

    LinearLayout linearLayout;
    TextView driverName,driverPhone, driverCar;
    ImageView driverProfile;
    RadioGroup radioGroup;
    RatingBar ratingBar;


    private boolean foundDriver=false,requestbol=false;
    private String foundDriverId="",service;

    Button logoutBTN,callUberBTN,settingBTN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map2);

        logoutBTN = findViewById(R.id.logoutBTN);
        callUberBTN = findViewById(R.id.callUberBTN);
        settingBTN = findViewById(R.id.settingBTN);

        linearLayout = findViewById(R.id.linearLayout);
        driverName = findViewById(R.id.driverName);
        driverPhone = findViewById(R.id.driverPhone);
        driverCar = findViewById(R.id.driverCar);
        driverProfile=findViewById(R.id.driverProfileIV);

        radioGroup = findViewById(R.id.radioGroup);
        radioGroup.check(R.id.uberX);
        ratingBar = findViewById(R.id.ratingBar);


        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){

            requestForPermission();

        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync((OnMapReadyCallback) this);

        logoutBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMap2.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        settingBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerMap2.this,CustomerSetting.class);
                startActivity(intent);
                return;
            }
        });


        callUberBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(requestbol){
                    requestbol=false;
                    geoQuery.removeAllListeners();

                    if(driverEventListener != null){
                         driverLocation.removeEventListener(driverEventListener);
                     }



                        if(foundDriverId!=null){
                            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(foundDriverId).child("CustomerId");
                            driverRef.removeValue();
                            foundDriverId=null;
                        }

                        foundDriver=false;
                        radius=1;

                        String customer_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Customer_pickup_location");

                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.removeLocation(customer_id, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });

                        if(pickupMarker!=null){
                            pickupMarker.remove();
                        }

                        driverName.setText("");
                        driverPhone.setText("");
                        driverCar.setText("");
                        linearLayout.setVisibility(View.GONE);
                        callUberBTN.setText("Call Uber");
                    }
                    else{

                    int selectRadioId = radioGroup.getCheckedRadioButtonId();
                    RadioButton radioButton = findViewById(selectRadioId);

                    if(radioButton.getText() == null){
                        return;
                    }
                    service = radioButton.getText().toString();

                    requestbol=true;
                    String customer_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Customer_pickup_location");

                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(customer_id, new GeoLocation(mlastlocation.getLatitude(), mlastlocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {

                        }
                    });

                    pickupLocation =new LatLng(mlastlocation.getLatitude(),mlastlocation.getLongitude());
                    pickupMarker  =  mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pick Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.customericon)));
                    getNearrstDriver();
                }


            }
        });

        //startService(new Intent(this,LocationServices.class));
    }



    private void getNearrstDriver(){

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("DriveAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!foundDriver && requestbol){

                    DatabaseReference serviceRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(key);
                    serviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                                Map<String,Object> driversercicemap = (Map<String, Object>) dataSnapshot.getValue();
                                if(foundDriver){
                                    return;
                                }
                                else if(driversercicemap.get("service").equals(service)){
                                    foundDriver=true;
                                    foundDriverId=dataSnapshot.getKey();

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(foundDriverId);
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                    HashMap map = new HashMap();
                                    map.put("CustomerId",customerId);
                                    driverRef.updateChildren(map);

                                    getDriverLocation();
                                    getDriverInfo();
                                    callUberBTN.setText("Waiting for driver.");
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                 if(!foundDriver){
                     radius++;
                     getNearrstDriver();
                 }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

        }

        ///Get All Drivers...............
     boolean calldriverAround = false;
     List<Marker> markerList = new ArrayList<Marker>();
   public void getDriverAround(){
        DatabaseReference findDrivers = FirebaseDatabase.getInstance().getReference("driverAvailable");
        GeoFire geoFire = new GeoFire(findDrivers);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mlastlocation.getLatitude(),mlastlocation.getLongitude()),1000);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                for(Marker markerIte:markerList){
                    if(markerIte.getTag().equals(key)){
                        return;
                    }
                }

                LatLng driverLatlan = new LatLng(location.latitude,location.longitude);

                Marker mdriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatlan));
                mdriverMarker.setTag(key);
            }

            @Override
            public void onKeyExited(String key) {
                for(Marker markerIte: markerList){
                    if(markerIte.getTag().equals(key)){
                        markerIte.remove();
                        markerList.remove(markerIte);
                        return;
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for(Marker markerIte: markerList){
                    if(markerIte.getTag().equals(key)){
                        markerIte.setPosition(new LatLng(location.latitude,location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    public void getDriverLocation(){
        driverLocation = FirebaseDatabase.getInstance().getReference().child("driverWorking").child(foundDriverId).child("l");
        driverEventListener = driverLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && requestbol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    callUberBTN.setText("Driver Reached");
                    double latitude=0;
                    double longitute =0;

                    if(map.get(0)!=null){
                        latitude = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1)!=null){
                        longitute = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng driverLatlan = new LatLng(latitude,longitute);
                    if(driverMarkerOpt!=null){
                        driverMarkerOpt.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatlan.latitude);
                    loc2.setLongitude(driverLatlan.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if(distance<100){
                        callUberBTN.setText("Driver Arrived");
                    }else{
                        callUberBTN.setText("Driver is coming"+ String.valueOf(distance));
                    }


                    driverMarkerOpt = mMap.addMarker(new MarkerOptions().position(driverLatlan).title("Driver Reached").icon(BitmapDescriptorFactory.fromResource(R.mipmap.caricon)));


                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    public void getDriverInfo(){

        linearLayout.setVisibility(View.VISIBLE);
        DatabaseReference   mdatabaseref = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(foundDriverId);
        mdatabaseref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map  = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        driverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        driverPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("car")!=null){
                        driverCar.setText(map.get("car").toString());
                    }
                    if(map.get("profileimage")!=null){
                        Glide.with(getApplication()).load( map.get("profileimage").toString()).into(driverProfile);
                    }

                    float ratingSum=0;
                    int   ratingTotal=0;
                    float ratingAvg=0;

                    for(DataSnapshot child : dataSnapshot.child("Rating").getChildren()){
                        ratingSum = ratingSum + Float.valueOf(child.getValue().toString());
                        ratingTotal++;

                    }
                    if(ratingTotal!=0){
                        ratingAvg = ratingSum/ratingTotal;
                        ratingBar.setRating(ratingAvg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case request_code:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        if(mgoogleappclient==null){
                            buildGoogleAppClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                }
                else {
                    Toast.makeText(this, "Permission Denied....", Toast.LENGTH_SHORT).show();
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }



    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        buildGoogleAppClient();
        mMap.setMyLocationEnabled(true);


    }

    protected  synchronized  void buildGoogleAppClient(){
        mgoogleappclient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mgoogleappclient.connect();
    }


    @Override
    public void onLocationChanged(Location location) {
        mlastlocation = location;

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        if (mgoogleappclient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mgoogleappclient,this);
        }


    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mlocationrequest = new LocationRequest();
        mlocationrequest.setInterval(1000);
        mlocationrequest.setFastestInterval(1000);
        mlocationrequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mgoogleappclient, mlocationrequest, this);
        if(!calldriverAround){
            getDriverAround();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public boolean requestForPermission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},request_code);
            }else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},request_code);
            }
            return false;
        }else{
            return true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();






    }
}
