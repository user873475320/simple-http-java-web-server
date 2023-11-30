import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequest {
	/*
	 * Pay attention: This class can work only with URLs like these:
	 * https://blog.example.com/post?article=123#section
	 * https://blog.example.com/post?article=123
	 * https://blog.example.com/post
	 * https://blog.example.com
	 * In other words, we can only have one "?" or/and one "#" or nothing of two
	 * */

	// Instance variables to store parsed HTTP request information
	private String fullRequest;
	private String method;
	private String path;
	private String protocolVersion;
	private String nameOfRequestedFile;
	private String extensionOfRequestedFile;
	private Map<String, String> headers;
	private Map<String, String> queryParameters;
	private String delimiterOfPostRequestBody;

	private BufferedReader input;

	// Constructor to initialize HttpRequest with input BufferedReader
	public HttpRequest(BufferedReader input) throws IOException {
		this.input = input;
		fullRequest = input.readLine();
		if (fullRequest != null) {
			fullRequest = fullRequest.strip();
		}
	}

	// Method to extract HTTP method from the full request
	private void _getMethod() {
		method = fullRequest.split(" ")[0].strip();
	}

	// Method to extract the path from the full request
	private void _getPath() {
		path = fullRequest.split(" ")[1].strip();
	}

	// Method to extract the HTTP protocol version from the full request
	private void _getProtocolVersion() {
		protocolVersion = fullRequest.split(" ")[2].strip();
	}

	// Method to extract headers from the full request
	private void _getHeaders() {
		Map<String, String> headers = new HashMap<>();
		String line;

		try {
			while (!(line = input.readLine()).isBlank()) {

				int colonIndex = line.indexOf(':');
				if (colonIndex > 0) {
					String headerName = line.substring(0, colonIndex).trim().toLowerCase().strip();
					String headerValue = line.substring(colonIndex + 1).strip();
					headers.put(headerName, headerValue);
				}
			}
			if (!headers.isEmpty()) this.headers = headers;
		} catch (IOException e) {
			// Exception handling: Add logging to a file and propagate a runtime exception
			throw new RuntimeException(e);
		}
	}

	/*
	 * In this method, we imply that in a URL we will have only one "#" and only one "?" sign. Also, we can't use "="
	 * in a URL not as a separator for a key and value
	 * */
	// Method to extract query parameters from the path
	private void _getQueryParameters() {
		Map<String, String> queryParameters = new LinkedHashMap<>();

		// The number of the first "?" sign
		int index = path.indexOf('?');
		if (index != -1) {
			String parameters = path.substring(index + 1, (path.indexOf('#') == -1) ? (path.length()) : (path.indexOf('#'))).strip();
			String kv[] = parameters.split("&");

			for (int i = 0; i < kv.length; i++) {
				queryParameters.put(kv[i].split("=")[0].strip(), kv[i].split("=")[1].strip());
			}
		}
		if (!queryParameters.isEmpty()) {
			this.queryParameters = queryParameters;
		}
	}

	// Method to extract the name of the requested file from the path
	private void _getNameOfRequestedFile() {
		if (path.indexOf('?') != -1) {
			int index = path.lastIndexOf('/');
			nameOfRequestedFile = path.substring(index + 1, path.indexOf('?')).strip();
		} else if (path.indexOf('#') != -1 && path.indexOf('?') == -1) {
			int index = path.lastIndexOf('/');
			nameOfRequestedFile = path.substring(index + 1, path.indexOf('#')).strip();
		} else {
			int index = path.lastIndexOf('/');
			nameOfRequestedFile = path.substring(index + 1).strip();
		}
	}

	// Method to extract the extension of the requested file from the name
	private void _getExtensionOfRequestedFile() {
		if (nameOfRequestedFile.indexOf('.') != -1) {
			extensionOfRequestedFile = nameOfRequestedFile.split("\\.")[1].strip();
		} else {
			extensionOfRequestedFile = "";
		}
	}

	// Method to extract the delimiter of the POST request body
	private void _getDelimiterOfPostRequestBody() {
		String boundaryPattern = "boundary=";
		Map<String, String> map = getHeaders();
		for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
			String key = stringStringEntry.getKey();
			String value = stringStringEntry.getValue();

			if (key.equalsIgnoreCase("Content-Type")) {
				int tmpIndex = value.indexOf(";", value.indexOf(boundaryPattern));
				int endIndex = (tmpIndex != -1) ? tmpIndex : value.length();
				int beginIndex = value.indexOf(boundaryPattern) + boundaryPattern.length();
				delimiterOfPostRequestBody = value.substring(beginIndex, endIndex).strip();
			}
		}
	}

	// Getter method to retrieve the name of the requested file
	public String getNameOfRequestedFile() {
		if (nameOfRequestedFile == null) _getNameOfRequestedFile();
		return nameOfRequestedFile;
	}

	// Getter method to retrieve the extension of the requested file
	public String getExtensionOfRequestedFile() {
		if (extensionOfRequestedFile == null) _getExtensionOfRequestedFile();
		return extensionOfRequestedFile;
	}

	// Getter method to retrieve the full request
	public String getFullRequest() {
		return fullRequest;
	}

	// Getter method to retrieve the HTTP method
	public String getMethod() {
		if (method == null) _getMethod();
		return method;
	}

	// Getter method to retrieve the request path
	public String getPath() {
		if (path == null) _getPath();
		return path;
	}

	// Getter method to retrieve the HTTP protocol version
	public String getProtocolVersion() {
		if (protocolVersion == null) _getProtocolVersion();
		return protocolVersion;
	}

	// Getter method to retrieve the request headers
	public Map<String, String> getHeaders() {
		if (headers == null) _getHeaders();
		return headers;
	}

	// Getter method to retrieve the query parameters
	public Map<String, String> getQueryParameters() {
		if (queryParameters == null) _getQueryParameters();
		return queryParameters;
	}

	// Getter method to retrieve the delimiter of the POST request body
	public String getDelimiterOfPostRequestBody() {
		if (delimiterOfPostRequestBody == null) _getDelimiterOfPostRequestBody();
		return delimiterOfPostRequestBody;
	}
}
