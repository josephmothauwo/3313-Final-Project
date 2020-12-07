import java.io.*;
import java.net.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Define Client Class
public class Client {

    private ObjectInputStream client_input; //
    private ObjectOutputStream client_output;
    private Socket socket;
    public boolean closeAllSockets;
    public volatile boolean keepOpen = true;

    String appName = "Java Client Server Chat App";
    Client mainGUI;
    JFrame newFrame = new JFrame(appName);
    JButton sendMessage;
    JTextField messageBox;
    JTextArea chatBox;
    JTextField usernameChooser;
    JTextField addressChooser;
    JTextField portChooser;
    JFrame preFrame;

    public void preDisplay() {
        newFrame.setVisible(false);
        preFrame = new JFrame(appName);

        usernameChooser = new JTextField(10);
        addressChooser = new JTextField(10);
        portChooser = new JTextField(10);

        JLabel chooseUsernameLabel = new JLabel("Pick a username:");
        JLabel chooseIPAddressLabel = new JLabel("Enter IP Address:");
        JLabel choosePortLabel = new JLabel("Enter Port Number:");

        JButton enterServer = new JButton("Enter Chat Server");
        enterServer.addActionListener(new enterServerButtonListener());
        JPanel prePanel = new JPanel(new GridBagLayout());

        GridBagConstraints preRight = new GridBagConstraints();
        preRight.insets = new Insets(20, 0, 0, 10);
        preRight.anchor = GridBagConstraints.EAST;

        GridBagConstraints preLeft = new GridBagConstraints();
        preLeft.anchor = GridBagConstraints.WEST;
        preLeft.insets = new Insets(20, 0, 0, 10);

        GridBagConstraints portLabel = new GridBagConstraints();
        portLabel.anchor = GridBagConstraints.EAST;
        portLabel.insets = new Insets(20, 20, 0, 10);

        GridBagConstraints portBox = new GridBagConstraints();
        portBox.anchor = GridBagConstraints.WEST;
        portBox.insets = new Insets(20, 0, 0, 10);

        GridBagConstraints addrLabel = new GridBagConstraints();
        addrLabel.anchor = GridBagConstraints.EAST;
        addrLabel.insets = new Insets(20, 20, 0, 10);

        GridBagConstraints addrBox = new GridBagConstraints();
        addrBox.anchor = GridBagConstraints.WEST;
        addrBox.insets = new Insets(20, 0, 0, 10);

        prePanel.add(chooseUsernameLabel, preLeft);
        prePanel.add(usernameChooser, preRight);
        prePanel.add(chooseIPAddressLabel, addrLabel);
        prePanel.add(addressChooser, addrBox);
        prePanel.add(choosePortLabel, portLabel);
        prePanel.add(portChooser, portBox);
        preFrame.add(BorderLayout.CENTER, prePanel);
        preFrame.add(BorderLayout.SOUTH, enterServer);
        preFrame.setSize(700, 400);
        preFrame.setVisible(true);

    }

    public void display() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel southPanel = new JPanel();
        southPanel.setBackground(Color.BLUE);
        southPanel.setLayout(new GridBagLayout());

        messageBox = new JTextField(30);
        messageBox.requestFocusInWindow();

        sendMessage = new JButton("Send Message");
        sendMessage.addActionListener(new sendMessageButtonListener());

        chatBox = new JTextArea();
        chatBox.setEditable(false);
        chatBox.setFont(new Font("Serif", Font.PLAIN, 15));
        chatBox.setLineWrap(true);

        chatBox.setText("Chat Commands:\n");
        chatBox.append("Functionality 1 - All general messages will be broadcasted to all active users\n");
        chatBox.append(
                "Functionality 2 - If you want to send a private message, add '@NameOfUser' before the message\n");
        chatBox.append("Functionality 3 - To see a list of all active Users, enter 'users'\n");
        chatBox.append("Functionality 4 - To logoff and exit, enter 'logout'\n");
        chatBox.append("Functionality 5 - To kill the chat and exit, enter 'kill_server'\n");
        chatBox.append("\n");

