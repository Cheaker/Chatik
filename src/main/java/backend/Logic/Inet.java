package backend.Logic;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by 4iker on 03.07.17.
 */
public class Inet {

    private int id;

    private String currentUser;

    private Connection connection;

    private DataOutputStream sendToClient;

    private BufferedReader receiveFromClient;

    private State state;

    public Inet(BufferedReader br, DataOutputStream dos) {
        state = State.RECEPTION;
        connection = SQLConnection.getInstance().getConnection();
        sendToClient = dos;
        receiveFromClient = br;
    }

    public void react(String command) throws IOException {
        switch (command) {
            case "-q":
                //close everything
                try {
                    if (connection != null)
                        connection.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
                sendToClient.writeBytes("exit\n");
                break;
            case "-s":
                state = State.REGISTRATION;
                reactOnState();
                break;
            case "-l":
                if (loggedIn()) {
                    state = State.HOME;
                    reactOnState();
                    break;
                } else return;


            default:
                sendToClient.writeBytes("No such operation, try again\n");
                break;
        }
    }

    private boolean loggedIn() throws IOException {
        boolean loggedIn = false;
        while (!loggedIn) {
            String nick;
            String pass;
            sendToClient.writeBytes("Enter your nick or -q to quit: \n");
            nick = receiveFromClient.readLine();
            if (nick.equals("-q")) return false;
            sendToClient.writeBytes("Enter your password: \n");
            pass = receiveFromClient.readLine();

            Statement stmt = null;
            ResultSet rs = null;

            try {
                stmt = connection.createStatement();
                rs = stmt.executeQuery("SELECT * FROM USERS WHERE NAME ='" + nick + "' AND PASSWORT = '" + pass + "';");

                if (rs.next()) {
                    // check nick and pass in DB if right => loggedIn = true
                    loggedIn = true;
                    currentUser = nick;
                    id = rs.getInt("id");

                } else {
                    sendToClient.writeBytes("Wrong name or password. Try again\n");
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {

                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }


        }
        return true;
    }

    private void reactOnState() throws IOException {
        if (state == State.RECEPTION) return;
        else if (state == State.REGISTRATION) {
            signIn();
            return;
        } else if (state == State.HOME) {
            reactHome();
        }
    }

    private void reactHome() throws IOException {

        while (true) {
            sendToClient.writeBytes("Welcome home, " + currentUser + " with id = " + id +
                    ". Type -q to quit, -s to search friend, -f to list all friends, -c 'friends_name' to start chat with friend." +
                    " If you want to end chat, type -q \n");
            // look for unread messages and fr.requests

            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = connection.createStatement();
                rs = stmt.executeQuery("Select * from " + currentUser + "_notifications where is_message = false limit 1");
                if (rs.next()) {
                    sendToClient.writeBytes("You have some friend request(s). Check it with -r\n");
                }

                rs = stmt.executeQuery("Select distinct from_smbd from " + currentUser + "_notifications where is_message = true limit 1");

                sendToClient.writeBytes("You have at least 1 new message from: \n");
                while (rs.next()) {
                    sendToClient.writeBytes(rs.getString("from_smbd") + "\n");
                }
                sendToClient.writeBytes("\n");

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }


            String command[] = receiveFromClient.readLine().split(" ");
            switch (command[0]) {
                case "-r":
                    checkFriends();
                    break;
                case "-q":
                    return;
                case "-s":
                    searchFriend();
                    break;
                case "-f":
                    listFriends();
                    break;
                case "-c":
                    if (command.length == 2) {
                        while (openChat(command[1])) {

                        }
                    } else sendToClient.writeBytes("Wrong command\n");
                    break;
                default:
                    sendToClient.writeBytes("No such command, try again\n");
            }

        }
    }

    private boolean openChat(String person) throws IOException {
        if (!isFriend(person)) {
            sendToClient.writeBytes("This user is not in your friend list\n");
            return false;
        } else {
            String tableName = determineTable(person);
            String message;
            while (true) {
                printMessages(tableName, person);
                sendToClient.writeBytes(currentUser + ": \n");
                message = receiveFromClient.readLine();
                if (message.equals("-q"))
                    break;
                else
                    sendMessage(person, tableName, message);
            }
        }
        return false;
    }

    private void sendMessage(String person, String tableName, String message) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("insert into " + tableName + " (message, sender, received) values ('" + message + "', '" + currentUser + "',false); ");
            stmt.executeUpdate("insert into " + person + "_notifications (from_smbd, is_message) values ('" + currentUser + "',true);");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String determineTable(String person) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("select  id from users where name = '" + person + "';");
            rs.next();
            int id = rs.getInt("id");
            if (id < this.id) return person + "_" + currentUser + "_messages";
            else return currentUser + "_" + person + "_messages";
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void printMessages(String tableName, String person) {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("update " + tableName + " set received = true where sender = '" + person + "' and received = false; ");
            stmt.executeUpdate("delete from " + currentUser + "_notifications where is_message = true; ");
            rs = stmt.executeQuery("select * from (select * from " + tableName + " ORDER BY sent DESC limit 10) as foo ORDER BY sent ASC;");

            while (rs.next()) {
                String sender = rs.getString("sender");
                String message = rs.getString("message");
                String date = rs.getString("sent");
                sendToClient.writeBytes(sender + " at " + date + ": " + message + "\n");
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    private void checkFriends() {
        Statement stmt = null;
        Statement stmt2 = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            stmt2 = connection.createStatement();
            rs = stmt.executeQuery("Select distinct * from " + currentUser + "_notifications where is_message = false;");
            while (rs.next()) {
                String person = rs.getString("from_smbd");
                sendToClient.writeBytes("Do you accept friend request from " + person + "?\n");
                boolean answered = false;
                while (!answered) {
                    sendToClient.writeBytes("y/n\n");
                    String answer = receiveFromClient.readLine();
                    if (answer.equalsIgnoreCase("y")) {
                        answered = true;
                        sendToClient.writeBytes("You have accepted the request from " + person + "\n");
                        stmt2.executeUpdate("Insert into " + currentUser + "_friends (name) values ('" + person + "');");
                        stmt2.executeUpdate("Insert into " + person + "_friends (name) values ('" + currentUser + "');");
                        stmt2.executeUpdate("Create table " + determineTable(person) + " " +
                                "(sender varchar(40), message text, " +
                                "sent timestamp with time zone default now(), received boolean);");
                    } else if (answer.equalsIgnoreCase("n")) {
                        sendToClient.writeBytes("Okay, no is no\n");
                        answered = true;
                    } else sendToClient.writeBytes("Type please y or n\n");
                    stmt2.executeUpdate("Delete from " + currentUser + "_notifications where from_smbd ='" + person + "';");
                }
            }

            sendToClient.writeBytes("You have not any requests\n");

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (stmt2 != null) {
                    stmt2.close();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    private void listFriends() throws IOException {
        sendToClient.writeBytes("Your friends are:\n");
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("select * from " + currentUser + "_friends");
            while (rs.next()) {
                sendToClient.writeBytes(rs.getString("name") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void searchFriend() throws IOException {
        boolean found = false;
        while (!found) {
            sendToClient.writeBytes("Type nickname of your friend or -q to go back\n");
            String name = receiveFromClient.readLine();
            if (name.equals("-q")) return;
            else {
                // find in DB. if found found = true, ask to send friend request
                Statement stmt = null;
                ResultSet rs = null;
                try {
                    stmt = connection.createStatement();
                    rs = stmt.executeQuery("SELECT * FROM USERS WHERE  NAME ='" + name + "';");
                    if (rs.next()) {
                        String person = rs.getString("name");
                        sendToClient.writeBytes("We found user " + person + " with id = " + rs.getInt("id") + "\n");
                        found = true;
                        boolean answered = false;
                        if (isFriend(person)) {
                            sendToClient.writeBytes("And this user is already your friend\n");
                            answered = true;
                        }
                        while (!answered) {
                            sendToClient.writeBytes("Do you want to send friend request? y/n \n");
                            String answer = receiveFromClient.readLine();
                            if (answer.equalsIgnoreCase("y")) {
                                answered = true;
                                stmt.executeUpdate("INSERT INTO " + person + "_notifications (from_smbd, is_message) values ('" + currentUser + "', false);");
                                sendToClient.writeBytes("You have sent the request to " + person + "\n");
                            } else if (answer.equalsIgnoreCase("n")) {
                                answered = true;
                                sendToClient.writeBytes("Okay, no is no\n");
                            } else sendToClient.writeBytes("Type please y or n\n");
                        }
                    } else sendToClient.writeBytes("No such user with mark " + name + "\n");
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (rs != null) {
                            rs.close();
                        }
                        if (stmt != null) {
                            stmt.close();
                        }

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean isFriend(String person) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("SELECT * FROM " + currentUser + "_friends WHERE  NAME ='" + person + "';");
            if (rs.next()) return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void signIn() throws IOException {

        String nickname;
        sendToClient.writeBytes("Choose your nickname: \n");
        nickname = receiveFromClient.readLine();
        //check for distinction
        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("SELECT * FROM USERS WHERE NAME ='" + nickname + "';");
            if (rs.next()) {
                sendToClient.writeBytes("There is already user with this nickname, try again\n");
                return;
            }


            sendToClient.writeBytes("Choose password:\n");
            String password = receiveFromClient.readLine();
            sendToClient.writeBytes("Repeat the password: \n");
            if (password.equals(receiveFromClient.readLine())) {
                // add to database

                stmt.executeUpdate("INSERT INTO USERS (NAME, PASSWORT) VALUES ('" + nickname + "', '" + password + "');");

                stmt.executeUpdate("CREATE TABLE " + nickname + "_notifications (from_smbd varchar(40), is_message boolean);");
                stmt.executeUpdate("CREATE TABLE " + nickname + "_FRIENDS (id integer, name varchar(40) primary key);");

                sendToClient.writeBytes("Registration succeed. Please log in now\n");
            } else {
                sendToClient.writeBytes("Passwords are not the same. Please try again\n");
                state = State.RECEPTION;
                reactOnState();
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
