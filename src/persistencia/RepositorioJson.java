package persistencia;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import excecoes.ArquivoDadosException;
import modelo.Categoria;
import modelo.Conta;
import modelo.Despesa;
import modelo.Envelope;
import modelo.Receita;
import modelo.Transacao;
import modelo.Transferencia;

/**
 * Camada de persistência do Money Buu: grava e lê os dados no arquivo dados.json.
 *
 * Formato do arquivo:
 * {
 *   "versao": 1,
 *   "categorias": [ {"id": 1, "nome": "Salário"} ],
 *   "contas": [ {"id": 1, "nome": "Carteira", "saldoInicial": 100.00,
 *                "envelopes": [ {"id": 1, "nome": "Viagem", "meta": 500.00, "valorReservado": 50.00} ]} ],
 *   "transacoes": [
 *     {"id": 1, "tipo": "RECEITA", "data": "2026-11-20", "valor": 50.00,
 *      "descricao": "Mesada", "categoriaId": 1, "contaId": 1},
 *     {"id": 2, "tipo": "TRANSFERENCIA", "data": "2026-11-21", "valor": 20.00,
 *      "descricao": "Poupar", "categoriaId": null, "origemId": 1, "destinoId": 2}
 *   ]
 * }
 *
 * As transações ficam numa lista própria (e não dentro de cada conta) porque a transferência
 * pertence a duas contas ao mesmo tempo: assim ela é gravada uma única vez.
 * O saldo atual não é gravado: ele é sempre recalculado pela Conta.
 */
public class RepositorioJson {
    private static final int VERSAO = 1;

    private final Path arquivo;

    public RepositorioJson(Path arquivo) {
        this.arquivo = arquivo;
    }

    /** Sobrecarga: usa o arquivo padrão dados.json na pasta onde o programa é executado. */
    public RepositorioJson() {
        this(Path.of("dados.json"));
    }

    public Path getArquivo() {
        return arquivo;
    }

    // =========================== GRAVAR ===========================

