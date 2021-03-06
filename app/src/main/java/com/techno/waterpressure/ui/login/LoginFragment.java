package com.techno.waterpressure.ui.login;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.techno.waterpressure.MainActivity;
import com.techno.waterpressure.R;
import com.techno.waterpressure.common.CommonActivity;
import com.techno.waterpressure.common.ReplaceFragment;
import com.techno.waterpressure.databinding.LoginFragmentBinding;
import com.techno.waterpressure.ui.device.DetailDeviceFragment;
import com.techno.waterpressure.ui.signup.SignUpFragment;

import java.util.Map;
import java.util.Objects;

import io.reactivex.Observable;

public class LoginFragment extends Fragment {
    public static final String ARG_EMAIL = "ARG_EMAIL";
    public static final String ARG_PASSWORD = "ARG_PASSWORD";
    private LoginViewModel loginViewModel;
    private Activity mActivity;
    private LoginFragmentBinding binding;
    private String idDevice = "";
    private SharedPreferences sharedPreferences;
    public static final String MyPREFERENCES = "MyPrefs";
    private FirebaseAuth mAuth;
    private EditText txtInputDevice;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");
    private String email = "";
    private String password = "";
    private int progressStatus = 0;
    private Dialog progressDialog;

    public static LoginFragment newInstance(String email, String password) {

        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_PASSWORD, password);

        LoginFragment fragment = new LoginFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        binding = DataBindingUtil.inflate(inflater, R.layout.login_fragment, container, false);
        getBundleData();
        unit();
        return binding.getRoot();
    }

    private void getBundleData() {
        Bundle bundle = getArguments();
        if (!CommonActivity.isNullOrEmpty(bundle)) {
            email = bundle.getString(ARG_EMAIL);
            password = bundle.getString(ARG_PASSWORD);

            binding.txtEmailAddress.setText(email);
            binding.txtPassword.setText(password);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    @SuppressLint("CheckResult")
    private void unit() {
        loginViewModel = ViewModelProviders.of((FragmentActivity) mActivity).get(LoginViewModel.class);
        mAuth = FirebaseAuth.getInstance();
        initSharedPreferences();
        binding.setLifecycleOwner(this);
        binding.setLoginViewModel(loginViewModel);

        if (mAuth.getCurrentUser() != null) {
            ReplaceFragment.replaceFragment(mActivity,
                    DetailDeviceFragment.newInstance(),
                    true);
        }

        binding.btnLogin.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            User inputUser = new User(loginViewModel.Email.getValue(),
                    loginViewModel.Password.getValue());
            if (CommonActivity.isNullOrEmpty(loginViewModel.Email.getValue())
                    || CommonActivity.isNullOrEmpty(loginViewModel.Password.getValue())) {
                CommonActivity.showConfirmValidate(mActivity, "Please fill in full login information!");
                return;
            }
            Observable.create(emitter -> {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                        inputUser.getEmail(),
                        inputUser.getPassword()
                ).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onLoginSuccess(inputUser);
                    } else {
                        CommonActivity.showConfirmValidate(mActivity, "Wrong email or password!");
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
            }).doOnSubscribe(disposable -> binding.progressBar.setVisibility(View.VISIBLE))
                    .doOnTerminate(() -> binding.progressBar.setVisibility(View.GONE))
                    .subscribe();
        });

        binding.signUp.setOnClickListener(v -> {
            ReplaceFragment.replaceFragment(mActivity, SignUpFragment.newInstance(), true);
        });
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

        loginViewModel.loadingVisibility.observe(this, visibility -> {
            if (visibility == View.VISIBLE) showLoading();
            else hideLoading();
        });
    }

    private void hideLoading() {
        progressDialog.dismiss();
    }

    private void showLoading() {
        progressDialog.show();

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

    private void onLoginSuccess(User inputUser) {
        Toast.makeText(mActivity, "Login success with " + inputUser.getEmail(), Toast.LENGTH_SHORT).show();
        if (binding.saveUser.isChecked()) {
//            String email = user.getEmail();
//            String password = user.getPassword();
            boolean saveUser = binding.saveUser.isChecked();

            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("email", email);
            editor.putString("password", password);
            editor.putBoolean("saveUser", saveUser);
            editor.apply();
        }
        ReplaceFragment.replaceFragment(mActivity,
                DetailDeviceFragment.newInstance(),
                true);
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

        alert.setNegativeButton(getString(R.string.cancel), (dialog, which)
                -> Toast.makeText(mActivity, "Cancel clicked", Toast.LENGTH_SHORT).show());

        alert.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            idDevice += txtInputDevice.getText().toString() + ",";
            loginViewModel.updateDevice(idDevice, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    ReplaceFragment.replaceFragment(mActivity,
                            DetailDeviceFragment.newInstance(),
                            true);
                }
            });
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(49374, resultCode, data);

        if (result != null) {
            if (result.getContents() != null) {
                if (requestCode == 1) {
                    txtInputDevice.setText(result.getContents());
                }
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity) Objects.requireNonNull(getActivity())).disableBackBtn();
    }

    private class AllOnCompleteListener implements OnCompleteListener<Map<DatabaseReference, DataSnapshot>> {
        private ProgressBar progressBar;

        @Override
        public void onComplete(@NonNull Task<Map<DatabaseReference, DataSnapshot>> task) {
            if (task.isSuccessful()) {
                final Map<DatabaseReference, DataSnapshot> result = task.getResult();
                if (result != null) {
                    DataSnapshot dataSnapshot = result.get(userRef);

                }
            } else {
                Log.e("AllOnCompleteListener", String.valueOf(task.getException()));
            }
        }
    }
}
