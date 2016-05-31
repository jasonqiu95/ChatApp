import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ClientGUI implements ListSelectionListener {
    JFrame frame;
    JPanel main;
    JTextField dataField;
    JTextArea messageArea;
    JList list;
    JSplitPane splitPane;
    JLabel user;
    JScrollPane listScrollPane;
    String[] userNames = { "All"};
    ChatClient client;
    
    public ClientGUI() {
    	frame = new JFrame("Client");
    	main = new JPanel();
        dataField = new JTextField(40);
        messageArea = new JTextArea(8, 60);
        // Layout GUI
        messageArea.setEditable(false);
        main.add(dataField);
        main.add(new JScrollPane(messageArea));

        list = new JList(userNames);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.addListSelectionListener(this);
         
        
        listScrollPane = new JScrollPane(list);
        user = new JLabel();
        user.setFont(user.getFont().deriveFont(Font.ITALIC));
        user.setHorizontalAlignment(JLabel.CENTER);
         
        JScrollPane userScrollPane = new JScrollPane(user);
 
        //Create a split pane with the two scroll panes in it.
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                   listScrollPane, main);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(150);
 
        //Provide minimum sizes for the two components in the split pane.
        Dimension minimumSize = new Dimension(100, 50);
        listScrollPane.setMinimumSize(minimumSize);
        userScrollPane.setMinimumSize(minimumSize);
 
        //Provide a preferred size for the split pane.
        splitPane.setPreferredSize(new Dimension(850, 300));
        frame.getContentPane().add(splitPane, "Center");
    }

	@Override
	public void valueChanged(ListSelectionEvent e) {
		JList list = (JList) e.getSource();
		client.target = userNames[list.getSelectedIndex()];
	}
	
	public void changeList(String[] values) {
		String[] names = new String[values.length + 1];
		names[0] = "All";
		for (int i = 1; i < names.length; i++) {
			names[i] = values[i - 1];
		}
		userNames = names;
		list = new JList(names);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.addListSelectionListener(this);
        listScrollPane.setViewportView(list);
	}
}
