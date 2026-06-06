package io.github.blacknoize404.miniJWS.primitives;

/**
 * Define los métodos compatibles con el protocolo HTTP.
 */
public enum HttpMethod {

    /**
     * Transfer a current representation of the target resource.
     */
    GET,

    /**
     * Same as GET, but do not transfer the response content.
     */
    HEAD,

    /**
     * Perform resource-specific processing on the request content.
     */
    POST,

    /**
     * Replace all current representations of the target resource with the request content.
     */
    PUT,

    /**
     * Remove all current representations of the target resource.
     */
    DELETE,

    /**
     * Establish a tunnel to the server identified by the target resource.
     */
    CONNECT,

    /**
     * Describe the communication options for the target resource.
     */
    OPTIONS,

    /**
     * Perform a message loop-back test along the path to the target resource.
     */
    TRACE
}