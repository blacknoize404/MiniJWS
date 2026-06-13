package io.github.blacknoize404.miniJWS.primitives;

import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

@FunctionalInterface
public interface RequestRunner {
    HttpResponse run(HttpRequest request);
}
