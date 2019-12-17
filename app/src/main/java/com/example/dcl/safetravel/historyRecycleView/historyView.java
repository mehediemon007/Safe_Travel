package com.example.dcl.safetravel.historyRecycleView;

public class historyView{

    private String rideId;
    private String timeStamp;


    public historyView(){

    }

    public historyView(String rideId, String timeStamp) {
        this.rideId = rideId;
        this.timeStamp = timeStamp;
    }

    public String getRideId() {
        return rideId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }
}
