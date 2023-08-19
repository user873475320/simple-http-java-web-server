package Practice.projects.HTTP_Server;

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

			String fullRequest = input.readLine();

			if (fullRequest != null) {
				System.out.println(fullRequest);

				String requestURL = getRequestURL(fullRequest);
				HashMap<String, String> headers = getRequestHeaders(input);
				String fileName = getFileName(requestURL);
				Path pathToFile = Path.of(pathToDir, fileName);

	//			// Print headers for debugging
	//			for (String s : headers.keySet()) {
	//				System.out.println(s + " " + headers.get(s));
	//			}
	//			System.out.println();
	//			System.out.println("fileName = " + fileName);
	//			System.out.println("requestURL = " + requestURL);
	//			System.out.println("path = " + path);
	//			System.out.println("contentType = " + contentType);

				if (Files.exists(pathToFile)) {
					if (!Files.isDirectory(pathToFile)) {
						String contentType = CONTENT_TYPES.get(getFileExtension(fileName));
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

	private String getMethod(String fullRequest) {
		return fullRequest.split(" ")[0].strip();
	}

	private String getRequestURL(String fullRequest) {
		return fullRequest.split(" ")[1].strip();
	}

	private String getFileName(String requestURL) {
		String[] pieces = requestURL.split("/");
		return pieces[pieces.length - 1].strip();
	}

	private String getFileExtension(String fileName) {
		return fileName.split("\\.")[1].strip();
	}

	private HashMap<String, String> getRequestHeaders(BufferedReader input) throws IOException{
		HashMap<String, String> headers = new HashMap<>();
		String line = null;

		while (!(line = input.readLine()).isBlank()) {
			int colonIndex = line.indexOf(':');
			if (colonIndex > 0) {
				String headerName = line.substring(0, colonIndex).trim().toLowerCase();
				String headerValue = line.substring(colonIndex + 1).trim();
				headers.put(headerName, headerValue);
			}
		}
		return headers;
	}

	private void sendHeaders(OutputStream outputStream, int statusCode, String statusText, String contentType, int length) {
		PrintStream ps = new PrintStream(outputStream);
		ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
		ps.printf("Content-Type: %s%n", contentType);

		// Here is important moment: You must add one more %n in the end of the last "header string"
		ps.printf("Content-Length: %s%n%n", length);
	}
}
