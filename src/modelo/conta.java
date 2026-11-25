package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import excecoes.SaldoInsuficienteException;
import excecoes.TransacaoNaoEncontradaException;
import excecoes.TransferenciaInvalidaException;
import excecoes.ValorInvalidoException;

/**
 * Conta financeira do usuário.
 * - Agrega transações (Conta -> Transacao): as transações existem por conta própria.
 * - Compõe envelopes (Conta -> Envelope): os envelopes só existem dentro da conta.
 * O saldo nunca é guardado: é sempre calculado a partir do saldo inicial + transações.
 */
public class Conta {
    private static int proximoId = 1;

    private int id;
    private String nome;
    private BigDecimal saldoInicial;
    private List<Transacao> transacoes;
    private List<Envelope> envelopes;

    public Conta(int id, String nome, BigDecimal saldoInicial) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da conta não pode ser vazio.");
        }
        if (saldoInicial == null || saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValorInvalidoException("O saldo inicial não pode ser negativo.");
        }
        this.id = id;
        this.nome = nome;
        this.saldoInicial = saldoInicial;
        this.transacoes = new ArrayList<>();
        this.envelopes = new ArrayList<>();
        if (id >= proximoId) {
            proximoId = id + 1;
        }
    }

    // Sobrecarga: id automático
    public Conta(String nome, BigDecimal saldoInicial) {
        this(proximoId, nome, saldoInicial);
    }

    // Sobrecarga: conta começando com saldo zero
    public Conta(String nome) {
        this(nome, BigDecimal.ZERO);
    }

    // ---------------- Saldo ----------------

    /** Saldo atual = saldo inicial + efeito de cada transação (polimorfismo em aplicar()). */
    public BigDecimal calcularSaldo() {
        BigDecimal saldo = saldoInicial;
        for (Transacao t : transacoes) {
            saldo = saldo.add(t.aplicar(this));
        }
        return saldo;
    }

    /** Saldo que pode ser usado livremente = saldo atual - valores reservados em envelopes. */
    public BigDecimal calcularSaldoDisponivel() {
        BigDecimal reservado = BigDecimal.ZERO;
        for (Envelope e : envelopes) {
            reservado = reservado.add(e.getValorReservado());
        }
        return calcularSaldo().subtract(reservado);
    }

    // ---------------- Transações ----------------

    /**
     * Adiciona uma transação à conta.
     * Se ela tirar dinheiro da conta, verifica se há saldo disponível.
     * Se for uma transferência, ela também é registrada na outra conta envolvida.
     */
    public void adicionarTransacao(Transacao t) {
        if (t == null) {
            throw new IllegalArgumentException("Transação não pode ser nula.");
        }
        if (transacoes.contains(t)) {
            return; // já registrada (evita duplicar no caso de transferências)
        }
        if (t instanceof Transferencia tr && tr.getOrigem() != this && tr.getDestino() != this) {
            throw new TransferenciaInvalidaException(
                    "A transferência #" + t.getId() + " não envolve a conta " + nome + ".");
        }

        BigDecimal efeito = t.aplicar(this);
        if (efeito.signum() < 0 && calcularSaldoDisponivel().add(efeito).signum() < 0) {
            throw new SaldoInsuficienteException(
                    "Saldo disponível insuficiente na conta " + nome
                    + ". Disponível: " + Moeda.formatar(calcularSaldoDisponivel())
                    + ", necessário: " + Moeda.formatar(efeito.negate()));
        }

        transacoes.add(t);

        if (t instanceof Transferencia tr) {
            Conta outra = (tr.getOrigem() == this) ? tr.getDestino() : tr.getOrigem();
            outra.adicionarTransacao(t);
        }
    }

    /** Remove uma transação pelo id. O saldo é recalculado automaticamente. */
    public void removerTransacao(int id) {
        Transacao t = buscarTransacao(id);
        Conta outra = outraConta(t);

        transacoes.remove(t);
        if (outra != null) {
            outra.transacoes.remove(t);
        }

        // Ex.: apagar uma receita cujo dinheiro já foi gasto deixaria a conta negativa.
        Conta negativa = contaNegativa(this, outra);
        if (negativa != null) {
            transacoes.add(t);
            if (outra != null) {
                outra.transacoes.add(t);
            }
            throw new SaldoInsuficienteException("Não dá para excluir a transação #" + id
                    + ": o saldo disponível da conta " + negativa.getNome() + " ficaria negativo.");
        }
    }

    /**
     * Edita uma transação (RF04) conferindo o saldo: se a mudança deixar alguma conta
     * envolvida com saldo disponível negativo, os dados antigos são restaurados.
     */
    public void editarTransacao(int id, LocalDate data, BigDecimal valor, String descricao, Categoria categoria) {
        Transacao t = buscarTransacao(id);
        LocalDate dataAntiga = t.getData();
        BigDecimal valorAntigo = t.getValor();
        String descricaoAntiga = t.getDescricao();
        Categoria categoriaAntiga = t.getCategoria();

        t.atualizarDados(data, valor, descricao, categoria);

        Conta negativa = contaNegativa(this, outraConta(t));
        if (negativa != null) {
            t.atualizarDados(dataAntiga, valorAntigo, descricaoAntiga, categoriaAntiga);
            throw new SaldoInsuficienteException("Edição cancelada: o saldo disponível da conta "
                    + negativa.getNome() + " ficaria negativo.");
        }
    }

    /** Para uma transferência, devolve a outra conta envolvida; senão, null. */
    private Conta outraConta(Transacao t) {
        if (t instanceof Transferencia tr) {
            return (tr.getOrigem() == this) ? tr.getDestino() : tr.getOrigem();
        }
        return null;
    }

    private static Conta contaNegativa(Conta a, Conta b) {
        if (a.calcularSaldoDisponivel().signum() < 0) {
            return a;
        }
        if (b != null && b.calcularSaldoDisponivel().signum() < 0) {
            return b;
        }
        return null;
    }

    /** Procura uma transação pelo id; lança exceção se não existir. */
    public Transacao buscarTransacao(int id) {
        for (Transacao t : transacoes) {
            if (t.getId() == id) {
                return t;
            }
        }
        throw new TransacaoNaoEncontradaException(
                "Transação #" + id + " não encontrada na conta " + nome + ".");
    }

    // ---------------- Extrato ----------------

    /** Extrato completo, ordenado por data. */
    public List<Transacao> listarExtrato() {
        List<Transacao> extrato = new ArrayList<>(transacoes);
        extrato.sort(Comparator.comparing(Transacao::getData));
        return extrato;
    }

    /**
     * Sobrecarga: extrato filtrado por período e categoria (RF07).
     * Passe null em inicio, fim ou categoria para não filtrar por aquele campo.
     */
    public List<Transacao> listarExtrato(LocalDate inicio, LocalDate fim, Categoria categoria) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser depois da data final.");
        }
        List<Transacao> filtrado = new ArrayList<>();
        for (Transacao t : listarExtrato()) {
            boolean dentroDoInicio = (inicio == null) || !t.getData().isBefore(inicio);
            boolean dentroDoFim = (fim == null) || !t.getData().isAfter(fim);
            boolean categoriaOk = (categoria == null) || mesmaCategoria(t.getCategoria(), categoria);
            if (dentroDoInicio && dentroDoFim && categoriaOk) {
                filtrado.add(t);
            }
        }
        return filtrado;
    }

    /** Mesmo critério do Relatorio: categorias iguais têm o mesmo nome (ignorando maiúsculas). */
    private boolean mesmaCategoria(Categoria a, Categoria b) {
        return a != null && a.getNome() != null && b.getNome() != null
                && a.getNome().equalsIgnoreCase(b.getNome());
    }

    // ---------------- Envelopes ----------------

    public void adicionarEnvelope(Envelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("Envelope não pode ser nulo.");
        }
        for (Envelope e : envelopes) {
            if (e.getId() == envelope.getId()) {
                throw new IllegalArgumentException(
                        "A conta " + nome + " já tem um envelope com id #" + envelope.getId() + ".");
            }
        }
        if (envelope.getValorReservado().compareTo(calcularSaldoDisponivel()) > 0) {
            throw new SaldoInsuficienteException(
                    "Saldo disponível insuficiente na conta " + nome + " para criar o envelope "
                    + envelope.getNome() + " com " + Moeda.formatar(envelope.getValorReservado()) + ".");
        }
        envelopes.add(envelope);
    }

    /** Remove o envelope; o dinheiro que estava reservado volta a ficar disponível. */
    public void removerEnvelope(int id) {
        envelopes.remove(buscarEnvelope(id));
    }

    public Envelope buscarEnvelope(int id) {
        for (Envelope e : envelopes) {
            if (e.getId() == id) {
                return e;
            }
        }
        throw new IllegalArgumentException("Envelope #" + id + " não encontrado na conta " + nome + ".");
    }

    /** Guarda dinheiro num envelope, conferindo antes se a conta tem saldo disponível. */
    public void guardarNoEnvelope(int idEnvelope, BigDecimal valor) {
        Envelope envelope = buscarEnvelope(idEnvelope);
        if (valor != null && valor.compareTo(calcularSaldoDisponivel()) > 0) {
            throw new SaldoInsuficienteException(
                    "Saldo disponível insuficiente na conta " + nome
                    + ". Disponível: " + Moeda.formatar(calcularSaldoDisponivel())
                    + ", quer guardar: " + Moeda.formatar(valor));
        }
        envelope.adicionarValor(valor); // valida valor nulo/zero/negativo
    }

    /** Tira dinheiro de um envelope, que volta a ficar disponível na conta. */
    public void retirarDoEnvelope(int idEnvelope, BigDecimal valor) {
        buscarEnvelope(idEnvelope).retirarValor(valor);
    }

    // ---------------- Carregamento do arquivo (Entrega 3) ----------------

    /**
     * Usado só pelo RepositorioJson ao ler o dados.json.
     * Não confere saldo, porque os dados já foram validados quando foram criados;
     * conferir de novo poderia dar erro falso dependendo da ordem de leitura.
     * No caso da transferência, o repositório chama este método nas duas contas.
     */
    public void carregarTransacao(Transacao t) {
        if (t == null) {
            throw new IllegalArgumentException("Transação não pode ser nula.");
        }
        if (t instanceof Transferencia tr && tr.getOrigem() != this && tr.getDestino() != this) {
            throw new TransferenciaInvalidaException(
                    "A transferência #" + t.getId() + " não envolve a conta " + nome + ".");
        }
        for (Transacao existente : transacoes) {
            if (existente.getId() == t.getId()) {
                throw new IllegalArgumentException(
                        "A conta " + nome + " já tem uma transação com id #" + t.getId() + ".");
            }
        }
        transacoes.add(t);
    }

    /** Usado só pelo RepositorioJson: recoloca um envelope salvo, sem conferir saldo. */
    public void carregarEnvelope(Envelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("Envelope não pode ser nulo.");
        }
        for (Envelope e : envelopes) {
            if (e.getId() == envelope.getId()) {
                throw new IllegalArgumentException(
                        "A conta " + nome + " já tem um envelope com id #" + envelope.getId() + ".");
            }
        }
        envelopes.add(envelope);
    }

    /** true se a conta tem alguma transferência com outra conta. */
    public boolean temTransferencias() {
        for (Transacao t : transacoes) {
            if (t instanceof Transferencia) {
                return true;
            }
        }
        return false;
    }

    // ---------------- Getters / Setters ----------------

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da conta não pode ser vazio.");
        }
        this.nome = nome;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    /** Cópia somente leitura, para ninguém alterar a lista por fora (encapsulamento). */
    public List<Transacao> getTransacoes() {
        return List.copyOf(transacoes);
    }

    public List<Envelope> getEnvelopes() {
        return List.copyOf(envelopes);
    }

    @Override
    public String toString() {
        return String.format("Conta #%d - %s | Saldo: %s | Disponível: %s",
                id, nome, Moeda.formatar(calcularSaldo()), Moeda.formatar(calcularSaldoDisponivel()));
    }
}
