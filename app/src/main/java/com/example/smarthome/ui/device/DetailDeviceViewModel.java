package com.example.smarthome.ui.device;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smarthome.utils.FireBaseCallBack;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DetailDeviceViewModel extends ViewModel {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference deviceRef = database.getReference("devices");
    public MutableLiveData<String> total = new MutableLiveData<>();
    public MutableLiveData<String> highTemp = new MutableLiveData<>();
    private String temperature = "";
    ValueEventListener valueEventListener;

    MutableLiveData<DataSnapshot> getAllDevice() {
        MutableLiveData<DataSnapshot> dataSnapshotMutableLiveData = new MutableLiveData<>();
        getDevice(dataSnapshotMutableLiveData::setValue);
        deviceRef.addValueEventListener(valueEventListener);
        return dataSnapshotMutableLiveData;
    }

    private void getDevice(FireBaseCallBack<DataSnapshot> fireBaseCallBack){
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                fireBaseCallBack.afterDataChanged(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("onCancelled", databaseError.toString());
            }
        };
    }

    public MutableLiveData<DataSnapshot> onTemperatureChange(int indexOfDevice){
        MutableLiveData<DataSnapshot> temp = new MutableLiveData<>();
        deviceRef.child(String.valueOf(indexOfDevice)).child("NO").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                temp.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return temp;
    }

    void setNG(int indexOfDevice, String ng){
        deviceRef.child(String.valueOf(indexOfDevice)).child("NG").setValue(ng);
    }

    public void setName(int indexOfDevice, String name){
        deviceRef.child(String.valueOf(indexOfDevice)).child("name").setValue(name);
    }

    void setTotal(int indexOfDevice, String total){
        deviceRef.child(String.valueOf(indexOfDevice)).child("total").setValue(total);
    }
    void setNumberOfHighTemp(int indexOfDevice, String number){
        deviceRef.child(String.valueOf(indexOfDevice)).child("high_temp").setValue(number);
    }

    void removeAllListeners(){
        deviceRef.removeEventListener(valueEventListener);
    }
}
