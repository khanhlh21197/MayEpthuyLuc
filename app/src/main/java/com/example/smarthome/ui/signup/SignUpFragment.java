package com.example.smarthome.ui.signup;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.smarthome.R;
import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.common.ReplaceFragment;
import com.example.smarthome.databinding.SignUpFragmentBinding;
import com.example.smarthome.ui.login.LoginFragment;
import com.example.smarthome.ui.login.User;
import com.example.smarthome.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class SignUpFragment extends Fragment implements Result<User> {
    private SignUpFragmentBinding mBinding;
    private SignUpViewModel viewModel;
    private FirebaseAuth mAuth;

    public static SignUpFragment newInstance() {

        Bundle args = new Bundle();

        SignUpFragment fragment = new SignUpFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.sign_up_fragment, container, false);
        viewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SignUpViewModel.class);
        mBinding.setLifecycleOwner(this);
        viewModel.setResult(this);
        mBinding.setSignUpViewModel(viewModel);

        mAuth = FirebaseAuth.getInstance();
        createAccount();
        mBinding.linkLogin.setOnClickListener(v -> goToLogIn());
        return mBinding.getRoot();
    }

    private void createAccount() {
        String email = viewModel.email.getValue();
        String password = viewModel.password.getValue();
        if (!CommonActivity.isNullOrEmpty(email) && !CommonActivity.isNullOrEmpty(password)) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("createAccount", "success");
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            Log.d("createAccount", "failure", task.getException());
                            Toast.makeText(getContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void goToLogIn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ReplaceFragment.replaceFragment(getActivity(), LoginFragment.newInstance(), true);
        }
    }

    @Override
    public void onFailure(String message) {
        CommonActivity.showConfirmValidate(getActivity(), message);
    }

    @Override
    public void onSuccess(User user, String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
}

