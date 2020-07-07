package com.example.smarthome.ui.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthome.R;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.dao.AppDatabase;
import com.example.smarthome.databinding.DetailDeviceFragmentBinding;
import com.example.smarthome.serializer.ObjectSerializer;
import com.example.smarthome.ui.device.model.Device;
import com.example.smarthome.warning.WarningService;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class DetailDeviceFragment extends Fragment implements View.OnClickListener, OnChartValueSelectedListener {
    private static final String SHARED_PREFS_HISTORY = "SHARED_PREFS_HISTORY";
    private static final String KEY_HISTORY = "HISTORY";
    private Intent warningService;
    private AppDatabase mDb;
    private CombinedChart mChart;

    private DetailDeviceFragmentBinding mBinding;
    private DetailDeviceViewModel viewModel;
    private Device device;
    private boolean flashingText = false;
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
        initChart();
        return mBinding.getRoot();
    }

    private void initChart() {
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH/mm");
        mChart = mBinding.combineChart;
        mChart.getDescription().setEnabled(false);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        mChart.setHighlightFullBarEnabled(false);
        mChart.setOnChartValueSelectedListener(this);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(0f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);

        final List<String> xLabel = new ArrayList<>();
        xLabel.add("Jan");
        xLabel.add("Feb");
        xLabel.add("Mar");
        xLabel.add("Apr");
        xLabel.add("May");
        xLabel.add("Jun");
        xLabel.add("Jul");
        xLabel.add("Aug");
        xLabel.add("Sep");
        xLabel.add("Oct");
        xLabel.add("Nov");
        xLabel.add("Dec");

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return xLabel.get((int) value % xLabel.size());
            }
        });

        CombinedData data = new CombinedData();
        LineData lineDatas = new LineData();
        lineDatas.addDataSet((ILineDataSet) dataChart());

        data.setData(lineDatas);

        xAxis.setAxisMaximum(data.getXMax() + 0.25f);

        mChart.setData(data);
        mChart.invalidate();
    }

    private static DataSet dataChart() {

        LineData d = new LineData();
        int[] data = new int[]{1, 2, 2, 1, 1, 1, 2, 1, 1, 2, 1, 9};

        ArrayList<Entry> entries = new ArrayList<Entry>();

        for (int index = 0; index < 12; index++) {
            entries.add(new Entry(index, data[index]));
        }

        LineDataSet set = new LineDataSet(entries, "Request Ots approved");
        set.setColor(Color.GREEN);
        set.setLineWidth(2.5f);
        set.setCircleColor(Color.GREEN);
        set.setCircleRadius(5f);
        set.setFillColor(Color.GREEN);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.GREEN);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);

        return set;
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

    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void unit() {
        mDb = AppDatabase.getDatabase(getContext());
//        getDB();
        warningService = new Intent(getActivity(), WarningService.class);
        mBinding.btnWarning.setAnimation(createFlashingAnimation());

        viewModel = ViewModelProviders
                .of(Objects.requireNonNull(getActivity()))
                .get(DetailDeviceViewModel.class);

        viewModel.getAllDevice().subscribe(devices -> {
            if (!CommonActivity.isNullOrEmpty(devices)) {
                for (Device device : devices) {
                    insertDevice(device);
                    if (device.getId().equals(DetailDeviceFragment.this.device.getId())) {
                        indexOfDevice = devices.indexOf(device);
//                        liveData.setValue(device);
                        mBinding.setDetailDevice(device);
                        @SuppressLint("SimpleDateFormat") String timeStamp
                                = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss").format(Calendar.getInstance().getTime());
                        device.setTime(timeStamp);
                        try {
                            compareTemp();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        });
    }

    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void compareTemp() {
        viewModel.observerDevice(indexOfDevice).subscribe(device1 -> {
            if (CommonActivity.isNullOrEmpty(device1.getNO()) || CommonActivity.isNullOrEmpty(device1.getNG()))
                return;
            try {
                if (Double.parseDouble(device1.getNO()) > Double.parseDouble(device1.getNG())) {
                    history.add(0, device1);
//            saveHistory(history);
                    historyAdapter.notifyItemInserted(0);
                    mBinding.listHistory.smoothScrollToPosition(0);
                    startWarning(device1.getNG());
                    mBinding.btnWarning.setOnClickListener(v -> {
                        cancelWarning();
                    });
                    Log.d("history", String.valueOf(history.size()));
                    Log.d("highTemp", String.valueOf(highTemp));
                } else {
                    history.add(device1);
//            saveHistory(history);
                    historyAdapter.notifyItemInserted(history.size());
                    cancelWarning();
                    mBinding.txtHumanTemp.clearAnimation();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        });
    }

    private void startWarning(String ng) {
        playWarningSound();
        if (!flashingText) {
            mBinding.txtHumanTemp.setAnimation(createFlashingAnimation());
            flashingText = true;
        }
        mBinding.btnWarning.setVisibility(View.VISIBLE);
//        showNoti();
    }

    private void playWarningSound() {
        WarningService.isRunning.observe(this, aBoolean -> {
            if (!aBoolean) {
                if (CommonActivity.isNullOrEmpty(getActivity())) return;
                Objects.requireNonNull(getActivity()).startService(warningService);
            }
        });
    }

    private void cancelWarning() {
        mBinding.btnWarning.setVisibility(View.GONE);
        mBinding.txtHumanTemp.clearAnimation();
        flashingText = false;
        if (getActivity() != null) {
            Objects.requireNonNull(getActivity()).stopService(warningService);
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
        super.onResume();
    }

    @Override
    public void onPause() {
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

    @SuppressLint("CheckResult")
    private void getDB() {
//        ArrayList<Device> devices = mDb.deviceDAO().getAllDevice();
//        Observable.just(devices).subscribe(device -> {
//
//        });
//        Log.d(TAG, "getDB: " + devices.size());
    }

    private void insertDevice(Device device) {
//        mDb.deviceDAO().insertDevice(device);
    }

    @SuppressLint("CheckResult")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDeleteHistory:
                prefs = Objects.requireNonNull(getActivity()).getSharedPreferences(SHARED_PREFS_HISTORY, Context.MODE_PRIVATE);
                editor = prefs.edit();
                editor.clear();
                editor.apply();
                history.clear();
                historyAdapter.notifyDataSetChanged();
                break;
            case R.id.btnThreshold:
                String ng = mBinding.edtThreshold.getText().toString().trim();
                if (!CommonActivity.isNullOrEmpty(ng)) {
                    viewModel.setNG(indexOfDevice, ng).subscribe(s -> {
                        if (s.equals("Success")) {
                            mBinding.edtThreshold.setText("");
                            Toast.makeText(getActivity(), "Cài đặt ngưỡng thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    CommonActivity.showConfirmValidate(getActivity(), "Vui lòng nhập giá trị ngưỡng");
                }
                break;
        }
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Toast.makeText(getActivity(), "Value: "
                + e.getY()
                + ", index: "
                + h.getX()
                + ", DataSet index: "
                + h.getDataSetIndex(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected() {

    }
}
