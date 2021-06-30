package org.dlands.ConnectionManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class SocketInterface extends Socket {

    public SocketInterface(String ip) throws IOException {
        super(ip, 8888);
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
