package org.dlands.ConnectionManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketInterface extends Socket {

    public static final String LOW = "0";
    public static final String HIGH = "1";

    public SocketInterface(String ip, int port) throws IOException {
        super(ip, port);
    }

    public void send(String message) throws IOException {
        DataOutputStream dataOutputStream = null;
            dataOutputStream = new DataOutputStream(this.getOutputStream());
            dataOutputStream.writeUTF(message);
            //System.out.println("message sent : " + message);
            dataOutputStream.flush();
    }

    public String read() throws IOException {
        String message = null;
        DataInputStream dataInputStream = new DataInputStream(this.getInputStream());
        return dataInputStream.readLine();
    }
}
