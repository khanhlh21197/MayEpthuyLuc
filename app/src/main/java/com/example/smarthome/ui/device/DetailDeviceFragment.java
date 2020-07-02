package com.example.smarthome.ui.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthome.R;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.databinding.DetailDeviceFragmentBinding;
import com.example.smarthome.serializer.ObjectSerializer;
import com.example.smarthome.ui.device.model.Device;
import com.example.smarthome.warning.WarningService;
import com.google.firebase.database.GenericTypeIndicator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class DetailDeviceFragment extends Fragment {
    private static final String SHARED_PREFS_HISTORY = "SHARED_PREFS_HISTORY";
    private static final String KEY_HISTORY = "HISTORY";
    private Vibrator v;
    private Intent warningService;

    private DetailDeviceFragmentBinding mBinding;
    private DetailDeviceViewModel viewModel;
    private Device device;
    private MutableLiveData<Device> liveData = new MutableLiveData<>();
    private HistoryAdapter historyAdapter;
    private int indexOfDevice = 0;
    private int highTemp = 0;
    private ArrayList<Device> history = new ArrayList<>();
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public static DetailDeviceFragment newInstance(Device device,
                                                   String idDevice) {

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
        getHistory();
        unit();
        initAdapter();
        editDeviceName();
        return mBinding.getRoot();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void editDeviceName() {
        mBinding.imgEdit.setOnClickListener(v -> {
            displayAlertDialog();
        });
    }

    private void getHistory() {
        if (CommonActivity.isNullOrEmpty(history)) {
            history = new ArrayList<>();
        }
        prefs = Objects.requireNonNull(getActivity()).getSharedPreferences(SHARED_PREFS_HISTORY, Context.MODE_PRIVATE);
        try {
            history = (ArrayList<Device>) ObjectSerializer.deserialize(prefs.getString(KEY_HISTORY, ObjectSerializer.serialize(new ArrayList<Device>())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initAdapter() {
        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(getActivity());
        mBinding.listHistory.setLayoutManager(linearLayoutManager);
        historyAdapter = new HistoryAdapter(getActivity(), history);
        mBinding.listHistory.setAdapter(historyAdapter);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getBundleData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            device = (Device) bundle.getSerializable("Device");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void unit() {
        warningService = new Intent(getActivity(), WarningService.class);

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
                        mBinding.setDetailDevice(device);
                        @SuppressLint("SimpleDateFormat") String timeStamp
                                = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss").format(Calendar.getInstance().getTime());
                        device.setTime(timeStamp);
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
                mBinding.edtThreshold.setText("");
                Toast.makeText(getActivity(), "Cài đặt ngưỡng thành công!", Toast.LENGTH_SHORT).show();
            } else {
                CommonActivity.showConfirmValidate(getActivity(), "Vui lòng nhập giá trị ngưỡng");
            }
        });

        mBinding.btnDeleteHistory.setOnClickListener(v -> {
            prefs = Objects.requireNonNull(getActivity()).getSharedPreferences(SHARED_PREFS_HISTORY, Context.MODE_PRIVATE);
            editor = prefs.edit();
            editor.clear();
            editor.apply();
            history.clear();
            historyAdapter.notifyDataSetChanged();
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

    @SuppressLint({"NewApi", "SetTextI18n"})
    private void observeTemperature() {
        liveData.observe(Objects.requireNonNull(getActivity()), device -> {
            mBinding.setDetailDevice(device);
            @SuppressLint("SimpleDateFormat") String timeStamp
                    = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss").format(Calendar.getInstance().getTime());
            device.setTime(timeStamp);
            try {
                compareTemp();
//                if (!CommonActivity.isNullOrEmpty(history)) {
//                    if (!device.getNO().equals(history.get(history.size() - 1).getNO())
//                            && !device.getNO().equals(history.get(0).getNO())) {
//                        compareTemp();
//                    }
//                } else {
//                    compareTemp();
//                }
//                mBinding.tvTotal.setText("Tổng số người đo : " + history.size());
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                e.printStackTrace();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void compareTemp() {
        if (CommonActivity.isNullOrEmpty(device.getNO()) || CommonActivity.isNullOrEmpty(device.getNG()))
            return;
        if (Double.parseDouble(device.getNO()) > Double.parseDouble(device.getNG())) {
            history.add(0, device);
//            saveHistory(history);
            historyAdapter.notifyItemInserted(0);
            mBinding.listHistory.smoothScrollToPosition(0);
            startWarning(device.getNG());
            mBinding.btnWarning.setOnClickListener(v -> {
                cancelWarning();
            });
            Log.d("history", String.valueOf(history.size()));
            Log.d("highTemp", String.valueOf(highTemp));
        } else {
            history.add(device);
//            saveHistory(history);
            historyAdapter.notifyItemInserted(history.size());
            cancelWarning();
        }
    }

    private void startWarning(String ng) {
        playWarningSound();
        vibrate();
        mBinding.txtHumanTemp.setAnimation(createFlashingAnimation());
        mBinding.btnWarning.setVisibility(View.VISIBLE);
        mBinding.btnWarning.setAnimation(createFlashingAnimation());
//        showNoti();
    }

    private void playWarningSound() {
        if (!WarningService.isRunning) {
            if (CommonActivity.isNullOrEmpty(getActivity())) return;
            Objects.requireNonNull(getActivity()).startService(warningService);
        }
    }

    private void cancelWarning() {
        if (v != null) {
            v.cancel();
        }
        mBinding.txtHumanTemp.clearAnimation();
        mBinding.btnWarning.clearAnimation();
        mBinding.btnWarning.setVisibility(View.GONE);
        Objects.requireNonNull(getActivity()).stopService(warningService);
    }

    private void vibrate() {
        if (v != null && v.hasVibrator()) {
            return;
        }
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

    private void saveHistory(ArrayList<Device> history) {
        // save the task list to preference
        prefs = Objects.requireNonNull(getActivity()).getSharedPreferences(SHARED_PREFS_HISTORY, Context.MODE_PRIVATE);
        editor = prefs.edit();
        try {
            editor.clear();
            editor.putString(KEY_HISTORY, ObjectSerializer.serialize(history));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void displayAlertDialog() {
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") View alertLayout = inflater.inflate(R.layout.dialog_edit_device_name, null);
        final EditText edtDeviceName = alertLayout.findViewById(R.id.edtDeviceName);
        final TextView txtWarning = alertLayout.findViewById(R.id.txtWarning);
        if (!CommonActivity.isNullOrEmpty(device.getName())) {
            edtDeviceName.setText(device.getName());
        } else {
            edtDeviceName.setText(device.getId());
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        alert.setTitle("Đổi tên thiết bị");
        alert.setView(alertLayout);
        alert.setCancelable(false);
        alert.setNegativeButton("Hủy", (dialog, which) -> Toast.makeText(getActivity(), "Hủy", Toast.LENGTH_SHORT).show());

        alert.setPositiveButton("Đồng ý", (dialog, which) -> {
            if (!CommonActivity.isNullOrEmpty(edtDeviceName.getText().toString())) {
                device.setName(edtDeviceName.getText().toString());
                viewModel.setName(indexOfDevice, device.getName());
                txtWarning.setVisibility(View.GONE);
            } else {
                txtWarning.setVisibility(View.VISIBLE);
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }
}
