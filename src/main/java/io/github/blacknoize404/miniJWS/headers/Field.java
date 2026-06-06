package io.github.blacknoize404.miniJWS.headers;

import java.util.ArrayList;
import java.util.Optional;
import java.util.StringTokenizer;

public class Field {


    /*
    This specification uses three rules to denote the use of linear whitespace:
    OWS (optional whitespace), RWS (required whitespace), and BWS ("bad" whitespace).

    The OWS rule is used where zero or more linear whitespace octets might appear.
    For protocol elements where optional whitespace is preferred to improve readability,
    a sender SHOULD generate the optional whitespace as a single SP; otherwise, a sender
    SHOULD NOT generate optional whitespace except as needed to overwrite invalid or
    unwanted protocol elements during in-place message filtering.

    The RWS rule is used when at least one linear whitespace octet is required to separate
    field tokens. A sender SHOULD generate RWS as a single SP.

    OWS and RWS have the same semantics as a single SP. Any content known to be defined
    as OWS or RWS MAY be replaced with a single SP before interpreting it or forwarding
    the message downstream.

    The BWS rule is used where the grammar allows optional whitespace only for historical
    reasons. A sender MUST NOT generate BWS in messages. A recipient MUST parse for such
    bad whitespace and remove it before interpreting the protocol element.

    BWS has no semantics. Any content known to be defined as BWS MAY be removed before
    interpreting it or forwarding the message downstream.
     */

    /**
     * Dato que define el tipo (OWS)
     */
    private String type;

    /**
     * Dato que define el tipo (OWS)
     */
    private Optional<String> subtype;

    /**
     * Parámetros
     */

    /*
      parameters      = *( OWS ";" OWS [ parameter ] )
      parameter       = parameter-name "=" parameter-value
      parameter-name  = token
      parameter-value = ( token / quoted-string )
     */
    Optional<ArrayList<Parameter>> parameters;


    public static void main(String[] args) {

        String s = "text/plain;format=fixed;q=0.4";

    }

    public static Field parse(String data) {

        Field newField = new Field();

        String[] parts = data.split(";");

        String className = parts[0];

        StringTokenizer st = new StringTokenizer(className);

        newField.type = st.nextToken("/");

        // Dividiendo el nombre de la clase para añadir el subtipo.
        if (st.hasMoreTokens()) {

            newField.subtype = Optional.ofNullable(st.nextToken());

        }
        else {

            newField.subtype = Optional.empty();

        }

        // Leer
        return newField;


    }

    public String getType() {
        return type;
    }

    public Optional<String> getSubtype() {
        return subtype;
    }

    public Optional<ArrayList<Parameter>> getParameters() {
        return parameters;
    }
}
