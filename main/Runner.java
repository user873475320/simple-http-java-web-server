import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Runner {
	// Constructor to initialize necessary parameters for the server
	private final int port;
	private final String pathToDir;
	private final String uploadedFolder;
	private final String redirectLink;

	public Runner(int port, String pathToDir, String uploadedFolder, String redirectLink) {
		this.port = port;
		this.pathToDir = pathToDir;
		this.uploadedFolder = uploadedFolder;
		this.redirectLink = redirectLink;
	}

	// Counter for tracking the number of threads spawned by the server
	static int threadCount = 0;
	// List to keep track of active threads(just as "debug" info)
	static LinkedList<Thread> threads = new LinkedList<>();

	// Method to start the server and handle incoming connections
	private void start() {
		try (ServerSocket ss = new ServerSocket(port)) {
			Socket socket;

			// Infinite loop to continuously accept incoming connections
			while (true) {
				socket = ss.accept();
				// Create a new UserHandler for each incoming connection
				UserHandler handler = new UserHandler(socket, pathToDir, uploadedFolder, redirectLink);
				// Create a new thread for each UserHandler instance
				Thread thr = new Thread(handler, "Thread #" + threadCount);
				// Add the thread to the list of active threads
				threads.add(thr);
				// Start the thread to handle the current connection
				thr.start();
			}
		}
		catch (IOException e) {
			// Exception handling: Log the error to a file and propagate a runtime exception
			// for higher-level error handling
			throw new RuntimeException(e);
		}
	}

	// Main method to launch the server
	public static void main(String[] args) {
		// Default server parameters
		int port = 8080;
		String pathToDir = "/home/none/Dropbox/main1/Java/simple-http-java-web-server/resources/files";
		// The uploaded folder is in the "resources" folder in the project
		String uploadedFolder = "/uploadedFiles";
		String redirectLink = "http://localhost:8080/upload.html";

		// Create a new Runner instance with specified parameters and start the server
		new Runner(port, pathToDir, uploadedFolder, redirectLink).start();
	}
}
