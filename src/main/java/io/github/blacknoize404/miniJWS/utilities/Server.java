package io.github.blacknoize404.miniJWS.utilities;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(80)) {
            System.out.println("Server is listening on port 80");

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("New client connected");

                    // Read the HTTP request
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    while (!(line = in.readLine()).isEmpty()) {
                        System.out.println("-> " + line);
                    }

                    // Load the binary file
                    File file = new File("data/index.html");
                    byte[] fileContent = new byte[(int) file.length()];
                    try (FileInputStream fis = new FileInputStream(file)) {
                        fis.read(fileContent);
                    }

                    // Write the HTTP response
                    OutputStream out = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(out, true);

                    writer.println("HTTP/1.1 200 OK");
                    writer.println("Content-Type: text/html");
                    writer.println("Content-Length: " + fileContent.length);
                    writer.println();
                    out.write(fileContent);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
