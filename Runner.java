package Practice.projects.HTTP_Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Runner {
	private int port;
	private String pathToDir;

	public Runner(int port, String pathToDir) {
		this.port = port;
		this.pathToDir = pathToDir;
	}

	static int debugCount = 0;

	private void start() {
		try (ServerSocket ss = new ServerSocket(port)) {
			Socket socket = null;
			while (true) {
				socket = ss.accept();
				UserHandler handler = new UserHandler(pathToDir, socket);
				Thread thr = new Thread(handler);
				thr.start();

//				try {
//					thr.join();
//				}
//				catch (InterruptedException e) {
//					throw new RuntimeException(e);
//				}
//
//				System.out.println("End of one request, " + debugCount); // Why does this statement execute two times?
//				debugCount++;
			}
		}
		catch (IOException e) {
			// TODO: Add logging to a file
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		int port = 8081;
		String path = "/home/none/Dropbox/main1/Java/Learn_Java/Practice/projects/HTTP_Server/files";
		new Runner(port, path).start();
	}
}
