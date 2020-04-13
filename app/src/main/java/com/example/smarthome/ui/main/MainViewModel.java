package com.example.smarthome.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smarthome.ui.device.model.Device;
import com.example.smarthome.utils.FireBaseCallBack;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    MutableLiveData<Device> deviceMutableLiveData = new MutableLiveData<>();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference deviceRef = database.getReference("devices");

    public MutableLiveData<DataSnapshot> getAllDevices() {
        MutableLiveData<DataSnapshot> liveDataSnapShot = new MutableLiveData<>();
        getFBData(liveDataSnapShot::setValue);
        return liveDataSnapShot;
    }


    private void getFBData(FireBaseCallBack<DataSnapshot> fireBaseCallBack) {
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                fireBaseCallBack.afterDataChanged(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
