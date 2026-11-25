package persistencia;

import java.util.List;

import modelo.Categoria;
import modelo.Conta;

/**
 * Tudo o que o Money Buu guarda no arquivo: as contas (com suas transações e envelopes)
 * e as categorias. É o que o RepositorioJson devolve ao carregar.
 */
public record DadosMoneyBuu(List<Conta> contas, List<Categoria> categorias) {

    /** Dados vazios, usados quando o arquivo ainda não existe. */
    public static DadosMoneyBuu vazio() {
        return new DadosMoneyBuu(List.of(), List.of());
    }

    public boolean estaVazio() {
        return contas.isEmpty() && categorias.isEmpty();
    }
}
