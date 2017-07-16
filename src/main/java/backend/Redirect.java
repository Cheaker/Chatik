package backend;

import backend.Logic.Inet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Wladimir on 11.07.2017.
 */
public class Redirect extends Thread {

    private ServerSocket ss;

    Redirect() throws IOException {
        ss = new ServerSocket(0);
        ss.setSoTimeout(10000);
        Server.redirectPort = ss.getLocalPort();
        System.out.println("creating new server socket on port " + Server.redirectPort);

    }

    @Override
    public void run() {
        super.run();
        try {
            System.out.println("waiting for connection");
            Socket socket = ss.accept();
            System.out.println("connection established");
            proceed(socket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void proceed(Socket socket) {
        System.out.println("proceed");

        String command = "";

        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
            Inet network = new Inet(inFromClient, outToClient);

            while (true) {

                outToClient.writeBytes("Welcome at reception. -q to quit, -s to sign up, -l to log in\n");
                outToClient.flush();


                command = inFromClient.readLine();

                network.react(command);

                if (command.equals("-q")) break;

            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                System.out.println("connection closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
