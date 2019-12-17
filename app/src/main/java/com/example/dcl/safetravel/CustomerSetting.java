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

public class CustomerSetting extends AppCompatActivity {

    EditText nameET,phoneET;
    Button confirmBTN,backBTN;
    ImageView imageIV;

    private FirebaseAuth mauth;
    private DatabaseReference mdatabaseref;
    private String customerId;
    private String name;
    private String phone;
    private Uri resultUri;
    private String profileimageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_setting);

        nameET = findViewById(R.id.nameET);
        phoneET = findViewById(R.id.phoneET);

        confirmBTN = findViewById(R.id.confirmBTN);
        backBTN = findViewById(R.id.backBTN);

        imageIV = findViewById(R.id.imageIV);

        mauth = FirebaseAuth.getInstance();
        customerId = mauth.getCurrentUser().getUid();
        mdatabaseref = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);

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
                startActivity(new Intent(CustomerSetting.this,CustomerMapsActivity.class));
                finish();
                return;
            }
        });
    }

    public void getUserInfo(){
        mdatabaseref.addValueEventListener(new ValueEventListener() {
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

        Map map = new HashMap();
        map.put("name",name);
        map.put("phone",phone);
        mdatabaseref.updateChildren(map);

        if(resultUri!=null){
            final StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("Profile_Images").child(customerId);
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
                        mdatabaseref.updateChildren(map);
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
