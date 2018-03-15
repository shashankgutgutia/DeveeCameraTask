package com.deevesoft.shashankgutgutia.deevesofttask;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {


    private static final int RC_PHOTO_PICKER =  2;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int PIC_CROP = 3;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private Camera camera;
    private CameraPreview preview;
    private File pictureFile;
    private FirebaseDatabase mFirebaseDatabse;
    private DatabaseReference mDatabaseReference;
    private FrameLayout mpreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFirebaseDatabse= FirebaseDatabase.getInstance();
        mDatabaseReference=mFirebaseDatabse.getReference().child("photos");


        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {


            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},
                        1);
            }

        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {


            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        2);
            }

        }


        firebaseStorage=FirebaseStorage.getInstance();
        storageReference=firebaseStorage.getReference().child("photos");
        camera = getCameraInstance();
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(90);
        camera.setParameters(parameters);
        // Create our Preview view and set it as the content of our activity.
        preview = new CameraPreview(this, camera);
        mpreview = (FrameLayout) findViewById(R.id.camera_preview);
        mpreview.addView(preview);

        FloatingActionButton floatingActionButton=findViewById(R.id.fcapture);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, mPicture);
            }
        });

        FloatingActionButton floatingActionButton2=findViewById(R.id.fedit);
        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pictureFile!=null) {
                    Uri ImageUri = Uri.fromFile(pictureFile);
                    Intent cropIntent = new Intent("com.android.camera.action.CROP");
                    cropIntent.setDataAndType(ImageUri, "image/*");

                    cropIntent.putExtra("crop", "true");

                    cropIntent.putExtra("aspectX", 1);
                    cropIntent.putExtra("aspectY", 1);

                    cropIntent.putExtra("outputX", 256);
                    cropIntent.putExtra("outputY", 256);

                    cropIntent.putExtra("return-data", true);
                    startActivityForResult(cropIntent, PIC_CROP);
                }
                else{
                    Toast.makeText(getApplicationContext(),"No photo selected",Toast.LENGTH_SHORT).show();
                }
            }
        });

        FloatingActionButton floatingActionButton3=findViewById(R.id.fupload);
        floatingActionButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pictureFile!=null) {
                    Uri ImageUri = Uri.fromFile(pictureFile);
                    StorageReference photoRef = storageReference.child(ImageUri.getLastPathSegment());
                    photoRef.putFile(ImageUri).addOnSuccessListener(MainActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            Photos photo = new Photos(downloadUrl.toString());
                            mDatabaseReference.push().setValue(photo);
                            Toast.makeText(getApplicationContext(), "Successfully Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else{
                    Toast.makeText(getApplicationContext(),"No photo selected",Toast.LENGTH_SHORT).show();
                }
            }
        });

        FloatingActionButton floatingActionButton4=findViewById(R.id.SeePics);
        floatingActionButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FirebaseImages.class);
                startActivity(intent);
            }
        });

        FloatingActionButton floatingActionButton5=findViewById(R.id.photoPickerButton);
        floatingActionButton5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null){
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
            Uri ImageUri=data.getData();
            StorageReference photoRef=storageReference.child(ImageUri.getLastPathSegment());
            photoRef.putFile(ImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();
                    Toast.makeText(getApplicationContext(),"Successfully Uploaded",Toast.LENGTH_SHORT).show();
                    Photos photo=new Photos(downloadUrl.toString());
                    mDatabaseReference.push().setValue(photo);
                }
            });
        }
        else if(requestCode == PIC_CROP){
            Bundle extras = data.getExtras();
            Bitmap thePic = extras.getParcelable("data");
            Log.d("Here","Captured");
            Drawable drawable=new BitmapDrawable(getResources(),thePic);
            mpreview.setForeground(drawable);
            pictureFile=makeFile(thePic);
        }
    }

    private File makeFile(Bitmap bitmap) {
        File filesDir = getApplicationContext().getFilesDir();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imageFile = new File(filesDir, "temp" +timeStamp+ ".jpg");

        OutputStream os;
        try {
            os = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
        }
        return imageFile;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){

        }
        return c;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d("TAG", "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                MediaScannerConnection.scanFile(MainActivity.this, new String[] { pictureFile.getPath() }, new String[] { "image/jpeg" }, null);
            } catch (FileNotFoundException e) {
                Log.d("TAG", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("TAG", "Error accessing file: " + e.getMessage());
            }
        }
    };


    private static File getOutputMediaFile(int type){

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }


}
