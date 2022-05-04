package com.example.videocall;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class RegisterActivity extends AppCompatActivity {
    Socket socket;
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream;
    //OutputStream outputStream;
    //BufferedReader in;
    private Toast toast;

    EditText createUserID;
    EditText createPassword;
    EditText createUsername;
    Button createConfirm;

    String userID;
    String password;
    String userName;
    String registerRequestMsg = "registerRequest\n";
    String registerMsg = "register\n";

    private Handler registerHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case -1:
                    registerFail(false);
                    break;
                case 0:
                    registerFail(true);
                    break;
                case 1:
                    registerSuccess();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toast = Toast.makeText(getApplicationContext(),"",Toast.LENGTH_SHORT);
        setContentView(R.layout.activity_register);

        Init();
    }

    void Init(){
        createUserID = findViewById(R.id.createUserID);
        createPassword = findViewById(R.id.createPassword);
        createUsername = findViewById(R.id.createUsername);
        createConfirm = findViewById(R.id.createConfirm);
        createConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Register();
            }
        });
    }

    void Register(){
        userID = createUserID.getText().toString();
        password = createPassword.getText().toString();
        userName = createUsername.getText().toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            System.out.println("注册服务器已连接");
                            register();
                        } else {
                            connect();
                            System.out.println("重新连接注册服务器");
                            register();
                        }
                    } else {
                        socket = new Socket();
                        connect();
                        System.out.println("第一次连接注册服务器");
                        register();
                    }
                }catch (Exception e){
                    System.out.println("注册服务器连接失败");
                    Tip("注册服务器连接失败！", Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void connect() throws Exception{
        //SocketAddress socAddress = new InetSocketAddress("192.168.168.100", 8923);
        SocketAddress socAddress = new InetSocketAddress("118.31.54.155", 8923);
        socket.connect(socAddress, 1000);

        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        objectOutputStream.writeObject("registerRequest");
        System.out.println("已向注册服务器发送注册请求预备："+registerRequestMsg);
    }


    private void register()throws Exception{
        sendRegisterMsg();
        readRegisterMsg();
    }

    private void sendRegisterMsg() throws Exception{
        objectOutputStream.writeObject("register");
        System.out.println("已向注册服务器发送注册请求："+registerMsg);
        objectOutputStream.writeObject(userID);
        objectOutputStream.writeObject(password);
        objectOutputStream.writeObject(userName);
        System.out.println("已向注册服务器发送注册信息");

    }

    private void readRegisterMsg() throws Exception{
        String result = (String) objectInputStream.readObject();

        if(result.equals("success")){
            Message msg = new Message();
            msg.what = 1;
            registerHandler.sendMessage(msg);
        }
        else{
            if(result.equals("fail")){
                Message msg = new Message();
                msg.what = 0;
                registerHandler.sendMessage(msg);
            }
            else{
                Message msg = new Message();
                msg.what = -1;
                registerHandler.sendMessage(msg);
            }

        }
    }

    private void registerSuccess(){
        System.out.println("注册成功！");
        Tip("注册成功！", Toast.LENGTH_LONG);
        close();
        startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
    }

    private void registerFail(boolean reason){
        if(reason) {
            System.out.println("用户ID存在");
            Tip("用户ID存在，请重新填写！", Toast.LENGTH_SHORT);
        }
        else {
            System.out.println("有某项为空");
            Tip("有某项为空，请重新填写！", Toast.LENGTH_SHORT);
        }
    }

    private void close(){
        try{
            if (objectOutputStream != null)
                objectOutputStream.close();
            if (objectInputStream != null)
                objectInputStream.close();
            if (socket != null)
                socket.close();
            System.out.println("输入输出流以及socket全部关闭");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void Tip(String str, int showTime) {
        toast.setText(str);
        toast.setDuration(showTime);
        toast.show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
