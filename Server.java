import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Thread {

	private int port;
	private boolean keepOpen;
	private static int uniqueIdentifier;
	private ArrayList<ThreadForClient> listOfClients; // ThreadForClient == ClientThread
	private static boolean keepAccepting;
	private static ServerSocket ServerSocketInstance;

	// Server class' constructor
	public Server(int port) {
		this.port = port;
		listOfClients = new ArrayList<ThreadForClient>();
		keepAccepting = true;
	}

	// -------------------------------------------------------------------------------------------------------

	public static void main(String[] args) {
		int port = 3000;
		Server serverInstance = new Server(port);
		Server newInstance = new Server(port);
		newInstance.run();
		serverInstance.begin();

	}

	// -------------------------------------------------------------------------------------------------------

	private synchronized boolean sendMessage(String msg) {

		for (int i = listOfClients.size() - 1; i >= 0; i--) {
			ThreadForClient currentThread = listOfClients.get(i);
			if (currentThread.sendToClient(msg) == false) {
				listOfClients.remove(i);
				System.out.println(currentThread.name + "was disconnected");
			}

		}

		return true;
	}

	private synchronized boolean sendDirectMessage(String msg, String name) {

		String[] splitMsg = msg.split(" ", 3);
		boolean privateMessage = false;
		if (splitMsg.length > 1 && splitMsg[1].charAt(0) == '@') {
			privateMessage = true;
		}
		if (privateMessage) {
			String receiverName = splitMsg[1].substring(1, splitMsg[1].length());
			msg = splitMsg[0] + splitMsg[2]; // concatenating the name with the message
			boolean discovered = false;

			for (int i = listOfClients.size() - 1; i >= 0; i--) {
				ThreadForClient currentThread = listOfClients.get(i);
				if (currentThread.name.equals((String) name))
					continue;
				if (currentThread.name.equals((String) receiverName)) {
					if (currentThread.sendToClient(msg) == false) {
						listOfClients.remove(i);
						System.out.println(currentThread.name + "was disconnected");
					}
					discovered = true;
					break;
				}
			}
			if (!discovered)
				return false;
		} else {
			for (int i = listOfClients.size() - 1; i >= 0; i--) {
				ThreadForClient currentThread = listOfClients.get(i);
				if (currentThread.name.equals((String) name))
					continue;
				if (currentThread.sendToClient(msg) == false) {
					listOfClients.remove(i);
					System.out.println(currentThread.name + "was disconnected");
				}

			}
		}
		return true;
	}

	synchronized void removeClient(int identifier) {
		String removedClient = "";
		for (int i = 0; i < listOfClients.size(); i++) {
			ThreadForClient currentThread = listOfClients.get(i);
			if (currentThread.identifier == identifier) {
				System.out.println(currentThread.name + " was removed from the server");
				removedClient = currentThread.name;
				try {
					currentThread.input.close();
					currentThread.output.close();
					currentThread.socket.close();
				} catch (Exception error) {

				}

				listOfClients.remove(i);
				break;
			}
		}
		sendMessage("***************");
		sendMessage(removedClient + " has disconnected from the server");
		sendMessage("***************");
	}

	// -------------------------------------------------------------------------------------------------------

	public void begin() {
		keepOpen = true;
		System.out.println("The Server socket is open and is waiting for some clients.");
		try {
			ServerSocketInstance = new ServerSocket(port);
			while (keepOpen == true) {
				ServerSocketInstance.setSoTimeout(1);
				Socket clientSocket = null;
				while (true) {
					try {
						clientSocket = ServerSocketInstance.accept();
					} catch (Exception error) {
						break;
					}
				}
				if (clientSocket == null) {
					continue;
				}
				// accept connection with client socket
				if (keepOpen == false) { // when is this ever set to false?
					break;
				}
				ThreadForClient newClientThread = new ThreadForClient(clientSocket);
				listOfClients.add(newClientThread);
				System.out.println("client added");
				newClientThread.start();
			}

			System.out.println("closing...");
			try {
				sendMessage("Server is shutting down");
				ServerSocketInstance.close();
				for (int i = 0; i < listOfClients.size(); i++) {
					ThreadForClient threadElement = listOfClients.get(i);
					try {
						System.out.println(threadElement.name);
						threadElement.input.close();
						threadElement.output.close();
						threadElement.socket.close();
						threadElement.toContinue = false;
						// it's cause begin is outside ClientThread so you can't just call
						// closeSocketIO() function which is inside
					} catch (Exception e) { // so have to access each individual one to close() which is a built in
											// function
						System.out.println("Something went wrong closing 2"); // all of these closes are built in things
					}
				}
			} catch (Exception e) {
				System.out.println("Something went wrong closing 3");
			}
		} catch (Exception e) {
			System.out.println("Something went wrong closing 4");
		}
	}

	// -----------------------------------------------------------------------------------------------------------
	// Each client will run one instance of ThreadForClient
	class ThreadForClient extends Thread {
		Socket socket;
		ObjectOutputStream output;
		ObjectInputStream input;
		volatile boolean toContinue = true;
		String name; // name to identify the client
		String msg; // message object that holds the message and the type of the message
		int identifier; // unique id to help with deconnection

		// Constructor for ThreadForClient that sets each thread's identifier, socket,
		// creates I/O streams, and sets the user's name
		ThreadForClient(Socket socket) {
			uniqueIdentifier = uniqueIdentifier + 1;
			identifier = uniqueIdentifier;
			this.socket = socket;
			// try to create input output streams
			try {
				output = new ObjectOutputStream(socket.getOutputStream());
				input = new ObjectInputStream(socket.getInputStream());
				name = (String) input.readObject();
				System.out.println(name);
				sendMessage("***************");
				sendMessage("Say hi to " + name + "! They just joined the room.");
				sendMessage("***************");
			} catch (Exception error) {
				return;
			}

		}

		public void run() {
			while (toContinue == true) {
				try {
					msg = (String) input.readObject(); // reading a string
				} catch (Exception error) {
					break;
				}

				if (msg.equals("logout")) {
					toContinue = false;
				} else if (msg.equals("users")) {
					sendToClient("***********************");
					sendToClient("The list of members is: ");
					for (int i = 0; i < listOfClients.size(); ++i) {
						ThreadForClient currentThread = listOfClients.get(i); // check if ++ is used correctly

						sendToClient(currentThread.name);
					}
					sendToClient("***********************");
				} else if (msg.equals("kill_server")) {
					// Server serverInstance = new Server(port);
					keepAccepting = false;
					keepOpen = false;
					kill_thread();
					// Socket socket = new Socket(3000);

				} else {
					if (!sendDirectMessage(name + ": " + msg, name)) {
						sendToClient("Sorry there were no members in the chat to send the message to");
					}
				}
			}
			removeClient(identifier);
			closeSocketIO();

		}

		private void kill_thread() {
			toContinue = false;
		}

		private void closeSocketIO() {
			try {
				if (output != null)
					output.close();
			} catch (Exception err) {
			}

			try {
				if (input != null)
					input.close();
			} catch (Exception err) {
			}

			try {
				if (socket != null)
					socket.close();
			} catch (Exception err) {
			}
		}

		private boolean sendToClient(String text) {
			if (socket.isConnected() == false) {
				try {
					socket.close();
					return false;
				} catch (Exception error) {
					// System.out.println("1");
				}
			}
			try {
				output.writeObject(text);
			} catch (Exception error) {
				System.out.println("2");
			}
			return true;
		}
	}
}
