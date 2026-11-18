package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import excecoes.TransferenciaInvalidaException;

/**
 * Movimentação de dinheiro entre duas contas distintas.
 * Para a conta de origem o valor sai (negativo); para a de destino, entra (positivo).
 */
public class Transferencia extends Transacao {
    private Conta origem;
    private Conta destino;

    public Transferencia(int id, LocalDate data, BigDecimal valor, String descricao,
                         Categoria categoria, Conta origem, Conta destino) {
        super(id, data, valor, descricao, categoria);
        validarContas(origem, destino);
        this.origem = origem;
        this.destino = destino;
    }

    // Sobrecarga: id automático
    public Transferencia(LocalDate data, BigDecimal valor, String descricao,
                         Categoria categoria, Conta origem, Conta destino) {
        super(data, valor, descricao, categoria);
        validarContas(origem, destino);
        this.origem = origem;
        this.destino = destino;
    }

    // Sobrecarga: id automático, data de hoje e sem categoria
    public Transferencia(BigDecimal valor, String descricao, Conta origem, Conta destino) {
        this(LocalDate.now(), valor, descricao, null, origem, destino);
    }

    private static void validarContas(Conta origem, Conta destino) {
        if (origem == null || destino == null) {
            throw new IllegalArgumentException("A transferência precisa de conta de origem e de destino.");
        }
        if (origem == destino) {
            throw new TransferenciaInvalidaException("A conta de origem e a de destino devem ser diferentes.");
        }
    }

    @Override
    public BigDecimal aplicar(Conta conta) {
        if (conta == origem) {
            return getValor().negate();
        }
        if (conta == destino) {
            return getValor();
        }
        return BigDecimal.ZERO; // não afeta outras contas
    }

    @Override
    public String getTipo() {
        return "TRANSFERENCIA";
    }

    public Conta getOrigem() {
        return origem;
    }

    public Conta getDestino() {
        return destino;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + origem.getNome() + " -> " + destino.getNome() + ")";
    }
}

