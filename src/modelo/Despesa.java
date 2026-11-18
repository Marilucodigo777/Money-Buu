import java.math.BigDecimal;
import java.time.LocalDate;

public class Despesa {
    private int id;
    private LocalDate data;
    private BigDecimal valor;
    private String descricao;
    private Categoria categoria; // Relação: Uma despesa tem uma categoria

    public Despesa(int id, LocalDate data, BigDecimal valor, String descricao, Categoria categoria) {
        this.id = id;
        this.data = data;
        this.valor = valor;
        this.descricao = descricao;
        this.categoria = categoria;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
