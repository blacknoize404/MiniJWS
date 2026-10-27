package io.github.blacknoize404.miniJWS.requests;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HttpDecoder {

    private static final int CR = 13;
    private static final int LF = 10;
    private static final int MAX_HEADER_LINE_LENGTH = 8_192;
    private static final int MAX_HEADERS = 100;
    private static final int MAX_CHUNK_SIZE = 10 * 1024 * 1024;
    private static final int MAX_CONTENT_LENGTH = 50 * 1024 * 1024;

    private HttpDecoder() {}

    public static Optional<HttpRequest> decode(InputStream inputStream) {
        return decode(new BufferedInputStream(inputStream));
    }

    public static Optional<HttpRequest> decode(BufferedInputStream reader) {
        return decode(reader, "unknown");
    }

    public static Optional<HttpRequest> decode(BufferedInputStream reader, String remoteAddress) {
        try {
            String requestLine = readLine(reader);
            if (requestLine == null || requestLine.isEmpty()) return Optional.empty();

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) return Optional.empty();

            HttpMethod method;
            try {
                method = HttpMethod.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }

            URI uri = parseUri(parts[1]);
            String protocol = parts[2];

            Map<String, List<String>> headers = new LinkedHashMap<>();
            String headerLine;
            int contentLength = -1;
            boolean contentLengthSet = false;
            boolean chunked = false;
            boolean transferEncodingSet = false;

            int headerCount = 0;
            while (true) {
                headerLine = readLine(reader);
                if (headerLine == null) return Optional.empty();
                if (headerLine.isEmpty()) break;
                if (++headerCount > MAX_HEADERS) return Optional.empty();

                // obs-fold (RFC 9112 §5.2): rechazar, no plegar
                if (headerLine.charAt(0) == ' ' || headerLine.charAt(0) == '\t') {
                    return Optional.empty();
                }

                int colon = headerLine.indexOf(':');
                if (colon == -1) continue;
                String key = headerLine.substring(0, colon).trim();
                String value = headerLine.substring(colon + 1).trim();
                headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);

                if (key.equalsIgnoreCase("Content-Length")) {
                    if (contentLengthSet) {
                        return Optional.empty();
                    }
                    contentLengthSet = true;
                    try {
                        contentLength = Integer.parseInt(value);
                        if (contentLength > MAX_CONTENT_LENGTH || contentLength < 0) {
                            return Optional.empty();
                        }
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                }
                if (key.equalsIgnoreCase("Transfer-Encoding")) {
                    if (transferEncodingSet) return Optional.empty();
                    transferEncodingSet = true;
                    if (value.toLowerCase(Locale.ROOT).contains("chunked")) {
                        chunked = true;
                    }
                }
            }

            // CL + TE a la vez: ambiguo, vector clásico de smuggling (RFC 9112 §6.1)
            if (chunked && contentLengthSet) return Optional.empty();

            Optional<byte[]> body = Optional.empty();
            if (chunked) {
                body = readChunkedBody(reader);
            } else if (contentLength > 0) {
                body = readExactBody(reader, contentLength);
            }

            var builder = new HttpRequest.Builder()
                    .setHttpMethod(method)
                    .setUri(uri)
                    .setProtocolVersion(protocol)
                    .setRemoteAddress(remoteAddress)
                    .setHeaders(headers);

            Optional<Map<String, String>> params = parseQueryParams(parts[1]);
            params.ifPresent(builder::setParameters);
            body.ifPresent(builder::setBody);

            return Optional.of(builder.build());

        } catch (IOException | URISyntaxException e) {
            return Optional.empty();
        }
    }

    private static String readLine(BufferedInputStream in) throws IOException {
        var buf = new ByteArrayOutputStream(256);
        int b;
        boolean crFound = false;

        while ((b = in.read()) != -1) {
            if (buf.size() > MAX_HEADER_LINE_LENGTH) return null;

            if (b == CR) {
                crFound = true;
            } else if (b == LF) {
                return buf.toString(StandardCharsets.US_ASCII);
            } else {
                if (crFound) {
                    buf.write(CR);
                    crFound = false;
                }
                buf.write(b);
            }
        }

        if (buf.size() == 0) return null;
        return buf.toString(StandardCharsets.US_ASCII);
    }

    private static Optional<byte[]> readChunkedBody(BufferedInputStream in) throws IOException {
        var body = new ByteArrayOutputStream();
        while (true) {
            String chunkSizeLine = readLine(in);
            if (chunkSizeLine == null) throw new EOFException("Incomplete chunked body");

            int semi = chunkSizeLine.indexOf(';');
            String sizeStr = (semi == -1) ? chunkSizeLine.trim() : chunkSizeLine.substring(0, semi).trim();

            int size;
            try {
                size = Integer.parseInt(sizeStr, 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size: " + sizeStr, e);
            }

            if (size < 0 || size > MAX_CHUNK_SIZE) {
                throw new IOException("Chunk size out of range: " + size);
            }
            if (size == 0) {
                consumeTrailers(in);
                break;
            }
            if (size > MAX_CONTENT_LENGTH - body.size()) {
                throw new IOException("Chunked body exceeds max content length");
            }

            byte[] chunk = readExact(in, size);
            body.write(chunk);
            String chunkEnd = readLine(in);
            if (chunkEnd == null || !chunkEnd.isEmpty()) {
                throw new IOException("Missing CRLF after chunk");
            }
        }
        return body.size() > 0 ? Optional.of(body.toByteArray()) : Optional.empty();
    }

    private static void consumeTrailers(BufferedInputStream in) throws IOException {
        while (true) {
            String trailer = readLine(in);
            if (trailer == null) throw new EOFException("Incomplete chunked trailers");
            if (trailer.isEmpty()) return;
        }
    }

    private static Optional<byte[]> readExactBody(BufferedInputStream in, int length) throws IOException {
        return Optional.of(readExact(in, length));
    }

    private static byte[] readExact(BufferedInputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        int total = 0;
        while (total < length) {
            int read = in.read(buffer, total, length - total);
            if (read == -1) throw new EOFException("Unexpected EOF");
            total += read;
        }
        return buffer;
    }

    private static URI parseUri(String raw) throws URISyntaxException {
        int idx = raw.indexOf('?');
        String path = (idx == -1) ? raw : raw.substring(0, idx);
        return new URI(path);
    }

    private static Optional<Map<String, String>> parseQueryParams(String raw) {
        int idx = raw.indexOf('?');
        if (idx == -1 || idx == raw.length() - 1) return Optional.empty();

        String query = raw.substring(idx + 1);
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                params.put(key, value);
            } else if (kv.length == 1) {
                params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), "");
            }
        }
        return params.isEmpty() ? Optional.empty() : Optional.of(params);
    }
}
