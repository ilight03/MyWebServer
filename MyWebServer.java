import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MyWebServer {
    private static ServerSocket serverSocket;
    private static int port;

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
                String requestLine = socketInput.readLine();
                System.out.println(requestLine);
                //parse it to separately get the header parts 
                //i can do this bc i know what the header will look like.
                String[] parts = requestLine.split(" ");
                String method = parts[0];
                String path = parts[1];
                String version = parts[2];

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
                PrintWriter socketOutput = new PrintWriter(clientSocket.getOutputStream(), true);
                String response = "HTTP/1.1 200 OK";

                System.out.println(response);
                socketOutput.println(response);
                socketOutput.println();
                socketOutput.flush();

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
