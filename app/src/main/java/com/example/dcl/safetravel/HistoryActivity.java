package com.example.dcl.safetravel;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.dcl.safetravel.historyRecycleView.HistoryViewAdapter;
import com.example.dcl.safetravel.historyRecycleView.historyView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    String customerordriver,userId;

    RecyclerView historyview;
    RecyclerView.Adapter historyAdapter;
    RecyclerView.LayoutManager historyLayoutManager;
    private List<historyView> resultHistory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        resultHistory = new ArrayList<>();
        historyview.setNestedScrollingEnabled(false);
        historyview.setHasFixedSize(true);

        historyLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        historyview.setLayoutManager(historyLayoutManager);

        customerordriver = getIntent().getExtras().getString("customer");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getuserhistory();

        historyAdapter = new HistoryViewAdapter(getDataset(),HistoryActivity.this);
        historyview.setAdapter(historyAdapter);


    }

    private void getuserhistory() {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child("History");
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot history : dataSnapshot.getChildren()){
                        FetchRideInformation(history.getKey());

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void FetchRideInformation(String key) {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History").child(key);
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String rideId = dataSnapshot.getKey();
                    Long timestamp = 0L;
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if(child.getKey().equals("timestamp")){
                            timestamp = Long.valueOf(child.getValue().toString());
                        }
                    }
                    historyView obj = new historyView(rideId,getDate(timestamp));
                    resultHistory.add(obj);
                    historyAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date = android.text.format.DateFormat.format("dd-MM-yyyy hh:mm",cal).toString();
        return date;
    }


    private List<historyView> getDataset() {
        return  resultHistory;
    }
}
