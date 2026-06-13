package io.github.blacknoize404.miniJWS.primitives;

import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

@FunctionalInterface
public interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}
