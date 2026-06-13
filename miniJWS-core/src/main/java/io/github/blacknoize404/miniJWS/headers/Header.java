package io.github.blacknoize404.miniJWS.headers;

import java.util.*;

public class Header {

    private final String name;
    private final List<Field> fields;

    public Header(String name, List<Field> fields) {
        this.name = name;
        this.fields = fields;
    }

    public static Header parse(String raw) {
        int colon = raw.indexOf(':');
        if (colon == -1) throw new IllegalArgumentException("Invalid header: " + raw);

        String name = raw.substring(0, colon).trim();
        String value = raw.substring(colon + 1).trim();

        List<Field> fields = new ArrayList<>();
        for (String part : value.split(",")) {
            fields.add(Field.parse(part.trim()));
        }

        return new Header(name, fields);
    }

    public String getName() { return name; }
    public List<Field> getFields() { return fields; }

    @Override
    public String toString() {
        return name + ": " + fields;
    }
}
