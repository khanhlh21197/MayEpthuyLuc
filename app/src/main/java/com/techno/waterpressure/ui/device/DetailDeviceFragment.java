package com.techno.waterpressure.ui.device;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.techno.waterpressure.R;
import com.techno.waterpressure.common.CommonActivity;
import com.techno.waterpressure.dao.AppDatabase;
import com.techno.waterpressure.databinding.DetailDeviceFragmentBinding;
import com.techno.waterpressure.serializer.ObjectSerializer;
import com.techno.waterpressure.ui.device.model.Device;
import com.techno.waterpressure.ui.main.TempMonitoringService;
import com.techno.waterpressure.warning.WarningService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import io.reactivex.Observable;

public class DetailDeviceFragment extends Fragment implements View.OnClickListener, OnChartValueSelectedListener {
    private static final String SHARED_PREFS_HISTORY = "SHARED_PREFS_HISTORY";
    private static final String KEY_HISTORY = "HISTORY";
    private Intent warningService;
    private AppDatabase mDb;
    private CombinedChart mChart;
    private XAxis xAxis;
    private List<String> timeLabel = new ArrayList<>();
    private ArrayList<Float> temperature = new ArrayList<>();
    private ArrayList<Float> temperature2 = new ArrayList<>();

    private DetailDeviceFragmentBinding mBinding;
    private DetailDeviceViewModel viewModel;
    private boolean flashingText = false;
    private HistoryAdapter historyAdapter;
    private int highTemp = 0;
    private long timeL;
    private ArrayList<Device> history = new ArrayList<>();
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    LinearLayoutManager linearLayoutManager;
    private Dialog progressDialog;
    private boolean update;
    private Device device;
    private Intent tempMonitoringService;

    public static DetailDeviceFragment newInstance() {
        return new DetailDeviceFragment();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding =
                DataBindingUtil.inflate(inflater,
                        R.layout.detail_device_fragment,
                        container,
                        false);
        initChart();
        unit();
        initAdapter();
        editDeviceName();
        return mBinding.getRoot();
    }

