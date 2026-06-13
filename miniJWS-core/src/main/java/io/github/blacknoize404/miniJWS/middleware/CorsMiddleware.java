package io.github.blacknoize404.miniJWS.middleware;

import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.primitives.Middleware;
import io.github.blacknoize404.miniJWS.primitives.MiddlewareChain;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.util.*;

public final class CorsMiddleware implements Middleware {

    private String allowOrigin = "*";
    private final List<String> allowMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    private final List<String> allowHeaders = new ArrayList<>(List.of("Content-Type", "Authorization"));
    private boolean allowCredentials = false;
    private int maxAge = -1;

    public CorsMiddleware allowOrigin(String origin) {
        this.allowOrigin = origin;
        return this;
    }

    public CorsMiddleware allowMethods(String... methods) {
        this.allowMethods.clear();
        this.allowMethods.addAll(Arrays.asList(methods));
        return this;
    }

    public CorsMiddleware allowHeaders(String... headers) {
        this.allowHeaders.clear();
        this.allowHeaders.addAll(Arrays.asList(headers));
        return this;
    }

    public CorsMiddleware allowCredentials(boolean allow) {
        if (allow && "*".equals(allowOrigin)) {
            throw new IllegalStateException(
                    "CORS: Cannot set allowCredentials(true) with allowOrigin(\"*\"). " +
                    "Use allowOrigin(\"https://specific.domain\") or set credentials first.");
        }
        this.allowCredentials = allow;
        return this;
    }

    public CorsMiddleware maxAge(int seconds) {
        this.maxAge = seconds;
        return this;
    }

    @Override
    public HttpResponse run(HttpRequest request, MiddlewareChain chain) {
        String origin = request.getHeader("Origin").orElse(null);

        if (origin == null) {
            return chain.next(request);
        }

        if (request.getHttpMethod() == HttpMethod.OPTIONS) {
            return buildPreflightResponse(origin);
        }

        HttpResponse response = chain.next(request);
        return addCorsHeaders(response, origin);
    }

    private String resolveOrigin(String requestOrigin) {
        if ("*".equals(allowOrigin) && allowCredentials) {
            return requestOrigin;
        }
        return allowOrigin;
    }

    private HttpResponse buildPreflightResponse(String origin) {
        var builder = new HttpResponse.Builder()
                .setStatusCode(204)
                .addHeader("Access-Control-Allow-Origin", resolveOrigin(origin))
                .addHeader("Access-Control-Allow-Methods", String.join(", ", allowMethods))
                .addHeader("Access-Control-Allow-Headers", String.join(", ", allowHeaders));

        if (allowCredentials) {
            builder.addHeader("Access-Control-Allow-Credentials", "true");
        }
        if (maxAge > 0) {
            builder.addHeader("Access-Control-Max-Age", String.valueOf(maxAge));
        }

        return builder.build();
    }

    private HttpResponse addCorsHeaders(HttpResponse response, String origin) {
        var builder = new HttpResponse.Builder()
                .setStatusCode(response.getStatusCode())
                .setProtocolVersion(response.getProtocolVersion())
                .setMethod(response.getMethod());

        for (var entry : response.getHeaders().entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }

        builder.addHeader("Access-Control-Allow-Origin", resolveOrigin(origin));

        if (allowCredentials) {
            builder.addHeader("Access-Control-Allow-Credentials", "true");
        }

        response.getBody().ifPresent(builder::setBody);
        return builder.build();
    }
}
