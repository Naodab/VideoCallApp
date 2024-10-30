package com.example.secondwebrtc.view;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.secondwebrtc.R;
import com.example.secondwebrtc.databinding.ActivityMainBinding;
import com.example.secondwebrtc.repository.CallRepository;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private CallRepository callRepository = CallRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    private void init() {
        binding.enterBtn.setOnClickListener(v ->  {
            PermissionX.init(this)
                .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .request((allGranted, grantedList, deniedList)  -> {
                    if (allGranted) {
                        callRepository.login(binding.username.getText().toString(), getApplicationContext(), () -> {
                            // If success then call
                            Intent intent = new Intent(LoginActivity.this, CallActivity.class);
                            startActivity(intent);
                        });
                    }
                }
            );
        });
    }
}