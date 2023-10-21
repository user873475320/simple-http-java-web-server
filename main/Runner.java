import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Runner {
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

	static int threadCount = 0;
	static LinkedList<Thread> threads = new LinkedList<>();

	private void start() {
		try (ServerSocket ss = new ServerSocket(port)) {
			Socket socket;

			while (true) {
				socket = ss.accept();
				UserHandler handler = new UserHandler(socket, pathToDir, uploadedFolder, redirectLink);
				Thread thr = new Thread(handler, "Thread #" + threadCount);
				threads.add(thr);
				thr.start();
			}
		}
		catch (IOException e) {
			// TODO: Add logging to a file
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		int port = 8080;
		String pathToDir = "/home/none/Dropbox/main1/Java/simple-http-java-web-server/resources/files";
		String uploadedFolder = "/uploadedFiles";
		String redirectLink = "http://localhost:8080/upload.html";
		new Runner(port, pathToDir, uploadedFolder, redirectLink).start();
	}
}