    public void salvar(List<Conta> contas, List<Categoria> categorias) {
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("versao", VERSAO);
        raiz.put("categorias", categoriasParaJson(categorias));
        raiz.put("contas", contasParaJson(contas));
        raiz.put("transacoes", transacoesParaJson(contas));

        String texto = Json.escrever(raiz);

        // Grava primeiro num arquivo temporário e depois troca pelo original.
        // Assim, se o programa cair no meio da gravação, o dados.json antigo não se corrompe.
        Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            Files.writeString(temporario, texto, StandardCharsets.UTF_8);
            try {
                Files.move(temporario, arquivo,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ArquivoDadosException("Não foi possível gravar o arquivo " + arquivo
                    + ": " + e.getMessage(), e);
        }
    }

    private List<Object> categoriasParaJson(List<Categoria> categorias) {
        List<Object> lista = new ArrayList<>();
        for (Categoria c : categorias) {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("id", c.getId());
            obj.put("nome", c.getNome());
            lista.add(obj);
        }
        return lista;
    }

    private List<Object> contasParaJson(List<Conta> contas) {
        List<Object> lista = new ArrayList<>();
        for (Conta conta : contas) {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("id", conta.getId());
            obj.put("nome", conta.getNome());
            obj.put("saldoInicial", conta.getSaldoInicial());

            List<Object> envelopes = new ArrayList<>();
            for (Envelope e : conta.getEnvelopes()) {
                Map<String, Object> env = new LinkedHashMap<>();
                env.put("id", e.getId());
                env.put("nome", e.getNome());
                env.put("meta", e.getMeta());
                env.put("valorReservado", e.getValorReservado());
                envelopes.add(env);
            }
            obj.put("envelopes", envelopes);
            lista.add(obj);
        }
        return lista;
    }

    private List<Object> transacoesParaJson(List<Conta> contas) {
        // Junta as transações de todas as contas sem repetir (a transferência aparece nas duas).
        Map<Integer, Map<String, Object>> porId = new HashMap<>();
        for (Conta conta : contas) {
            for (Transacao t : conta.getTransacoes()) {
                if (porId.containsKey(t.getId())) {
                    continue;
                }
                Map<String, Object> obj = new LinkedHashMap<>();
                obj.put("id", t.getId());
                obj.put("tipo", t.getTipo());
                obj.put("data", t.getData().toString()); // formato ISO: aaaa-mm-dd
                obj.put("valor", t.getValor());
                obj.put("descricao", t.getDescricao());
                obj.put("categoriaId", t.getCategoria() == null ? null : t.getCategoria().getId());
                if (t instanceof Transferencia tr) {
                    obj.put("origemId", tr.getOrigem().getId());
                    obj.put("destinoId", tr.getDestino().getId());
                } else {
                    obj.put("contaId", conta.getId());
                }
                porId.put(t.getId(), obj);
            }
        }
        List<Map<String, Object>> ordenadas = new ArrayList<>(porId.values());
        ordenadas.sort(Comparator.comparingInt(o -> (Integer) o.get("id")));
        return new ArrayList<>(ordenadas);
    }

    // =========================== LER ===========================

    /**
     * Lê o arquivo e recria as contas, categorias, envelopes e transações.
     * Se o arquivo ainda não existe (primeira execução), devolve dados vazios, sem erro.
     */
    public DadosMoneyBuu carregar() {
        if (!Files.exists(arquivo)) {
            return DadosMoneyBuu.vazio();
        }

        String texto;
        try {
            texto = Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ArquivoDadosException("Não foi possível ler o arquivo " + arquivo
                    + ": " + e.getMessage(), e);
        }
        if (texto.isBlank()) {
            return DadosMoneyBuu.vazio();
        }

        try {
            Map<String, Object> raiz = comoObjeto(Json.ler(texto), "raiz do arquivo");

            // 1) Categorias
            List<Categoria> categorias = new ArrayList<>();
            Map<Integer, Categoria> categoriaPorId = new HashMap<>();
            for (Object item : comoLista(raiz.get("categorias"), "categorias")) {
                Map<String, Object> obj = comoObjeto(item, "categoria");
                Categoria c = new Categoria(inteiro(obj, "id"), texto(obj, "nome"));
                categorias.add(c);
                categoriaPorId.put(c.getId(), c);
            }

            // 2) Contas (precisam existir antes das transações, por causa das transferências)
            List<Conta> contas = new ArrayList<>();
            Map<Integer, Conta> contaPorId = new HashMap<>();
            Map<Conta, List<Object>> envelopesPorConta = new HashMap<>();
            for (Object item : comoLista(raiz.get("contas"), "contas")) {
                Map<String, Object> obj = comoObjeto(item, "conta");
                Conta conta = new Conta(inteiro(obj, "id"), texto(obj, "nome"), decimal(obj, "saldoInicial"));
                if (contaPorId.put(conta.getId(), conta) != null) {
                    throw new ArquivoDadosException("Há duas contas com o id #" + conta.getId() + ".");
                }
                contas.add(conta);
                envelopesPorConta.put(conta, comoLista(obj.get("envelopes"), "envelopes"));
            }

            // 3) Transações: o campo "tipo" diz qual subclasse recriar
            for (Object item : comoLista(raiz.get("transacoes"), "transacoes")) {
                Map<String, Object> obj = comoObjeto(item, "transação");
                recriarTransacao(obj, contaPorId, categoriaPorId);
            }

            // 4) Envelopes (depois das transações, para o saldo já estar completo)
            for (Conta conta : contas) {
                for (Object item : envelopesPorConta.get(conta)) {
                    Map<String, Object> obj = comoObjeto(item, "envelope");
                    conta.carregarEnvelope(new Envelope(inteiro(obj, "id"), texto(obj, "nome"),
                            decimal(obj, "meta"), decimal(obj, "valorReservado")));
                }
            }

            return new DadosMoneyBuu(contas, categorias);

        } catch (ArquivoDadosException e) {
            throw e;
        } catch (RuntimeException e) {
            // Ex.: valor negativo no arquivo (ValorInvalidoException), transferência para a mesma conta etc.
            throw new ArquivoDadosException("O arquivo " + arquivo + " tem dados inválidos: "
                    + e.getMessage(), e);
        }
    }

    private void recriarTransacao(Map<String, Object> obj, Map<Integer, Conta> contaPorId,
                                  Map<Integer, Categoria> categoriaPorId) {
        int id = inteiro(obj, "id");
        String tipo = texto(obj, "tipo");
        LocalDate data = data(obj, "data");
        BigDecimal valor = decimal(obj, "valor");
        String descricao = textoOpcional(obj, "descricao");
        Categoria categoria = null;
        Integer categoriaId = inteiroOpcional(obj, "categoriaId");
        if (categoriaId != null) {
            categoria = categoriaPorId.get(categoriaId); // categoria apagada do arquivo -> fica sem categoria
        }

        switch (tipo.toUpperCase()) {
            case "RECEITA" -> {
                Conta conta = conta(contaPorId, inteiro(obj, "contaId"), id);
                conta.carregarTransacao(new Receita(id, data, valor, descricao, categoria));
            }
            case "DESPESA" -> {
                Conta conta = conta(contaPorId, inteiro(obj, "contaId"), id);
                conta.carregarTransacao(new Despesa(id, data, valor, descricao, categoria));
            }
            case "TRANSFERENCIA" -> {
                Conta origem = conta(contaPorId, inteiro(obj, "origemId"), id);
                Conta destino = conta(contaPorId, inteiro(obj, "destinoId"), id);
                Transferencia t = new Transferencia(id, data, valor, descricao, categoria, origem, destino);
                origem.carregarTransacao(t);
                destino.carregarTransacao(t);
            }
            default -> throw new ArquivoDadosException(
                    "Transação #" + id + " tem um tipo desconhecido: \"" + tipo + "\".");
        }
    }

    private Conta conta(Map<Integer, Conta> contaPorId, int idConta, int idTransacao) {
        Conta conta = contaPorId.get(idConta);
        if (conta == null) {
            throw new ArquivoDadosException("A transação #" + idTransacao
                    + " aponta para a conta #" + idConta + ", que não existe no arquivo.");
        }
        return conta;
    }

    /** Se o arquivo estiver estragado, guarda uma cópia dele antes de o programa gravar por cima. */
    public Path fazerBackup() {
        Path copia = arquivo.resolveSibling(arquivo.getFileName() + ".corrompido");
        try {
            Files.copy(arquivo, copia, StandardCopyOption.REPLACE_EXISTING);
            return copia;
        } catch (IOException e) {
            throw new ArquivoDadosException("Não foi possível fazer a cópia de segurança: " + e.getMessage(), e);
        }
    }

    // ================= Funções de apoio para ler os campos =================

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comoObjeto(Object valor, String nome) {
        if (valor instanceof Map<?, ?> mapa) {
            return (Map<String, Object>) mapa;
        }
        throw new ArquivoDadosException("Era esperado um objeto JSON em \"" + nome + "\".");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> comoLista(Object valor, String nome) {
        if (valor == null) {
            return List.of(); // campo ausente = lista vazia
        }
        if (valor instanceof List<?> lista) {
            return (List<Object>) lista;
        }
        throw new ArquivoDadosException("Era esperada uma lista em \"" + nome + "\".");
    }

    private static Object obrigatorio(Map<String, Object> obj, String campo) {
        Object valor = obj.get(campo);
        if (valor == null) {
            throw new ArquivoDadosException("Campo obrigatório \"" + campo + "\" ausente no arquivo.");
        }
        return valor;
    }

    private static int inteiro(Map<String, Object> obj, String campo) {
        Object valor = obrigatorio(obj, campo);
        if (valor instanceof BigDecimal b) {
            try {
                return b.intValueExact();
            } catch (ArithmeticException e) {
                // cai no erro abaixo
            }
        }
        throw new ArquivoDadosException("O campo \"" + campo + "\" deveria ser um número inteiro.");
    }

    private static Integer inteiroOpcional(Map<String, Object> obj, String campo) {
        return obj.get(campo) == null ? null : inteiro(obj, campo);
    }

    private static BigDecimal decimal(Map<String, Object> obj, String campo) {
        Object valor = obrigatorio(obj, campo);
        if (valor instanceof BigDecimal b) {
            return b;
        }
        throw new ArquivoDadosException("O campo \"" + campo + "\" deveria ser um número.");
    }

    private static String texto(Map<String, Object> obj, String campo) {
        Object valor = obrigatorio(obj, campo);
        if (valor instanceof String s) {
            return s;
        }
        throw new ArquivoDadosException("O campo \"" + campo + "\" deveria ser um texto.");
    }

    private static String textoOpcional(Map<String, Object> obj, String campo) {
        return obj.get(campo) == null ? null : texto(obj, campo);
    }

    private static LocalDate data(Map<String, Object> obj, String campo) {
        String valor = texto(obj, campo);
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new ArquivoDadosException("Data inválida no campo \"" + campo + "\": " + valor
                    + " (use aaaa-mm-dd).");
        }
    }
}
