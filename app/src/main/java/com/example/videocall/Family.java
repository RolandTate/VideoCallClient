package com.example.videocall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Family extends AppCompatActivity {
    private Socket socket;
    public static ObjectOutputStream objectOutputStream;
    public static ObjectInputStream objectInputStream;

    public static Lock streamLock  =   new ReentrantLock()   ;

    ApplicationTool appTool;
    Toast toast;

    public static boolean threadActive = true;
    public static ListenThread listenThread;

    private ListFragment fragment_1;//已开启设备界面
    private MeFragment fragment_2;//全部设备界面

    private LinearLayout linear_first;//用于切换界面
    private LinearLayout linear_second;

    private ImageView iv_first;//界面对应选择按钮的图标
    private ImageView iv_second;

    private TextView tv_first;//界面选择按钮的文字
    private TextView tv_second;

    SQLiteDatabase db;//数据库
    DBHelper dbHelper;

    private Handler familyHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case 1:
                    startCall();
                    break;

            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toast = Toast.makeText(getApplicationContext(),"",Toast.LENGTH_SHORT);
        setContentView(R.layout.activity_family);

        InitView();//初始化各个变量

        //getUsers();

        InitEvent();//为界面选择按钮添加对应的点击事件

        InitFragment(1);//默认选择已开启设备列表界面

        iv_first.setImageResource(R.drawable.main_tab_friends_selected);//选中已开启设备列表

        listenToServer();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    void InitView(){
        //初始化变量
        linear_first =  findViewById(R.id.line1);
        linear_second = findViewById(R.id.line2);

        iv_first = findViewById(R.id.icon_1);
        iv_second = findViewById(R.id.icon_2);

        tv_first = findViewById(R.id.textview_1);
        tv_second =  findViewById(R.id.textview_2);

        dbHelper=new DBHelper(this);
        db=dbHelper.getWritableDatabase();

        appTool = (ApplicationTool) Family.this.getApplication();

        socket = appTool.getSocket();
        objectOutputStream = appTool.getObjectOutputStream();
        objectInputStream = appTool.getObjectInputStream();

        listenThread = new ListenThread();
    }

    private void listenToServer(){
        listenThread.start();
    }

    private void startCall() {
        try {
            startActivity(new Intent(Family.this, MainActivity.class));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //为界面选择按钮添加点击事件
    private void InitEvent(){
        //为朋友界面按钮添加点击事件
        linear_first.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetButton();//让图标恢复默认
                iv_first.setImageResource(R.drawable.main_tab_friends_selected);//将图标设置为选中样式
                InitFragment(1);//跳转到Fragment_1
            }
        });
        //个人信息界面按钮
        linear_second.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetButton();
                iv_second.setImageResource(R.drawable.main_tab_me_selected);
                InitFragment(2);
            }
        });
    }

    //根据index值选择显示哪个界面，1为已开启设备，2为全部设备
    private void InitFragment(int index){

        //获取FragmentManager进行Activity内部Fragment切换显示
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        hideAllFragment(transaction);//隐藏全部Fragment

        //根据index切换Fragment显示
        switch (index){
            case 1:
                if(fragment_1 == null){
                    fragment_1 = new ListFragment();
                    transaction.add(R.id.frame_content,fragment_1);
                }
                else{
                    transaction.show(fragment_1);
                }
                break;

            case 2:
                if (fragment_2== null){
                    fragment_2 = new MeFragment();
                    transaction.add(R.id.frame_content,fragment_2);
                }
                else{
                    transaction.show(fragment_2);
                }
                break;

        }
        transaction.commit();//提交事务请求

    }

    //隐藏全部Fragment，以便选择显示某一个
    private void hideAllFragment(FragmentTransaction transaction){
        if (fragment_1!= null){
            transaction.hide(fragment_1);
        }

        if (fragment_2!= null){
            transaction.hide(fragment_2);
        }
    }

    //将选择界面按钮的图标设置为未选中的状态
    private void resetButton(){
        //设置为未点击状态
        iv_first.setImageResource(R.drawable.main_tab_friends);
        iv_second.setImageResource(R.drawable.main_tab_me);
    }
    public void Tip(String str, int showTime) {
        toast.setText(str);
        toast.setDuration(showTime);
        toast.show();
    }

    public class ListenThread extends Thread{

        @Override
        public void run() {
            while (threadActive){
                try{
                    //要设置锁避免死锁
                    String msg = (String) objectInputStream.readObject();
                    if(msg.equals("StartVideo")){
                        System.out.println("收到视频通话邀请");
                        String channel = (String)objectInputStream.readObject();
                        appTool.setChannel(channel);
                        System.out.println("setChannel: " + channel);
                        Message handlerMessage = new Message();
                        handlerMessage.what = 1;
                        familyHandler.sendMessage(handlerMessage);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        listenThread.interrupt();
    }
}
