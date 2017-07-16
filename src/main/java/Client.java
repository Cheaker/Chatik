import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;

/**
 * Created by Wladimir on 09.07.2017.
 */
public class Client {
    private static final int port = 1234;
    private static final String host = "localhost";
    private static boolean onlineState = false;

    public static boolean isOnlineState() {
        return onlineState;
    }

    public static void setOnlineState(boolean onlineState) {
        Client.onlineState = onlineState;
    }

    public static void main(String argv[]) {
        Socket toMainPort = null;
        Socket clientSocket = null;
        try {
            toMainPort = new Socket(host, port);
            System.out.println("connected to 1234");
            BufferedReader toMainServ = new BufferedReader(new InputStreamReader(toMainPort.getInputStream()));
            System.out.println("receiving port");
            int port = Integer.parseInt(toMainServ.readLine()) - 10;
            toMainPort.close();
            toMainServ.close();

            System.out.println("creating new socket with port " + port);


            clientSocket = new Socket("localhost", port);

            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            ClientWriting output = new ClientWriting(outToServer);

            onlineState = true;

            output.start();

            while (Client.isOnlineState()) {
                try {
                    String in = inFromServer.readLine();
                    if (in != null) {
                        if (in.equals("exit")) {
                            System.out.println("exit arrived");
                            Client.setOnlineState(false);
                            break;
                        }
                        System.out.println(in);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Session ended or failed");
                    try {
                        inFromServer.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }

        } catch (ConnectException ce) {
            System.out.println("Connection to main server failed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
