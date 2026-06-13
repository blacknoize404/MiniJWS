package io.github.blacknoize404.miniJWS.primitives;

import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

@FunctionalInterface
public interface MiddlewareChain {
    HttpResponse next(HttpRequest request);
}
