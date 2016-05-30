
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private BufferedReader in;
    private PrintWriter out;
    private ClientGUI gui;

    // this class runs in another thread and always listens for
    // server message and displays it in messageArea
    private class ServerListener extends Thread {
    	@Override
    	public void run() {
    		while (true) {
    			try {
    				System.out.println("Client is listening for input");
    				String response = in.readLine();
    				System.out.println("Client is receiving input = " + response);
    				if (response == null || response.equals("")) {
                        System.exit(0);
    				}
    				gui.messageArea.append(response + "\n");
    			} catch (IOException e) {
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
        // Layout GUI
        gui.messageArea.setEditable(false);
        gui.frame.getContentPane().add(gui.dataField, "North");
        gui.frame.getContentPane().add(new JScrollPane(gui.messageArea), "Center");

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
                out.println(gui.dataField.getText());
                System.out.println("Client sent message " + gui.dataField.getText());

                gui.dataField.setText("");
            }
        });
    }

    public String login() {
    	try {
    		String response;
    		// first read two lines of description
    		for (int i = 0; i < 2; i++) {
    			gui.messageArea.append(in.readLine()+"\n");
    		}
    		response = in.readLine();
    		System.out.println(response.equals("AGAIN\n"));
    		System.out.println("response" + response);
    		if (response.equals("AGAIN")) {
    			String userName = JOptionPane.showInputDialog(
    	                gui.frame,
    	                "Enter your user name:",
    	                "Welcome to the EasyChat",
    	                JOptionPane.QUESTION_MESSAGE);
    			out.println(userName);
    			// try to login again
    			return login();
    		} else {
    			return response;
    		}
    	} catch (IOException e) {
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
        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        // write the login username to server
        out.println(userName);
        String response = login();
        
        // Consume the initial welcoming messages from the server
        gui.messageArea.append(response+"\n");
        for (int i = 0; i < 2; i++) {
            gui.messageArea.append(in.readLine() + "\n");
        }
        new ServerListener().start();
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