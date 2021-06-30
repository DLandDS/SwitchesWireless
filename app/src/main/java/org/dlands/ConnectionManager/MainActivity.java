package org.dlands.ConnectionManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.taufiqurahman.ConnectionManager.R;

import org.json.JSONException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    List<SocketInterface> server = new ArrayList<SocketInterface>();
    JSONInterface jsonInterface;
    FileManager fileManager;
    RecyclerView recyclerView;

    Dialog dialog;
    EditText editTitle, editIP;
    Button saveButton, deleteButton;

    ThreadPoolExecutor threadPoolExecutor;

    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()/2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //JSON File Setup
        fileManager = new FileManager(this);
        if(fileManager.exists()) {
            try {
                System.out.println("Filenya ada : " + fileManager.readFiletoString());
                jsonInterface = new JSONInterface(fileManager.readFiletoString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Filenya tidak ada");
        }

        //RecyclerView Setup
        recyclerView = findViewById(R.id.recyclerView);
        RowDataHandler rowDataHandler = new RowDataHandler(this, jsonInterface, fileManager);
        recyclerView.setAdapter(rowDataHandler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });

        //ThreadPoolExecutor
        System.out.println("Number of cores : " + NUMBER_OF_CORES);
        threadPoolExecutor = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES * 2,
                5,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(10));

        //Ping Update Loop
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Thread Started");
                for(int i = 0; i < jsonInterface.length(); i++){
                    try {
                        server.add(i, new SocketInterface(jsonInterface.getString(i, "ip")));
                    } catch (IOException e) {
                        server.add(i,null);
                        e.printStackTrace();
                    }
                }
                while (true){
                    for(int i = 0; i < jsonInterface.length(); i++) {
                        int index = i;
                        threadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("Thread " + Thread.currentThread().getId() + "  : Job " + index);
                                if(server.get(index) != null){
                                    try {
                                        long startTime;
                                        String state = null;
                                        startTime = System.currentTimeMillis();
                                        //recyclerView.post(new LayoutUpdate(index).message(((long) System.currentTimeMillis() - startTime) + " ms"));
                                        if(server.get(index) != null){
                                            server.get(index).send("s");
                                            state = server.get(index).read();
                                            recyclerView.post(new LayoutUpdate(index).message(((long) System.currentTimeMillis() - startTime) + " ms"));
                                            recyclerView.post(new LayoutUpdate(index).buttonStatus(state));
                                        }
                                        //System.out.println("State : " + state);
                                    } catch (SocketTimeoutException e){
                                        recyclerView.post(new LayoutUpdate(index).errorMessage("Disconnected"));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    recyclerView.post(new LayoutUpdate(index).errorMessage("Failed"));
                                }
                            }
                        });
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

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

    class LayoutUpdate {

        TextView ping;
        Button button;

        LayoutUpdate(int position){
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    ping = recyclerView.getLayoutManager().findViewByPosition(position).findViewById(R.id.ping);
                    button = recyclerView.getLayoutManager().findViewByPosition(position).findViewById(R.id.ToggleSwitch);
                }
            });
        }



        Runnable errorMessage(String message){
            return new Runnable() {
                @Override
                public void run() {
                    ping.setText(message);
                    ping.setTextColor(Color.parseColor("#ff0000"));
                    button.setVisibility(View.GONE);
                }
            };
        }

        Runnable message(String message){
            return new Runnable() {
                @Override
                public void run() {
                    ping.setText(message);
                }
            };
        }

        Runnable buttonStatus(String State) {
            return new Runnable() {
                @Override
                public void run() {
                    button.setVisibility(View.VISIBLE);
                    if (State.equalsIgnoreCase("0")){
                        button.setBackgroundColor(Color.parseColor("#ffff4444"));
                        button.setText("OFF");
                    } else if (State.equalsIgnoreCase("1")){
                        button.setBackgroundColor(Color.parseColor("#ff0099cc"));
                        button.setText("ON");
                    } else {
                        button.setVisibility(View.GONE);
                    }
                }
            };
        }
    }

}