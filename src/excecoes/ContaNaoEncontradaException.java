package excecoes;

/** Lançada quando se procura uma conta (por id ou nome) que não está cadastrada. */
public class ContaNaoEncontradaException extends RuntimeException {
    public ContaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}

