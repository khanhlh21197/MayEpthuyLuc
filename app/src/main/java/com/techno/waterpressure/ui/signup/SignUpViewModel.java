package com.techno.waterpressure.ui.signup;

import android.os.Build;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.techno.waterpressure.common.CommonActivity;
import com.techno.waterpressure.ui.login.User;
import com.techno.waterpressure.utils.Result;

import java.util.Objects;

public class SignUpViewModel extends ViewModel {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");
    private Result<User> result;

    public MutableLiveData<String> email = new MutableLiveData<>();
    public MutableLiveData<String> password = new MutableLiveData<>();
    public MutableLiveData<String> rePassword = new MutableLiveData<>();
    public MutableLiveData<Integer> loading = new MutableLiveData<>(View.GONE);

    private String idDevice = "HHA000001, HHA000002, HHA000003";

    public void setResult(Result<User> result) {
        this.result = result;
    }

    public void onClick(View v) {
        User user = new User(email.getValue(), password.getValue());
        String message = "";
        if (CommonActivity.isNullOrEmpty(email)) {
            result.onFailure("Please in put email!");
        } else if (!CommonActivity.isNullOrEmpty(password.getValue())
                && password.getValue().length() < 5) {
            message = "Password must be greater than 5 characters!";
            result.onFailure(message);
        } else if (CommonActivity.isNullOrEmpty(rePassword.getValue())) {
            message = "Please in put re-password!";
            result.onFailure(message);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && !Objects.equals(rePassword.getValue(), password.getValue())) {
            message = "Wrong re-password!";
            result.onFailure(message);
        } else if (!CommonActivity.isNullOrEmpty(email.getValue())
                && (!Objects.requireNonNull(email.getValue()).matches(String.valueOf(Patterns.EMAIL_ADDRESS)))) {
            message = "Wrong Email!";
            result.onFailure(message);
        } else {
            loading.setValue(View.VISIBLE);
            FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(Objects.requireNonNull(email.getValue()),
                            Objects.requireNonNull(password.getValue()))
                    .addOnCompleteListener(task -> {
                        loading.setValue(View.GONE);
                        if (task.isSuccessful()) {
                            String childId = "";
                            if (!CommonActivity.isNullOrEmpty(FirebaseAuth.getInstance().getCurrentUser())) {
                                childId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                String finalChildId = childId;
                                userRef.child(childId).setValue(user.toMap(), (databaseError, databaseReference) -> {
                                    if (databaseError != null) {
                                        Log.d("onError", databaseError.toString());
                                        return;
                                    }
                                    Log.d("onCreate", "success");
                                    Log.d("key", finalChildId);
                                });
                            }
                            result.onSuccess(user, "Register success!");
                        } else {
                            result.onFailure("Account already exists!");
                        }
                    });
        }
    }
}
