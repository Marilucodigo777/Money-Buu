package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;


public class Relatorio {

    private final List<Conta> contas;
    private final LocalDate inicio;
    private final LocalDate fim;
    private final Categoria categoriaFiltro;

   
    public Relatorio(List<Conta> contas, LocalDate inicio, LocalDate fim,
                     Categoria categoriaFiltro) {
        Objects.requireNonNull(contas, "A lista de contas nao pode ser nula.");
        if (contas.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("A lista de contas nao pode conter null.");
        }
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual a final.");
        }
        this.contas = List.copyOf(contas);
        this.inicio = inicio;
        this.fim = fim;
        this.categoriaFiltro = categoriaFiltro;
    }

    
    public Relatorio(List<Conta> contas) {
        this(contas, null, null, null);
    }

    
    public BigDecimal total(YearMonth mes) {
        Objects.requireNonNull(mes, "O mes nao pode ser nulo.");
        return calcularResultado(mes.atDay(1), mes.atEndOfMonth(), null);
    }

    
    public BigDecimal total(YearMonth mes, Categoria categoria) {
        Objects.requireNonNull(mes, "O mes nao pode ser nulo.");
        Objects.requireNonNull(categoria, "A categoria nao pode ser nula.");
        return calcularResultado(mes.atDay(1), mes.atEndOfMonth(), categoria);
    }

   
    public BigDecimal total(LocalDate inicio, LocalDate fim) {
        return calcularResultado(inicio, fim, null);
    }

    
    public BigDecimal totalReceitas(YearMonth mes) {
        Objects.requireNonNull(mes, "O mes nao pode ser nulo.");
        return somarPorTipo(mes.atDay(1), mes.atEndOfMonth(), null, "RECEITA");
    }

    
    public BigDecimal totalDespesas(YearMonth mes) {
        Objects.requireNonNull(mes, "O mes nao pode ser nulo.");
        return somarPorTipo(mes.atDay(1), mes.atEndOfMonth(), null, "DESPESA");
    }

    private BigDecimal calcularResultado(LocalDate inicio, LocalDate fim,
                                        Categoria categoria) {
        BigDecimal receitas = somarPorTipo(inicio, fim, categoria, "RECEITA");
        BigDecimal despesas = somarPorTipo(inicio, fim, categoria, "DESPESA");
        return receitas.subtract(despesas);
    }

    private BigDecimal somarPorTipo(LocalDate inicio, LocalDate fim,
                                   Categoria categoria, String tipo) {
        validarPeriodo(inicio, fim);
        BigDecimal soma = BigDecimal.ZERO;

        for (Conta conta : contas) {
            for (Transacao transacao : conta.listarExtrato()) {
                if (transacao.getData().isBefore(inicio)
                        || transacao.getData().isAfter(fim)) {
                    continue;
                }

                if (!tipo.equalsIgnoreCase(transacao.getTipo())) {
                    continue; 
                }

                if (categoria != null && !mesmaCategoria(transacao.getCategoria(), categoria)) {
                    continue;
                }

                soma = soma.add(transacao.getValor());
            }
        }
        return soma;
    }

    private boolean mesmaCategoria(Categoria a, Categoria b) {
        return a != null && a.getNome() != null && b.getNome() != null
                && a.getNome().equalsIgnoreCase(b.getNome());
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        Objects.requireNonNull(inicio, "A data inicial nao pode ser nula.");
        Objects.requireNonNull(fim, "A data final nao pode ser nula.");
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual a final.");
        }
    }
}

