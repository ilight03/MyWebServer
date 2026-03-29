import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter; //for date headers

public class MyWebServer {
    private static final String SERVER_NAME = "MyWebServer";
    private static ServerSocket serverSocket;
    private static int port;
    //not tryna redo the date everytime    why not make a function for all the common headers
    private static String getDate() {
    return ZonedDateTime.now(java.time.ZoneId.of("GMT"))
            .format(DateTimeFormatter.RFC_1123_DATE_TIME);
   }

    private static String formatHttpDate(long epochMillis) {
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), java.time.ZoneId.of("GMT"))
            .format(DateTimeFormatter.RFC_1123_DATE_TIME);
   }

    private static void sendHeaders(PrintWriter out, String status, long contentLength, String contentType, String lastModified, String connectionHeader) {
    System.out.println(status);
    out.println(status);
    //get the date and then print it so its the same
    String date = getDate();
    System.out.println("Date: " + date);
    out.println("Date: " + date);

    System.out.println("Server: " + SERVER_NAME);
    out.println("Server: " + SERVER_NAME);

    System.out.println("Content-Length: " + contentLength);
    out.println("Content-Length: " + contentLength);

    if (contentType != null) {
        System.out.println("Content-Type: " + contentType);
        out.println("Content-Type: " + contentType);
    }

    if (lastModified != null) {
        System.out.println("Last-Modified: " + lastModified);
        out.println("Last-Modified: " + lastModified);
    }

    if (connectionHeader != null) {
        System.out.println("Connection: " + connectionHeader);
        out.println("Connection: " + connectionHeader);
    }

    System.out.println();
    out.println();

    out.flush();
}

