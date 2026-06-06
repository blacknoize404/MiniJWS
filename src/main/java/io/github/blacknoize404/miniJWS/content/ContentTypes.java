package io.github.blacknoize404.miniJWS.content;

import java.util.HashMap;
import java.util.Map;

public class ContentTypes {

    public static final Map<Integer, String> CONTENT_TYPES = new HashMap<>() {{
        put(100, "text/css");
        put(101, "application/json");
        put(200, "OK");
        put(203, "text/html; charset=utf-8");
        put(204, "multipart/form-data; boundary=ExampleBoundaryString");
        put(206, "application/x-www-form-urlencoded");
        put(304, "NOT_MODIFIED");
        put(400, "BAD_REQUEST");
        put(404, "NOT_FOUND");
        put(426, "UPGRADE_REQUIRED");
        put(500, "INTERNAL_SERVER_ERROR");
    }};

    public static final Map<Integer, String> RESPONSE_TYPES = new HashMap<>() {{
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
