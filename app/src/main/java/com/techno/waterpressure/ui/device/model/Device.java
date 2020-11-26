package com.techno.waterpressure.ui.device.model;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.firebase.database.PropertyName;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@RequiresApi(api = Build.VERSION_CODES.O)
@Entity
public class Device implements Serializable {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    int autoGeneID;

    @SerializedName("id")
    @Expose
    String id;
    @SerializedName("user")
    @Expose
    String user;

    @SerializedName("name1")
    @Expose
    String name1;

    @SerializedName("name2")
    @Expose
    String name2;

    @SerializedName("NG1")
    @PropertyName("NG1")
    @ColumnInfo(name = "ng1")
    @Expose
    String NG1;

    @SerializedName("NG2")
    @PropertyName("NG2")
    @ColumnInfo(name = "ng2")
    @Expose
    String NG2;

    @SerializedName("NO1")
    @PropertyName("NO1")
    @ColumnInfo(name = "no1")
    @Expose
    String NO1;

    @SerializedName("NO2")
    @PropertyName("NO2")
    @ColumnInfo(name = "no2")
    @Expose
    String NO2;

    @PropertyName("time")
    @ColumnInfo(name = "time")
    String time;
    int temp;
    int temp2;
    String position;
    boolean active;

    public void setTemp2(int temp2) {
        this.temp2 = temp2;
    }

    public int getTemp() {
        try {
            return (int) Float.parseFloat(NO1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getTemp2() {
        try {
            return (int) Float.parseFloat(NO2);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getAutoGeneID() {
        return autoGeneID;
    }

    public void setAutoGeneID(int autoGeneID) {
        this.autoGeneID = autoGeneID;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void loadPicture(String picture) {
    }

    public Device() {
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

    public void setTemp(int temp) {
        this.temp = temp;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public String getNG1() {
        return NG1;
    }

    public void setNG1(String NG1) {
        this.NG1 = NG1;
    }

    public String getNG2() {
        return NG2;
    }

    public void setNG2(String NG2) {
        this.NG2 = NG2;
    }

    public String getNO1() {
        return NO1;
    }

    public void setNO1(String NO1) {
        this.NO1 = NO1;
    }

    public String getNO2() {
        return NO2;
    }

    public void setNO2(String NO2) {
        this.NO2 = NO2;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
