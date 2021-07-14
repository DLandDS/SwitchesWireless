package com.dlands.ConnectionManager;

import java.util.ArrayList;
import java.util.List;

public class DataSetServers {
    public final static int NOTREADY = -1;
    public final static int LOW = 0;
    public final static int HIGH = 1;
    public final static int TIME_OUT = 2;
    public final static int UNREACHABLE = 3;
    public final static int ERROR = 4;
    public static final int ESTABLISHED = 5;
    public final static int FAILED = 9;
    public static final int REFUSED = 6;

    private List<SocketInterface> serverSocket;
    private List<Integer> serverPing;
    private List<Integer> serverState;
    private List<Boolean> isFree;

    private boolean threadState;
    private boolean isAlive;

    DataSetServers(){
        serverSocket = new ArrayList<>();
        serverPing = new ArrayList<>();
        serverState = new ArrayList<>();
        isFree = new ArrayList<>();
    }

    public List<SocketInterface> getSocket(){return serverSocket;}
    public List<Integer> getPing(){return serverPing;}
    public List<Integer> getState(){return serverState;}
    public List<Boolean> getIsFree(){return isFree;}

    //Thread Controller
    public boolean getThreadState(){return threadState;}
    public void startThread(){threadState = true;}
    public void stopThread(){threadState = false;}
    public void setAlive(boolean state){isAlive = state;}
    public void waitUntilAlive(){ while (!isAlive){delay(100);} }
    public void waitUntilDead(){ while (isAlive) {delay(100);} }
    public void waitUntilFree(int index){
        try {
            while (!getIsFree().get(index)){
                delay(100);
            }
        } catch (IndexOutOfBoundsException e) {

        }
    }
    public void waitUntilAllFree(){
        for(int i = 0; getIsFree().size() < i; i++){
            waitUntilFree(i);
        }
    }
    public void threadRestartSequence(){
        waitUntilAllFree();
        stopThread();
        waitUntilDead();
    }

    public void clear(){
        serverSocket.clear();
        serverPing.clear();
        serverState.clear();
        isFree.clear();
    }

    private void delay(long milis){
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class Color {
        public final static int HIGH     = android.graphics.Color.parseColor("#FF6200EE");
        public final static int LOW      = android.graphics.Color.parseColor("#ffff4444");
        public final static int DANGER   = android.graphics.Color.parseColor("#dc3545");
        public final static int DEFAULT  = android.graphics.Color.parseColor("#8a000000");
    }

}
