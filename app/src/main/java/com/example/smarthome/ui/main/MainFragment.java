package com.example.smarthome.ui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
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
import com.example.smarthome.ui.login.LoginViewModel;
import com.example.smarthome.utils.FireBaseCallBack;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.zxing.integration.android.IntentIntegrator;

import java.util.ArrayList;
import java.util.Objects;

public class MainFragment extends Fragment {

    private MainViewModel mainViewModel;
    private LoginViewModel loginViewModel;
    private DetailDeviceViewModel detailDeviceViewModel;
    private BaseBindingAdapter<Device> adapter;
    private MainFragmentBinding mainFragmentBinding;
    private String idDevice;
    private EditText txtInputDevice;

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
        onFabClicked();
        return mainFragmentBinding.getRoot();
    }

    private void onFabClicked() {
        mainFragmentBinding.fab.setOnClickListener(v -> {
            displayAlertDialog();
        });
    }

    private void getBundleData() {
        Bundle bundle = getArguments();
        if (!CommonActivity.isNullOrEmpty(bundle)) {
            idDevice = bundle.getString("idDevice");
        }
    }

    @SuppressLint("NewApi")
    private void unit() {
        loginViewModel = ViewModelProviders
                .of(Objects.requireNonNull(getActivity()))
                .get(LoginViewModel.class);
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
                    adapter.setData(devicesOfUser);
                }
            });
            Intent tempMonitoringService = new Intent(getActivity(), TempMonitoringService.class);
            tempMonitoringService.putExtra("idDevice", idDevice);
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
        GenericTypeIndicator<ArrayList<Device>> t = new GenericTypeIndicator<ArrayList<Device>>() {
        };
        ArrayList<Device> devices = dataSnapshot.getValue(t);
        callBack.afterDataChanged(devices);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void displayAlertDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.enter_firebase_url_dialog, null);
        txtInputDevice = alertLayout.findViewById(R.id.txtInputDevice);
        ImageView scanBarcode = alertLayout.findViewById(R.id.scanBarcode);

        AlertDialog.Builder alert = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        alert.setTitle(R.string.app_name);
        alert.setView(alertLayout);
        alert.setCancelable(false);

        scanBarcode.setOnClickListener(v -> {
            Intent scanIntent = new IntentIntegrator(getActivity())
                    .setBeepEnabled(false)
                    .createScanIntent();
            startActivityForResult(scanIntent, 1);
        });

        alert.setNegativeButton("Hủy", (dialog, which)
                -> Toast.makeText(getActivity(), "Cancel clicked", Toast.LENGTH_SHORT).show());

        alert.setPositiveButton("Đồng ý", (dialog, which) -> {
            if (txtInputDevice.getText() != null) {
                idDevice += txtInputDevice.getText().toString();
                loginViewModel.insertDevice(idDevice);
            } else {
                Toast.makeText(getActivity(), "Vui lòng nhập tên thiết bị", Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }

}
