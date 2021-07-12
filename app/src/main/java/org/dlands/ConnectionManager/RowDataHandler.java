package org.dlands.ConnectionManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.taufiqurahman.ConnectionManager.R;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

public class RowDataHandler extends RecyclerView.Adapter<RowDataHandler.myViewHolder> {

    FileManager fileManager;
    JSONInterface listDevices;
    Context context;
    DataSetServers dataSetServers;
    ConnectionThreadLooper requestThreadLooper;

    RowDataHandler(Context context,
                   JSONInterface jsonInterface,
                   FileManager file,
                   DataSetServers dataSetServers){
        this.listDevices = jsonInterface;
        this.fileManager = file;
        this.context = context;
        this.dataSetServers = dataSetServers;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        requestThreadLooper = new ConnectionThreadLooper();
        requestThreadLooper.start();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View veiw = layoutInflater.inflate(R.layout.row_list, parent, false);
        return new myViewHolder(veiw);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        //holder.img_icon.setImageResource(image[position]);
        holder.tv_title.setText(listDevices.getString(position, "title"));
        holder.tv_ip.setText(listDevices.getString(position, "ip"));

        //Tap to Row
        holder.constraintLayout.setOnClickListener(view -> {
            holder.dialog.show();
            //holder.d_img_icon.setImageResource(image[position]);
            holder.d_et_title.setText(listDevices.getString(position, "title"));
            holder.d_et_ip.setText(listDevices.getString(position, "ip"));
        });

        //Delete Button on Dialog
        holder.d_delete.setOnClickListener(view -> {
            AlertDialog.Builder deleteConfirm = new AlertDialog.Builder(context);
            deleteConfirm.setMessage("Hapus devices ini?").setCancelable(false).setPositiveButton("Ya", (dialogInterface, i) -> {
                dataSetServers.stopThread();
                dataSetServers.waitUntilDead();
                listDevices.deleteObject(position);
                dataSetServers.startThread();
                dialogInterface.dismiss();
                holder.dialog.dismiss();
                try {
                    fileManager.writeString(listDevices.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            })
            .setNegativeButton("Tidak", (dialogInterface, i) -> dialogInterface.cancel());
            deleteConfirm.setTitle("Hapus");
            deleteConfirm.show();
        });

        //Save Button on Dialog
        holder.d_save.setOnClickListener(view -> {
            dataSetServers.stopThread();
            listDevices.setString(position,"title", holder.d_et_title.getText().toString());
            listDevices.setString(position,"ip", holder.d_et_ip.getText().toString());
            dataSetServers.waitUntilDead();
            dataSetServers.startThread();
            try {
                fileManager.writeString(listDevices.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            holder.dialog.dismiss();
        });

        holder.toggle.setOnClickListener(v -> {
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

        //Ping
        /*
        try {
            //System.out.println("Devices " + position + " : " + dataSetServers.getState().get(position) + " " + dataSetServers.getPing().get(position) + " ms ");
            switch (dataSetServers.getState().get(position)){
                case DataSetServers.NOTREADY:
                    holder.ping.setText("Loading...");
                    holder.ping.setTextColor(DataSetServers.Color.DEFAULT);
                    holder.toggle.setVisibility(View.GONE);
                    break;
                case DataSetServers.LOW:
                    holder.ping.setTextColor(DataSetServers.Color.DEFAULT);
                    holder.ping.setText(dataSetServers.getPing().get(position) + " ms");
                    holder.toggle.setText("OFF");
                    holder.toggle.setBackgroundColor(DataSetServers.Color.LOW);
                    holder.toggle.setVisibility(View.VISIBLE);
                    break;
                case DataSetServers.HIGH:
                    holder.ping.setTextColor(DataSetServers.Color.DEFAULT);
                    holder.ping.setText(dataSetServers.getPing().get(position) + " ms");
                    holder.toggle.setText("ON");
                    holder.toggle.setBackgroundColor(DataSetServers.Color.HIGH);
                    holder.toggle.setVisibility(View.VISIBLE);
                    break;
                case DataSetServers.TIME_OUT:
                    holder.ping.setTextColor(DataSetServers.Color.DANGER);
                    holder.ping.setText("Time Out");
                    holder.toggle.setVisibility(View.GONE);
                    break;
                case DataSetServers.UNREACHABLE:
                    holder.ping.setTextColor(DataSetServers.Color.DANGER);
                    holder.ping.setText("Unreachable");
                    holder.toggle.setVisibility(View.GONE);
                    break;
                case DataSetServers.ERROR:
                    holder.ping.setTextColor(DataSetServers.Color.DANGER);
                    holder.ping.setText("Error");
                    holder.toggle.setVisibility(View.GONE);
                    break;
                case DataSetServers.ESTABLISHED:
                    holder.ping.setTextColor(DataSetServers.Color.DEFAULT);
                    holder.ping.setText("Connected");
                    holder.toggle.setVisibility(View.GONE);
                    break;
                case DataSetServers.FAILED:
                    holder.ping.setTextColor(DataSetServers.Color.DANGER);
                    holder.ping.setText("Failed");
                    holder.toggle.setVisibility(View.GONE);
                    break;
            }
        } catch (IndexOutOfBoundsException e){

        } catch (NullPointerException e){

        }
         */

}

    @Override
    public int getItemCount() {
        return listDevices.length();
    }

    public class myViewHolder extends RecyclerView.ViewHolder {

        //Row Variable
        TextView tv_title, tv_ip, ping;
        ImageView img_icon;
        Dialog dialog;
        ConstraintLayout constraintLayout;

        //Dialog Varable
        EditText d_et_title, d_et_ip;
        ImageView d_img_icon;
        Button d_save, d_delete, toggle;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);

            //RecyclerView Setup
            constraintLayout = itemView.findViewById(R.id.rowList);
            tv_ip = itemView.findViewById(R.id.ip);
            tv_title = itemView.findViewById(R.id.title);
            img_icon = itemView.findViewById(R.id.icon);
            toggle = itemView.findViewById(R.id.ToggleSwitch);

            //Edit Dialog Setup
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.activity_edit_content);
            d_et_ip = dialog.findViewById(R.id.editIP);
            d_et_title = dialog.findViewById(R.id.editTitlte);
            d_img_icon = dialog.findViewById(R.id.editImage);
            d_save = dialog.findViewById(R.id.editButton);
            d_delete = dialog.findViewById(R.id.deleteButton);
        }
    }
}
