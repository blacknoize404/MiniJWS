package io.github.blacknoize404.miniJWS.primitives;

import java.util.HashMap;
import java.util.Map;

/**
 * Map of status code values and meanings.
 */
public class HttpStatusCode {

    /*
    If the request method is HEAD or the response status code is 204 (No Content) or 304 (Not Modified), there is no content in the response.
    If the request method is GET and the response status code is 200 (OK), the content is a representation of the target resource (Section 7.1).
    If the request method is GET and the response status code is 203 (Non-Authoritative Information), the content is a potentially modified or enhanced representation of the target resource as provided by an intermediary.
    If the request method is GET and the response status code is 206 (Partial Content), the content is one or more parts of a representation of the target resource.
    If the response has a Content-Location header field and its field value is a reference to the same URI as the target URI, the content is a representation of the target resource.
    If the response has a Content-Location header field and its field value is a reference to a URI different from the target URI, then the sender asserts that the content is a representation of the resource identified by the Content-Location field value. However, such an assertion cannot be trusted unless it can be verified by other means (not defined by this specification).
    Otherwise, the content is unidentified by HTTP, but a more specific identifier might be supplied within the content itself.
     */
    public static final Map<Integer, String> STATUS_CODES = new HashMap<>() {{
        put(100, "CONTINUE");
        put(101, "SWITCHING_PROTOCOLS");
        put(200, "OK");
        put(203, "NON_AUTHORITATIVE_INFORMATION");
        put(204, "NO_CONTENT");
        put(206, "PARTIAL_CONTENT");
        put(304, "NOT_MODIFIED");
        put(400, "BAD_REQUEST");
        put(404, "NOT_FOUND");
        put(426, "UPGRADE_REQUIRED");
        put(500, "INTERNAL_SERVER_ERROR");
    }};

}