import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Thread {

	private int port;
	private boolean keepOpen;	//keeps the server open 
	private static int uniqueIdentifier;	//increases by 1 every time a new client thread is created
	private ArrayList<ThreadForClient> listOfClients; // ThreadForClient == ClientThread
	private static boolean keepAccepting;
	private static ServerSocket ServerSocketInstance;

	// Server class' constructor
	public Server(int port) {
		this.port = port;
		listOfClients = new ArrayList<ThreadForClient>();
		keepAccepting = true;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static void main(String[] args) {
		int port = 3000;
		Server serverInstance = new Server(port);
		serverInstance.begin();	//start runnning the server
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------

	public void begin() {
		keepOpen = true;
		System.out.println("The Server is open and is waiting for some clients.");
		try {
			ServerSocketInstance = new ServerSocket(port);
			while (keepOpen == true) {
				ServerSocketInstance.setSoTimeout(1);	//reading from input is blocked for 1ms
				Socket clientSocket = null;
				while (true) {
					try {
						clientSocket = ServerSocketInstance.accept();	//waiting for client connections
					}	
					catch (Exception error) {	//exit while loop if no longer accepting connections
						break;
					}	
				}
				if (clientSocket == null) continue;
				if (keepOpen == false) break;

				//create the thread, add it to the list, and start it
				ThreadForClient newClientThread = new ThreadForClient(clientSocket);
				listOfClients.add(newClientThread);
				System.out.println("client added");
				newClientThread.start();	//starting a thread for each client that joins.
			}
			//Logic of a thread continues below the next section

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------

			//once leave the above while loop, enter this section that deals with closing the server
			System.out.println("closing...");
			try {
				sendMessage("Server is shutting down");
				ServerSocketInstance.close();
				for (int i = 0; i < listOfClients.size(); i++) {
					ThreadForClient threadElement = listOfClients.get(i);

					//closing the input stream, output stream, and socket of each client thread
					//cannot use closeSocketIO() which is inside the ThreadForClient class
					try {	
						System.out.println(threadElement.name);
						threadElement.input.close();				
						threadElement.output.close();
						threadElement.socket.close();
						threadElement.toContinue = false;
					} catch (Exception e) {
											
						System.out.println("Something went wrong closing 2");
					}
				}
			} catch (Exception e) {
				System.out.println("Something went wrong closing 3");
			}
		} catch (Exception e) {
			System.out.println("Something went wrong closing 4");
		}
	}

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------

	// Each client will run one instance of ThreadForClient
	//The ThreadForClient class includes: constructor, run(), sendToClient(), kill_thread(), and closeSocketIO()
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
			uniqueIdentifier = uniqueIdentifier + 1;	//for each new thread, has its own unique identifier
			identifier = uniqueIdentifier;
			this.socket = socket;
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

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------------

		//Execution of each client thread
		public void run() {
			while (toContinue == true) {
				try {
					msg = (String) input.readObject();
				} catch (Exception error) {
					break;
				}

				if (msg.equals("logout")) {
					toContinue = false;
				}
				else if (msg.equals("users")) {					// lists the names of all the clients
					sendToClient("***********************");
					sendToClient("The list of members is: ");
					for (int i = 0; i < listOfClients.size(); ++i) {
						ThreadForClient currentThread = listOfClients.get(i);

						sendToClient(currentThread.name);
					}
					sendToClient("***********************");
				}
				else if (msg.equals("kill_server")) {
					keepAccepting = false;
					keepOpen = false;
					kill_thread();	//also just sets toContinue to false
				}
				else {	//Client sends message to other client and the method returns a true if the client with that name exists
					if (!sendDirectMessage(name + ": " + msg, name)) {
						sendToClient("Sorry there were no members in the chat to send the message to");
					}
				}
			}
			removeClient(identifier);	//once client enters logout or kill_server, exits loop, removes client from list, and closes the streams
			closeSocketIO();

		}

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

		//Method to handle the actual writing functionality
		private boolean sendToClient(String text) {
			if (socket.isConnected() == false) {
				try {
					socket.close();
					return false;
				} catch (Exception error) {}
			}
			try {
				output.writeObject(text);
			} catch (Exception error) {}
			return true;
		}

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

		private void kill_thread() {
			toContinue = false;
		}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

		//Used to close all streams for each thread
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
	}//end of ThreadForClient class

//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	//Used for server broadcasted messages 
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

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	//Used for clients sending messages to everyone or privately
	//makes sure only one client is sending a message at a time
	private synchronized boolean sendDirectMessage(String msg, String name) {

		//splits the message into "name: ", "@John", and the actual message or just "name: " & actual message
		// second parameter '3' is the limit, meaning the " " regex will be applied at most 2 times
		String[] splitMsg = msg.split(" ", 3);	
		boolean privateMessage = false;
		if (splitMsg.length > 1 && splitMsg[1].charAt(0) == '@') {	//checking whether the message is intended to be private or not
			privateMessage = true;
		}
		if (privateMessage) {	//if message is private
			String receiverName = splitMsg[1].substring(1, splitMsg[1].length()); //from 2nd index (1st contains '@') to last index, get receiver client's name
			msg = splitMsg[0] + splitMsg[2]; // concatenating the name with the message
			boolean discovered = false;

			for (int i = listOfClients.size() - 1; i >= 0; i--) {
				ThreadForClient currentThread = listOfClients.get(i);
				if (currentThread.name.equals((String) name)) continue;
				if (currentThread.name.equals((String) receiverName)) {
					if (currentThread.sendToClient(msg) == false) {		//send the msg to the receiver. Return false means no longer there so remove client from list
						listOfClients.remove(i);
						System.out.println(currentThread.name + "was disconnected");
					}
					discovered = true;
					break;
				}
			}
			if (!discovered) return false;
		}
		else {	//if message is not private
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

//-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	//Closes the streams of the client and removes it from the list
	//Makes sure only one client is removed at a time
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
}
