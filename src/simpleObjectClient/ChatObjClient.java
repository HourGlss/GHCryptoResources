package simpleObjectClient;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatObjClient {

	ObjectInputStream in;
	ObjectOutputStream out;
	JFrame frame = new JFrame("Chat Client");
	JTextField textField = new JTextField("Write your messages here!");
	JTextArea messageArea = new JTextArea(8, 40);



	/**
	 * Constructs the client by laying out the GUI and registering a
	 * listener with the textfield so that pressing Return in the
	 * listener sends the textfield contents to the server.  Note
	 * however that the textfield is initially NOT editable, and
	 * only becomes editable AFTER the client receives the NAMEACCEPTED
	 * message from the server.
	 */
	public ChatObjClient() {

		// Layout GUI
		textField.setEditable(false);
		messageArea.setEditable(false);
		frame.getContentPane().add(textField, "North");
		frame.getContentPane().add(new JScrollPane(messageArea), "Center");
		frame.pack();

		// Add Listeners
		textField.addActionListener(new ActionListener() {
			/**
			 * Responds to pressing the enter key in the textfield by sending
			 * the contents of the text field to the server.    Then clear
			 * the text area in preparation for the next message.
			 */
			public void actionPerformed(ActionEvent e) {
				String outMessage = textField.getText();
				if(!outMessage.startsWith("/")){
					try {
						out.writeObject(outMessage);
						out.flush();
					} catch (IOException e1) {
						System.out.println("Failed to send message");
					}
					textField.setText("");
				}else{
					String[] breakDown = outMessage.split(",");
					SerObj so = new SerObj(Integer.parseInt(breakDown[0].substring(1)),breakDown[1]);
					System.out.println(so.toString());
					try {
						System.out.println(so.getClass());
						out.writeObject(so);
						out.flush();
					} catch (IOException e1) {
						System.out.println("Failed to send object");
						frame.dispose();
					}
					textField.setText("");

				}
			}
		});
	}

	/**
	 * Prompt for and return the address of the server.
	 */
	private String getServerAddress() {
		return JOptionPane.showInputDialog(
				frame,
				"Enter IP Address of the Server:",
				"Welcome to the Chatter",
				JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * Prompt for and return the desired screen name.
	 */
	private String getName() {
		return JOptionPane.showInputDialog(
				frame,
				"Choose a screen name:",
				"Screen name selection",
				JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * Connects to the server then enters the processing loop.
	 * @throws IOException 
	 */
	@SuppressWarnings("resource")
	private void run() throws IOException{
		System.out.println("Client obj started");

		// Make connection and initialize streams
		String serverAddress = getServerAddress();
		System.out.println(serverAddress);
		Socket socket;
		try {
			System.out.println("open the server socket");
			socket = new Socket(serverAddress, 9001);
			try {
				System.out.println("open the output stream"); 
				out = new ObjectOutputStream(socket.getOutputStream());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				System.out.println("open the input stream"); 
				in = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			System.out.println("Cant connect to "+serverAddress);
		}



		// Process all messages from server, according to the protocol.
		System.out.println("make it this far");

		while (true){
			String line = "";
			Object obj = null;
			try {
				obj = in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			SerObj soTest = new SerObj(1, "");
			SerObj so = null;
			if(obj != null){
				if(obj.getClass() == line.getClass()) {
					line = (String)obj;

					System.out.println("Line: "+line);
					if (line.startsWith("SUBMITNAME")) {
						out.writeObject(getName());
						out.flush();
					} else if (line.startsWith("NAMEACCEPTED")) {
						textField.setEditable(true);
					} else if (line.startsWith("MESSAGE")) {
						messageArea.append(line.substring(8) + "\n");
					}
				}else if(obj.getClass() == soTest.getClass()){
					so = (SerObj)obj;
					messageArea.append("recieved "+so.toString() + "\n");
				}
			}
		}

	}

	/**
	 * Runs the client as an application with a closeable frame.
	 */
	public static void main(String[] args) throws Exception {
		ChatObjClient client = new ChatObjClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}
}