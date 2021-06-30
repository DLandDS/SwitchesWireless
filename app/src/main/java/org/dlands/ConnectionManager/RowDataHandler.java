package org.dlands.ConnectionManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class RowDataHandler extends RecyclerView.Adapter<RowDataHandler.myViewHolder> {

    FileManager fileManager;
    JSONInterface listDevices;
    Context context;

    RowDataHandler(Context ct, JSONInterface jsonInterface, FileManager file){
        listDevices = jsonInterface;
        fileManager = file;
        context = ct;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.dialog.show();
                //holder.d_img_icon.setImageResource(image[position]);
                holder.d_et_title.setText(listDevices.getString(position, "title"));
                holder.d_et_ip.setText(listDevices.getString(position, "ip"));
            }
        });

        //Delete Button on Dialog
        holder.d_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder deleteConfirm = new AlertDialog.Builder(context);
                deleteConfirm.setMessage("Hapus devices ini?").setCancelable(false).setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listDevices.deleteObject(position);
                        notifyDataSetChanged();
                        dialogInterface.dismiss();
                        holder.dialog.dismiss();
                        try {
                            fileManager.writeString(listDevices.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                deleteConfirm.setTitle("Hapus");
                deleteConfirm.show();
            }
        });

        //Save Button on Dialog
        holder.d_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listDevices.setString(position,"title", holder.d_et_title.getText().toString());
                listDevices.setString(position,"ip", holder.d_et_ip.getText().toString());
                notifyDataSetChanged();
                try {
                    fileManager.writeString(listDevices.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                holder.dialog.dismiss();
            }
        });

    }

    @Override
    public int getItemCount() {
        return listDevices.length();
    }

    public class myViewHolder extends RecyclerView.ViewHolder {

        //Row Global Variable
        TextView tv_title, tv_ip;
        ImageView img_icon;
        Dialog dialog;
        ConstraintLayout constraintLayout;

        //Dialog Global Varable
        EditText d_et_title, d_et_ip;
        ImageView d_img_icon;
        Button d_save, d_delete;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            //RecyclerView Setup
            constraintLayout = itemView.findViewById(R.id.rowList);
            tv_ip = itemView.findViewById(R.id.ip);
            tv_title = itemView.findViewById(R.id.title);
            img_icon = itemView.findViewById(R.id.icon);

            //Dialog Setup
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
