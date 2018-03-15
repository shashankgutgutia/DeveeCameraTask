package com.deevesoft.shashankgutgutia.deevesofttask;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class FirebaseImages extends AppCompatActivity {


    private PhotoAdapter mPhotoAdapter;
    List<Photos> PhotoList;
    private ListView mPhotoListView;
    private ChildEventListener mChildEventListener;
    private FirebaseDatabase mFirebaseDatabse;
    private DatabaseReference mDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_images);
        mPhotoListView=findViewById(R.id.photolist2);
        mFirebaseDatabse= FirebaseDatabase.getInstance();
        mDatabaseReference=mFirebaseDatabse.getReference().child("photos");
        PhotoList=new ArrayList<>();
        mPhotoAdapter = new PhotoAdapter(this, R.layout.photodisplay, PhotoList);
        mPhotoListView.setAdapter(mPhotoAdapter);
        attachDatabaseReadListener();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener(){
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Photos photo = dataSnapshot.getValue(Photos.class);
                    mPhotoAdapter.add(photo);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    public void detachDatabaseReadListener(){
        if(mChildEventListener!=null) {
            mDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }
}
