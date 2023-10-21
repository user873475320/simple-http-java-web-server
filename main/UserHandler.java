import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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

	// UPLOAD_FOLDER specifies regarding "pathToDir"
	private String uploadedFolder;
	private String redirectLink;
	private final String FILE_NOT_FOUND = "Error: File Not Found";
	private final String FILE_IS_DIRECTORY = "Error: \"File\" is directory";


	// socket, pathToDir, uploadedFolder, redirectLink
	public UserHandler(Socket socket, String pathToDir, String uploadedFolder, String redirectLink) {
		this.pathToDir = pathToDir;
		this.socket = socket;
		this.uploadedFolder = uploadedFolder;
		this.redirectLink = redirectLink;
	}

	@Override
	public void run() {
		try (var socketInputStream = socket.getInputStream();
		     var socketBufferedReader = new BufferedReader(new InputStreamReader(socketInputStream));
		     var socketOutputStream = socket.getOutputStream();
		     var socketBufferedWriter = new BufferedWriter(new OutputStreamWriter(socketOutputStream))) {

			requestObj = new HttpRequest(socketBufferedReader);

			String fullRequest = requestObj.getFullRequest();

			if (fullRequest != null) {
				String requestPath = requestObj.getPath();
				String method = requestObj.getMethod();

				// Make separate method for GET handling
				if (method.equals("GET")) getHandling(socketOutputStream);
				else if (method.equals("POST") && requestPath.equals("/upload"))
					postHandling(socketBufferedReader, socketOutputStream);
			}
		}
		catch (IOException e) {
			// TODO: Add logging to a file
			throw new RuntimeException(e);
		}
	}

	private void redirect(String link, String message, int code, OutputStream socketOutputStream) throws IOException {
		String httpResponse = "HTTP/1.1 " + code + " " + message + "\r\n" + "Location: " + link + "\r\n\r\n";
		PrintStream ps = new PrintStream(socketOutputStream);
		ps.write(httpResponse.getBytes("UTF-8"));
	}

	private void postHandling(BufferedReader socketBufferedReader, OutputStream socketOutputStream) {
		printDebugInfo();

		// Define the directory where uploaded files will be saved
		File uploadDirectory = new File(pathToDir, uploadedFolder);
		if (!uploadDirectory.exists()) uploadDirectory.mkdirs();

		try {
			uploadTxtFileUsingBufferedReader(uploadDirectory, socketBufferedReader);
		}
		catch (IllegalArgumentException e) {
			try {
				redirect(redirectLink, "Not a single file was selected. Redirecting to the start page",
						302, socketOutputStream);
				return;
			}
			catch (IOException exception) {
				// TODO: Add logging to a file
				throw new RuntimeException(exception);
			}
		}

		try {
			redirect(redirectLink, "File has been uploaded successfully. Redirecting to the start page",
					302, socketOutputStream);
		}
		catch (IOException e) {
			// TODO: Add logging to a file
			throw new RuntimeException(e);
		}
	}

	private int countFileOccurrences(String fileName, File pathToUploadedDir) {
		int count = 0;
		if (pathToUploadedDir.isDirectory()) {
			File[] files = pathToUploadedDir.listFiles();

			if (files != null) {
				String[] stringFiles = new String[files.length];
				for (int i = 0; i < files.length; i++) stringFiles[i] = files[i].getName();
				List<String> listOfFiles = new ArrayList<>(List.of(stringFiles));

				while (true) {
					if (count == 0) {
						if (listOfFiles.contains(fileName)) count++;
						else return 0;
					} else {
						String namePart = fileName.split("\\.")[0];
						String extensionPart = fileName.split("\\.")[1];
						String tmpFileName = namePart + "(" + count + ")" + "." + extensionPart;
						if (listOfFiles.contains(tmpFileName)) count++;
						else return count;
					}
				}
			}
		}
		return count;
	}

	private void uploadTxtFileUsingBufferedReader(File uploadDirectory, BufferedReader socketBufferedReader) {
		String fileName = "";

		try {
			// Getting the name of the received file from the POST request body and reading other headers from the body
			String tmpLine;
			while (!(tmpLine = socketBufferedReader.readLine()).isEmpty()) {
				if (tmpLine.contains("Content-Disposition")) {
					String boundaryPattern = "filename=";
					int tmpIndex = tmpLine.indexOf(";", tmpLine.indexOf(boundaryPattern));
					int endIndex = (tmpIndex != -1) ? tmpIndex : tmpLine.length();
					int beginIndex = tmpLine.indexOf(boundaryPattern) + boundaryPattern.length();
					fileName = tmpLine.substring(beginIndex, endIndex).strip().replace("\"", "");
				}
			}
		}
		catch (IOException e) {
			// TODO: Add logging to a file
			throw new RuntimeException(e);
		}

		if (fileName.isEmpty()) {
			throw new IllegalArgumentException("Error: The file name wasn't received");
		}

		// Change a file name
		int fileCount = countFileOccurrences(fileName, uploadDirectory);
		if (fileCount != 0) {
			String namePart = fileName.split("\\.")[0];
			String extensionPart = fileName.split("\\.")[1];
			fileName = namePart + "(" + fileCount + ")" + "." + extensionPart;
		}

		// Write the data to the output file
		File outputFile = new File(uploadDirectory, fileName);
		try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
			String tmpLine;
			String endString = requestObj.getDelimiterOfPostRequestBody() + "--";
			String previousLine = socketBufferedReader.readLine();
			while (true) {
				tmpLine = socketBufferedReader.readLine();
				if (tmpLine.contains(endString)) {
					outputStream.write(previousLine.getBytes());
					break;
				} else {
					outputStream.write((previousLine + "\n").getBytes());
					previousLine = tmpLine;
				}
			}
		}
		catch (IOException e) {
			// TODO: Add logging to a file
			throw new RuntimeException(e);
		}
	}

	// This method doesn't work as expected. So you must rewrite it. This method here, because it shows how to
	// use InputStream in order to write the body of POST request
	private void uploadFileUsingInputStream(File uploadDirectory, String nameOfUploadedFile, InputStream socketInputStream) {
		File outputFile = new File(uploadDirectory, nameOfUploadedFile);
		try (FileOutputStream fos = new FileOutputStream(outputFile)) {
			byte[] buffer = new byte[1024]; // Adjust the buffer size as needed
			// !TROUBLE CAN TAKES PLACE BECAUSE I USE INITIALLY USE "READER" CLASS IN ORDER TO READ THE INFO FROM
			// !THE POST HTTP REQUEST BUT NOW I USE "INPUTSTREAM" CLASS
			int bytesRead;
			while ((bytesRead = socketInputStream.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}

		}
		catch (IOException e) {
			// TODO: Add logging to a file
			throw new RuntimeException(e);
		}
	}


	private void getHandling(OutputStream socketOutputStream) {
		String fileName = requestObj.getNameOfRequestedFile();
		String fileExtension = requestObj.getExtensionOfRequestedFile();
		Path pathToFile = Path.of(pathToDir, fileName);

		// Prints debug info
		printDebugInfo();

		if (Files.exists(pathToFile)) {
			if (!Files.isDirectory(pathToFile)) {
				String contentType = CONTENT_TYPES.get(fileExtension);

				try {
					byte[] fileBytes = Files.readAllBytes(pathToFile);
					Map<String, String> headersMap = new HashMap<>(Map.of(
							"Content-Type", String.valueOf(fileBytes.length),
							"Content-Length", contentType
					));
					sendHeaders(socketOutputStream, 200, "OK", headersMap);
					socketOutputStream.write(fileBytes);
				}
				catch (IOException e) {
					// TODO: Add logging to a file
					throw new RuntimeException(e);
				}
			} else {
				String contentType = CONTENT_TYPES.get("");
				Map<String, String> headersMap = new HashMap<>(Map.of(
						"Content-Type", String.valueOf(FILE_IS_DIRECTORY.length()),
						"Content-Length", contentType
				));

				sendHeaders(socketOutputStream, 404, FILE_IS_DIRECTORY, headersMap);
			}
		} else {
			String contentType = CONTENT_TYPES.get("");
			Map<String, String> headersMap = new HashMap<>(Map.of(
					"Content-Type", String.valueOf(FILE_IS_DIRECTORY.length()),
					"Content-Length", contentType
			));
			sendHeaders(socketOutputStream, 404, FILE_NOT_FOUND, headersMap);
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
		if (method.equalsIgnoreCase("GET")) {
			System.out.println("pathToFile = " + pathToFile);
			System.out.println("pathToDir = " + pathToDir);
			System.out.println("fileName = " + fileName);
			System.out.println("fileExtension = " + fileExtension);
		}
	}


	private void sendHeaders(OutputStream outputStream, int statusCode, String statusText, Map<String, String> headersMap) {
		PrintStream ps = new PrintStream(outputStream);
		ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);

		var headersList = new ArrayList<>(headersMap.keySet());
		for (int i = 0; i < headersList.size(); i++) {
			// Here is important moment: You must add one more %n in the end of the last "header's line"
			if ((i == headersMap.size() - 1) && (headersList.get(i) != null)) {
				ps.printf("%s: %s%n%n", headersList.get(i), headersMap.get(headersList.get(i)));
			} else if ((i != headersMap.size() - 1) && (headersList.get(i) != null)) {
				ps.printf("%s: %s%n", headersList.get(i), headersMap.get(headersList.get(i)));
			}
		}
	}
}
