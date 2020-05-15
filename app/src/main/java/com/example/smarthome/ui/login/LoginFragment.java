package com.example.smarthome.ui.login;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.example.smarthome.R;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.common.ReplaceFragment;
import com.example.smarthome.databinding.LoginFragmentBinding;
import com.example.smarthome.ui.main.MainFragment;
import com.example.smarthome.ui.signup.SignUpFragment;
import com.example.smarthome.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Objects;

public class LoginFragment extends Fragment implements Result {
    private LoginViewModel loginViewModel;
    private Activity mActivity;
    private LoginFragmentBinding binding;
    private String idDevice = "";
    private SharedPreferences sharedPreferences;
    public static final String MyPREFERENCES = "MyPrefs";
    private FirebaseAuth mAuth;
    private EditText txtInputDevice;

    public static LoginFragment newInstance() {

        Bundle args = new Bundle();

        LoginFragment fragment = new LoginFragment();
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
        mActivity = getActivity();
        binding = DataBindingUtil.inflate(inflater, R.layout.login_fragment, container, false);
        unit();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    private void unit() {
        loginViewModel = ViewModelProviders.of((FragmentActivity) mActivity).get(LoginViewModel.class);
        mAuth = FirebaseAuth.getInstance();
        initSharedPreferences();
        binding.setLifecycleOwner(this);
        binding.setLoginViewModel(loginViewModel);

        loginViewModel.getAllUsersLiveData();
        loginViewModel.setResult(this);

        binding.signUp.setOnClickListener(v -> {
            ReplaceFragment.replaceFragment(mActivity, SignUpFragment.newInstance(), true);
        });
    }

    private void initSharedPreferences() {
        sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");
        String password = sharedPreferences.getString("password", "");
        boolean saveUser = sharedPreferences.getBoolean("saveUser", false);
        if (!CommonActivity.isNullOrEmpty(email) && !CommonActivity.isNullOrEmpty(password)) {
            loginViewModel.Email.setValue(email);
            loginViewModel.Password.setValue(password);
            binding.txtEmailAddress.setText(email);
            binding.txtPassword.setText(password);
            binding.saveUser.setChecked(saveUser);
        }
    }

    @Override
    public void onFailure(String message) {
        CommonActivity.showConfirmValidate(mActivity, message);
    }

    @Override
    public void onSuccess(Object o, String message) {
        if (binding.saveUser.isChecked()) {
            User user = (User) o;
            String email = user.getEmail();
            String password = user.getPassword();
            boolean saveUser = binding.saveUser.isChecked();

            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("email", email);
            editor.putString("password", password);
            editor.putBoolean("saveUser", saveUser);
            editor.apply();
        }

        Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
        displayAlertDialog();
    }

    private void displayAlertDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.enter_firebase_url_dialog, null);
        txtInputDevice = alertLayout.findViewById(R.id.txtInputDevice);
        ImageView scanBarcode = alertLayout.findViewById(R.id.scanBarcode);

        AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
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
                -> Toast.makeText(mActivity, "Cancel clicked", Toast.LENGTH_SHORT).show());

        alert.setPositiveButton("Đồng ý", (dialog, which) -> {
            idDevice = loginViewModel.getUser().getIdDevice() + ";" + txtInputDevice.getText().toString();
            loginViewModel.insertDevice(idDevice);
            ReplaceFragment.replaceFragment(mActivity,
                    MainFragment.newInstance(idDevice),
                    true);
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(49374, resultCode, data);

        if (result != null){
            if (result.getContents() != null){
                if (requestCode == 1){
                    txtInputDevice.setText(result.getContents());
                }
            }
        }
    }
}
