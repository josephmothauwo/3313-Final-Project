import java.util.*;
import java.io.*;
import java.net.*;

// Define Client Class

public class Client {

    private ObjectInputStream client_input; //
    private ObjectOutputStream client_output;
    private Socket socket;
    private String server, client_name;
    private int PORT;
    public static boolean closeAllSockets;

    private void logout() { // Error thrown, then client connection is ended
        System.out.println("logging out...");
        try {
            client_input.close();
        } catch (Exception error) {
        }

        try {
            client_output.close();
        } catch (Exception error) {

        }

        try {
            socket.close();
        } catch (Exception error) {
        }
    }

    public String get_client_name() {
        return client_name;
    }

    public void set_client_name(String username) {
        this.client_name = username;
    }

    void userOutput(String message) {
        try {
            client_output.writeObject(message);
        } catch (IOException error) {
            System.out.println("There has been an error" + error);
        }

    }

    // A client object constructor takes in a server, port number and a client name
    Client(String server, int PORT, String client_name) {
        this.server = server;
        this.PORT = PORT;
        this.client_name = client_name;
        closeAllSockets = false;
    }

    public String init_connection() {

        try { // Start off with a try catch block as recommended in Lab 3
            socket = new Socket(server, PORT);
        } catch (Exception error) {
            return ("Connection Refused" + error);
        }

        try { // Try catch block to establish data streams
            client_input = new ObjectInputStream(socket.getInputStream());
            client_output = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException error) {
            return ("Data Streams Failed" + error);
        }

        System.out.println("Connection Accepted"); // Everything is all good and there is communcation between
                                                   // client and server

        new ServerResponse().start(); // Start a new thread for listening to

        try {
            client_output.writeObject(client_name); // send to server the username
        } catch (IOException error) {
            System.out.println("Could Not Register Username");
            logout();
            System.out.println("Disconnected");
            return ("Disconnected");
        }

        // incoming clients
        String response = "it works";
        return response;
        // All checks pass
    }

    // Set up response handling from server with thread class
    class ServerResponse extends Thread {

        public void run() {
            while (true) {
                try { // Tries to read the incoming stream
                    String response = (String) client_input.readObject();
                    System.out.println(response);
                } catch (IOException error) {
                    closeAllSockets = true;
                    System.out.println("Thanks for signing in! See you later... the Socket was closed"); // Breaks if
                                                                                                         // error is
                                                                                                         // caught
                    break; // Break and exit while loop
                } catch (ClassNotFoundException error) {
                    System.out.println("Thanks for signing in! See you later... the Socket was closed"); // Breaks if
                                                                                                         // error is
                                                                                                         // caught
                    break; // Break and exit while loop
                }
            }
        }
    }

    // DECLARE MAIN FUNCTION TO RUN
    public static void main(String args[]) {

        System.out.println("Welcome To 3313 Chat Application, type in your credentials below");

        // new object of the Scanner class stored it in the variable input
        // Calls constructor of the class with the parameter System.in
        // Enables us to read from program's standard input stream
        Scanner input = new Scanner(System.in);

        // Declaring and initializing new variables
        // this is the server's IP address
        String serverIP;
        // this is the default port to connect with server
        String socketPort;
        int final_SocketPort = 0; // integer port
        // this is the default name assgined to a user
        String name;

        // Collecting User Values to secure connection
        // Server IP address
        System.out.println("Please Input Desired Server IP address (Skip for Default): ");
        serverIP = input.nextLine();

        // Server communication socket port
        System.out.println("Please Input Desired Server Socket Port (Skip for Default): ");
        socketPort = input.nextLine();

        // Setting the username
        System.out.println("What is the Username you desire? ");
        name = input.nextLine();

        // this is the server's IP address
        if (serverIP == "") {
            serverIP = "127.0.0.1";
            // this is the default address to connect with server
        }

        if (socketPort == "") {
            final_SocketPort = 3000;
            // this is the default port to connect with the server
        }

        else if (socketPort != "") {
            // Convert port from string into int
            try {
                final_SocketPort = Integer.parseInt(socketPort);
            } catch (Exception error) {
                System.out.println("Invalid port number." + error);
            }
        }

        if (name == "") {
            name = "Unidentified";
            // this is the default name assgined to a user
        }

        System.out.println(
                "You are: " + name + " Operating from IP address: " + serverIP + " and port: " + final_SocketPort);

        // create the Client object from client class
        Client new_client = new Client(serverIP, final_SocketPort, name);

        // try to connect to the server and return if not connected

        new_client.init_connection();

        System.out.println("Chat Commands:");
        System.out.println("Functionality 1 - All general messages will be broadcasted to all active users");
        System.out.println(
                "Functionality 2 - If you want to send a private message, add '@NameOfUser' before the message");
        System.out.println("Functionality 3 - To see a list of all active Users, enter 'users'");
        System.out.println("Functionality 4 - To logoff and exit, enter 'logoff'");
        System.out.println("Functionality 5 - To kill the chat and exit, enter 'kill_server'");

        // infinite loop to get the input from the user until user types "client_exit"
        // (secret code)
        // serer will stay on until a user enters "kill_server"
        boolean canContinue = true; // bool value for loop flag
        System.out.println("Enter Message: ");
        do {

            // Ask user to input a message

            // this will read the user's message and store the input
            String userInput = input.nextLine();

            // Logoff and exit if message is "client_exit"
            if (userInput.equalsIgnoreCase("logout") || userInput.equalsIgnoreCase("kill_server")) {
                canContinue = false;
            }

            // regular text message
            new_client.userOutput(userInput);

        } while (canContinue == true && closeAllSockets == false);

        // closes scanner
        input.close();

        // client gracefully exits
        new_client.logout();

    }

}