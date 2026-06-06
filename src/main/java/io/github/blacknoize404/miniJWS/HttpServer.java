package io.github.blacknoize404.miniJWS;

import io.github.blacknoize404.miniJWS.primitives.HttpHandler;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.primitives.RequestRunner;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HttpServer {

    /**
     * Rutas configuradas,
     */
    private final Map<String, RequestRunner> routes;

    /**
     * Socket abierto a la comunicación
     */
    private final ServerSocket socket;

    /**
     * Distribuidor de carga de ejecución
     */
    private final Executor threadPool;

    /**
     * Manejador de peticiones
     */
    private HttpHandler handler;

    /**
     * Cantidad máxima de peticiones a ser procesadas a la vez.
     */
    private final int parallelism;

    /**
     * Nombre del servidor sin tildes.
     */
    public static final String serverName = "BI Tech";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public HttpServer(int port) throws IOException {
        routes = new HashMap<>();
        parallelism = 100;
        threadPool = Executors.newScheduledThreadPool(1);
        socket = new ServerSocket(port);
    }

    public void addRoute(HttpMethod opCode, String route, RequestRunner runner) {
        routes.put(opCode.name().concat(route), runner);
    }

    public void run() {

        this.isRunning.set(true);

        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] El servidor ha sido iniciado");
        handler = new HttpHandler(routes);

        while (isRunning.get()) {

            try (Socket client = socket.accept()) {

                handleConnection(client);

            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }

        }

        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] El servidor ha sido detenido");

    }

    public void idle() {

        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        this.isRunning.set(false);
        Thread.currentThread().interrupt();
    }

    /**
     * Captura cada petición o respuesta en un hilo ejecutado en un threadPool
     */
    private void handleConnection(Socket clientConnection) {
        System.out.println("----------------------------------------------");
        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Conexión entrante de: " + clientConnection);

        Runnable httpRequestRunner = () -> {

            try {

                // Si el cliente no envió ninguna información, pues cierro el socket
//                if (clientConnection.getInputStream().available() == 0) {
//
//                    clientConnection.close();
//                    return;
//                }


                handler.handleConnection(clientConnection.getInputStream(), clientConnection.getOutputStream());
            } catch (IOException e) {

                System.out.println(e.getLocalizedMessage());
            }

        };

        httpRequestRunner.run();
//        threadPool.execute(httpRequestRunner);
    }
}
