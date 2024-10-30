package com.example.secondwebrtc.webrtc;

import android.content.Context;
import android.util.Log;

import com.example.secondwebrtc.utils.DataModel;
import com.example.secondwebrtc.utils.DataModelType;
import com.google.gson.Gson;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class WebRTCClient {
    private final Gson gson = new Gson();
    private final Context context;
    private final String username;
    private final EglBase.Context eglBaseContext= EglBase.create().getEglBaseContext();
    private final PeerConnectionFactory peerConnectionFactory;
    private final PeerConnection peerConnection;
    private final List<PeerConnection.IceServer> iceServer = new ArrayList<>();
    private CameraVideoCapturer videoCapturer;
    private final VideoSource localVideoSource;
    private final AudioSource localAudioSource;
    private final String localTrackId = "local_track";
    private final String localStreamId = "local_stream";
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private MediaStream localStream;
    private final MediaConstraints mediaConstraints = new MediaConstraints();
    private final MediaConstraints audioConstraints = new MediaConstraints();

    public Listener listener;

    public WebRTCClient(Context context, PeerConnection.Observer observer, String username) {
        this.context = context;
        this.username = username;
        initPeerConnectionFactory();
        peerConnectionFactory = createPeerConnectionFactory();
        iceServer.add(PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R").createIceServer());
        peerConnection = createPeerConnection(observer);
        localVideoSource = peerConnectionFactory.createVideoSource(false);
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        localAudioSource = peerConnectionFactory.createAudioSource(audioConstraints);
    }

    private void initPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(context).setFieldTrials("WebRTC-H264HighProfile/Enabled/").setEnableInternalTracer(true).createInitializationOptions();
        PeerConnectionFactory.initialize(options);
    }

    private PeerConnectionFactory createPeerConnectionFactory() {
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableEncryption = false;
        options.disableNetworkMonitor = false;
        return PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBaseContext,true,true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBaseContext))
                .setOptions(options).createPeerConnectionFactory();
    }

    private PeerConnection createPeerConnection(PeerConnection.Observer observer){
        return peerConnectionFactory.createPeerConnection(iceServer,observer);
    }

    public void initSurfaceViewRendere(SurfaceViewRenderer viewRenderer){
        viewRenderer.setEnableHardwareScaler(true);
        viewRenderer.setMirror(true);
        viewRenderer.init(eglBaseContext,null);
    }

    public void initLocalSurfaceView(SurfaceViewRenderer view){
        initSurfaceViewRendere(view);
        startLocalVideoStreaming(view);
    }

    private void startLocalVideoStreaming(SurfaceViewRenderer view) {
        SurfaceTextureHelper helper= SurfaceTextureHelper.create(
                Thread.currentThread().getName(), eglBaseContext
        );

        videoCapturer = getVideoCapturer();
        videoCapturer.initialize(helper,context,localVideoSource.getCapturerObserver());
        videoCapturer.startCapture(480,360,15);
        localVideoTrack = peerConnectionFactory.createVideoTrack(
                localTrackId+"_video",localVideoSource
        );
        localVideoTrack.addSink(view);

        localAudioTrack = peerConnectionFactory.createAudioTrack(localTrackId+"_audio",localAudioSource);
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId);
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(localAudioTrack);
        peerConnection.addStream(localStream);
    }

    private CameraVideoCapturer getVideoCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(context);

        String[] deviceNames = enumerator.getDeviceNames();

        for (String device: deviceNames){
            if (enumerator.isFrontFacing(device)){
                return enumerator.createCapturer(device,null);
            }
        }
        throw new IllegalStateException("front facing camera not found");
    }

    public void initRemoteSurfaceView(SurfaceViewRenderer view){
        initSurfaceViewRendere(view);
    }

    public void onRemoteSessionReceived(SessionDescription sessionDescription){
        peerConnection.setRemoteDescription(new MySdpObserver(), sessionDescription);
    }

    public void addIceCandidate(IceCandidate iceCandidate){
        peerConnection.addIceCandidate(iceCandidate);
    }

    public void sendIceCandidate(IceCandidate iceCandidate, String target){
        addIceCandidate(iceCandidate);
        if (listener!=null){
            listener.onTransferDataToOtherPeer(new DataModel(
                    target, username, gson.toJson(iceCandidate), DataModelType.IceCandidate
            ));
        }
    }

    public void switchCamera() {
        videoCapturer.switchCamera(null);
    }

    public void toggleVideo(Boolean shouldBeMuted){
        localVideoTrack.setEnabled(shouldBeMuted);
    }

    public void toggleAudio(Boolean shouldBeMuted){
        localAudioTrack.setEnabled(shouldBeMuted);
    }

    public void closeConnection(){
        try{
            localVideoTrack.dispose();
            videoCapturer.stopCapture();
            videoCapturer.dispose();
            peerConnection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void call(String target){
        try{
            peerConnection.createOffer(new MySdpObserver(){
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);
                    peerConnection.setLocalDescription(new MySdpObserver(){
                        @Override
                        public void onSetSuccess() {
                            super.onSetSuccess();
                            //its time to transfer this sdp to other peer
                            if (listener!=null){
                                listener.onTransferDataToOtherPeer(new DataModel(
                                        target, username, sessionDescription.description, DataModelType.Offer
                                ));
                            }
                        }
                    },sessionDescription);
                }
            },mediaConstraints);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void answer(String target){
        try{
            peerConnection.createAnswer(new MySdpObserver(){
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);
                    Log.d("DEBUG", "Answer of webrtc client");
                    peerConnection.setLocalDescription(new MySdpObserver(){
                        @Override
                        public void onSetSuccess() {
                            super.onSetSuccess();
                            if (listener!=null){
                                Log.d("DEBUG", "Answer of webrtc client");
                                listener.onTransferDataToOtherPeer(new DataModel(target, username,
                                        sessionDescription.description, DataModelType.Answer
                                ));
                            }
                        }
                    },sessionDescription);
                }

                @Override
                public void onCreateFailure(String s) {
                    Log.d("DEBUG", "ERROR");
                }
            },mediaConstraints);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public interface Listener {
        void onTransferDataToOtherPeer(DataModel model);
    }
}
