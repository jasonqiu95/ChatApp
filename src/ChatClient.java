
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the capitalization server.
 * It has a main frame window with a text field for entering
 * strings and a textarea to see the results of capitalizing
 * them.
 */
public class ChatClient {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private ClientGUI gui;

    // a specialized task to fetch all currently logged in user names
    private class FetchUserNamesTask extends Thread {
    	Socket socket;
    	
    	public FetchUserNamesTask(Socket s) {
    		socket = s;
    	}
    	@Override
    	public void run() {
    		try {
				ObjectInputStream oos = new ObjectInputStream(socket.getInputStream());
				String[] names = (String[])oos.readObject();
				System.out.println(names);
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    }
    
    // this class runs in another thread and always listens for
    // server message and displays it in messageArea
    private class ServerListener extends Thread {
    	@Override
    	public void run() {
    		while (true) {
    			try {
    				System.out.println("Client is listening for input");
    				ChatMessage response = (ChatMessage)in.readObject();
    				System.out.println("Client is receiving input = " + response);
    				if (response == null ||
    					response.content == null ||
    					response.content.equals("")) {
                        System.exit(0);
    				}
    				gui.messageArea.append(response.content + "\n");
    			} catch (Exception e) {
    				System.out.println("Cannot read from input stream");
    			}
    		}
    	}
    }
    
    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Enter in the
     * listener sends the textfield contents to the server.
     */
    public ChatClient() {
    	gui = new ClientGUI();

        // Add Listeners
        gui.dataField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield
             * by sending the contents of the text field to the
             * server and displaying the response from the server
             * in the text area.  If the response is "." we exit
             * the whole application, which closes all sockets,
             * streams and windows.
             */
            public void actionPerformed(ActionEvent e) {
            	System.out.println("Client is sending message to server");
            	ChatMessage mess = new ChatMessage(ChatMessage.BROAD, gui.dataField.getText(), null);
            	try {
					out.writeObject(mess);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
                System.out.println("Client sent message " + gui.dataField.getText());

                gui.dataField.setText("");
            }
        });
    }

    public String login() {
    	try {
    		ChatMessage response = (ChatMessage)in.readObject();
//    		System.out.println(response.content.equals("AGAIN"));
//    		System.out.println("response " + response.content);
    		if (response.type == ChatMessage.AGAIN) {
    			gui.messageArea.append(response.content);
    			String userName = JOptionPane.showInputDialog(
    	                gui.frame,
    	                "Enter your user name:",
    	                "Welcome to the EasyChat",
    	                JOptionPane.QUESTION_MESSAGE);
    			ChatMessage mess = new ChatMessage(ChatMessage.LOG, userName, null);
    			out.writeObject(mess);
    			// try to login again
    			return login();
    		} else {
    			return response.content;
    		}
    	} catch (Exception e) {
    		System.exit(0);
    	}
		return null;
    }
    
    /**
     * Implements the connection logic by prompting the end user for
     * the server's IP address, connecting, setting up streams, and
     * consuming the welcome messages from the server.  The Capitalizer
     * protocol says that the server sends three lines of text to the
     * client immediately after establishing a connection.
     */
    public void connectToServer() throws IOException {

        // Get the server address from a dialog box.
        String serverAddress = JOptionPane.showInputDialog(
            gui.frame,
            "Enter IP Address of the Server:",
            "Welcome to the EasyChat",
            JOptionPane.QUESTION_MESSAGE);
        String userName = JOptionPane.showInputDialog(
                gui.frame,
                "Enter your user name:",
                "Welcome to the EasyChat",
                JOptionPane.QUESTION_MESSAGE);

        // Make connection and initialize streams
        Socket socket = new Socket(serverAddress, 9898);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        // write the login username to server
        out.writeObject(new ChatMessage(ChatMessage.LOG, userName,null));
        String response = login();
        
        // Consume the initial welcoming messages from the server
        gui.messageArea.append(response+"\n");
        new ServerListener().start();
        //new FetchUserNamesTask(socket).start();
    }

    /**
     * Runs the client application.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.gui.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.gui.frame.pack();
        client.gui.frame.setVisible(true);
        client.connectToServer();
    }
}