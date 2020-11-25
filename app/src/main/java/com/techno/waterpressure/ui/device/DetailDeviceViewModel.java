package com.techno.waterpressure.ui.device;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.techno.waterpressure.ui.device.model.Device;
import com.techno.waterpressure.utils.FireBaseCallBack;

import java.util.ArrayList;
import java.util.Objects;

import io.reactivex.Observable;

public class DetailDeviceViewModel extends ViewModel {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference deviceRef = database.getReference("devices");
    ValueEventListener valueEventListener;

//    MutableLiveData<DataSnapshot> getAllDevice() {
//        MutableLiveData<DataSnapshot> dataSnapshotMutableLiveData = new MutableLiveData<>();
//        getDevice(value -> dataSnapshotMutableLiveData.setValue(value));
//        deviceRef.addValueEventListener(valueEventListener);
//        return dataSnapshotMutableLiveData;
//    }

    Observable<ArrayList<Device>> getAllDevice() {
        return Observable.create(emitter -> {
            deviceRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ArrayList<Device> devices;
                    GenericTypeIndicator<ArrayList<Device>> t =
                            new GenericTypeIndicator<ArrayList<Device>>() {
                            };
                    devices = dataSnapshot.getValue(t);
                    emitter.onNext(devices);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        });
    }

    private void getDevice(FireBaseCallBack<DataSnapshot> fireBaseCallBack) {
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

    public MutableLiveData<DataSnapshot> onTemperatureChange(int indexOfDevice) {
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

    public Observable<Device> monitoringDevice() {
        return Observable.create(emitter -> {
            deviceRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    emitter.onNext(Objects.requireNonNull(dataSnapshot.getValue(Device.class)));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    emitter.onError(new Throwable(databaseError.toString()));
                }
            });
        });
    }

    public Observable<Device> observerDevice(int index) {
        return Observable.create(emitter -> {
            deviceRef.child(String.valueOf(index)).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    emitter.onNext(dataSnapshot.getValue(Device.class));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        });
    }

    io.reactivex.Observable<String> setNG(String ng) {
        return Observable.create(emitter -> {
            deviceRef.child("NG").setValue(ng)
                    .addOnSuccessListener(aVoid -> emitter.onNext("Success"));
        });
    }

    io.reactivex.Observable<String> reset(int indexOfDevice, String rST) {
        return Observable.create(emitter -> {
            deviceRef.child(String.valueOf(indexOfDevice)).child("RST").setValue(rST)
                    .addOnSuccessListener(aVoid -> emitter.onNext("Success"));
        });
    }

    io.reactivex.Observable<String> setOffset(String offSet) {
        return Observable.create(emitter -> {
            deviceRef.child("NDU").setValue(offSet)
                    .addOnSuccessListener(aVoid -> emitter.onNext("Success"));
        });
    }

    io.reactivex.Observable<String> setLoopingTime(String looping) {
        return Observable.create(emitter -> {
            deviceRef.child("NCL").setValue(looping)
                    .addOnSuccessListener(aVoid -> emitter.onNext("Success"));
        });
    }

    public void setName(String name) {
        deviceRef.child("name1").setValue(name);
    }

    void setTotal(int indexOfDevice, String total) {
        deviceRef.child(String.valueOf(indexOfDevice)).child("total").setValue(total);
    }

    void setNumberOfHighTemp(int indexOfDevice, String number) {
        deviceRef.child(String.valueOf(indexOfDevice)).child("highTemp").setValue(number);
    }

    void removeAllListeners() {
        deviceRef.removeEventListener(valueEventListener);
    }
}
