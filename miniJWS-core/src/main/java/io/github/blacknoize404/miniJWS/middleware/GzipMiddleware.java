package io.github.blacknoize404.miniJWS.middleware;

import io.github.blacknoize404.miniJWS.primitives.Middleware;
import io.github.blacknoize404.miniJWS.primitives.MiddlewareChain;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

public final class GzipMiddleware implements Middleware {

    private static final int MIN_SIZE = 256;
    private final int level;

    public GzipMiddleware() {
        this(6);
    }

    public GzipMiddleware(int level) {
        this.level = Math.max(1, Math.min(9, level));
    }

    @Override
    public HttpResponse run(HttpRequest request, MiddlewareChain chain) {
        String enc = request.getHeader("Accept-Encoding").orElse("");
        if (!enc.toLowerCase().contains("gzip")) {
            return chain.next(request);
        }

        HttpResponse response = chain.next(request);
        if (response.getBody().isEmpty()) return response;

        boolean alreadyEncoded = response.getHeaders().keySet().stream()
                .anyMatch(k -> k.equalsIgnoreCase("Content-Encoding"));
        if (alreadyEncoded) return response;

        byte[] body = response.getBody().get();
        if (body.length < MIN_SIZE) return response;

        try {
            byte[] compressed = gzipCompress(body);
            if (compressed.length >= body.length) return response;

            var builder = new HttpResponse.Builder()
                    .setStatusCode(response.getStatusCode())
                    .setProtocolVersion(response.getProtocolVersion())
                    .setMethod(response.getMethod());

            for (var entry : response.getHeaders().entrySet()) {
                if (!entry.getKey().equalsIgnoreCase("Content-Encoding") &&
                    !entry.getKey().equalsIgnoreCase("Content-Length")) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            builder.addHeader("Content-Encoding", "gzip");
            builder.setBody(compressed);

            return builder.build();
        } catch (IOException e) {
            return response;
        }
    }

    private byte[] gzipCompress(byte[] data) throws IOException {
        var bos = new ByteArrayOutputStream(data.length / 2);
        var gz = new GZIPOutputStream(bos) {{
            def.setLevel(level);
        }};
        try {
            gz.write(data);
        } finally {
            gz.close();
        }
        return bos.toByteArray();
    }
}
