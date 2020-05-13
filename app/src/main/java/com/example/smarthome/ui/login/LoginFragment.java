package com.example.smarthome.ui.login;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

import com.example.smarthome.R;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.common.ReplaceFragment;
import com.example.smarthome.databinding.LoginFragmentBinding;
import com.example.smarthome.ui.main.MainFragment;
import com.example.smarthome.ui.signup.SignUpFragment;
import com.example.smarthome.utils.Result;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

@RequiresApi(api = Build.VERSION_CODES.M)
public class LoginFragment extends Fragment implements Result {
    private LoginViewModel loginViewModel;
    private Activity mActivity;
    private LoginFragmentBinding binding;
    private MutableLiveData<String> idDevice = new MutableLiveData<>();
    private KeyStore keyStore;
    public static final String KEY_NAME = "SmartHome";
    private Cipher cipher;
    private SharedPreferences sharedPreferences;
    public static final String MyPREFERENCES = "MyPrefs";

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
        fingerprint();
        return binding.getRoot();
    }

    private void fingerprint() {
        KeyguardManager manager
                = (KeyguardManager) Objects.requireNonNull(getActivity()).getSystemService(Context.KEYGUARD_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            FingerprintManager fingerprintManager
                    = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
            if (fingerprintManager != null) {
                if (fingerprintManager.isHardwareDetected()) {
                    Toast.makeText(mActivity, "Thiết bị không sử dụng vân tay!", Toast.LENGTH_SHORT).show();
                } else {
                    if (ActivityCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(mActivity, "Vui lòng cấp quyền sử dụng vân tay cho ứng dụng !", Toast.LENGTH_SHORT).show();
                    } else {
                        if (!fingerprintManager.hasEnrolledFingerprints()) {
                            Toast.makeText(mActivity, "Vui lòng đăng ký vân tay!", Toast.LENGTH_SHORT).show();
                        } else {
                            if (!manager.isKeyguardSecure()) {
                                Toast.makeText(mActivity, "Chưa bật khóa màn hình!", Toast.LENGTH_SHORT).show();
                            } else {
                                generateKey();
                                if (cipherInit()) {
                                    FingerprintManager.CryptoObject cryptoObject
                                            = new FingerprintManager.CryptoObject(cipher);
                                    FingerprintHandler helper = new FingerprintHandler(getActivity());
                                    helper.startAuth(fingerprintManager, cryptoObject);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
        }

        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get KeyGenerator instance", e);
        }

        try {
            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException |
                InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    private void unit() {
        sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");
        String password = sharedPreferences.getString("password", "");
        boolean saveUser = sharedPreferences.getBoolean("saveUser", false);
        if (!CommonActivity.isNullOrEmpty(email) && !CommonActivity.isNullOrEmpty(password)) {
            binding.txtEmailAddress.setText(email);
            binding.txtPassword.setText(password);
            binding.saveUser.setChecked(saveUser);
            binding.txtEmailAddress.setHint("");
            binding.txtPassword.setHint("");
            new Handler().postDelayed(() -> binding.btnLogin.performClick(), 10);
        }

        loginViewModel = ViewModelProviders.of((FragmentActivity) mActivity).get(LoginViewModel.class);
        binding.setLifecycleOwner(this);
        binding.setLoginViewModel(loginViewModel);

        loginViewModel.getAllUsersLiveData();
        loginViewModel.setResult(this);

        binding.signUp.setOnClickListener(v -> {
            ReplaceFragment.replaceFragment(mActivity, SignUpFragment.newInstance(), true);
        });
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

    public void displayAlertDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.enter_firebase_url_dialog, null);


        AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
        alert.setTitle(R.string.app_name);
        alert.setView(alertLayout);
        alert.setCancelable(false);
        alert.setNegativeButton("Hủy", (dialog, which)
                -> Toast.makeText(mActivity, "Cancel clicked", Toast.LENGTH_SHORT).show());

        alert.setPositiveButton("Đồng ý", (dialog, which)
                -> ReplaceFragment.replaceFragment(mActivity,
                MainFragment.newInstance(loginViewModel.getUser().getIdDevice()),
                true));
        AlertDialog dialog = alert.create();
        dialog.show();
    }
}
