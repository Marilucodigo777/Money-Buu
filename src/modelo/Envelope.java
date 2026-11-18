package modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import excecoes.SaldoInsuficienteException;
import excecoes.ValorInvalidoException;

/**
 * "Caixinha" virtual dentro de uma conta, usada para guardar dinheiro para um objetivo.
 * O dinheiro reservado continua na conta (entra no saldo), mas sai do saldo disponível.
 * Composição: o envelope pertence a uma única conta e só existe dentro dela.
 */
public class Envelope {
    private static int proximoId = 1;

    private int id;
    private String nome;
    private BigDecimal meta;
    private BigDecimal valorReservado;

    // Construtor completo (usado, por exemplo, ao carregar dados do arquivo)
    public Envelope(int id, String nome, BigDecimal meta, BigDecimal valorReservado) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do envelope não pode ser vazio.");
        }
        if (meta == null || meta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoException("A meta do envelope deve ser maior que zero.");
        }
        if (valorReservado == null || valorReservado.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValorInvalidoException("O valor reservado não pode ser negativo.");
        }
        this.id = id;
        this.nome = nome;
        this.meta = meta;
        this.valorReservado = valorReservado;
        if (id >= proximoId) {
            proximoId = id + 1;
        }
    }

    // Sobrecarga: envelope novo, vazio, com id automático
    public Envelope(String nome, BigDecimal meta) {
        this(proximoId, nome, meta, BigDecimal.ZERO);
    }

    /** Coloca dinheiro no envelope. Use Conta.guardarNoEnvelope() para conferir o saldo da conta. */
    public void adicionarValor(BigDecimal valor) {
        validarValor(valor);
        valorReservado = valorReservado.add(valor);
    }

    /** Tira dinheiro do envelope (ele volta a ficar disponível na conta). */
    public void retirarValor(BigDecimal valor) {
        validarValor(valor);
        if (valor.compareTo(valorReservado) > 0) {
            throw new SaldoInsuficienteException(
                    "O envelope " + nome + " só tem " + Moeda.formatar(valorReservado)
                    + " reservados, não dá para retirar " + Moeda.formatar(valor) + ".");
        }
        valorReservado = valorReservado.subtract(valor);
    }

    /** Porcentagem da meta já alcançada (pode passar de 100 se guardar mais que a meta). */
    public BigDecimal calcularProgresso() {
        return valorReservado.multiply(BigDecimal.valueOf(100))
                .divide(meta, 2, RoundingMode.HALF_UP);
    }

    public boolean metaAtingida() {
        return valorReservado.compareTo(meta) >= 0;
    }

    private static void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoException("O valor deve ser maior que zero.");
        }
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getMeta() {
        return meta;
    }

    public BigDecimal getValorReservado() {
        return valorReservado;
    }

    @Override
    public String toString() {
        return String.format("Envelope #%d - %s | %s de %s (%s%%)",
                id, nome, Moeda.formatar(valorReservado), Moeda.formatar(meta), calcularProgresso());
    }
}
