import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ClientGUI {
    JFrame frame;
    JTextField dataField;
    JTextArea messageArea;
    
    public ClientGUI() {
    	frame = new JFrame("Client");
        dataField = new JTextField(40);
        messageArea = new JTextArea(8, 60);
        // Layout GUI
        messageArea.setEditable(false);
        frame.getContentPane().add(dataField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
    }
}
