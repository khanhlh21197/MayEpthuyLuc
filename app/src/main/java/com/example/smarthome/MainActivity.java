package com.example.smarthome;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smarthome.common.CommonActivity;
import com.example.smarthome.common.ReplaceFragment;
import com.example.smarthome.ui.device.DetailDeviceFragment;
import com.example.smarthome.ui.login.LoginFragment;
import com.example.smarthome.ui.main.MainFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ActionBar toolBar;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Find the toolbar view inside the activity layout
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.frame_container, LoginFragment.newInstance()).commit();
//        String menuFragment = getIntent().getStringExtra("menuFragment");
//        String idDevice = getIntent().getStringExtra("idDevice");
//        if (!CommonActivity.isNullOrEmpty(menuFragment)){
//            if (menuFragment.equals("DetailDeviceFragment")){
//                ReplaceFragment.replaceFragment(this, MainFragment.newInstance(idDevice), false);
//            }
//        }
//        BottomNavigationView bottom = findViewById(R.id.navigation);
//        toolBar = getSupportActionBar();
//        bottom.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
//        ImageView btnBack = findViewById(R.id.btnBack);
//        btnBack.setOnClickListener(v -> {
//            onBackPressed();
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment fragment;
            switch (item.getItemId()) {
                case R.id.navigation_shop:
                    toolBar.setTitle("Shop");
                    return true;
                case R.id.navigation_gifts:
                    toolBar.setTitle("My Gifts");
                    return true;
                case R.id.navigation_cart:
                    toolBar.setTitle("Cart");
                    return true;
                case R.id.navigation_profile:
                    toolBar.setTitle("Profile");
                    return true;
            }
            return false;
        }
    };
}
