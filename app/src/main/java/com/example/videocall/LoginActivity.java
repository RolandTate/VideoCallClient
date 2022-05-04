package com.example.videocall;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    Socket socket;
    //OutputStream outputStream;
    //InputStream inputStream;

    //BufferedInputStream bufferedInputStream;

    //BufferedReader bufferedReader;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;

    EditText userID;
    EditText password;
    TextView register;
    Button login;
    Toast toast;
    String ID;
    String pass;
    String loginRequestMsg = "loginRequest";
    String loginMsg = "Login";
    boolean loginChecked;

    SQLiteDatabase db;//数据库
    DBHelper dbHelper;

    private Handler loginHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case 0:
                    loginFail();
                    break;
                case 1:
                    getUsers();
                    break;
                case 2:
                    loginSuccess();
                    break;

            }
            super.handleMessage(msg);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toast = Toast.makeText(getApplicationContext(),"",Toast.LENGTH_SHORT);
        setContentView(R.layout.activity_login);

        Init();
    }

    void Init(){
        userID = findViewById(R.id.userID);
        password = findViewById(R.id.password);
        register = findViewById(R.id.register);
        login = findViewById(R.id.login);

        dbHelper=new DBHelper(this);
        db=dbHelper.getWritableDatabase();

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this,RegisterActivity.class));
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check();
            }
        });
    }

    void check(){
        //ID = userID.getText().toString() + "\n";
        //pass = password.getText().toString() + "\n";
        ID = userID.getText().toString() ;
        pass = password.getText().toString() ;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            System.out.println("登录服务器已连接");
                            Login();
                        }else{
                            connect();
                            System.out.println("重新连接登录服务器");
                            Login();
                        }
                    } else {
                        socket = new Socket();
                        connect();
                        System.out.println("第一次连接登录服务器");
                        Login();
                    }
                }catch (Exception e){
                    System.out.println("登录服务器连接失败");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void connect() throws Exception{
        SocketAddress socAddress = new InetSocketAddress("192.168.168.100", 8923);
        //SocketAddress socAddress = new InetSocketAddress("118.31.54.155", 8923);
        socket.connect(socAddress, 1000);

        //outputStream = socket.getOutputStream();
        //outputStream.write(loginRequestMsg.getBytes());

        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());

        objectOutputStream.writeObject(loginRequestMsg);
        System.out.println("已向登录服务器发送登录请求预备："+loginRequestMsg);
    }

    private void Login() throws Exception{
        sendLoginMsg();
        readLoginMsg();
    }

    private void sendLoginMsg() throws Exception{
        //outputStream.write(loginMsg.getBytes());
        objectOutputStream.writeObject(loginMsg);
        System.out.println("已向登录服务器发送登录请求："+loginMsg);

        //outputStream.write(ID.getBytes());
        //outputStream.write(pass.getBytes());
        objectOutputStream.writeObject(ID);
        objectOutputStream.writeObject(pass);
        System.out.println("已向登录服务器发送ID密码");
    }

    private void readLoginMsg() throws Exception{
        //inputStream = socket.getInputStream();

        //bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        //String result = bufferedReader.readLine();

        String result = (String) objectInputStream.readObject();

        if (result.equals("success")) {
            System.out.println("密码验证正确");
            Message msg = new Message();
            msg.what = 1;
            loginHandler.sendMessage(msg);
        } else {
            System.out.println("密码验证错误");
            Message msg = new Message();
            msg.what = 0;
            loginHandler.sendMessage(msg);
        }
    }

    private void loginSuccess(){
        System.out.println("登录密码验证成功");
        ApplicationTool appTool = (ApplicationTool) LoginActivity.this.getApplication();
        appTool.setSocket(socket);
        appTool.setObjectOutputStream(objectOutputStream);
        appTool.setObjectInputStream(objectInputStream);
        appTool.setID(userID.getText().toString());
        startActivity(new Intent(LoginActivity.this, Family.class));
    }

    private void getUsers (){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("开始准备用户信息");

                    //outputStream.write("GetUsers\n".getBytes());
                    objectOutputStream.writeObject("GetUsers");
                    System.out.println("向服务器请求用户信息");

                    List<User> users = new ArrayList();
                    UserList userMsg = new UserList();


                    userMsg = (UserList) objectInputStream.readObject();
                    System.out.println("已获取服务器发来的用户信息，准备插入数据库");

                    db.execSQL("drop table UsersInformation");
                    db.execSQL("create table UsersInformation(id text primary key , name text , state text)");
                    users = userMsg.getUsers();
                    for (int i = 0; i < users.size(); i++) {
                        ContentValues userInfo = new ContentValues();
                        userInfo.put("id", users.get(i).getID());
                        userInfo.put("name", users.get(i).getName());
                        userInfo.put("state", users.get(i).getState());
                        System.out.println("id: " + users.get(i).getID() +
                                " name: " + users.get(i).getName() +
                                " state: " + users.get(i).getState());
                        //往数据库中插入设备信息
                        db.insert("UsersInformation", null, userInfo);
                    }
                    //online = true;
                    System.out.println("获取用户列表成功");
                    Message msg = new Message();
                    msg.what = 2;
                    loginHandler.sendMessage(msg);
                }catch (Exception e){
                    System.out.println("获取用户列表失败");
                    Message msg = new Message();
                    msg.what = 0;
                    loginHandler.sendMessage(msg);
                    e.printStackTrace();

                }
            }
        }).start();

    }

    private void loginFail(){
        System.out.println("登录密码验证错误");
        Tip("用户ID或者密码错误！请重试！", Toast.LENGTH_SHORT);
    }

    private void closeAll(){
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
}
