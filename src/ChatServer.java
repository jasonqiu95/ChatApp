
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A server program which accepts requests from clients to capitalize strings.
 * When clients connect, a new thread is started to handle an interactive dialog
 * in which the client sends in a string and the server thread sends back the
 * capitalized version of the string.
 *
 * The program is runs in an infinite loop, so shutdown in platform dependent.
 * If you ran it from a console window with the "java" interpreter, Ctrl+C
 * generally will shut it down.
 */
public class ChatServer {

	private static JFrame frame;
	private static JTextArea messageArea;
	// global across all ClientServer
	private static Map<String, ClientConnection> clients = new HashMap<String, ClientConnection>();

	/**
	 * Application method to run the server runs in an infinite loop listening
	 * on port 9898. When a connection is requested, it spawns a new thread to
	 * do the servicing and immediately returns to listening. The server keeps a
	 * unique client number for each client that connects just to show
	 * interesting logging messages. It is certainly not necessary to do this.
	 */
	public static void main(String[] args) {
		frame = new JFrame("Server");
		messageArea = new JTextArea(8, 60);

		// Layout GUI
		messageArea.setEditable(false);
		frame.getContentPane().add(new JScrollPane(messageArea), "Center");

		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		JButton start = new JButton("Start");
		JButton exit = new JButton("Exit");

		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ServerSocket listener = new ServerSocket(9898);
					ServerStarter server = new ServerStarter(listener);
					server.start();
				} catch (Exception ex) {
					log(e.toString());
				}
			}
		});

		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		panel.add(start);
		panel.add(exit);
		panel.setVisible(true);
		frame.getContentPane().add(panel, "South");

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private static class ServerStarter extends Thread {
		public ServerSocket socket;

		public ServerStarter(ServerSocket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				log("The chat server is running.");
				while (true) {
					new ClientServer(socket.accept()).start();
				}
			} catch (Exception e) {
				log(e.toString());
			} finally {
				try {
					socket.close();
				} catch (Exception e) {
					log(e.toString());
				}
			}
		}
	}

	private static class UpdateUserNameTask extends Thread {
		Socket socket;

		public UpdateUserNameTask(Socket s) {
			socket = s;
		}

		@Override
		public void run() {
			while (true) {
				String[] names = (String[]) clients.keySet().toArray();
				try {
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject(names);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * A private thread to handle client requests on a particular socket. The
	 * client terminates the dialogue by sending "bye".
	 */
	private static class ClientServer extends Thread {
		private Socket socket;
		private ChatMessage mess;
		ObjectInputStream in;
		ObjectOutputStream out;
		ClientConnection client;

		// this is used to tell whether the client is actually logged in
		// because he could try to use others' name and doesn't log in
		private boolean served;

		public ClientServer(Socket socket) {
			this.socket = socket;
			served = false;
		}

		/**
		 * Services this thread's client by first sending the client a welcome
		 * message then repeatedly reading strings and sending back the
		 * capitalized version of the string.
		 */
		@Override
		public void run() {
			try {
				out = new ObjectOutputStream(socket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(socket.getInputStream());
				
				boolean keepGoing = true;
				while (keepGoing) {
					ChatMessage mess = (ChatMessage)in.readObject();
					switch (mess.type) {
						case ChatMessage.LOG:
							handleLogin(mess);
							break;
						case ChatMessage.BROAD:
							keepGoing = handleBroad(mess);
							break;
						case ChatMessage.PRIV:
							keepGoing = handlePriv(mess);
						default:
							break;
					}
				}

				// update username
				// new UpdateUserNameTask(socket).start();
			} catch (Exception e) {
				log("Error handling client " + client.name + ": " + e);
			} finally {
				try {
					socket.close();
					log("Clients size " + clients.size());
					// if the user is actually logged in
					// then we remove it from total connections
					// and notify all other users
					if (served) {
						clients.remove(client.name);
						for (ClientConnection conn : clients.values()) {
							conn.writer.writeObject(new ChatMessage(ChatMessage.BROAD, 
																	client.name +
																	" left.", null));
							conn.update();
						}
					}
				} catch (Exception e) {
					log("Couldn't close a socket, what's going on?");
				}
				log("Connection with client " + client.name + " closed");
			}
		}
		
		private boolean handlePriv(ChatMessage mess)
				throws IOException, ClassNotFoundException {
			if (mess.content == null || mess.content.equals("bye")) {
				return false;
			}
			ClientConnection conn = clients.get(mess.target);
			conn.writer.writeObject(new ChatMessage(ChatMessage.BROAD,
													"**PRIVATE** " 
													+ client.name + " says: " 
													+ mess.content, null));
			return true;
		}
		
		/*
		 * @return true if broad sucess
		 * 		   false if input is null or "bye"
		 */
		private boolean handleBroad(ChatMessage mess) 
				throws IOException, ClassNotFoundException {
			log("Client " + client.name + " says: " + mess.content);
			if (mess.content == null || mess.content.equals("bye")) {
				return false;
			}
			for (ClientConnection all : clients.values()) {
				log("Tries to print to Client " + all.name);
				all.writer.writeObject(new ChatMessage(ChatMessage.BROAD, 
												   client.name + " says: " +
												   mess.content+"\n", "all"));
				log("Finish printing to Client " + all.name);
			}
			return true;
		}

		/*
		 * @pre: mess.type = ChatMessage.LOG
		 */
		private void handleLogin(ChatMessage mess) 
				throws IOException, ClassNotFoundException {
            //check whether client with the same name is already logged in
            String userName = mess.content;
            while (clients.containsKey(userName)) {
        		ChatMessage response = new ChatMessage(ChatMessage.AGAIN, 
        							"This user is already logged in"
        							+ "\nPlease change a user name\n",
        							null);
        		out.writeObject(response);
        		mess = (ChatMessage)in.readObject();
        		userName = mess.content;
            }
            // the user has a unique name
			served = true;
			log("New connection with client " + userName + " at " + socket);
			client = new ClientConnection(in, out, socket, userName);
			clients.put(userName, client);
			// Send a welcome message to the client.
			out.writeObject(new ChatMessage(ChatMessage.BROAD, 
							"Hello, " + userName + ".\n"
							+ "Enter \"bye\" to quit\n",
							null));
			for (ClientConnection conn : clients.values()) {
				conn.writer.writeObject(new ChatMessage(ChatMessage.BROAD, client.name + " joined.", null));
				conn.update();
			}
        }
	}

	/**
	 * Logs a simple message. In this case we just write the message to the
	 * server applications standard output.
	 */
	private static void log(String message) {
		messageArea.append(message + "\n");
	}

	private static class ClientConnection {
		public ObjectInputStream reader;
		public ObjectOutputStream writer;
		public Socket socket;
		public String name;

		public ClientConnection(ObjectInputStream reader, ObjectOutputStream writer, Socket socket, String name) {
			this.reader = reader;
			this.writer = writer;
			this.socket = socket;
			this.name = name;
		}
		
		public void update() 
				throws IOException, ClassNotFoundException {
			String[] names = new String[clients.keySet().size()];
			clients.keySet().toArray(names);
			ChatMessage newMess = new ChatMessage(ChatMessage.UPDATE, "update", null);
			newMess.names  = names;
			writer.writeObject(newMess);
		}
	}
}