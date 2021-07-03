package org.dlands.ConnectionManager;

import java.util.ArrayList;
import java.util.List;

public class DataSetServers {
    List<SocketInterface> serverSocket;
    List<Integer> serverPing;
    List<String> serverState;

    DataSetServers(){
        serverSocket = new ArrayList<>();
        serverPing = new ArrayList<>();
        serverState = new ArrayList<>();
    }

    public List<SocketInterface> getSocket(){return serverSocket;}
    public List<Integer> getPing(){return serverPing;}
    public List<String> getState(){return serverState;}
}
