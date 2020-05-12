package com.example.smarthome.ui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.smarthome.R;
import com.example.smarthome.common.BaseBindingAdapter;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.common.ReplaceFragment;
import com.example.smarthome.databinding.MainFragmentBinding;
import com.example.smarthome.ui.device.DetailDeviceFragment;
import com.example.smarthome.ui.device.DetailDeviceViewModel;
import com.example.smarthome.ui.device.model.Device;
import com.example.smarthome.utils.FireBaseCallBack;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.ArrayList;
import java.util.Objects;

public class MainFragment extends Fragment {

    private MainViewModel mainViewModel;
    private DetailDeviceViewModel detailDeviceViewModel;
    private BaseBindingAdapter<Device> adapter;
    private MainFragmentBinding mainFragmentBinding;
    private String idDevice;

    public static MainFragment newInstance(String idDevice) {

        Bundle args = new Bundle();

        args.putString("idDevice", idDevice);
        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getBundleData();
        mainFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false);
        unit();
        return mainFragmentBinding.getRoot();
    }

    private void getBundleData() {
        Bundle bundle = getArguments();
        if (!CommonActivity.isNullOrEmpty(bundle)) {
            idDevice = bundle.getString("idDevice");
        }
    }

    @SuppressLint("NewApi")
    private void unit() {
        detailDeviceViewModel = ViewModelProviders
                .of(Objects.requireNonNull(getActivity()))
                .get(DetailDeviceViewModel.class);
        adapter = new BaseBindingAdapter<>(getActivity(), R.layout.item_device);
        mainViewModel =
                ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(MainViewModel.class);
        mainFragmentBinding.setLifecycleOwner(this);
        mainFragmentBinding.listDevice.setAdapter(adapter);
        mainFragmentBinding.listDevice
                .setLayoutManager(new GridLayoutManager(getActivity(), 3));

        // TODO: Use the ViewModel
        // Read from the database
        mainViewModel.getAllDevices().observe(getActivity(), dataSnapshot -> {
            ArrayList<Device> devicesOfUser = new ArrayList<>();
            getData(dataSnapshot, devices -> {
                if (devices != null) {
                    for (Device device : devices) {
                        if (idDevice.contains(device.getId())) {
                            devicesOfUser.add(device);
                        }
                    }
                }
            });
            adapter.setData(devicesOfUser);
            Intent tempMonitoringService = new Intent(getActivity(), TempMonitoringService.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("devicesOfUser", devicesOfUser);
            tempMonitoringService.putExtras(bundle);
            getActivity().startService(tempMonitoringService);
        });

        adapter.setOnItemClickListener(item -> {
            ReplaceFragment.replaceFragment(getActivity(),
                    DetailDeviceFragment.newInstance(item, idDevice),
                    true);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getData(DataSnapshot dataSnapshot, FireBaseCallBack<ArrayList<Device>> callBack) {
        GenericTypeIndicator<ArrayList<Device>> t = new GenericTypeIndicator<ArrayList<Device>>() {};
        ArrayList<Device> devices = dataSnapshot.getValue(t);
        callBack.afterDataChanged(devices);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

}
