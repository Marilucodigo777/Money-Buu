package excecoes;

/** Lançada quando uma transferência é inválida (ex.: origem igual ao destino). */
public class TransferenciaInvalidaException extends RuntimeException {
    public TransferenciaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
