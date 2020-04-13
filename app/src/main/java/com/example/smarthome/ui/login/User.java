package com.example.smarthome.ui.login;

import androidx.annotation.NonNull;

import com.example.smarthome.common.CommonActivity;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("password")
    @Expose
    private String password;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("mobile")
    @Expose
    private String mobile;
    @SerializedName("idDevice")
    @PropertyName("idDevice")
    @Expose
    private String idDevice;

    public User(String email, String password, String name, String address, String mobile, String idDevice) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.address = address;
        this.mobile = mobile;
        this.idDevice = idDevice;
    }

    public User(String email, String password, String idDevice) {
        this.email = email;
        this.password = password;
        this.idDevice = idDevice;
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIdDevice() {
        return idDevice;
    }

    public void setIdDevice(String idDevice) {
        this.idDevice = idDevice;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        if (!CommonActivity.isNullOrEmpty(idDevice)) {
            result.put("idDevice", idDevice);
        }
        if (!CommonActivity.isNullOrEmpty(name)) {
            result.put("name", name);
        }
        if (!CommonActivity.isNullOrEmpty(email)) {
            result.put("email", email);
        }
        if (!CommonActivity.isNullOrEmpty(address)) {
            result.put("address", address);
        }
        if (!CommonActivity.isNullOrEmpty(mobile)) {
            result.put("mobile", mobile);
        }
        if (!CommonActivity.isNullOrEmpty(password)) {
            result.put("password", password);
        }
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return email + password;
    }
}
