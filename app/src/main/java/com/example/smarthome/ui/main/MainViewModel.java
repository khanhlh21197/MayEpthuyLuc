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

    public MutableLiveData<DataSnapshot> getDevicesOfUser(String idDevice) {
        MutableLiveData<DataSnapshot> liveDataSnapShot = new MutableLiveData<>();
        getFBDevicesOfUser(liveDataSnapShot::setValue, idDevice);
        return liveDataSnapShot;
    }


    public MutableLiveData<DataSnapshot> monitoringJustTemp(String idDevice) {
        MutableLiveData<DataSnapshot> temp = new MutableLiveData<>();
        getFBJustTemp(item -> {
            for (DataSnapshot dataSnapshot : item.getChildren()) {
                temp.setValue(dataSnapshot);
            }
        }, idDevice);
        return temp;
    }

    private void getFBJustTemp(FireBaseCallBack<DataSnapshot> callBack, String idDevice) {
        deviceRef.child(idDevice).child("no").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                callBack.afterDataChanged(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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

    private void getFBDevicesOfUser(FireBaseCallBack<DataSnapshot> fireBaseCallBack, String idDevice) {
        deviceRef.child(idDevice).addValueEventListener(new ValueEventListener() {
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
