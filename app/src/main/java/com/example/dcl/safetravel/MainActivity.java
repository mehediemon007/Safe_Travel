package com.example.dcl.safetravel;

import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    Button driverBTN;
    private static final String TAG ="MainActivity";
    private static final int ERROR_DIALOG_REQUEST=9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        driverBTN = findViewById(R.id.driver);

        isServuceOk();

        startService(new Intent(MainActivity.this,onAppKilled.class));

        driverBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Intent intent = new Intent(MainActivity.this,DriverSignInActivity.class);
                startActivity(intent);
                finish();
                return;

            }
        });

        findViewById(R.id.customer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,CustomerSignInActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }

    public boolean isServuceOk(){
        Log.d(TAG,"isServiceOK: checking google service version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            Log.d(TAG,"isServiceOk:google play service is working");
            return true;
        }else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){

            Log.d(TAG,"isServiceOk:There is an error but user can fix it");

            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this,available,ERROR_DIALOG_REQUEST);
            dialog.show();

        }
        else {
            Toast.makeText(this, "You can't request a map ", Toast.LENGTH_SHORT).show();
        }

        return false;
    }
}
