import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Wladimir on 12.07.2017.
 */
public class ClientWriting extends Thread {
    private DataOutputStream dos;

    ClientWriting(DataOutputStream dos) {
        this.dos = dos;
    }

    @Override
    public void run() {
        super.run();
        Scanner sc = new Scanner(System.in);
        while (Client.isOnlineState()) {
            try {
                String command = sc.nextLine();
                if (Client.isOnlineState())
                    dos.writeBytes(command + '\n');
                else {
                    System.out.println("session closed");
                    try {
                        dos.close();
                        sc.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                Thread.sleep(500);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
