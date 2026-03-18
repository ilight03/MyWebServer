import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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

    private static void sendHeaders(PrintWriter out, String status, long contentLength, String contentType) {
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

    private static void sendErrorResponse(PrintWriter out, String status, String method, String body) { //add method header parameter to make it more flexible for different error types
    byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    sendHeaders(out, status, bodyBytes.length, "text/plain; charset=UTF-8");
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
                String requestLine = socketInput.readLine();
                System.out.println(requestLine);
                if (requestLine == null || requestLine.isBlank()) { //since i added method paramater, and 400's call error response before method is parsed, just gonna use UNKNOWN for now
                    sendErrorResponse(socketOutput, "HTTP/1.1 400 Bad Request","UNKNOWN","Bad Request");
                    return;
                }
                //parse it to separately get the header parts 
                //i can do this bc i know what the header will look like.
                String[] parts = requestLine.split(" ");
                if (parts.length < 3) {
                    sendErrorResponse(socketOutput, "HTTP/1.1 400 Bad Request","UNKNOWN","Bad Request");
                    return;
                }
                String method = parts[0];
                if (!method.equals("GET") && !method.equals("HEAD")) {
                    sendErrorResponse(socketOutput, "HTTP/1.1 501 Not Implemented",method, "Not Implemented");
                    return;
                }

                String path = parts[1];
                if(path.equals("/")){//wrote a little hello html file for quick testing. 
                   path = "/hello.html";
                }
                
                String version = parts[2];
                File file = new File("." + path);
                if (!file.exists() || !file.isFile()) {
                    sendErrorResponse(socketOutput, "HTTP/1.1 404 Not Found", method, "Not Found");
                    System.out.println("Requested file not found: " + path);
                    return;
                }
                
                System.out.println("Method = " + method);
                System.out.println("Path = " + path);
                System.out.println("Version = " + version);

                String line = null;
                //gotta read the first couple headers separately first
                //DO NOT SPLIT THE REST, THAT BROKE EVERYTHING
                
                while( (line=socketInput.readLine()) != null && !line.equals("")) {
                    System.out.println(line);
                }

                System.out.println("\n");
                OutputStream rawOut = clientSocket.getOutputStream();
                sendHeaders(socketOutput, "HTTP/1.1 200 OK", file.length(), getContentType(path)); 

                if (method.equals("GET")) { //file output is get only
                    FileInputStream fileInput = new FileInputStream(file);
                    int b;
                    while((b=fileInput.read()) != -1){
                        rawOut.write(b);
                    }
                    rawOut.flush();
                    fileInput.close();
                } //HEAD doesn't send body so we don't need to do anything else for it.

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
