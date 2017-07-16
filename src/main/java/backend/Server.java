package backend;

import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Wladimir on 09.07.2017.
 */
public class Server {
    public static int redirectPort = 0;

    public static void main(String[] args) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(1234);
        System.out.println(welcomeSocket.getInetAddress());

        while (true) {
            System.out.println("listening on port: " + welcomeSocket.getLocalPort());
            Socket connectionSocket = welcomeSocket.accept();
            System.out.println(connectionSocket.getInetAddress());
            System.out.println("established");
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            Redirect rd = new Redirect();
            System.out.println("sending to client port to redirect " + redirectPort);
            outToClient.writeBytes(String.valueOf(redirectPort + '\n'));
            rd.start();
            connectionSocket.close();
        }
    }
}