    private void initChart() {
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

        timeLabel = new ArrayList<>();

        xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                if (timeLabel == null) return "";
                return timeLabel.get((int) value % timeLabel.size());
            }
        });
    }

    private static DataSet dataChart(ArrayList<Float> temperature, String label, int color) {
        if (CommonActivity.isNullOrEmpty(temperature)) return null;

        LineData d = new LineData();

        ArrayList<Entry> entries = new ArrayList<Entry>();

        for (int index = 0; index < temperature.size(); index++) {
            entries.add(new Entry(index, temperature.get(index)));
        }

        LineDataSet set = new LineDataSet(entries, label);

        set.setColor(color);
        set.setLineWidth(2.5f);
        set.setCircleColor(color);
        set.setCircleRadius(2f);
        set.setFillColor(color);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(color);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);

        return set;
    }

    private void clearChart() {
        timeLabel.clear();
        temperature.clear();
        temperature2.clear();
        mChart.invalidate();
        mChart.clear();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void editDeviceName() {
        mBinding.imgEdit.setOnClickListener(v -> {
            displayAlertDialog();
        });
    }

    private void initAdapter() {
        linearLayoutManager
                = new LinearLayoutManager(getActivity());
        mBinding.listHistory.setLayoutManager(linearLayoutManager);
        historyAdapter = new HistoryAdapter(getActivity(), history);
        mBinding.listHistory.setAdapter(historyAdapter);
    }

    private void startService() {
        tempMonitoringService = new Intent(getActivity(), TempMonitoringService.class);
        if (getActivity() != null) {
            Objects.requireNonNull(getActivity()).startService(tempMonitoringService);
        }
    }

    private void stopService() {
        if (!CommonActivity.isNullOrEmpty(tempMonitoringService) && getActivity() != null) {
            Objects.requireNonNull(getActivity()).stopService(tempMonitoringService);
        }
    }

    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void unit() {
        mDb = AppDatabase.getDatabase(getContext());
        getDB();
        initSpinner();
        initProgress();
        startService();
        warningService = new Intent(getActivity(), WarningService.class);
        mBinding.btnWarning.setAnimation(createFlashingAnimation());
//        mBinding.btnThreshold.setOnClickListener(this);
//        mBinding.btnOffset.setOnClickListener(this);
//        mBinding.btnLoopingTime.setOnClickListener(this);
        mBinding.deleteHistory.setOnClickListener(this);
        mBinding.reset.setOnClickListener(this);

        viewModel = ViewModelProviders
                .of(Objects.requireNonNull(getActivity()))
                .get(DetailDeviceViewModel.class);

        viewModel.monitoringDevice().subscribe(device1 -> {
            mBinding.setDetailDevice(device1);
            startHandler("1000L", device1);
        });

//        viewModel.observerDevice(device.getIndex()).subscribe(device -> {
//            if (update) {
//                mBinding.setDetailDevice(device);
//                update = false;
//            }
//            startHandler(device.getNCL(), device);
//        });
    }

    private void initProgress() {
        progressDialog = new Dialog(requireActivity());

        Window window = progressDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }
        progressDialog.
                setContentView(R.layout.progress_dialog);
        progressDialog.
                setCancelable(true);
        progressDialog.
                setCanceledOnTouchOutside(false);
    }

    private void initSpinner() {
        ArrayAdapter<CharSequence> adapter
                = ArrayAdapter.createFromResource(Objects.requireNonNull(getActivity()),
                R.array.history_display,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mBinding.spnHistoryDisplay.setAdapter(adapter);
        mBinding.spnHistoryDisplay.setSelection(0);

        mBinding.spnHistoryDisplay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mBinding.combineChart.setVisibility(View.VISIBLE);
                    mBinding.listHistory.setVisibility(View.GONE);
                } else {
                    mBinding.combineChart.setVisibility(View.GONE);
                    mBinding.listHistory.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveDevice(Device device) {
//        Single.create(emitter -> {
//            Device d = new Device(device.getId(), device.getNO(), device.getTime());
//            AppDatabase.getDatabase(getActivity()).deviceDAO().insertDevice(d);
//        }).subscribe((o, throwable) -> {
//            if (throwable == null) {
//                Log.d("saveDevice", o.toString());
//            } else {
//                Log.d("Error", throwable.toString());
//            }
//        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startHandler(String time, Device device) {
        try {
            timeL = Long.parseLong(time) * 1000;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        new Handler().postDelayed(() -> {
            mBinding.setDetailDevice(device);
            @SuppressLint("SimpleDateFormat") String timeStamp
                    = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss").format(Calendar.getInstance().getTime());
            device.setTime(timeStamp);
            saveDevice(device);

            @SuppressLint("SimpleDateFormat") String currentTime
                    = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
            timeLabel.add(currentTime);
            float temp = 0;
            float temp2 = 0;
            try {
                temp = Float.parseFloat(device.getNO1());
                temp2 = Float.parseFloat(device.getNO2());

                temperature.add(temp);
                temperature2.add(temp2);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            updateChart();
            try {
                compareTemp(device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, timeL);
    }

    private void updateChart() {
        CombinedData data = new CombinedData();
        LineData lineDatas = new LineData();
        lineDatas.addDataSet((ILineDataSet) dataChart(temperature, "Mâm nhiệt 1", Color.GREEN));

        lineDatas.addDataSet((ILineDataSet) dataChart(temperature2, "Mâm nhiệt 2", Color.RED));

        data.setData(lineDatas);

        xAxis.setAxisMaximum(data.getXMax() + 0.25f);

        mChart.setData(data);
        mChart.invalidate();
    }

    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void compareTemp(Device device1) {
        if (CommonActivity.isNullOrEmpty(device1.getNO1())
                || CommonActivity.isNullOrEmpty(device1.getNG1())
                || CommonActivity.isNullOrEmpty(device1.getNO2())
                || CommonActivity.isNullOrEmpty(device1.getNG2()))
            return;
        try {
            history.add(0, device1);
            historyAdapter.notifyItemInserted(0);
            mBinding.listHistory.smoothScrollToPosition(0);

            if (Double.parseDouble(device1.getNO1()) > (1.2 * Double.parseDouble(device1.getNG1()))) {
                startWarning(1);
                mBinding.btnWarning.setOnClickListener(v -> {
                    cancelWarning();
                });
                Log.d("history", String.valueOf(history.size()));
                Log.d("highTemp", String.valueOf(highTemp));
            } else if (Double.parseDouble(device1.getNO2()) > (1.2 * Double.parseDouble(device1.getNG2()))) {
                startWarning(2);
                mBinding.btnWarning.setOnClickListener(v -> {
                    cancelWarning();
                });
                Log.d("history", String.valueOf(history.size()));
                Log.d("highTemp", String.valueOf(highTemp));
            } else {
                cancelWarning();
                mBinding.txtHumanTemp.clearAnimation();
                mBinding.txtFirst.clearAnimation();
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void startWarning(int index) {
        playWarningSound();
        if (!flashingText) {
            if (index == 1) {
                mBinding.txtFirst.setAnimation(createFlashingAnimation());
            }
            if (index == 2) {
                mBinding.txtHumanTemp.setAnimation(createFlashingAnimation());
            }
            flashingText = true;
        }
        mBinding.btnWarning.setVisibility(View.VISIBLE);
        mBinding.btnWarning.setAnimation(createFlashingAnimation());
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
        mBinding.btnWarning.clearAnimation();
        mBinding.txtFirst.clearAnimation();
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
        update = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        if (!CommonActivity.isNullOrEmpty(device.getName1())) {
            edtDeviceName.setText(device.getName1());
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
                device.setName1(edtDeviceName.getText().toString());
                viewModel.setName(device.getName1());
                txtWarning.setVisibility(View.GONE);
            } else {
                txtWarning.setVisibility(View.VISIBLE);
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("CheckResult")
    private void getDB() {
        ArrayList<Device> devices = (ArrayList<Device>) mDb.deviceDAO().getAllDevice();
        history = new ArrayList<>(devices);
        for (Device device : devices) {
            timeLabel.add(device.getTime());
            try {
                temperature.add(Float.parseFloat(device.getNO1()));
                temperature2.add(Float.parseFloat(device.getNO2()));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        Observable.just(devices).subscribe(device -> {
            Log.d("getDB", "getDB: " + devices.size());
        });
        updateChart();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("CheckResult")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.reset:
//                CommonActivity.createDialog(requireActivity(),
//                        R.string.reset_device,
//                        R.string.app_name,
//                        R.string.cancel,
//                        R.string.ok,
//                        null,
//                        v1 -> {
//                            progressDialog.show();
//                            viewModel.reset(device.getIndex(), "1").subscribe(s -> {
//                                if (s.equals("Success")) {
//                                    progressDialog.dismiss();
//                                    Toast.makeText(getActivity(), "Reset thành công!", Toast.LENGTH_SHORT).show();
//                                } else {
//                                    Toast.makeText(getActivity(), "Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                            progressDialog.dismiss();
//                        }).show();
                break;
            case R.id.deleteHistory:
                Objects.requireNonNull(CommonActivity.createDialog(getActivity(),
                        R.string.delete_history,
                        R.string.app_name,
                        R.string.ok,
                        R.string.cancel,
                        v1 -> {
                            AppDatabase.getDatabase(getActivity()).deviceDAO().deleteHistory();
                            clearChart();
                            history.clear();
                            historyAdapter.notifyDataSetChanged();
                            Toast.makeText(getActivity(), "Xóa thành công !", Toast.LENGTH_LONG).show();
                        },
                        null)).show();
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("CheckResult")
    private void configureLoopingTime() {
        createPopUp(Objects.requireNonNull(getActivity()),
                getString(R.string.looping_time),
                ParamType.LOOPINGTIME,
                "Thời gian");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("CheckResult")
    private void configureOffset() {
        createPopUp(Objects.requireNonNull(getActivity()),
                getString(R.string.offset),
                ParamType.OFFSET,
                "Offset");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("CheckResult")
    private void configureThreshold() {
        createPopUp(Objects.requireNonNull(getActivity()),
                getString(R.string.threshold),
                ParamType.THRESHOLD,
                "Ngưỡng");
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("CheckResult")
    public void createPopUp(Activity activity,
                            String title,
                            ParamType type,
                            String paramName) {
        LayoutInflater inflater = activity.getLayoutInflater();
        @SuppressLint("InflateParams") View alertLayout = inflater.inflate(R.layout.dialog_with_edittext, null);
        final EditText editText = alertLayout.findViewById(R.id.editText);
        final TextView tvParamName = alertLayout.findViewById(R.id.tvParamName);

        tvParamName.setText(paramName);

        androidx.appcompat.app.AlertDialog.Builder alert =
                new androidx.appcompat.app.AlertDialog.Builder(activity);
        alert.setTitle(title);
        alert.setView(alertLayout);
        alert.setCancelable(false);

        alert.setNegativeButton(activity.getString(R.string.cancel), (dialog1, which) -> {
            Toast.makeText(getActivity(), getString(R.string.cancel), Toast.LENGTH_SHORT).show();
        });
        alert.setPositiveButton(activity.getString(R.string.ok), (dialog12, which) -> {
            switch (type) {
                case OFFSET:
                    String offSet = editText.getText().toString().trim();
                    if (!CommonActivity.isNullOrEmpty(offSet)) {
                        viewModel.setOffset(offSet).subscribe(s -> {
                            if (s.equals("Success")) {
                                editText.setText("");
                                Toast.makeText(getActivity(), "Cài đặt offset thành công!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
                case THRESHOLD:
                    String ng = editText.getText().toString().trim();
                    if (!CommonActivity.isNullOrEmpty(ng)) {
                        if ("HHA000002".equals(device.getId())) {
                            try {
                                int ngDouble = Integer.parseInt(ng) * 10 + 2730;
                                ng = String.valueOf(ngDouble);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        viewModel.setNG(ng).subscribe(s -> {
                            if (s.equals("Success")) {
                                editText.setText("");
                                Toast.makeText(getActivity(), "Cài đặt ngưỡng thành công!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        CommonActivity.showConfirmValidate(getActivity(), "Vui lòng nhập giá trị ngưỡng");
                    }
                    break;
                case LOOPINGTIME:
                    String loopingTime = editText.getText().toString().trim();
                    if (!CommonActivity.isNullOrEmpty(loopingTime)) {
                        viewModel.setLoopingTime(loopingTime).subscribe(s -> {
                            if (s.equals("Success")) {
                                editText.setText("");
                                Toast.makeText(getActivity(), "Cài đặt thời gian thành công!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
            }
        });

        androidx.appcompat.app.AlertDialog dialog = alert.create();
        dialog.show();
    }

    enum ParamType {
        OFFSET(1),
        THRESHOLD(2),
        LOOPINGTIME(3);

        private int value;

        ParamType(int i) {
            i = value;
        }
    }
}
