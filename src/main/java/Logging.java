import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Wladimir on 10.07.2017.
 */
public class Logging {
    static void printTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/YYY HH:mm:ss.SSS");
        Date date = new Date();
        System.out.println(dateFormat.format(date));
    }
}