private static String getContentType(String path) {
    if (path == null) {
        return "application/octet-stream";
    }

    if (path.endsWith(".html") || path.endsWith(".htm"))
        return "text/html";

    if (path.endsWith(".txt"))
        return "text/plain";

    if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
        return "image/jpeg";

    if (path.endsWith(".png"))
        return "image/png";

    if (path.endsWith(".gif"))
        return "image/gif";

    if (path.endsWith(".css"))
        return "text/css";

    if (path.endsWith(".js"))
        return "application/javascript";

    return "application/octet-stream"; // unknown type handler -> just send it as binary data
}

    private static void sendErrorResponse(PrintWriter out, String status, String method, String body, String connectionHeader) { //add method header parameter to make it more flexible for different error types
    byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    long contentLength = method.equals("HEAD") ? 0 : bodyBytes.length;
    sendHeaders(out, status, contentLength, "text/plain; charset=UTF-8", null, connectionHeader);
    if(!method.equals("HEAD")) { //only send body for get requests, not head
        out.print(body);
    }
    out.flush();
}

    public static void main(String[] args) throws IOException {
        port = 8888;

        serverSocket = new ServerSocket(port);
        System.out.println("The server is ready to receive on port " + port + "\n");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            Thread clientThread = new Thread(new ClientHandler(clientSocket));
            clientThread.start();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                // ###### Fill in Start ######
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter socketOutput = new PrintWriter(clientSocket.getOutputStream(), true);
                OutputStream rawOut = clientSocket.getOutputStream();

                while (true) {
                    String requestLine = socketInput.readLine();
                    if (requestLine == null) {
                        break;
                    }
                    if (requestLine.isBlank()) {
                        continue;
                    }

                    System.out.println(requestLine);

                    String[] parts = requestLine.split(" ");
                    if (parts.length < 3) {
                        sendErrorResponse(socketOutput, "HTTP/1.1 400 Bad Request", "UNKNOWN", "Bad Request", "close");
                        break;
                    }

                    String method = parts[0];
                    String path = parts[1];
                    String version = parts[2];

                    String line = null;
                    String ifModifiedSinceValue = null;
                    String connectionRequest = null;

                    while ((line = socketInput.readLine()) != null && !line.equals("")) {
                        System.out.println(line);
                        if (line.regionMatches(true, 0, "If-Modified-Since:", 0, "If-Modified-Since:".length())) {
                            ifModifiedSinceValue = line.substring("If-Modified-Since:".length()).trim();
                            System.out.println("If-Modified-Since header value: " + ifModifiedSinceValue);
                        }
                        if (line.regionMatches(true, 0, "Connection:", 0, "Connection:".length())) {
                            connectionRequest = line.substring("Connection:".length()).trim();
                        }
                    }

                    if (line == null) {
                        break;
                    }

                    boolean keepAlive = "HTTP/1.1".equalsIgnoreCase(version);
                    if (connectionRequest != null) {
                        if (connectionRequest.equalsIgnoreCase("close")) {
                            keepAlive = false;
                        } else if (connectionRequest.equalsIgnoreCase("keep-alive")) {
                            keepAlive = true;
                        }
                    }
                    String connectionResponse = keepAlive ? "keep-alive" : "close";

                    if (!method.equals("GET") && !method.equals("HEAD")) {
                        sendErrorResponse(socketOutput, "HTTP/1.1 501 Not Implemented", method, "Not Implemented", connectionResponse);
                        if (!keepAlive) {
                            break;
                        }
                        continue;
                    }

                    if (path.equals("/")) {
                        path = "/hello.html";
                    }
                    if (path.contains("../") || path.contains("..\\")) {
                        sendErrorResponse(socketOutput, "HTTP/1.1 403 Forbidden", method, "Forbidden", connectionResponse);
                        if (!keepAlive) {
                            break;
                        }
                        continue;
                    }

                    File root = new File(".").getCanonicalFile();
                    File target = new File(root, path.startsWith("/") ? path.substring(1) : path).getCanonicalFile();
                    if (!target.getPath().startsWith(root.getPath() + File.separator)) {
                        sendErrorResponse(socketOutput, "HTTP/1.1 403 Forbidden", method, "Forbidden", connectionResponse);
                        if (!keepAlive) {
                            break;
                        }
                        continue;
                    }

                    File file = target;
                    if (!file.exists() || !file.isFile()) {
                        sendErrorResponse(socketOutput, "HTTP/1.1 404 Not Found", method, "Not Found", connectionResponse);
                        System.out.println("Requested file not found: " + target.getPath());
                        if (!keepAlive) {
                            break;
                        }
                        continue;
                    }

                    System.out.println("Method = " + method);
                    System.out.println("Path = " + path);
                    System.out.println("Version = " + version);

                    ZonedDateTime ifModifiedSince = null;
                    if (ifModifiedSinceValue != null && !ifModifiedSinceValue.isEmpty()) {
                        try {
                            ifModifiedSince = ZonedDateTime.parse(ifModifiedSinceValue, DateTimeFormatter.RFC_1123_DATE_TIME);
                        } catch (Exception e) {
                            System.out.println("Invalid If-Modified-Since header: " + ifModifiedSinceValue);
                        }
                    }

                    if (ifModifiedSince != null) {
                        long fileMillis = file.lastModified();
                        long headerMillis = ifModifiedSince.toInstant().toEpochMilli();
                        System.out.println("If-Modified-Since parsed: " + ifModifiedSince);
                        System.out.println("File lastModified millis: " + fileMillis);
                        System.out.println("Header millis: " + headerMillis);

                        if ((fileMillis / 1000) <= (headerMillis / 1000)) {
                            sendHeaders(socketOutput, "HTTP/1.1 304 Not Modified", 0, null, null, connectionResponse);
                            if (!keepAlive) {
                                break;
                            }
                            continue;
                        }
                    }

                    System.out.println("\n");
                    String lastModified = formatHttpDate(file.lastModified());
                    sendHeaders(socketOutput, "HTTP/1.1 200 OK", file.length(), getContentType(path), lastModified, connectionResponse);

                    if (method.equals("GET")) {
                        FileInputStream fileInput = new FileInputStream(file);
                        int b;
                        while ((b = fileInput.read()) != -1) {
                            rawOut.write(b);
                        }
                        rawOut.flush();
                        fileInput.close();
                    }

                    if (!keepAlive) {
                        break;
                    }
                }

                // ###### Fill in End ######

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
