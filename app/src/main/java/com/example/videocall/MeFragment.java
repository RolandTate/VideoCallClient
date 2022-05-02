package com.example.videocall;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;


public class MeFragment extends Fragment {
    SQLiteDatabase db=null;//数据库
    DBHelper dbHelper=null;//数据库Helper

    ApplicationTool appTool;
    Socket socket;
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream;

    Button logout;
    TextView currentUsername;

    public MeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_me,container,false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Init();
    }

    private void Init(){
        currentUsername = getActivity().findViewById(R.id.currentUsername);

        appTool = (ApplicationTool) getActivity().getApplication();
        socket = appTool.getSocket();
        objectOutputStream = appTool.getObjectOutputStream();
        objectInputStream = appTool.getObjectInputStream();

        logout = getActivity().findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logout();
            }
        });
    }

    private void Logout(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Family.threadActive = false;
                    sendEndConnectionMsg();
                    socket.close();
                    startActivity(new Intent(getActivity(),LoginActivity.class));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void sendEndConnectionMsg() throws Exception{
        //OutputStream outputStream = socket.getOutputStream();
        //outputStream.write("EndConnection".getBytes());
        objectOutputStream.writeObject("EndConnection");
    }
}
