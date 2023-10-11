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
	 *
	 * */


	private String fullRequest;
	private String method;
	private String path;
	private String protocolVersion;
	private String fragmentIdentifier;
	private String nameOfRequestedFile;
	private String extensionOfRequestedFile;
	private Map<String, String> headers;
	private Map<String, String> queryParameters;

	private BufferedReader input;

	public HttpRequest(BufferedReader input) throws IOException {
		this.input = input;
		fullRequest = input.readLine();
		if (fullRequest != null) {
			fullRequest = fullRequest.strip();
		}
	}


	private void _getMethod() {
		method = fullRequest.split(" ")[0].strip();
	}

	private void _getPath() {
		path = fullRequest.split(" ")[1].strip();
	}

	private void _getProtocolVersion() {
		protocolVersion = fullRequest.split(" ")[2].strip();
	}

	private void _getHeaders() {
		Map<String, String> headers = new HashMap<>();
		String line;

		try {
			while (!(line = input.readLine()).isBlank()) {
				int colonIndex = line.indexOf(':');
				if (colonIndex > 0) {
					String headerName = line.substring(0, colonIndex).trim().toLowerCase();
					String headerValue = line.substring(colonIndex + 1).trim();
					headers.put(headerName, headerValue);
				}
			}
			if (!headers.isEmpty()) this.headers = headers;
		}
		catch (IOException e) {
			// Add a logging to a file
			e.printStackTrace();
		}
	}


	/*
	 * In this method we imply that in a URL we will have only one "#" and only one "?" sign. Also, we can't use "="
	 * in a URL not as separator for a key and value
	 * */
	private void _getQueryParameters() {
		Map<String, String> queryParameters = new LinkedHashMap<>();

		// The number of the first "?" sign
		int index = path.indexOf('?');
		if (index != -1) {
			String parameters = path.substring(index + 1, (path.indexOf('#') == -1) ? (path.length()) : (path.indexOf('#')));
			String kv[] = parameters.split("&");

			for (int i = 0; i < kv.length; i++) {
				queryParameters.put(kv[i].split("=")[0], kv[i].split("=")[1]);
			}
		}
		if (!queryParameters.isEmpty()) {
			this.queryParameters = queryParameters;
		}
	}

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

	private void _getExtensionOfRequestedFile() {
		if (nameOfRequestedFile.indexOf('.') != -1) {
			extensionOfRequestedFile = nameOfRequestedFile.split("\\.")[1].strip();
		} else {
			extensionOfRequestedFile = "";
		}

	}

	public String getNameOfRequestedFile() {
		if (nameOfRequestedFile == null) _getNameOfRequestedFile();
		return nameOfRequestedFile;
	}

	public String getExtensionOfRequestedFile() {
		if (extensionOfRequestedFile == null) _getExtensionOfRequestedFile();
		return extensionOfRequestedFile;
	}

	public String getFullRequest() {
		return fullRequest;
	}

	public String getMethod() {
		if (method == null) _getMethod();
		return method;
	}

	public String getPath() {
		if (path == null) _getPath();
		return path;
	}

	public String getProtocolVersion() {
		if (protocolVersion == null) _getProtocolVersion();
		return protocolVersion;
	}

	public Map<String, String> getHeaders(){
		if (headers == null) _getHeaders();
		return headers;
	}

	public Map<String, String> getQueryParameters() {
		if (queryParameters == null) _getQueryParameters();
		return queryParameters;
	}
}