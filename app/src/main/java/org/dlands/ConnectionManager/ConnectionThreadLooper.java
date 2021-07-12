package org.dlands.ConnectionManager;

import android.os.Handler;
import android.os.Looper;


public class ConnectionThreadLooper extends Thread {
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