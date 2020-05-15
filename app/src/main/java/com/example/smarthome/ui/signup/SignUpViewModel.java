package com.example.smarthome.ui.signup;

import android.os.Build;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.AsyncListUtil;

import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.ui.login.User;
import com.example.smarthome.utils.Result;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class SignUpViewModel extends ViewModel {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");
    private Result<User> result;

    public MutableLiveData<String> name = new MutableLiveData<>();
    public MutableLiveData<String> address = new MutableLiveData<>();
    public MutableLiveData<String> email = new MutableLiveData<>();
    public MutableLiveData<String> mobile = new MutableLiveData<>();
    public MutableLiveData<String> password = new MutableLiveData<>();
    public MutableLiveData<String> rePassword = new MutableLiveData<>();

    private User user = null;
    private String idDevice = "HHA000001, HHA000002, HHA000003";

    public void setResult(Result<User> result) {
        this.result = result;
    }

    public void onClick(View v) {
        user = new User(email.getValue(),
                password.getValue(),
                name.getValue(),
                address.getValue(),
                mobile.getValue(),
                idDevice);
        if (validate()) {
            String childId = userRef.push().getKey();
            if (childId != null) {
                userRef.child(childId).setValue(user.toMap(), (databaseError, databaseReference) -> {
                    Log.d("onCreate", "success");
                    Log.d("key", childId);
                });
            }
        }
    }

    private boolean validate() {
        String message = "";
        if (!CommonActivity.isNullOrEmpty(password.getValue())
                && password.getValue().length() < 5) {
            message = "Mật khẩu phải lớn hơn 5 kí tự!";
            result.onFailure(message);
            return false;
        } else if (!CommonActivity.isNullOrEmpty(rePassword.getValue())) {
            message = "Vui lòng nhập lại mật khẩu!";
            result.onFailure(message);
            return false;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (!Objects.equals(rePassword.getValue(), password.getValue())) {
                message = "Nhập lại password chưa chính xác!";
                result.onFailure(message);
                return false;
            }
        } else if (!CommonActivity.isNullOrEmpty(email.getValue())
                && Objects.requireNonNull(email.getValue()).matches(String.valueOf(Patterns.EMAIL_ADDRESS))) {
            message = "Email chưa đúng định dạng!";
            result.onFailure(message);
            return false;
        } else if (!CommonActivity.isNullOrEmpty(name.getValue())) {
            message = "Tên không được để trống!";
            result.onFailure(message);
            return false;
        } else {
            result.onSuccess(user, "Đăng ký thành công!");
            return true;
        }
        return false;
    }
}
