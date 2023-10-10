package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Runner {
	private final int port;
	private final String pathToDir;

	public Runner(int port, String pathToDir) {
		this.port = port;
		this.pathToDir = pathToDir;
	}

	static int debugCount = 0;
	static LinkedList<Thread> threads = new LinkedList<>();

	private void start() {
		try (ServerSocket ss = new ServerSocket(port)) {
			Socket socket;
			while (true) {
				socket = ss.accept();
				UserHandler handler = new UserHandler(pathToDir, socket);
				Thread thr = new Thread(handler, "Thread #" + debugCount);
				threads.add(thr);
				thr.start();

//				try {
//					thr.join();
//				}
//				catch (InterruptedException e) {
//					throw new RuntimeException(e);
//				}

//				System.out.println("End of one request, " + debugCount);
				debugCount++;

//				System.out.println("==========");
//				for (Thread thread : threads) {
//					System.out.println(thread.getName());
//				}
//				System.out.println("==========");
				System.out.println();
			}
		}
		catch (IOException e) {
			// TODO: Add logging to a file
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		int port = 8081;
		String path = "/home/none/Dropbox/main1/Java/simple-http-java-web-server/files";
		new Runner(port, path).start();
	}
}
