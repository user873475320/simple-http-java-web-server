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

			HttpRequest requestObj = new HttpRequest(input);

			String fullRequest = requestObj.getFullRequest();

			if (fullRequest != null) {
				Map<String, String> headers = requestObj.getHeaders();
				String requestPath = requestObj.getPath();
				String method = requestObj.getMethod();
				String protocolVersion = requestObj.getProtocolVersion();
				System.out.println(fullRequest);

				String fileName = requestObj.getNameOfRequestedFile();
				String fileExtension = requestObj.getExtensionOfRequestedFile();
				Path pathToFile = Path.of(pathToDir, fileName);
				Map<String, String> queryParameters = requestObj.getQueryParameters();

//				 Print headers for debugging
//				for (String s : headers.keySet()) {
//					System.out.println(s + " " + headers.get(s));
//				}

//				// Print query parameters
//				System.out.println("--------------");
//				for (String s : queryParameters.keySet()) {
//					System.out.println(s + " " + queryParameters.get(s));
//				}
//				System.out.println("--------------");

	//			System.out.println();
	//			System.out.println("fileName = " + fileName);
	//			System.out.println("requestURL = " + requestURL);
	//			System.out.println("path = " + path);
	//			System.out.println("contentType = " + contentType);

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
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private void sendHeaders(OutputStream outputStream, int statusCode, String statusText, String contentType, int length) {
		PrintStream ps = new PrintStream(outputStream);
		ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
		ps.printf("Content-Type: %s%n", contentType);

		// Here is important moment: You must add one more %n in the end of the last "header string"
		ps.printf("Content-Length: %s%n%n", length);
	}
}
