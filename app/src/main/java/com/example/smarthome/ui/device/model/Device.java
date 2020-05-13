package com.example.smarthome.ui.device.model;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.smarthome.common.CommonActivity;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.O)
public class Device implements Serializable {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("user")
    @Expose
    private String user;
    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("NCL")
    @PropertyName("NCL")
    @Expose
    private String nCL;
    @SerializedName("NCLT")
    @PropertyName("NCLT")
    @Expose
    private String nCLT;
    @SerializedName("ND")
    @PropertyName("ND")
    @Expose
    private String nD;
    @SerializedName("NDU")
    @PropertyName("NDU")
    @Expose
    private String nDU;
    @SerializedName("NG")
    @PropertyName("NG")
    @Expose
    private String nG;
    @SerializedName("NO")
    @PropertyName("NO")
    @Expose
    private String nO;
    @PropertyName("total")
    private String total;
    @PropertyName("high_temp")
    private String highTemp;
    @PropertyName("time")
    private String time;

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getHighTemp() {
        return highTemp;
    }

    public void setHighTemp(String highTemp) {
        this.highTemp = highTemp;
    }

    public String getTime() {
        Date date = Calendar.getInstance().getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat
                = new SimpleDateFormat("dd-MM-yyyy");
        time = dateFormat.format(date);
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNCL() {
        return nCL;
    }

    public void setNCL(String nCL) {
        this.nCL = nCL;
    }

    public String getNCLT() {
        return nCLT;
    }

    public void setNCLT(String nCLT) {
        this.nCLT = nCLT;
    }

    public String getND() {
        return nD;
    }

    public void setND(String nD) {
        this.nD = nD;
    }

    public String getNDU() {
        return nDU;
    }

    public void setNDU(String nDU) {
        this.nDU = nDU;
    }

    public String getNG() {
        return nG;
    }

    public void setNG(String nG) {
        this.nG = nG;
    }

    public String getNO() {
        return nO;
    }

    public void setNO(String nO) {
        this.nO = nO;
    }

    public Device() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAverageND() {
        int sum = 0;
        String[] arrayND = nD.split("&");
        for (String c : arrayND) {
            sum += Integer.parseInt(c);
        }
        return String.valueOf(sum / arrayND.length);
    }

    @NonNull
    @Override
    public String toString() {
        return id;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        if (!CommonActivity.isNullOrEmpty(nCL)) {
            result.put("NCL", nCL);
        }
        if (!CommonActivity.isNullOrEmpty(nCLT)) {
            result.put("NCLT", nCLT);
        }
        if (!CommonActivity.isNullOrEmpty(nD)) {
            result.put("ND", nD);
        }
        if (!CommonActivity.isNullOrEmpty(nDU)) {
            result.put("NDU", nDU);
        }
        if (!CommonActivity.isNullOrEmpty(nG)) {
            result.put("NG", nG);
        }
        if (!CommonActivity.isNullOrEmpty(nO)) {
            result.put("NO", nO);
        }
        if (!CommonActivity.isNullOrEmpty(id)) {
            result.put("id", id);
        }
        if (!CommonActivity.isNullOrEmpty(user)) {
            result.put("user", user);
        }
        if (!CommonActivity.isNullOrEmpty(highTemp)) {
            result.put("highTemp", highTemp);
        }
        if (!CommonActivity.isNullOrEmpty(total)) {
            result.put("total", total);
        }
        return result;
    }
}