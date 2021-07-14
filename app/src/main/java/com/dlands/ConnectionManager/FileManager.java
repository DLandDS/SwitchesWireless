package com.dlands.ConnectionManager;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileManager extends File {

    Context context;

    FileManager(Context context){
        super(context.getFilesDir(), "data.json");
        this.context = context;
        if(!this.exists()){
            try {
                writeString("{" +
                            "devices:[" +
                            "]" +
                        "}");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public void writeString(String string) throws IOException {
        FileOutputStream fos = null;
        fos = context.openFileOutput(getName(), Context.MODE_PRIVATE);
        fos.write(string.getBytes());
    }


    public String readFiletoString() throws Exception {
        String ret = null;
        //Make sure you close all streams.
        FileInputStream fin = new FileInputStream(this);
        ret = convertStreamToString(fin);
        fin.close();
        return ret;
    }

}
