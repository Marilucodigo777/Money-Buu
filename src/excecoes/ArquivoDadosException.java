package excecoes;

/**
 * Lançada quando há problema para gravar ou ler o arquivo de dados (dados.json):
 * erro de leitura/escrita no disco, JSON mal formado ou dados inconsistentes no arquivo.
 */
public class ArquivoDadosException extends RuntimeException {
    public ArquivoDadosException(String mensagem) {
        super(mensagem);
    }

    public ArquivoDadosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
