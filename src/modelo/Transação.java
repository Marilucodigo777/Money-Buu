package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import excecoes.ValorInvalidoException;

/**
 * Classe abstrata que representa qualquer movimentação financeira.
 * Receita, Despesa e Transferencia herdam dela e implementam
 * aplicar() e getTipo() do seu próprio jeito (polimorfismo).
 */
public abstract class Transacao {
    private static int proximoId = 1;

    private int id;
    private LocalDate data;
    private BigDecimal valor;
    private String descricao;
    private Categoria categoria;

    // Construtor completo (usado, por exemplo, ao carregar dados do arquivo)
    public Transacao(int id, LocalDate data, BigDecimal valor, String descricao, Categoria categoria) {
        validarValor(valor);
        if (data == null) {
            throw new IllegalArgumentException("A data da transação não pode ser nula.");
        }
        this.id = id;
        this.data = data;
        this.valor = valor;
        this.descricao = descricao;
        this.categoria = categoria;
        if (id >= proximoId) {
            proximoId = id + 1;
        }
    }

    // Sobrecarga: gera o id automaticamente
    public Transacao(LocalDate data, BigDecimal valor, String descricao, Categoria categoria) {
        this(proximoId, data, valor, descricao, categoria);
    }

    // Sobrecarga: usa a data de hoje
    public Transacao(BigDecimal valor, String descricao, Categoria categoria) {
        this(LocalDate.now(), valor, descricao, categoria);
    }

    /**
     * Retorna o efeito desta transação no saldo da conta informada:
     * valor positivo soma ao saldo, valor negativo subtrai.
     */
    public abstract BigDecimal aplicar(Conta conta);

    /** Retorna o tipo da transação ("RECEITA", "DESPESA", "TRANSFERENCIA"). */
    public abstract String getTipo();

    /** Edita os dados da transação (RF04). O saldo é recalculado automaticamente pela Conta. */
    public void atualizarDados(LocalDate data, BigDecimal valor, String descricao, Categoria categoria) {
        validarValor(valor);
        if (data == null) {
            throw new IllegalArgumentException("A data da transação não pode ser nula.");
        }
        this.data = data;
        this.valor = valor;
        this.descricao = descricao;
        this.categoria = categoria;
    }

    /**
     * Próximo id livre. Útil para criar Receita/Despesa, que recebem o id no construtor,
     * sem repetir ids já usados por outras transações.
     */
    public static int getProximoId() {
        return proximoId;
    }

    private static void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoException("O valor da transação deve ser maior que zero.");
        }
    }

    public int getId() {
        return id;
    }

    public LocalDate getData() {
        return data;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    @Override
    public String toString() {
        String nomeCategoria = (categoria == null) ? "-" : categoria.getNome();
        return String.format("#%d | %s | %-13s | %13s | %s | %s",
                id, data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), getTipo(), Moeda.formatar(valor), nomeCategoria, descricao);
    }
}

