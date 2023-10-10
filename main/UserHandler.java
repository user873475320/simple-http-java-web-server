package main;

import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class UserHandler implements Runnable {

	private String pathToDir;
	private Socket socket;
	private HttpRequest requestObj = null;
	private HashMap<String, String> CONTENT_TYPES = new HashMap<>(Map.of(
			"txt", "text/plain",
			"html", "text/html",
			"jpg", "image/jpeg",
			"js", "application/javascript",
			"css", "text/css",
			"png", "image/png",
			"", "text/plain"
	));

	public UserHandler(String pathToDir, Socket socket) {
		this.pathToDir = pathToDir;
		this.socket = socket;
	}

	@Override
	public void run() {
		try (var input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		     var output = socket.getOutputStream()) {

			requestObj = new HttpRequest(input);

			String fullRequest = requestObj.getFullRequest();

			if (fullRequest != null) {
				Map<String, String> headers = requestObj.getHeaders();
				String requestPath = requestObj.getPath();
				String method = requestObj.getMethod();
				String protocolVersion = requestObj.getProtocolVersion();

				// Prints debug info
				printDebugInfo();

				// Make separate method for GET handling
				if (method.equals("GET")) {
					String fileName = requestObj.getNameOfRequestedFile();
					String fileExtension = requestObj.getExtensionOfRequestedFile();
					Path pathToFile = Path.of(pathToDir, fileName);
					Map<String, String> queryParameters = requestObj.getQueryParameters();

					if (Files.exists(pathToFile)) {
						if (!Files.isDirectory(pathToFile)) {
							String contentType = CONTENT_TYPES.get(fileExtension);
							byte[] fileBytes = Files.readAllBytes(pathToFile);
							sendHeaders(output, 200, "OK", contentType, fileBytes.length);
							output.write(fileBytes);
						} else {
							String statusText = "Error: \"File\" is directory";
							String contentType = CONTENT_TYPES.get("txt");
							sendHeaders(output, 404, statusText, contentType, statusText.length());
						}
					} else {
						String statusText = "Error: File Not Found";
						String contentType = CONTENT_TYPES.get("txt");
						sendHeaders(output, 404, statusText, contentType, statusText.length());
					}
				} else if (method.equals("POST")) {
					String path = requestObj.getPath();


					// Send a response to the client
					PrintStream ps = new PrintStream(output);
					ps.printf("HTTP/1.1 %s %s%n%n", 200, "nice");
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void printDebugInfo() {
		String fullRequest = requestObj.getFullRequest();
		Map<String, String> headers = requestObj.getHeaders();
		String requestPath = requestObj.getPath();
		String method = requestObj.getMethod();
		String protocolVersion = requestObj.getProtocolVersion();

		String fileName = requestObj.getNameOfRequestedFile();
		String fileExtension = requestObj.getExtensionOfRequestedFile();
		Path pathToFile = Path.of(pathToDir, fileName);
		Map<String, String> queryParameters = requestObj.getQueryParameters();

		System.out.println(fullRequest);
		System.out.println("-------Info from this request-------");
		System.out.println("Headers: ");
		if (headers != null) {
			for (String s : headers.keySet()) {
				System.out.println(s + " " + headers.get(s));
			}
		}
		System.out.println();

		// Print query parameters
		System.out.println("\nThe query parameters");
		if (queryParameters != null) {
			for (String s : queryParameters.keySet()) System.out.println(s + " " + queryParameters.get(s));
		}
		System.out.println();

		// Print another information
		System.out.println("Another information");

		System.out.println("fullRequest = " + fullRequest);
		System.out.println("method = " + method);
		System.out.println("requestPath = " + requestPath);
		System.out.println("protocolVersion = " + protocolVersion);
		System.out.println("pathToFile = " + pathToFile);
		System.out.println("pathToDir = " + pathToDir);
		System.out.println("fileName = " + fileName);
		System.out.println("fileExtension = " + fileExtension);
	}


	private void sendHeaders(OutputStream outputStream, int statusCode, String statusText, String contentType, int length) {

		PrintStream ps = new PrintStream(outputStream);
		ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
		ps.printf("Content-Type: %s%n", contentType);

		// Here is important moment: You must add one more %n in the end of the last "header string"
		ps.printf("Content-Length: %s%n%n", length);
	}
}
