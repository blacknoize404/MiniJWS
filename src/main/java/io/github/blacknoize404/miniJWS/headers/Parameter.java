package io.github.blacknoize404.miniJWS.headers;

public class Parameter {

    private String name;
    private String value;

    public static Parameter parse(String data) {

        Parameter newParameter = new Parameter();

        String[] parts = data.split("=");

        newParameter.name = parts[0].trim();
        newParameter.value = parts[1].trim();

        return newParameter;

    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
