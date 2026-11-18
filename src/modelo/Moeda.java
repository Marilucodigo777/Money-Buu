package modelo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formata valores em reais no padrão brasileiro (ex.: R$ 1.500,00). */
public final class Moeda {
    private static final NumberFormat FORMATO = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private Moeda() {
    }

    public static String formatar(BigDecimal valor) {
        return FORMATO.format(valor);
    }
}
