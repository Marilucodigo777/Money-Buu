package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;


public class Receita extends Transacao {

    public Receita(int id, LocalDate data, BigDecimal valor,
                   String descricao, Categoria categoria) {
        super(id, data, valor, descricao, categoria);
    }

    @Override
    public BigDecimal aplicar(Conta conta) {
        if (conta == null) {
            throw new IllegalArgumentException("A conta nao pode ser nula.");
        }
        
        return getValor();
    }

    @Override
    public String getTipo() {
        return "RECEITA";
    }
}

