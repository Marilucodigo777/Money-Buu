package excecoes;

/** Lançada quando se tenta buscar/remover uma transação que não existe na conta. */
public class TransacaoNaoEncontradaException extends RuntimeException {
    public TransacaoNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
