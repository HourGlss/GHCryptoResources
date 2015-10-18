package simpleObjectServer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatObjServer {

	/**
	 * The port that the server listens on.
	 */
	private static final int PORT = 9001;

	/**
	 * The set of all names of clients in the chat room.  Maintained
	 * so that we can check that new clients are not registering name
	 * already in use.
	 */
	private static HashSet<String> names = new HashSet<String>();

	/**
	 * The set of all the print writers for all the clients.  This
	 * set is kept so we can easily broadcast messages.
	 */
	private static HashSet<ObjectOutputStream> writers = new HashSet<ObjectOutputStream>();

	/**
	 * The appplication main method, which just listens on a port and
	 * spawns handler threads.
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("The obj chat server is running.");
		ServerSocket listener = new ServerSocket(PORT,100);
		try {
			System.out.println("Inside the try");
			while (true) {
				System.out.println("Start the Listener");
				new Handler(listener.accept()).start();
			}
		} finally {
			System.out.println("close the Listener");

			listener.close();
		}
	}

	/**
	 * A handler thread class.  Handlers are spawned from the listening
	 * loop and are responsible for a dealing with a single client
	 * and broadcasting its messages.
	 */
	private static class Handler extends Thread {
		private String name;
		private Socket socket;
		private ObjectInputStream in;
		private ObjectOutputStream out;

		/**
		 * Constructs a handler thread, squirreling away the socket.
		 * All the interesting work is done in the run method.
		 */
		public Handler(Socket socket) {
			System.out.println("Handler Constructor");
			this.socket = socket;
		}

		/**
		 * Services this thread's client by repeatedly requesting a
		 * screen name until a unique one has been submitted, then
		 * acknowledges the name and registers the output stream for
		 * the client in a global set, then repeatedly gets inputs and
		 * broadcasts them.
		 */
		public void run() {
			// Create character streams for the socket.
			System.out.println("in/out 1");
			try {
				out = new ObjectOutputStream(socket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("in/out 2");

			// Request a name from this client.  Keep requesting until
			// a name is submitted that is not already used.  Note that
			// checking for the existence of a name and adding the name
			// must be done while locking the set of names.
			try {
				while (true)
				{

					out.writeObject("SUBMITNAME");

					out.flush();
					System.out.println("write SUBMITNAME");
					name = ( String ) in.readObject(); 
					System.out.println(name);
					if (name == null) {
						return;
					}
					synchronized (names) {
						if (!names.contains(name)) {
							names.add(name);
							break;
						}
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Now that a successful name has been chosen, add the
			// socket's print writer to the set of all writers so
			// this client can receive broadcast messages.
			try {
				out.writeObject("NAMEACCEPTED");

				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (ObjectOutputStream writer : writers) {
				try {
					writer.writeObject("MESSAGE " + name + " has connected");
					writer.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			writers.add(out);


			// Accept messages from this client and broadcast them.
			// Ignore other clients that cannot be broadcasted to.
			try {
				while (true){
					String input = "";
					Object obj;

					obj = in.readObject();

					System.out.println("input getclass "+input.getClass());
					System.out.println("obj getclass   "+obj.getClass());
					if(obj != null && obj.getClass() == input.getClass()){
						input = (String) obj;

						for (ObjectOutputStream writer : writers) {
							writer.writeObject("MESSAGE " + name + ": " + input);
							writer.flush();
						}
					}else{
						if(obj != null){
							System.out.println("it all worked");
							for (ObjectOutputStream writer : writers) {
								writer.writeObject(obj);
								writer.flush();
							}
							System.out.println(obj.toString());
						}
					}
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				System.out.println("Finally");
				// This client is going down!  Remove its name and its print
				// writer from the sets, and close its socket.
				if (name != null) {
					names.remove(name);
				}
				if (out != null) {
					writers.remove(out);
				}
				try {
					for (ObjectOutputStream writer : writers) {
						writer.writeObject("MESSAGE " + name + " has disconnected");
						writer.flush();
					}
					socket.close();
				} catch (IOException e) {
				}
			}

		}
	}
}