package com.example.dcl.safetravel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CustomerMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMarkerDragListener,
        RoutingListener{

    private GoogleMap mMap;
    private GoogleApiClient client;
    private FusedLocationProviderClient mfusedlocationclient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Marker lastLocationMarker;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};


    private String location_path="location";

    public static final int locationRequest_code =99;

    int PROXIMITY_RADIUS=1000;
    double latitude,longitude,end_latitude,end_longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);

       if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            Toast.makeText(this, "It is old version", Toast.LENGTH_SHORT).show();
            checkLocationpermission();
        }

        polylines = new ArrayList<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
            buildAppClient();
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMarkerDragListener(this);




    }

    protected synchronized void buildAppClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        client.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        if (lastLocationMarker != null) {
            lastLocationMarker.remove();
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(location_path);
        reference.setValue(location);

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

       lastLocationMarker = mMap.addMarker(markerOptions);


        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10));

        if (client != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client,this);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest = new LocationRequest();

        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        //mfusedlocationclient = LocationServices.getFusedLocationProviderClient(this);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){


        LocationServices.FusedLocationApi.requestLocationUpdates(client,locationRequest,this);

       }

        /*mfusedlocationclient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    Toast.makeText(CustomerMapsActivity.this, String.valueOf(location.getLatitude()), Toast.LENGTH_SHORT).show();
                }
            }
        });*/





    }

    public  boolean checkLocationpermission(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},locationRequest_code);
            }else{
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},locationRequest_code);
            }
            return false;
        }else{
           return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case locationRequest_code:
                if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        if(client==null){
                            buildAppClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public void OnClick(View view) {

        NearByPlace nearByPlace = new NearByPlace();
        Object transferData[]= new Object[2];
        if(view.getId()==R.id.button){
            EditText ET_search = findViewById(R.id.ET_seaerch);
            String location = ET_search.getText().toString();
            List<Address> addressList=null ;
            MarkerOptions mo = new MarkerOptions();

            if(!location.isEmpty()){
                Geocoder geocoder = new Geocoder(this);
                try {
                    addressList = geocoder.getFromLocationName(location,5);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for(int i=0;i<addressList.size();i++){
                    Address myAddress = addressList.get(i);
                    LatLng latLng = new LatLng(myAddress.getLatitude(),myAddress.getLongitude());
                    mo.position(latLng);
                    mo.title("This is your location");
                    mMap.addMarker(mo);
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }
            }
        }
         else if(view.getId()==R.id.hospitalBTN){
            mMap.clear();
            String url = getUrl(latitude,longitude,"hospital");
            transferData[0]=mMap;
            transferData[1]=url;

            nearByPlace.execute(transferData);
            Toast.makeText(this, "Hospital Shows..", Toast.LENGTH_SHORT).show();

        }
        else if(view.getId()==R.id.resturentBTN){
            mMap.clear();
            String url = getUrl(latitude,longitude,"resturant");
            transferData[0]=mMap;
            transferData[1]=url;
            nearByPlace.execute(transferData);
            Toast.makeText(this, "resturant Shows..", Toast.LENGTH_SHORT).show();

        }

        else if(view.getId()==R.id.schoolBTN){
            mMap.clear();
            String url = getUrl(latitude,longitude,"school");
            transferData[0]=mMap;
            transferData[1]=url;
            nearByPlace.execute(transferData);
            Toast.makeText(this, "school Shows..", Toast.LENGTH_SHORT).show();

        }

        else if(view.getId()==R.id.distanceBTN){
            mMap.clear();
            MarkerOptions  markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(end_latitude,end_longitude));
            markerOptions.title("Destination");
            markerOptions.draggable(true);

            float[] results = new float[10];
            Location.distanceBetween(latitude,longitude,end_latitude,end_longitude,results);
            markerOptions.snippet("Distance"+results[0]);
            mMap.addMarker(markerOptions);

            //call for history;
            Intent intent = new Intent();
            intent.putExtra("customerordriver","customer");
            startActivity(intent);

        }
    }

    public String getUrl(double latitude, double longitude, String nerabyplace){
        StringBuilder googleplace = new StringBuilder();
        googleplace.append("//maps.googleapis.com/maps/api/place/findplacefromtext/json?");
        googleplace.append("location"+latitude+","+longitude);
        googleplace.append("&radius="+PROXIMITY_RADIUS);
        googleplace.append("&type="+nerabyplace);
        googleplace.append("&sensor=true");
        googleplace.append("&key="+"AIzaSyC9Lt6E__xgcGpP0HwC0J6WAbjLpE7j5p0");

        return googleplace.toString();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.setDraggable(true);
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

        end_latitude = marker.getPosition().latitude;
        end_longitude = marker.getPosition().longitude;

        LatLng endPointLatlan = new LatLng(end_latitude,end_longitude);
        getRouteToMarker(endPointLatlan);

    }

    private void getRouteToMarker(LatLng endPointLatlan) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(true)
                .waypoints(new LatLng(latitude,longitude),endPointLatlan)
                .build();
        routing.execute();
    }

    private void erasePolyline() {
        for(Polyline line:polylines){
            line.remove();
        }
        polylines.clear();
    }


    @Override
    public void onRoutingFailure(RouteException e) {
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

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(new LatLng(latitude,longitude));
        builder.include(new LatLng(end_latitude,end_longitude));
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width *0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,padding);
        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude)).title("pickUp location"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(end_latitude,end_longitude)).title("Destination"));



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
