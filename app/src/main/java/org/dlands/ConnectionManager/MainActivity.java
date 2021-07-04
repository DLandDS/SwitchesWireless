package org.dlands.ConnectionManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.taufiqurahman.ConnectionManager.R;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    //Data Sets
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()/2;
    DataSetServers dataSetServers = new DataSetServers();
    JSONInterface jsonInterface;
    FileManager fileManager;

    //View Control
    RecyclerView recyclerView;
    RowDataHandler rowDataHandler;
    SwipeRefreshLayout swipeRefreshLayout;

    Dialog dialog;
    EditText editTitle, editIP;
    Button saveButton, deleteButton;

    //Threading
    ThreadPoolExecutor threadPoolExecutor;
    Handler UIThreadHandler;
    ConnectionThreadLooper connectionThreadLooper = new ConnectionThreadLooper();
    ConnectionThreadLooper requestThreadLooper = new ConnectionThreadLooper();
    ConnectionSequence connectionSequence = new ConnectionSequence();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UIThreadHandler = new Handler(getMainLooper());

        //JSON File Setup
        fileManager = new FileManager(this);
        if(fileManager.exists()) {
            try {
                jsonInterface = new JSONInterface(fileManager.readFiletoString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Filenya tidak ada");
        }

        //ThreadPoolExecutor
        System.out.println("Number of cores : " + NUMBER_OF_CORES);
        threadPoolExecutor = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES * 2,
                5,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2));

        //RecyclerView Setup
        recyclerView = findViewById(R.id.recyclerView);
        rowDataHandler = new RowDataHandler(
                this,
                jsonInterface,
                fileManager,
                dataSetServers
        );
        recyclerView.setAdapter(rowDataHandler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            dataSetServers.stopThread();
            connectionThreadLooper.handler.post(new ConnectionSequence());
            while (dataSetServers.getThreadState()){}
            rowDataHandler.notifyDataSetChanged();
        });


        //Dialog New Devices Setup
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_edit_content);

        editTitle = dialog.findViewById(R.id.editTitlte);
        editTitle.setText(null);
        editTitle.setHint("Device Name");

        editIP = dialog.findViewById(R.id.editIP);
        editIP.setText(null);
        editIP.setHint("IP");

        deleteButton = dialog.findViewById(R.id.deleteButton);
        deleteButton.setVisibility(View.INVISIBLE);

        saveButton = dialog.findViewById(R.id.editButton);
        saveButton.setOnClickListener(view -> {
            jsonInterface.addNewObject(
                    editTitle.getText().toString(),
                    editIP.getText().toString(),
                    0
            );
            rowDataHandler.notifyDataSetChanged();
            dialog.dismiss();
            try {
                fileManager.writeString(jsonInterface.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        //ConnectionThreadLooper
        connectionThreadLooper.start();
        requestThreadLooper.start();
        connectionThreadLooper.waitUntilReady();
        connectionThreadLooper.handler.post(connectionSequence);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.newDevices:
                editIP.setText("");
                editTitle.setText("");
                dialog.show();
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    class ConnectionSequence implements Runnable {
        @Override
        public void run() {
            dataSetServers.setThreadStarted();
            swipeRefreshLayout.setRefreshing(true);
            //Load Devices
            for(int i = 0; i < jsonInterface.length(); i++){
                try {
                    dataSetServers.getSocket().add(new SocketInterface(jsonInterface.getString(i, "ip")));
                    dataSetServers.getState().add(DataSetServers.ESTABLISHED);
                } catch (NoRouteToHostException e){
                    dataSetServers.getSocket().add(null);
                    dataSetServers.getState().add(DataSetServers.UNREACHABLE);
                } catch (Exception e) {
                    dataSetServers.getSocket().add(null);
                    dataSetServers.getState().add(DataSetServers.FAILED);
                    e.printStackTrace();
                }
                dataSetServers.getPing().add(0);
                dataSetServers.getIsFree().add(true);
                recyclerView.post(new InitialObject(i));
                UIThreadHandler.post(() -> rowDataHandler.notifyDataSetChanged());
            }
            swipeRefreshLayout.setRefreshing(false);
            //System.out.println(dataSetServers.getThreadState()+"");

            //Devices Data Update
            while (dataSetServers.getThreadState()){
                //System.out.println("Data Update Started");
                for(int i = 0; i < jsonInterface.length(); i++) {
                    int index = i;
                    if(dataSetServers.getIsFree().get(index)){
                        dataSetServers.getIsFree().set(index, false);
                        threadPoolExecutor.execute(() -> {
                            //System.out.println("Thread " + Thread.currentThread().getId() + "  : Job " + index + " Started");
                            int ping = 0;
                            int state = dataSetServers.getState().get(index);
                            if(dataSetServers.getSocket().get(index) != null){
                                dataSetServers.getIsFree().set(index, false);
                                try {
                                    long startTime;
                                    startTime = System.currentTimeMillis();
                                    if(dataSetServers.getSocket().get(index) != null){
                                        dataSetServers.getSocket().get(index).send("s");
                                        state = "0".equalsIgnoreCase(dataSetServers.getSocket().get(index).read())?DataSetServers.LOW:DataSetServers.HIGH;
                                        ping = (int) (System.currentTimeMillis()-startTime);
                                    }
                                } catch (SocketTimeoutException e){
                                    state = DataSetServers.TIME_OUT;
                                    e.printStackTrace();
                                } catch (SocketException e){
                                    state = DataSetServers.ERROR;
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    state = DataSetServers.FAILED;
                                    e.printStackTrace();
                                }
                            }
                            try {
                                dataSetServers.getPing().set(index, ping);
                                dataSetServers.getState().set(index, state);
                                dataSetServers.getIsFree().set(index, true);
                            } catch (IndexOutOfBoundsException e){
                                e.printStackTrace();
                            }
                            //System.out.println("Thread " + Thread.currentThread().getId() + "  : Job " + index + " Ended");
                        });
                    }
                    UIThreadHandler.post(() -> rowDataHandler.notifyDataSetChanged());
                    //System.out.println("Data Update Ended");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            //Devices Close Connection and clearing
            for(int i = 0; i < jsonInterface.length(); i++){
                try {
                    if (dataSetServers.getSocket().get(i) != null) {
                        dataSetServers.getSocket().get(i).close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            dataSetServers.clear();
        }
    }

    static class ConnectionThreadLooper extends Thread {
        Handler handler;

        @Override
        public void run() {
            Looper.prepare();
            handler = new Handler(Looper.myLooper());
            Looper.loop();
        }
        void waitUntilReady(){
            while (handler == null){

            }
        }
    }


    class InitialObject implements Runnable {

        int position;
        Button button;

        InitialObject(int position) {
            this.position = position;
            recyclerView.post(()->{
                button = Objects.requireNonNull(
                        Objects.requireNonNull(recyclerView.getLayoutManager()).findViewByPosition(position)
                ).findViewById(R.id.ToggleSwitch);
            });
        }

        @Override
        public void run() {
            button.setOnClickListener(v -> {
                String messageSocket = null;
                System.out.println("Button Pressed");
                switch (dataSetServers.getState().get(position)) {
                    case DataSetServers.LOW:
                        messageSocket = SocketInterface.HIGH;
                        break;
                    case DataSetServers.HIGH:
                        messageSocket = SocketInterface.LOW;
                        break;
                }
                if (messageSocket != null) {
                    String finalMessageSocket = messageSocket;
                    requestThreadLooper.handler.post(() -> {
                        try {
                            dataSetServers.getSocket().get(position).send(finalMessageSocket);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
        }
    }

}