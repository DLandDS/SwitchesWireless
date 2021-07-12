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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.taufiqurahman.ConnectionManager.R;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
    EditText editTitle, editIP, editPort;
    Button saveButton, deleteButton;
    CheckBox customPort;

    //Threading
    ThreadPoolExecutor threadPoolExecutor;
    Handler UIThreadHandler;
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
            dataSetServers.threadRestartSequence();
        });


        //Dialog New Devices Setup
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_edit_content);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        editTitle = dialog.findViewById(R.id.editTitle);
        editIP = dialog.findViewById(R.id.editIP);
        editPort = dialog.findViewById(R.id.editPort);

        deleteButton = dialog.findViewById(R.id.deleteButton);
        deleteButton.setVisibility(View.INVISIBLE);

        customPort = dialog.findViewById(R.id.custom_port);
        customPort.setOnClickListener((view)->{
            editPort.setVisibility(customPort.isChecked()? View.VISIBLE: View.GONE);
        });

        saveButton = dialog.findViewById(R.id.editButton);
        saveButton.setOnClickListener(view -> {
            dataSetServers.stopThread();
            dataSetServers.waitUntilDead();
            jsonInterface.addNewObject(
                    editTitle.getText().toString(),
                    editIP.getText().toString(),
                    customPort.isChecked()?Integer.parseInt(editPort.getText().toString()):8888,
                    0
            );
            dialog.dismiss();
            try {
                fileManager.writeString(jsonInterface.toString());
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Saving device failed!", Toast.LENGTH_SHORT).show();
            }
            dataSetServers.startThread();
        });
        //ConnectionThreadLooper
        dataSetServers.startThread();
        new Thread(()->{
            while (true){
                if(!dataSetServers.getThreadState()){
                    continue;
                }
                dataSetServers.setAlive(true);
                connectionSequence.run();
                dataSetServers.setAlive(false);
            }
        }).start();

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
                editPort.setText("");
                editPort.setVisibility(View.GONE);
                customPort.setChecked(false);
                dialog.show();
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    class ConnectionSequence implements Runnable {
        TextView ping[];
        Button button[];

        @Override
        public void run() {
            UIThreadHandler.post(()->{
                rowDataHandler.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(true);
            });


            ping = new TextView[jsonInterface.length()];
            button = new Button[jsonInterface.length()];
            //Load Devices

            for(int i = 0; i < jsonInterface.length(); i++){
            
                try {
                    dataSetServers.getSocket().add(new SocketInterface(jsonInterface.index(i).getString("ip"), 8888));
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

                //Get Address Element
                int finalI = i;
                recyclerView.post(()->{
                    ping[finalI] = recyclerView.getLayoutManager().findViewByPosition(finalI).findViewById(R.id.ping);
                    button[finalI] = recyclerView.getLayoutManager().findViewByPosition(finalI).findViewById(R.id.ToggleSwitch);
                });
            }
            UIThreadHandler.post(()->{swipeRefreshLayout.setRefreshing(false);});

            
            //Devices Data Update
            while (dataSetServers.getThreadState()){
                //System.out.println("Data Update Started");
                //System.out.println("Thread state : " + dataSetServers.getThreadState());
                for(int i = 0; i < jsonInterface.length(); i++) {
                    int index = i;
                    if(dataSetServers.getIsFree().get(index)){
                        dataSetServers.getIsFree().set(index, false);
                        int finalI = i;
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
                                    //e.printStackTrace();
                                } catch (SocketException e){
                                    state = DataSetServers.ERROR;
                                    //e.printStackTrace();
                                } catch (IOException e) {
                                    state = DataSetServers.FAILED;
                                    //e.printStackTrace();
                                }
                            }
                            try {
                                dataSetServers.getPing().set(index, ping);
                                dataSetServers.getState().set(index, state);
                                dataSetServers.getIsFree().set(index, true);
                            } catch (IndexOutOfBoundsException e){
                                e.printStackTrace();
                            }
                            recyclerView.post(()->{
                                try {
                                    setState(finalI);
                                } catch (IndexOutOfBoundsException e){

                                } catch (NullPointerException e){

                                }
                            });
                            //System.out.println("Thread " + Thread.currentThread().getId() + "  : Job " + index + " Ended");
                        });
                    }
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
        
        private void setState(int position) throws IndexOutOfBoundsException{
            switch (dataSetServers.getState().get(position)){
                case DataSetServers.NOTREADY:
                    ping[position].setText("Loading...");
                    ping[position].setTextColor(DataSetServers.Color.DEFAULT);
                    button[position].setVisibility(View.GONE);
                    break;
                case DataSetServers.LOW:
                    ping[position].setTextColor(DataSetServers.Color.DEFAULT);
                    ping[position].setText(dataSetServers.getPing().get(position) + " ms");
                    button[position].setText("OFF");
                    button[position].setBackgroundColor(DataSetServers.Color.LOW);
                    button[position].setVisibility(View.VISIBLE);
                    break;
                case DataSetServers.HIGH:
                    ping[position].setTextColor(DataSetServers.Color.DEFAULT);
                    ping[position].setText(dataSetServers.getPing().get(position) + " ms");
                    button[position].setText("ON");
                    button[position].setBackgroundColor(DataSetServers.Color.HIGH);
                    button[position].setVisibility(View.VISIBLE);
                    break;
                case DataSetServers.TIME_OUT:
                    ping[position].setTextColor(DataSetServers.Color.DANGER);
                    ping[position].setText("Time Out");
                    button[position].setVisibility(View.GONE);
                    break;
                case DataSetServers.UNREACHABLE:
                    ping[position].setTextColor(DataSetServers.Color.DANGER);
                    ping[position].setText("Unreachable");
                    button[position].setVisibility(View.GONE);
                    break;
                case DataSetServers.ERROR:
                    ping[position].setTextColor(DataSetServers.Color.DANGER);
                    ping[position].setText("Error");
                    button[position].setVisibility(View.GONE);
                    break;
                case DataSetServers.ESTABLISHED:
                    ping[position].setTextColor(DataSetServers.Color.DEFAULT);
                    ping[position].setText("Connected");
                    button[position].setVisibility(View.GONE);
                    break;
                case DataSetServers.FAILED:
                    ping[position].setTextColor(DataSetServers.Color.DANGER);
                    ping[position].setText("Failed");
                    button[position].setVisibility(View.GONE);
                    break;
            }
        } 

    }


}