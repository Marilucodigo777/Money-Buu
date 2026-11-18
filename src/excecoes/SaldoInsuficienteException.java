package excecoes;

/** Lançada quando a conta não tem saldo disponível para uma saída de dinheiro. */
public class SaldoInsuficienteException extends RuntimeException {
    public SaldoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}

