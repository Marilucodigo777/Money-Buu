package persistencia;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import excecoes.ArquivoDadosException;

/**
 * Leitor e escritor de JSON simples, feito sem bibliotecas externas.
 * Tipos usados em Java:
 *   objeto JSON  -> Map<String, Object>
 *   lista JSON   -> List<Object>
 *   texto        -> String
 *   número       -> BigDecimal (sem perder centavos)
 *   true/false   -> Boolean
 *   null         -> null
 */
public final class Json {

    private Json() {
    }

    // ======================= ESCRITA =======================

    /** Converte um valor (Map, List, String, número, Boolean ou null) em texto JSON identado. */
    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escreverValor(sb, valor, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void escreverValor(StringBuilder sb, Object valor, int nivel) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof String s) {
            escreverTexto(sb, s);
        } else if (valor instanceof BigDecimal b) {
            sb.append(b.toPlainString());
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else if (valor instanceof Map<?, ?> mapa) {
            escreverObjeto(sb, mapa, nivel);
        } else if (valor instanceof List<?> lista) {
            escreverLista(sb, lista, nivel);
        } else {
            throw new ArquivoDadosException("Tipo não suportado no JSON: " + valor.getClass().getSimpleName());
        }
    }

    private static void escreverObjeto(StringBuilder sb, Map<?, ?> mapa, int nivel) {
        if (mapa.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<?, ?> e : mapa.entrySet()) {
            identar(sb, nivel + 1);
            escreverTexto(sb, String.valueOf(e.getKey()));
            sb.append(": ");
            escreverValor(sb, e.getValue(), nivel + 1);
            if (++i < mapa.size()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        identar(sb, nivel);
        sb.append('}');
    }

    private static void escreverLista(StringBuilder sb, List<?> lista, int nivel) {
        if (lista.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[\n");
        for (int i = 0; i < lista.size(); i++) {
            identar(sb, nivel + 1);
            escreverValor(sb, lista.get(i), nivel + 1);
            if (i < lista.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        identar(sb, nivel);
        sb.append(']');
    }

    private static void escreverTexto(StringBuilder sb, String s) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static void identar(StringBuilder sb, int nivel) {
        sb.append("  ".repeat(nivel));
    }

    // ======================= LEITURA =======================

    /** Lê um texto JSON e devolve Map, List, String, BigDecimal, Boolean ou null. */
    public static Object ler(String texto) {
        Leitor leitor = new Leitor(texto);
        leitor.pularEspacos();
        Object valor = leitor.lerValor();
        leitor.pularEspacos();
        if (!leitor.acabou()) {
            throw leitor.erro("conteúdo a mais depois do fim do JSON");
        }
        return valor;
    }

    /** Percorre o texto caractere por caractere (analisador recursivo). */
    private static class Leitor {
        private final String texto;
        private int pos = 0;

        Leitor(String texto) {
            this.texto = texto;
        }

        boolean acabou() {
            return pos >= texto.length();
        }

        void pularEspacos() {
            while (!acabou() && Character.isWhitespace(texto.charAt(pos))) {
                pos++;
            }
        }

        char atual() {
            if (acabou()) {
                throw erro("o arquivo terminou antes do esperado");
            }
            return texto.charAt(pos);
        }

        void esperar(char c) {
            if (atual() != c) {
                throw erro("esperava '" + c + "' mas encontrou '" + atual() + "'");
            }
            pos++;
        }

        Object lerValor() {
            char c = atual();
            if (c == '{') {
                return lerObjeto();
            }
            if (c == '[') {
                return lerLista();
            }
            if (c == '"') {
                return lerTexto();
            }
            if (c == '-' || Character.isDigit(c)) {
                return lerNumero();
            }
            if (texto.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (texto.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            if (texto.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw erro("valor inesperado '" + c + "'");
        }

        Map<String, Object> lerObjeto() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            esperar('{');
            pularEspacos();
            if (atual() == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                pularEspacos();
                String chave = lerTexto();
                pularEspacos();
                esperar(':');
                pularEspacos();
                mapa.put(chave, lerValor());
                pularEspacos();
                if (atual() == ',') {
                    pos++;
                } else {
                    esperar('}');
                    return mapa;
                }
            }
        }

        List<Object> lerLista() {
            List<Object> lista = new ArrayList<>();
            esperar('[');
            pularEspacos();
            if (atual() == ']') {
                pos++;
                return lista;
            }
            while (true) {
                pularEspacos();
                lista.add(lerValor());
                pularEspacos();
                if (atual() == ',') {
                    pos++;
                } else {
                    esperar(']');
                    return lista;
                }
            }
        }

        String lerTexto() {
            esperar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = atual();
                pos++;
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char esc = atual();
                pos++;
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (pos + 4 > texto.length()) {
                            throw erro("código \\u incompleto");
                        }
                        try {
                            sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException e) {
                            throw erro("código \\u inválido");
                        }
                        pos += 4;
                    }
                    default -> throw erro("escape inválido \\" + esc);
                }
            }
        }

        BigDecimal lerNumero() {
            int inicio = pos;
            while (!acabou() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) {
                pos++;
            }
            try {
                return new BigDecimal(texto.substring(inicio, pos));
            } catch (NumberFormatException e) {
                throw erro("número inválido");
            }
        }

        ArquivoDadosException erro(String detalhe) {
            int linha = 1;
            for (int i = 0; i < Math.min(pos, texto.length()); i++) {
                if (texto.charAt(i) == '\n') {
                    linha++;
                }
            }
            return new ArquivoDadosException("JSON inválido na linha " + linha + ": " + detalhe + ".");
        }
    }
}
