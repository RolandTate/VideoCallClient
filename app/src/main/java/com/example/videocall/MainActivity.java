package com.example.videocall;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class MainActivity extends AppCompatActivity {

    ApplicationTool appTool;

    // 填写从 Agora 控制台获取的项目的 App ID。
    private String appId = "23d4cf4473a24ee78a3810a8c61067b7";
    // 填写频道名称。
    private String channelName = "first";
    // 填写 Agora 控制台中生成的临时 Token。
    private String token = "00623d4cf4473a24ee78a3810a8c61067b7IAAqXVxS3jdbq0VO0liJQYKwIeAlGrsrGd+BT94wYs6EQlfucZKGXltnIgCw9G8hxIFWYgQAAQBUPlViAgBUPlViAwBUPlViBABUPlVi";
    private RtcEngine mRtcEngine;
    private String localUID = "20220421";

    private boolean mCallEnd;

    private FrameLayout mLocalContainer;
    private RelativeLayout mRemoteContainer;
    private VideoCanvas mLocalVideo;
    private VideoCanvas mRemoteVideo;

    private ImageView mCallBtn;

    Socket socket;
    OutputStream outputStream;
    InputStream inputStream;
    private Toast toast;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        // 监听频道内的远端用户，获取用户的 uid 信息。
        public void onUserJoined(final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 从 onUserJoined 回调获取 uid 后，调用 setupRemoteVideo，设置远端视频视图。
                    setupRemoteVideo(uid);
                    Log.e("1", String.valueOf(uid));
                }
            });
        }
        @Override
        public void onUserOffline(final int uid , int reason ) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //mLogView.logI("User offline, uid: " + (uid & 0xFFFFFFFFL));
                    onRemoteUserLeft(uid);
                }
            });
        }
    };



    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toast = Toast.makeText(getApplicationContext(),"",Toast.LENGTH_SHORT);
        setContentView(R.layout.activity_main);

        initByServer();
        initUI();
        // 如果已经授予所有权限，初始化 RtcEngine 对象并加入一个频道。
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            initializeAndJoinChannel();
        }
    }

    private void initUI() {
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);

        mCallBtn = findViewById(R.id.btn_call);
    }

    private void initByServer(){
        appTool = (ApplicationTool) MainActivity.this.getApplication();
        localUID = appTool.getID();
        channelName = appTool.getChannel();

        new Thread(new Runnable() {
            @Override
            public void run() {
                socket = null;
                if (socket != null) {
                    if (socket.isConnected()) {
                        Tip("已连接！", Toast.LENGTH_SHORT);
                    }
                } else {
                    socket = new Socket();
                    //ipconfig获得到的地址
                    //SocketAddress socAddress = new InetSocketAddress("192.168.168.102", 8922);
                    SocketAddress socAddress = new InetSocketAddress("118.31.54.155", 8922);
                    try {
                        socket.connect(socAddress, 1000);
                        System.out.println("token服务器连接成功");
                        outputStream = socket.getOutputStream();
                        outputStream.write(localUID.getBytes());

                        System.out.println("向token服务器发送用户id：" + localUID);

                        inputStream = socket.getInputStream();
                        byte[] bytes = new byte[1024];
                        int len = inputStream.read(bytes);
                        token = new String(bytes,0,len);
                        System.out.println("token is: "+token);
                        Tip("token获取成功！", Toast.LENGTH_SHORT);
                        socket.close();
                    } catch (IOException e) {
                        Tip("token服务器连接失败！", Toast.LENGTH_SHORT);
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    protected void onDestroy() {
        super.onDestroy();

        mRtcEngine.leaveChannel();
        mRtcEngine.destroy();
    }

    private void initializeAndJoinChannel() {

        initializeEngine();
        setupVideoConfig();
        setupLocalVideo();
        joinChannel();


        // 将 SurfaceView 对象传入 Agora，以渲染本地视频。
        //mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, Integer.parseInt(localUID)));
        // 使用 Token 加入频道。
        //mRtcEngine.joinChannel(token, channelName, "", Integer.parseInt(localUID));

    }

    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), appId, mRtcEventHandler);
        } catch (Exception e) {
            Log.e("initializeEngine", Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoConfig() {
        // In simple use cases, we only need to enable video capturing
        // and rendering once at the initialization step.
        // Note: audio recording and playing is enabled by default.
        mRtcEngine.enableVideo();

        // Please go to this page for detailed explanation
        // https://docs.agora.io/en/Video/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_rtc_engine.html#af5f4de754e2c1f493096641c5c5c1d8f
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {
        // This is used to set a local preview.
        // The steps setting local and remote view are very similar.
        // But note that if the local User do not have a uid or do
        // not care what the uid is, he can set his uid as ZERO.
        // Our server will assign one and return the uid via the event
        // handler callback function (onJoinChannelSuccess) after
        // joining the channel successfully.
        SurfaceView view = RtcEngine.CreateRendererView(getBaseContext());
        view.setZOrderMediaOverlay(true);
        mLocalContainer.addView(view);
        // Initializes the local video view.
        // RENDER_MODE_HIDDEN: Uniformly scale the video until it fills the visible boundaries. One dimension of the video may have clipped contents.
        mLocalVideo = new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, Integer.parseInt(localUID));
        mRtcEngine.setupLocalVideo(mLocalVideo);
    }

    private void joinChannel() {
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name that
        // you use to generate this token.
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null; // default, no token
        }
        mRtcEngine.joinChannel(token, channelName, "Extra Optional Data", Integer.parseInt(localUID));
    }

    private void setupRemoteVideo(int uid) {
        ViewGroup parent = mRemoteContainer;
        if (parent.indexOfChild(mLocalVideo.view) > -1) {
            parent = mLocalContainer;
        }

        if (mRemoteVideo != null) {
            return;
        }

        SurfaceView view = RtcEngine.CreateRendererView(getBaseContext());
        view.setZOrderMediaOverlay(parent == mLocalContainer);
        parent.addView(view);
        mRemoteVideo = new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid);
        // Initializes the video view of a remote User.
        mRtcEngine.setupRemoteVideo(mRemoteVideo);
    }

    private void onRemoteUserLeft(int uid) {
        if (mRemoteVideo != null && mRemoteVideo.uid == uid) {
            removeFromParent(mRemoteVideo);
            // Destroys remote view
            mRemoteVideo = null;
            leaveChannel();
        }
    }

    public void Tip(String str, int showTime) {
        toast.setText(str);
        toast.setDuration(showTime);
        toast.show();
    }

    private ViewGroup removeFromParent(VideoCanvas canvas) {
        if (canvas != null) {
            ViewParent parent = canvas.view.getParent();
            if (parent != null) {
                ViewGroup group = (ViewGroup) parent;
                group.removeView(canvas.view);
                return group;
            }
        }
        return null;
    }

    private void switchView(VideoCanvas canvas) {
        ViewGroup parent = removeFromParent(canvas);
        if (parent == mLocalContainer) {
            if (canvas.view instanceof SurfaceView) {
                ((SurfaceView) canvas.view).setZOrderMediaOverlay(false);
            }
            mRemoteContainer.addView(canvas.view);
        } else if (parent == mRemoteContainer) {
            if (canvas.view instanceof SurfaceView) {
                ((SurfaceView) canvas.view).setZOrderMediaOverlay(true);
            }
            mLocalContainer.addView(canvas.view);
        }
    }

    public void onLocalContainerClick(View view) {
        switchView(mLocalVideo);
        switchView(mRemoteVideo);
    }

    public void onCallClicked(View view) {
        if (mCallEnd) {
            startCall();
            mCallEnd = false;
            mCallBtn.setImageResource(R.drawable.btn_endcall);
        } else {
            endCall();
            mCallEnd = true;
            mCallBtn.setImageResource(R.drawable.btn_startcall);
        }
    }

    private void startCall() {
        setupLocalVideo();
        joinChannel();
    }

    private void endCall() {
        removeFromParent(mLocalVideo);
        mLocalVideo = null;
        removeFromParent(mRemoteVideo);
        mRemoteVideo = null;
        leaveChannel();

    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
        appTool = (ApplicationTool)MainActivity.this.getApplication();
        socket = appTool.getSocket();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream out = socket.getOutputStream();
                    out.write("EndVideo\n".getBytes());

                    Socket DBServerSocket = appTool.getSocket();
                    OutputStream DBServerOut = DBServerSocket.getOutputStream();
                    DBServerOut.write("EndCall\n".getBytes());
                    System.out.println("结束通话");
                    Family.threadActive = true;
                    startActivity(new Intent(MainActivity.this,Family.class));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
