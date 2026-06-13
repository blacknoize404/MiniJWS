package io.github.blacknoize404.miniJWS.headers;

import java.util.Objects;

public class Parameter {

    private final String name;
    private final String value;

    private Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static Parameter parse(String data) {
        Objects.requireNonNull(data);
        String[] parts = data.split("=", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid parameter: " + data);
        return new Parameter(parts[0].trim(), parts[1].trim());
    }

    public String getName() { return name; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
