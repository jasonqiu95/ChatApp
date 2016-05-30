
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
 * A server program which accepts requests from clients to
 * capitalize strings.  When clients connect, a new thread is
 * started to handle an interactive dialog in which the client
 * sends in a string and the server thread sends back the
 * capitalized version of the string.
 *
 * The program is runs in an infinite loop, so shutdown in platform
 * dependent.  If you ran it from a console window with the "java"
 * interpreter, Ctrl+C generally will shut it down.
 */
public class ChatServer {
	
	private static JFrame frame;
	private static JTextArea messageArea;
	// global across all ClientServer
    private static Map<String, ClientConnection> clients = new HashMap<String, ClientConnection>();

    /**
     * Application method to run the server runs in an infinite loop
     * listening on port 9898.  When a connection is requested, it
     * spawns a new thread to do the servicing and immediately returns
     * to listening.  The server keeps a unique client number for each
     * client that connects just to show interesting logging
     * messages.  It is certainly not necessary to do this.
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
				} catch(Exception ex) {
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
    	public int clientNumber;
    	
    	public ServerStarter(ServerSocket socket) {
    		this.socket = socket;
    		clientNumber = 0;
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
	    		String[] names = (String[])clients.keySet().toArray();
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
     * A private thread to handle client requests on a particular
     * socket.  The client terminates the dialogue by sending "bye".
     */
    private static class ClientServer extends Thread {
        private Socket socket;
        private String userName;
        
        // this is used to tell whether the client is actually logged in
        // because he could try to use others' name and doesn't log in
        private boolean served;
        
        public ClientServer(Socket socket) {
            this.socket = socket;
            served = false;
        }

        /**
         * Services this thread's client by first sending the
         * client a welcome message then repeatedly reading strings
         * and sending back the capitalized version of the string.
         */
        public void run() {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // continue to ask the user for a new name 
                // until a unique name is provided
                boolean dup = true;
                while (dup) {
	                userName = in.readLine();
	                //check whether client with the same name is already logged in
	                if (clients.containsKey(userName)) {
	                	out.println("This user is already logged in");
	            		out.println("Please change a user name");
	            		// a string *CODE* to tell the client to enter username again
	            		out.println("AGAIN");
	                } else {
	                	dup = false;
	                }
                }
                
            	// the user has a unique name
            	served = true;
                log("New connection with client " + userName + " at " + socket);
                ClientConnection client = new ClientConnection(in, out, socket, userName);
                clients.put(userName, client);
                // Send a welcome message to the client.
                out.println("Hello, " + userName + ".");
                out.println("Enter \"bye\" to quit\n");

                // update username
                //new UpdateUserNameTask(socket).start();
                
                // Get messages from the client, line by line
                while (true) {
                    String input = client.reader.readLine();
                    log("Client " + userName + " says: " + input);
                    if (input == null || input.equals("bye")) {
                        break;
                    }
                    for (ClientConnection all : clients.values()) {
                    	System.out.println("Tries to print to Client " + all.name);
                    	all.writer.println(userName + " says:");
                    	all.writer.println(input);
                    	System.out.println("Finish printing to Client " + all.name);
                    }
                }
            } catch (IOException e) {
                log("Error handling client " + userName + ": " + e);
            } finally {
                try {
                    socket.close();
                    log("Clients size " + clients.size());
                    // if the user is actually logged in
                	// then we remove it from total connections
                    // and notify all other users
                	if (served) {
                        clients.remove(userName);
	                    for (ClientConnection conn : clients.values()) {
	                    	conn.writer.println("Client " + userName + " left.");
	                    }
                	}
                } catch (IOException e) {
                    log("Couldn't close a socket, what's going on?");
                }
                log("Connection with client " + userName + " closed");
            }
        }
    }
    
    /**
     * Logs a simple message.  In this case we just write the
     * message to the server applications standard output.
     */
    private static void log(String message) {
        messageArea.append(message+"\n");
    }
    
    private static class ClientConnection {
    	public BufferedReader reader;
    	public PrintWriter writer;
    	public Socket socket;
    	public String name;
    	
    	public ClientConnection(BufferedReader reader, PrintWriter writer, Socket socket, String name) {
    		this.reader = reader;
    		this.writer = writer;
    		this.socket = socket;
    		this.name = name;
    	}
    }
}