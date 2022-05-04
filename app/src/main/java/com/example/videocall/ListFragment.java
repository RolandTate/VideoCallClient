package com.example.videocall;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ListFragment extends Fragment {
    SQLiteDatabase db=null;//数据库
    DBHelper dbHelper=null;//数据库Helper
    ListView userList;//列表
    List<Map<String,Object>> usersSource ;//列表显示的数据源
    SimpleAdapter adapter;//将数据源的数据显示到列表的adapter
    ApplicationTool appTool;
    Socket socket;

    //键值对保存相应设备信息

    public ListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Init();//初始化

    }

    void Init(){
        System.out.println("开始显示ListFragment");

        dbHelper=new DBHelper(getActivity());
        db=dbHelper.getWritableDatabase();

        Cursor cursor=db.rawQuery("select * from UsersInformation",new String[]{});

        userList = getActivity().findViewById(R.id.familyMembers);
        usersSource = new ArrayList<Map<String, Object>>();
        appTool = (ApplicationTool) getActivity().getApplication();
        socket = appTool.getSocket();

        while(cursor.moveToNext()){
            System.out.println("member id: " + cursor.getString(0)+ " appTool id: " + appTool.getID());
            if(!cursor.getString(0).equals(appTool.getID())){
                Map<String,Object> userTemp = new HashMap<String,Object>();
                userTemp.put("id",cursor.getString(0));
                userTemp.put("name",cursor.getString(1));
                userTemp.put("state",cursor.getString(2));
                usersSource.add(userTemp);
            }
        }
        showList();
        System.out.println("显示ListFragment完成");
    }

    void showList(){
        adapter = new SimpleAdapter(
                getActivity(),
                usersSource,
                R.layout.member,
                new String[]{"name","id","state"},
                new int[]{R.id.name,R.id.memberID,R.id.state}
        ){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView == null){
                    convertView = View.inflate(getActivity(),R.layout.member,null);
                }
                //获取该项对应的信息
                final Map<String ,Object> temp = (Map<String, Object>)getItem(position);
                final String memberId = (String)temp.get("id");
                final String memberName = (String)temp.get("name");
                final String memberState = (String)temp.get("state");

                Button button= convertView.findViewById(R.id.state);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(memberState.equals("online"))
                            StartVideoCall(memberId );
                        else
                            System.out.println(memberName + " 离线，无法拨通");
                    }
                });
                return super.getView(position, convertView, parent);
            }
        };
        userList.setAdapter(adapter);
    }

    void StartVideoCall(String memberId){
        final String ID = memberId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                        Family.objectOutputStream.writeObject("StartCall");
                        System.out.println("向服务器请求视频通话");
                        Family.objectOutputStream.writeObject(ID);

                }catch (Exception e){
                    System.out.println("已释放锁");
                    e.printStackTrace();
                }
            }
        }).start();

    }

}
