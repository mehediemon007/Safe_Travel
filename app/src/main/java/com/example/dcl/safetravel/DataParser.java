package com.example.dcl.safetravel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataParser {

    public HashMap<String, String> getPlace(JSONObject googlePlacejson) {
        HashMap<String, String> googlePlaceMap = new HashMap<>();
        String placename = "-NA-";
        String vicinity = "-NA-";
        String latitude = "";
        String longitude = "";
        String reference = "";
        try {
            if (!googlePlacejson.isNull("name")) {
                placename = googlePlacejson.getString("name");
            }
            if (!googlePlacejson.isNull("vicinity")) {
                vicinity = googlePlacejson.getString("vicinity");
            }

            latitude = googlePlacejson.getJSONObject("geometry").getJSONObject("location").getString("lat");
            longitude = googlePlacejson.getJSONObject("geometry").getJSONObject("location").getString("lag");

            reference = googlePlacejson.getString("reference");

            googlePlaceMap.put("place_name", placename);
            googlePlaceMap.put("vicinity", vicinity);
            googlePlaceMap.put("lat", latitude);
            googlePlaceMap.put("lag", longitude);
            googlePlaceMap.put("reference", reference);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return googlePlaceMap;

    }

    public List<HashMap<String,String>>  getPlaces(JSONArray jsonArray){
        int count= jsonArray.length();
        List<HashMap<String,String>> placesList = new ArrayList<>();
        HashMap<String,String> placeMap =null;
        for(int i=0;i<count;i++){
            try {
                placeMap = getPlace((JSONObject) jsonArray.get(i));
                placesList.add(placeMap);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return placesList;
    }

    public List<HashMap<String,String>>  parseData(String jsondata){

        JSONArray jsonArray=null;
        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(jsondata);
            jsonArray = jsonObject.getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return getPlaces(jsonArray);
    }
}
