package excecoes;


public class ValorInvalidoException extends RuntimeException {

    public ValorInvalidoException() {
        super("O valor da transacao deve ser maior que zero.");
    }

    public ValorInvalidoException(String mensagem) {
        super(mensagem);
    }
}

