package com.example.smarthome.ui.login;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.utils.FireBaseCallBack;
import com.example.smarthome.utils.Result;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class LoginViewModel extends ViewModel {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");

    public MutableLiveData<String> Email = new MutableLiveData<>();
    public MutableLiveData<String> Password = new MutableLiveData<>();
    private User user = null;
    private String userID = "";
    private ArrayList<User> users = new ArrayList<>();
    private Result<User> result;

    void setResult(Result<User> result) {
        this.result = result;
    }

    ArrayList<User> getAllUsersLiveData() {
        getData((DataSnapshot item) -> {
            users.clear();
            for (DataSnapshot d : item.getChildren()) {
                User user = d.getValue(User.class);
                if (user != null) {
                    Log.d("User: ", user.toString());
                }
                if (user != null) {
                    user.setUid(d.getKey());
                }
                users.add(user);
            }
        });
        return users;
    }

    private void getData(FireBaseCallBack<DataSnapshot> callBack) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                callBack.afterDataChanged(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("getData", "onCancelled");
            }
        });
    }

    public void onClick(View view) {
        User inputUser = new User(Email.getValue(), Password.getValue());
        if (CommonActivity.isNullOrEmpty(Email.getValue()) || CommonActivity.isNullOrEmpty(Password.getValue())) {
            result.onFailure("Vui lòng điền đủ thông tin đăng nhập!");
            return;
        }
        if (isLoginSuccess(inputUser)) {
            result.onSuccess(inputUser, "Đăng nhập thành công với " + inputUser.getEmail());
        } else {
            result.onFailure("Sai tên email hoặc mật khẩu!");
        }
    }

    private boolean isLoginSuccess(User inputUser) {
        if (!CommonActivity.isNullOrEmpty(users)) {
            for (User user : users) {
                if (user.getEmail().equals(inputUser.getEmail())
                        && user.getPassword().equals(inputUser.getPassword())) {
                    userID = user.getUid();
                    Log.d("isLoginSuccess", userID);
                    this.user = user;
                    return true;
                }
            }
        } else {
            return false;
        }
        return false;
    }

    public User getUser() {
        return user;
    }

    public void insertDevice(String idDevice, OnCompleteListener<Void> onCompleteListener) {
        if (userID != null) {
            userRef.child(userID).child("idDevice")
                    .setValue(idDevice)
                    .addOnCompleteListener(onCompleteListener);
            Log.d(userID, "insert success");
        } else {
            Log.d("userID", "insert fail");
        }
    }
}
