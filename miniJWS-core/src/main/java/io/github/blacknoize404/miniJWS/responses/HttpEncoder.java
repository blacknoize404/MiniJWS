package io.github.blacknoize404.miniJWS.responses;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class HttpEncoder {

    private HttpEncoder() {}

    public static void sendResponse(HttpResponse response, OutputStream outputStream) {
        try {
            var writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII));

            int statusCode = response.getStatusCode();
            String statusMsg = getStatusMessage(statusCode);
            String protocol = response.getProtocolVersion();

            writer.write(protocol + " " + statusCode + " " + statusMsg + "\r\n");

            for (var entry : response.getHeaders().entrySet()) {
                String key = entry.getKey();
                String value = String.join(", ", entry.getValue());
                writer.write(key + ": " + value + "\r\n");
            }

            if (response.getBody().isEmpty()) {
                writer.write("\r\n");
                writer.flush();
                return;
            }

            byte[] data = response.getBody().get();
            writer.write("Content-Length: " + data.length + "\r\n");
            writer.write("\r\n");
            writer.flush();
            outputStream.write(data);
            outputStream.flush();

        } catch (IOException e) {
            System.err.println("[HttpEncoder] Error writing response: " + e.getMessage());
        }
    }

    private static String getStatusMessage(int code) {
        return io.github.blacknoize404.miniJWS.primitives.HttpStatusCode.getMessage(code);
    }
}
