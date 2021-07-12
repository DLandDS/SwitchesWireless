package org.dlands.ConnectionManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.taufiqurahman.ConnectionManager.R;

import org.json.JSONException;

import java.io.IOException;

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
        try {
            holder.tv_title.setText(listDevices.index(position).getString("title"));
            holder.tv_ip.setText(
                    listDevices.index(position).getInt("port")==8888?
                            listDevices.index(position).getString("ip")
                            :listDevices.index(position).getString("ip") + ":"
                                + listDevices.index(position).getString("port"));
        } catch (JSONException e) {
            holder.tv_title.setText("Title not Found");
            holder.tv_ip.setText("IP not found");
            holder.tv_title.setTextColor(DataSetServers.ERROR);
            holder.tv_ip.setTextColor(DataSetServers.ERROR);
            e.printStackTrace();
        }


        //Tap to Row
        holder.constraintLayout.setOnClickListener(view -> {
            holder.dialog.show();
            //holder.d_img_icon.setImageResource(image[position]);
            try {
                holder.d_et_title.setText(listDevices.index(position).getString("title"));
                holder.d_et_ip.setText(listDevices.index(position).getString("ip"));
                holder.d_et_port.setText(listDevices.index(position).getString("port"));
                holder.cb_customPort.setChecked(listDevices.index(position).getInt("port")!=8888);
            } catch (JSONException e) {
                Toast.makeText(context, "Data not found!", Toast.LENGTH_SHORT).show();
                holder.dialog.dismiss();
                e.printStackTrace();
            }

        });

        holder.cb_customPort.setOnClickListener(v -> {
            holder.d_et_port.setVisibility(holder.cb_customPort.isChecked()?View.VISIBLE:View.GONE);
        });

        //Delete Button on Dialog
        holder.d_delete.setOnClickListener(view -> {
            AlertDialog.Builder deleteConfirm = new AlertDialog.Builder(context);
            deleteConfirm.setMessage("Hapus devices ini?").setCancelable(false).setPositiveButton("Ya", (dialogInterface, i) -> {
                dataSetServers.stopThread();
                dataSetServers.waitUntilDead();
                listDevices.remove(position);
                dataSetServers.startThread();
                dialogInterface.dismiss();
                holder.dialog.dismiss();
                try {
                    fileManager.writeString(listDevices.toString());
                    Toast.makeText(context, "Deleting Success!", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(context, "Deleting Failed!", Toast.LENGTH_SHORT).show();
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
            dataSetServers.waitUntilDead();
            listDevices.setData(position,"icon", 0);
            listDevices.setData(position, "title", holder.d_et_title.getText().toString());
            listDevices.setData(position,"ip", holder.d_et_ip.getText().toString());
            listDevices.setData(position, "port", holder.cb_customPort.isChecked()?
                    holder.d_et_port.getText().toString()
                    :"8888");
            dataSetServers.startThread();
            try {
                fileManager.writeString(listDevices.toString());
                Toast.makeText(context, "Editing Success!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(context, "Editing Failed!", Toast.LENGTH_SHORT).show();
            }
            holder.dialog.dismiss();
        });

        //Switch Button for NodeMCU
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
        EditText d_et_title, d_et_ip, d_et_port;
        ImageView d_img_icon;
        Button d_save, d_delete, toggle;
        CheckBox cb_customPort;

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
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            d_et_ip = dialog.findViewById(R.id.editIP);
            d_et_title = dialog.findViewById(R.id.editTitle);
            d_et_port = dialog.findViewById(R.id.editPort);
            d_img_icon = dialog.findViewById(R.id.editImage);
            d_save = dialog.findViewById(R.id.editButton);
            d_delete = dialog.findViewById(R.id.deleteButton);
            cb_customPort = dialog.findViewById(R.id.custom_port);
        }
    }
}
