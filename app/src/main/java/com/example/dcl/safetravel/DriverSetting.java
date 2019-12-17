package com.example.dcl.safetravel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSetting extends AppCompatActivity {

    EditText nameET,phoneET,carET;
    Button confirmBTN,backBTN;
    ImageView imageIV;
    RadioGroup radioGroup;

    private FirebaseAuth mauth;
    private DatabaseReference mdriverdatabaseref;
    private String driverId;
    private String name,phone,car,service;
    private Uri resultUri;
    private String profileimageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_setting);

        nameET = findViewById(R.id.nameET);
        phoneET = findViewById(R.id.phoneET);
        carET  = findViewById(R.id.carET);

        confirmBTN = findViewById(R.id.confirmBTN);
        backBTN = findViewById(R.id.backBTN);

        imageIV = findViewById(R.id.imageIV);
        radioGroup = findViewById(R.id.radioGroup);

        mauth = FirebaseAuth.getInstance();
        driverId = mauth.getCurrentUser().getUid();
        mdriverdatabaseref = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverId);

        getUserInfo();

        imageIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });


        confirmBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                saveUserInfo();

            }
        });

        backBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });
    }

    public void getUserInfo(){
        mdriverdatabaseref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map  = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        name = map.get("name").toString();
                        nameET.setText(name);
                    }
                    if(map.get("phone")!=null){
                        phone = map.get("phone").toString();
                        phoneET.setText(phone);
                    }
                    if(map.get("car")!=null){
                        car = map.get("car").toString();
                        carET.setText(car);
                    }
                    if(map.get("service")!=null){
                        service = map.get("service").toString();
                        switch (service){
                            case "UberX":
                                radioGroup.check(R.id.uberX);
                                break;
                            case "UberXi":
                                radioGroup.check(R.id.uberXi);
                                break;
                            case "UberBlack":
                                radioGroup.check(R.id.uberBlack);
                                break;
                        }
                    }
                    if(map.get("profileimage")!=null){
                        profileimageUri=map.get("profileimage").toString();
                        Glide.with(getApplication()).load(profileimageUri).into(imageIV);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public  void saveUserInfo(){

        name = nameET.getText().toString();
        phone = phoneET.getText().toString();
        car  = carET.getText().toString();

        int selectRadioId = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(selectRadioId);

        if(radioButton.getText() == null){
            return;
        }
        service = radioButton.getText().toString();

        Map map = new HashMap();
        map.put("name",name);
        map.put("phone",phone);
        map.put("car",car);
       // map.put("service",service);
        mdriverdatabaseref.updateChildren(map);

        if(resultUri!=null){
            final StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("Profile_Images").child(driverId);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }



            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = imageRef.putBytes(data);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        Uri imageUri = task.getResult();
                        Map map = new HashMap();
                        map.put("profileimage",imageUri.toString());
                        mdriverdatabaseref.updateChildren(map);
                    }
                }
            });
            finish();
            return;
        }else{
            finish();
        }



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode== Activity.RESULT_OK && data!=null &&data.getData()!=null ){
            final Uri imageUri = data.getData();
            resultUri=imageUri;
            imageIV.setImageURI(resultUri);

        }

    }
}
