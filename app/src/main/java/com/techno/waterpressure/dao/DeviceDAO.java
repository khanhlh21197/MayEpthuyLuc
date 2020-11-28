package com.techno.waterpressure.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.techno.waterpressure.ui.device.model.Device;

import java.util.List;

@Dao
public interface DeviceDAO {
    @Query("SELECT * FROM device")
    List<Device> getAllDevice();

    @Query("SELECT * FROM device WHERE id = :id")
    List<Device> getAllDevice(String id);

    @Insert
    void insertDevice(Device device);

    @Query("DELETE FROM device")
    int deleteHistory();

    @Delete
    void deleteDevice(Device device);
}
