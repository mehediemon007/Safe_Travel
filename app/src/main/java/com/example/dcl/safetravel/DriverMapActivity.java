package com.example.dcl.safetravel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, RoutingListener {

    private GoogleMap mMap;
    public float ridedistance;

    GoogleApiClient mgoogleappclient;
    Location mlastlocation;
    LocationRequest mlocationrequest;
    Marker pickUpMarker;
    DatabaseReference assignedCustomerPickedupRef;
    ValueEventListener assignedCustomerPickpListener;
    public  String customerId;
    Boolean isLogout= false;

    Button logoutBTN,settingBTN;
    Switch swithchBTN;

    LinearLayout linearLayout;
    TextView customerName,customerPhone;
    ImageView customerprofile;


    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};


    private static final int request_code=101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);


        logoutBTN = findViewById(R.id.logoutBTN);
        settingBTN = findViewById(R.id.settingBTN);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            requestForPermission();
        }

        swithchBTN  = findViewById(R.id.switchBTN);
        swithchBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                 if(b){
                    connectDriver();
                 }else{
                     RemoveDriverAvailable();
                 }
            }
        });


        polylines = new ArrayList<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        linearLayout = findViewById(R.id.linearLayout);
        customerName = findViewById(R.id.customerName);
        customerPhone = findViewById(R.id.customerPhone);
        customerprofile=findViewById(R.id.customerProfileIV);


        logoutBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLogout=true;
                RemoveDriverAvailable();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        settingBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriverMapActivity.this,DriverSetting.class);
                startActivity(intent);
                return;

            }
        });

        getAssignedCustomer();
    }

    private void connectDriver() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mgoogleappclient, mlocationrequest, this);
    }


    public void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("User").child("Riders").child(driverId).child("CustomerId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !customerId.equals("")){
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickUpLocation();
                    getAssignedCustomerInfo();

                }else{
                     erasePolyline();
                      customerId="";
                      ridedistance=0;
                      if(pickUpMarker!=null){
                          pickUpMarker.remove();
                      }
                      if(assignedCustomerPickpListener !=null){
                          assignedCustomerPickedupRef.removeEventListener(assignedCustomerPickpListener);
                      }


                      customerName.setText("");
                      customerPhone.setText("");
                      customerprofile.setImageResource(R.mipmap.customericon_round);
                      linearLayout.setVisibility(View.GONE);


                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void erasePolyline() {
        for(Polyline line:polylines){
            line.remove();
        }
        polylines.clear();
    }


    public void getAssignedCustomerPickUpLocation(){
        assignedCustomerPickedupRef= FirebaseDatabase.getInstance().getReference().child("Customer_pickup_location").child(customerId).child("l");
        assignedCustomerPickpListener =assignedCustomerPickedupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Object> map = (List<Object>) dataSnapshot.getValue();

                double latitude=0;
                double longitute =0;

                if(map.get(0)!=null){
                    latitude = Double.parseDouble(map.get(0).toString());
                }
                if(map.get(1)!=null){
                    longitute = Double.parseDouble(map.get(1).toString());
                }

                LatLng driverLatlan = new LatLng(latitude,longitute);

               pickUpMarker = mMap.addMarker(new MarkerOptions().position(driverLatlan).title("Customer Waiting").icon(BitmapDescriptorFactory.fromResource(R.mipmap.customericon)));

               getRouteToMarker(driverLatlan);






            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker(LatLng driverLatlan) {

        if(driverLatlan!=null && mlastlocation!=null){
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(new LatLng(mlastlocation.getLatitude(),mlastlocation.getLongitude()),driverLatlan)
                    .build();
            routing.execute();
        }
        }



    public void getAssignedCustomerInfo(){

        linearLayout.setVisibility(View.VISIBLE);
        DatabaseReference   mdatabaseref = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mdatabaseref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map  = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        customerName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        customerPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileimage")!=null){
                        Glide.with(getApplication()).load( map.get("profileimage").toString()).into(customerprofile);
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
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        if(mgoogleappclient==null){
                            buildGoogleAppClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                }
                else {
                    Toast.makeText(this, "Permission Denied....", Toast.LENGTH_SHORT).show();
                }

                return;
        }

    }




    @Override
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

        if(!customerId.equals("")){
            ridedistance = mlastlocation.distanceTo(location)/1000;
        }
        mlastlocation = location;

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference Availableref = FirebaseDatabase.getInstance().getReference("DriveAvailable");
        DatabaseReference Workingref = FirebaseDatabase.getInstance().getReference("DriverWorking");

        GeoFire AvailablegeoFire = new GeoFire(Availableref);
        GeoFire WorkingGeoFire = new GeoFire(Workingref);

        switch (customerId){
            case "":

                AvailablegeoFire.setLocation(driverId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });

                WorkingGeoFire.removeLocation(driverId, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });

                break;

             default:

                 AvailablegeoFire.removeLocation(driverId, new GeoFire.CompletionListener() {
                     @Override
                     public void onComplete(String key, DatabaseError error) {

                     }
                 });
                 WorkingGeoFire.setLocation(driverId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                     @Override
                     public void onComplete(String key, DatabaseError error){
                         rideHistroy();
                     }
                 });

                 break;
        }






        if (mgoogleappclient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mgoogleappclient,this);
        }

    }

    private void rideHistroy() {

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverHistory = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child("History");
        DatabaseReference customerHistory = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child("History");
        DatabaseReference  RideHistory = FirebaseDatabase.getInstance().getReference("RideHistory");
        String rideId = RideHistory.push().getKey();
        driverHistory.child(rideId).setValue(true);
        customerHistory.child(rideId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver",driverId);
        map.put("customer",customerId);
        map.put("rating",0);
        map.put("timeStamp",getCurrentTimestamp());
        map.put("location/from/lat",mlastlocation.getLatitude());
        map.put("location/from/lon",mlastlocation.getLongitude());
        map.put("location/to/lat",mlastlocation.getLatitude());
        map.put("location/to/lon",mlastlocation.getLongitude());
        map.put("distance",ridedistance);
        RideHistory.child(rideId).updateChildren(map);
    }

    private Long getCurrentTimestamp() {
        Long timeStamp = System.currentTimeMillis()/1000;
        return timeStamp;
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
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public boolean requestForPermission(){

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},request_code);
            }else{
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

        if(!isLogout){
            RemoveDriverAvailable();
        }

    }

    public void RemoveDriverAvailable(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriveAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(driverId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });

    }

    @Override
    public void onRoutingFailure(RouteException e)  {
        if(e!=null){
            Toast.makeText(this, "Error"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Something Going wrong", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingCancelled() {

    }
}
