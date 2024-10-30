package com.example.secondwebrtc.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.secondwebrtc.R;
import com.example.secondwebrtc.databinding.ActivityCallBinding;
import com.example.secondwebrtc.repository.CallRepository;
import com.example.secondwebrtc.utils.DataModelType;

public class CallActivity extends AppCompatActivity implements CallRepository.Listener {
    CallRepository callRepository = CallRepository.getInstance();
    ActivityCallBinding binding;
    boolean isMicrophoneMuted = false;
    boolean isCameraMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_call);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    private void init() {
        binding.callBtn.setOnClickListener(v -> {
            callRepository.sendCallRequest(() -> {
                Toast.makeText(this, "couldnt find the target", Toast.LENGTH_SHORT).show();
            });
        });

        callRepository.subscribeForLatestEvent(data -> {
            if (data.getType()== DataModelType.StartCall) {
                runOnUiThread(() -> {
                    callRepository.startCall(data.getSender());
                });
            }
        });

        callRepository.initLocalView(binding.localView);
        callRepository.initRemoteView(binding.remoteView);
        callRepository.listener = this;

        binding.switchCameraButton.setOnClickListener(v->{
            callRepository.switchCamera();
        });

        binding.micButton.setOnClickListener(v->{
            if (isMicrophoneMuted){
                binding.micButton.setImageResource(R.drawable.ic_aseline_mic_off_24);
            }else {
                binding.micButton.setImageResource(R.drawable.ic_baseline_mic_24);
            }
            callRepository.toggleAudio(isMicrophoneMuted);
            isMicrophoneMuted=!isMicrophoneMuted;
        });

        binding.videoButton.setOnClickListener(v->{
            if (isCameraMuted){
                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24);
            }else {
                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_24);
            }
            callRepository.toggleVideo(isCameraMuted);
            isCameraMuted=!isCameraMuted;
        });

        binding.endCallButton.setOnClickListener(v->{
            callRepository.endCall();
            finish();
        });
    }

    @Override
    public void webrtcConnected() {
        runOnUiThread(()->{
            binding.incomingCallLayout.setVisibility(View.GONE);
            binding.whoToCallLayout.setVisibility(View.GONE);
            binding.callLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void webrtcClosed() {
        runOnUiThread(this::finish);
    }
}