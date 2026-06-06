package io.github.blacknoize404.miniJWS.primitives;

import org.intellij.lang.annotations.RegExp;

public class RegexPatterns {

    /**
     * Patrón regex para leer una url por completo, incluyendo grupos para el protocolo, subdominio, dominio, tld,
     * categorías, recurso de petición y parámetros.
     */
    @RegExp
    public static String regexUrlPattern = "^(?<protocol>https?)://(?:(?<subdomain>\\w+)\\.)?(?<domain>\\w+)\\.(?<tld>\\w{2,})(?:/(?<categories>[\\w/]*(?:/))?|)(?<resource>\\w+)?(?:\\?(?<parameters>.+)|)";


    /**
     * Lista los nombres de los productos en https://www.tuambia.com/offers?opt=offers&page=1&sort=order
     * https://regex101.com/r/eb2EM1/1
     */
    @RegExp
    public static String regexProductNamePattern = "<h3 class=\"MuiTypography-root MuiTypography-h3 mt-2 md:mt-0 css-12r7xb8\" title=\"[()Ññ .,a-zA-Z0-9áéíóúÁÉÍÓÚ\";&%¨]+\">([()Ññ .,a-zA-Z0-9áéíóúÁÉÍÓÚ\";&%¨]+)</h3>";
}
