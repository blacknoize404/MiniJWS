package io.github.blacknoize404.miniJWS.headers;

import java.util.*;

public class Field {

    private String type;
    private Optional<String> subtype;
    private Optional<List<Parameter>> parameters;

    private Field() {}

    public static Field parse(String data) {
        Field field = new Field();
        String[] parts = data.split(";");

        String[] typeParts = parts[0].split("/", 2);
        field.type = typeParts[0].trim();
        field.subtype = typeParts.length > 1
                ? Optional.of(typeParts[1].trim())
                : Optional.empty();

        if (parts.length > 1) {
            List<Parameter> params = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                params.add(Parameter.parse(parts[i].trim()));
            }
            field.parameters = Optional.of(params);
        } else {
            field.parameters = Optional.empty();
        }

        return field;
    }

    public String getType() { return type; }
    public Optional<String> getSubtype() { return subtype; }
    public Optional<List<Parameter>> getParameters() { return parameters; }

    @Override
    public String toString() {
        return "Field{" +
                "type='" + type + '\'' +
                ", subtype=" + subtype.orElse("none") +
                ", params=" + parameters.map(p -> p.toString()).orElse("none") +
                '}';
    }
}
