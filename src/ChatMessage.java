import java.io.Serializable;

public class ChatMessage implements Serializable {
	static final int BROAD = 0;
	static final int LOG = 1;
	static final int UPDATE = 2;
	static final int PRIV = 3;
	static final int AGAIN = 4;
	
	int type;
	String from;
	String content;
	String target;  // for priv message
	
	public ChatMessage(int type, String content, String target) {
		this.type = type;
		this.content = content;
		this.target = target;
	}
}
