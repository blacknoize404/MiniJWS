package io.github.blacknoize404.miniJWS.utilities;

import com.google.zxing.WriterException;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniQR.QRCodeGenerator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MinServer {

    static ServerSocket serverSocket;
    static Executor executor;

    static Path root = Path.of("data");

    static Map<String, Consumer<OutputStream>> routes = new HashMap<>() {{
        put("/", outputStream -> {
            File file = new File(root + "/index.html");
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file.toPath());

                String contentString = new String(bytes, StandardCharsets.UTF_8);

                String qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/enzona.apk", 200);
                contentString = contentString.replaceAll("\\{\\{enZonaSVG}}", qrString);

                qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/elsaborcubano.apk", 200);
                contentString = contentString.replaceAll("\\{\\{elSaborCubanoSVG}}", qrString);

                qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/ticket.apk", 200);
                contentString = contentString.replaceAll("\\{\\{ticketSVG}}", qrString);

                PrintWriter writer = new PrintWriter(outputStream, true);

                bytes = contentString.getBytes(StandardCharsets.UTF_8);
                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: text/html");
                writer.println("Content-Length: " + bytes.length);
                writer.println();
                outputStream.write(bytes);

            } catch (WriterException | IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        });
        put("/style.css", outputStream -> {
            File file = new File("data/style.css");
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file.toPath());

                PrintWriter writer = new PrintWriter(outputStream, true);

                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: text/css");
                writer.println("Content-Length: " + bytes.length);
                writer.println();
                outputStream.write(bytes);

            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        });
        put("/images/enzonalogo.png", outputStream -> {
            File file = new File("data/images/enzonalogo.png");
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file.toPath());

                PrintWriter writer = new PrintWriter(outputStream, true);

                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: image/png");
                writer.println("Content-Length: " + bytes.length);
                writer.println();
                outputStream.write(bytes);

            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        });
        put("/fonts/DMSans-VariableFont.ttf", outputStream -> {
            File file = new File("data/fonts/DMSans-VariableFont.ttf");
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file.toPath());

                PrintWriter writer = new PrintWriter(outputStream, true);

                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: application/octet-stream");
                writer.println("Content-Length: " + bytes.length);
                writer.println();
                outputStream.write(bytes);

            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        });
        put("/rep/enzona.apk", outputStream -> {
            File file = new File("data/rep/enzona.apk");
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file.toPath());

                PrintWriter writer = new PrintWriter(outputStream, true);

                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: application/octet-stream");
                writer.println("Content-Length: " + bytes.length);
                writer.println();
                outputStream.write(bytes);

            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        });
    }};


    public static void generateSite() {

        traverseDirectory(root.toFile());

    }


    public static void createRoute(File file) {

        assert file != null;

        String path = file.getPath()
                .replaceFirst(root.toString(), "")
                .replaceAll("\\\\", "/");

        int separator = file.getName().lastIndexOf(".");
        String fileFullName = file.getName();
        String fileExtension = fileFullName.substring(separator + 1);

        if (path.equals("/index.html")) {
            path = path.replace("index.html", "");
        } else if (fileFullName.endsWith("index.html")) {
            path = path.replace("index.html", "");
        }

        String finalPath = path;
        routes.put(path, outputStream -> {

            try {
                byte[] bytes = Files.readAllBytes(file.toPath());

                ContentType contentType = ContentType
                        .getTypeOf(fileExtension.toUpperCase())
                        .orElse(ContentType.FILE);

                PrintWriter writer = new PrintWriter(outputStream, true);

                if (finalPath.equals("/")) {

                    String contentString = new String(bytes, StandardCharsets.UTF_8);

                    String qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/data/cu.xetid.apk.enzona-v20000.apk", 200);
                    contentString = contentString.replaceAll("\\{\\{enZonaSVG}}", qrString);

                    qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/data/net.xutil.saborcubano.saborcubano-v24.apk", 200);
                    contentString = contentString.replaceAll("\\{\\{elSaborCubanoSVG}}", qrString);

                    qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/data/cu.xetid.ticket-v37.apk", 200);
                    contentString = contentString.replaceAll("\\{\\{ticketSVG}}", qrString);

                    bytes = contentString.getBytes(StandardCharsets.UTF_8);

                }

                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: " + contentType.getType());
                writer.println("Content-Length: " + bytes.length);
                writer.println();
                outputStream.write(bytes);

            } catch (WriterException | IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        });

        if (file.isDirectory()) throw new IllegalArgumentException("Error creando una ruta con un directorio");

    }


    public static void traverseDirectory(File f) {

        if (!f.isDirectory()) {
            createRoute(f);
            return;
        }

        File[] files = f.listFiles();

        assert files != null;

        for (File file : files) {
            traverseDirectory(file);
        }

    }

    public static void main(String[] args) throws IOException {

        serverSocket = new ServerSocket(80);
        executor = Executors.newFixedThreadPool(4);

        routes.clear();
        generateSite();

        System.out.println("Server is listening on port 80");

        while (true) {
            Socket socket = serverSocket.accept();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    // Read the HTTP request
                    MinRequest minRequest;
                    try {
                        System.out.println("[" + Thread.currentThread().getName() + "] New client connected");
                        minRequest = MinRequest.create(socket.getInputStream());
                        System.out.println(minRequest);

                        // Send response
                        Consumer<OutputStream> response = routes.get(minRequest.uri().getRawPath());

                        if (response != null) response.accept(socket.getOutputStream());

                        socket.getOutputStream().flush();
                        socket.close();
                    } catch (IOException | URISyntaxException e) {
                        System.out.println(e.getLocalizedMessage());
                    }

                }
            };

            executor.execute(r);

        }

    }

}
