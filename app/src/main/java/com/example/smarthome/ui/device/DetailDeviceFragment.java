package com.example.smarthome.ui.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthome.R;
import com.example.smarthome.common.BaseBindingAdapter;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.databinding.DetailDeviceFragmentBinding;
import com.example.smarthome.ui.device.model.Device;
import com.example.smarthome.warning.WarningService;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.ArrayList;
import java.util.Objects;

public class DetailDeviceFragment extends Fragment {
    private static final String CHANNEL_ID = "NotificationChannel";
    private DetailDeviceFragmentBinding mBinding;
    private DetailDeviceViewModel viewModel;
    private Device device;
    private MutableLiveData<Device> liveData = new MutableLiveData<>();
    private BaseBindingAdapter<Device> mAdapter;
    private HistoryAdapter historyAdapter;
    private Intent warningIntent;
    private Vibrator v;
    private String deviceId = "";
    private int indexOfDevice = 0;
    private int total = 0;
    private String idDevice;
    private int highTemp = 0;
    private ArrayList<Device> history = new ArrayList<>();
    private MutableLiveData<String> followTemp = new MutableLiveData<>();

    public static DetailDeviceFragment newInstance(Device device, String idDevice) {

        Bundle args = new Bundle();
        args.putSerializable("Device", device);
        args.putString("idDevice", idDevice);
        DetailDeviceFragment fragment = new DetailDeviceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getBundleData();
        mBinding =
                DataBindingUtil.inflate(inflater,
                        R.layout.detail_device_fragment,
                        container,
                        false);
        unit();
        initAdapter();
        return mBinding.getRoot();
    }

    private void initAdapter() {
        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(getActivity());
        mBinding.listHistory.setLayoutManager(linearLayoutManager);
        historyAdapter = new HistoryAdapter(getActivity(), history);
        mBinding.listHistory.setAdapter(historyAdapter);
    }

    private void getBundleData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            device = (Device) bundle.getSerializable("Device");
            idDevice = bundle.getString("idDevice");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void unit() {
        viewModel = ViewModelProviders
                .of(Objects.requireNonNull(getActivity()))
                .get(DetailDeviceViewModel.class);

        viewModel.getAllDevice().observe(getActivity(), dataSnapshot -> {
            ArrayList<Device> devices;
            GenericTypeIndicator<ArrayList<Device>> t =
                    new GenericTypeIndicator<ArrayList<Device>>() {
                    };
            devices = dataSnapshot.getValue(t);
            if (!CommonActivity.isNullOrEmpty(devices)) {
                for (Device device : devices) {
                    if (device.getId().equals(DetailDeviceFragment.this.device.getId())) {
                        indexOfDevice = devices.indexOf(device);
                        liveData.setValue(device);
                        break;
                    }
                }
            }
        });

        observeTemperature();

        mBinding.btnThreshold.setOnClickListener(v -> {
            String ng = mBinding.edtThreshold.getText().toString().trim();
            if (!CommonActivity.isNullOrEmpty(ng)) {
                viewModel.setNG(indexOfDevice, ng);
//                observeTemperature();
            } else {
                CommonActivity.showConfirmValidate(getActivity(), "Vui lòng nhập giá trị ngưỡng");
            }
        });

//        viewModel.onTemperatureChange(indexOfDevice).observe(Objects.requireNonNull(getActivity()), dataSnapshot -> {
//            String temp = dataSnapshot.getValue(String.class);
//            if (temp != null) {
//                Log.d("Temp", temp);
//            }
//            total++;
//            viewModel.setTotal(indexOfDevice, String.valueOf(total));
//        });
    }

    @SuppressLint("NewApi")
    private void observeTemperature() {
        liveData.observe(Objects.requireNonNull(getActivity()), device -> {
            mBinding.setDetailDevice(device);
            deviceId = device.getId();
            try {
                if (Double.parseDouble(device.getNO()) > Double.parseDouble(device.getNG())) {
                    history.add(device);
                    historyAdapter.setData(history);
//                    highTemp++;
//                    viewModel.setTotal(indexOfDevice, String.valueOf(highTemp));
                    startWarning(device.getNG());
                    mBinding.btnWarning.setOnClickListener(v -> {
                        cancelWarning();
                    });
                    Log.d("history", String.valueOf(history.size()));
                    Log.d("highTemp", String.valueOf(highTemp));
                } else {
                    cancelWarning();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startWarning(String ng) {
        playWarningSound();
        vibrate();
        mBinding.txtHumanTemp.setAnimation(createFlashingAnimation());
        mBinding.btnWarning.setVisibility(View.VISIBLE);
        mBinding.btnWarning.setAnimation(createFlashingAnimation());
//        showNoti();
    }

    private void cancelWarning() {
        Objects.requireNonNull(getActivity()).stopService(warningIntent);
        this.v.cancel();
        mBinding.txtHumanTemp.clearAnimation();
        mBinding.btnWarning.clearAnimation();
        mBinding.btnWarning.setVisibility(View.GONE);
        Toast.makeText(getActivity(), "Warning Cancelled", Toast.LENGTH_LONG).show();
    }

    private void playWarningSound() {
        warningIntent = new Intent(getActivity(), WarningService.class);
        Objects.requireNonNull(getActivity()).startService(warningIntent);
    }

    private void vibrate() {
        if (!CommonActivity.isNullOrEmpty(getActivity())) {
            v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            if (!CommonActivity.isNullOrEmpty(v)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(3000);
                }
            }
        }
    }

    public static Animation createFlashingAnimation() {
        final Animation flashingAnimation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        flashingAnimation.setDuration(500); // duration - half a second
        flashingAnimation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        flashingAnimation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        flashingAnimation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
        return flashingAnimation;
    }

    @Override
    public void onResume() {
//        if (!CommonActivity.isNullOrEmpty(getActivity()))
//        getActivity().registerReceiver(receiver, new IntentFilter(".warning.WarningBroadcastReceiver"));
        super.onResume();
    }

    @Override
    public void onPause() {
//        if (!CommonActivity.isNullOrEmpty(getActivity()))
//        getActivity().unregisterReceiver(receiver);
        super.onPause();
    }
}