        mainPanel.add(new JScrollPane(chatBox), BorderLayout.CENTER);

        GridBagConstraints left = new GridBagConstraints();
        left.anchor = GridBagConstraints.LINE_START;
        left.fill = GridBagConstraints.HORIZONTAL;
        left.weightx = 512.0D;
        left.weighty = 1.0D;

        GridBagConstraints right = new GridBagConstraints();
        right.insets = new Insets(0, 10, 0, 0);
        right.anchor = GridBagConstraints.LINE_END;
        right.fill = GridBagConstraints.NONE;
        right.weightx = 1.0D;
        right.weighty = 1.0D;

        southPanel.add(messageBox, left);
        southPanel.add(sendMessage, right);

        mainPanel.add(BorderLayout.SOUTH, southPanel);

        newFrame.add(mainPanel);
        newFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        newFrame.setSize(700, 400);
        newFrame.setVisible(true);
    }

    class sendMessageButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (messageBox.getText().length() < 1) {
                // do nothing
            } else if (messageBox.getText().equals(".clear")) {
                chatBox.setText("Cleared all messages\n");
                messageBox.setText("");
            } else if (messageBox.getText().equals("logout")) {
                userOutput("logout");
                chatBox.append("Logged Out" + "\n");
                logout();
            } else if (messageBox.getText().equals("kill_server")) {
                userOutput("kill_server");
                chatBox.append("Server Closed" + "\n");
                logout();
            } else {
                chatBox.append("<" + client_name + ">:  " + messageBox.getText() + "\n");
                userOutput(messageBox.getText());
                messageBox.setText("");
            }
            messageBox.requestFocusInWindow();
        }
    }

    public String server, client_name;
    public int PORT;

    class enterServerButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            client_name = usernameChooser.getText();
            server = addressChooser.getText();
            PORT = Integer.parseInt(portChooser.getText());
            closeAllSockets = false;

            if (client_name.length() < 1 || server.length() < 1 || PORT < 1) {
                client_name = "Unknown";
                server = "127.0.0.1";
                PORT = 3000;
                System.out.println("Invalid");
            } else {

                try { // Start off with a try catch block as recommended in Lab 3
                    socket = new Socket(server, PORT);
                } catch (Exception error) {
                    System.out.println("Connection Refused" + error);
                }

                try { // Try catch block to establish data streams
                    client_input = new ObjectInputStream(socket.getInputStream());
                    client_output = new ObjectOutputStream(socket.getOutputStream());
                } catch (IOException error) {
                    System.out.println("Data Streams Failed" + error);
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
                }

                // incoming clients
                System.out.println("it works");
                // All checks pass
                preFrame.setVisible(false);
                display();
            }
        }

    }

    private void logout() { // Error thrown, then client connection is ended
        keepOpen = false;
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
           
        }

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
            while (keepOpen) {
                try { // Tries to read the incoming stream
                    String response = (String) client_input.readObject();
                    chatBox.append(response + "\n");
                } catch (IOException error) {
                    closeAllSockets = true;
                    System.out.println("Thanks for signing in! See you later... the Socket was closed"); // Breaks if
                    chatBox.append("Thanks for signing in! See you later... the Socket was closed"); // error is
                    // caught
                    break; // Break and exit while loop
                } catch (ClassNotFoundException error) {
                    System.out.println("Thanks for signing in! See you later... the Socket was closed"); // Breaks if
                    chatBox.append("Thanks for signing in! See you later... the Socket was closed"); // error is
                                                                                                     // caught
                    break; // Break and exit while loop
                }
            }
        }
    }

    // DECLARE MAIN FUNCTION TO RUN
    public static void main(String args[]) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Client mainGUI = new Client();
                mainGUI.preDisplay();
            }
        });
    }
}
