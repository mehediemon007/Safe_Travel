package com.example.dcl.safetravel;

import android.os.AsyncTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class NearByPlace extends AsyncTask<Object,String,String> {
    String nearbyplacesData;
    GoogleMap mMap;
    String url;
    @Override
    protected String doInBackground(Object... objects) {

        mMap = (GoogleMap)objects[0];
        url = (String)objects[1];

        DownloadUrl downloadUrl = new DownloadUrl();
        try {
            nearbyplacesData = downloadUrl.readUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nearbyplacesData;
    }

    @Override
    protected void onPostExecute(String s) {
        List<HashMap<String,String>> nearbyPlacesList=null;
        DataParser parser = new DataParser();
        nearbyPlacesList = parser.parseData(s);
        showNearByPlaces(nearbyPlacesList);
    }

    private  void showNearByPlaces(List<HashMap<String ,String>> nearbyplacesList){
        for(int i=0;i<nearbyplacesList.size();i++){
            HashMap<String,String> googleyPlace = nearbyplacesList.get(i);
            MarkerOptions markerOptions = new MarkerOptions();

            String placeName = googleyPlace.get("place_name");
            String vicinity  = googleyPlace.get("vicinity");
            double lat = Double.parseDouble(googleyPlace.get("lat"));
            double lang = Double.parseDouble(googleyPlace.get("lag"));

            LatLng latLng = new LatLng(lat,lang);

            markerOptions.position(latLng);
            markerOptions.title(placeName+":"+vicinity);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

            mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(10));


        }
    }
}
