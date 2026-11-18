package app;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

import excecoes.ContaNaoEncontradaException;
import excecoes.SaldoInsuficienteException;
import excecoes.TransacaoNaoEncontradaException;
import excecoes.TransferenciaInvalidaException;
import excecoes.ValorInvalidoException;
import modelo.Categoria;
import modelo.Conta;
import modelo.Despesa;
import modelo.Envelope;
import modelo.Receita;
import modelo.Relatorio;
import modelo.Transacao;
import modelo.Transferencia;

/**
 * Menu interativo de console do Money Buu.
 * O usuário cria contas, registra receitas, despesas e transferências, consulta extratos,
 * usa envelopes e gera relatórios. Os erros (exceções) são mostrados sem fechar o programa.
 */
public class Menu {
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private final Scanner entrada = new Scanner(System.in);
    private final List<Conta> contas = new ArrayList<>();
    private final List<Categoria> categorias = new ArrayList<>();
    private int proximoIdCategoria = 1;

    public static void main(String[] args) {
        new Menu().executar();
    }

    public Menu() {
        for (String nome : List.of("Salário", "Alimentação", "Moradia", "Transporte", "Lazer", "Saúde", "Outros")) {
            categorias.add(new Categoria(proximoIdCategoria++, nome));
        }
    }

    public void executar() {
        System.out.println("=========================================");
        System.out.println("     MONEY BUU - suas finanças no terminal");
        System.out.println("=========================================");

        try {
            boolean rodando = true;
            while (rodando) {
                mostrarMenuPrincipal();
                int opcao = lerInt("Escolha uma opção: ");
                System.out.println();
                try {
                    switch (opcao) {
                        case 1 -> criarConta();
                        case 2 -> listarContas();
                        case 3 -> registrarReceita();
                        case 4 -> registrarDespesa();
                        case 5 -> transferir();
                        case 6 -> verExtrato();
                        case 7 -> editarTransacao();
                        case 8 -> excluirTransacao();
                        case 9 -> menuEnvelopes();
                        case 10 -> relatorioDoMes();
                        case 11 -> menuCategorias();
                        case 0 -> rodando = false;
                        default -> System.out.println("Opção inválida.");
                    }
                } catch (ContaNaoEncontradaException | TransacaoNaoEncontradaException
                         | SaldoInsuficienteException | ValorInvalidoException
                         | TransferenciaInvalidaException e) {
                    System.out.println("[Erro] " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.out.println("[Erro] " + e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            // a entrada acabou (ex.: Ctrl+D): sai normalmente
        }
        System.out.println("\nAté mais! 👻💰");
    }

    private void mostrarMenuPrincipal() {
        System.out.println("\n---------------- MENU ----------------");
        System.out.println(" 1 - Criar conta");
        System.out.println(" 2 - Listar contas e saldos");
        System.out.println(" 3 - Registrar receita");
        System.out.println(" 4 - Registrar despesa");
        System.out.println(" 5 - Transferir entre contas");
        System.out.println(" 6 - Ver extrato");
        System.out.println(" 7 - Editar transação");
        System.out.println(" 8 - Excluir transação");
        System.out.println(" 9 - Envelopes");
        System.out.println("10 - Relatório do mês");
        System.out.println("11 - Categorias");
        System.out.println(" 0 - Sair");
    }

    // =============== Contas ===============

    private void criarConta() {
        System.out.println("== Criar conta ==");
        String nome = lerTexto("Nome da conta: ");
        BigDecimal saldoInicial = lerValor("Saldo inicial (ex.: 1500,00): R$ ");
        Conta conta = new Conta(nome, saldoInicial);
        contas.add(conta);
        System.out.println("Conta criada! " + conta);
    }

    private void listarContas() {
        System.out.println("== Contas ==");
        if (contas.isEmpty()) {
            System.out.println("Nenhuma conta cadastrada. Use a opção 1 para criar uma.");
            return;
        }
        for (Conta c : contas) {
            System.out.printf("#%d - %-20s Saldo: %-14s Disponível: %s%n",
                    c.getId(), c.getNome(), dinheiro(c.calcularSaldo()), dinheiro(c.calcularSaldoDisponivel()));
        }
    }

    /** Procura a conta pelo id; se não existir, lança ContaNaoEncontradaException. */
    private Conta buscarConta(int id) {
        for (Conta c : contas) {
            if (c.getId() == id) {
                return c;
            }
        }
        throw new ContaNaoEncontradaException("Conta #" + id + " não encontrada.");
    }

    private Conta escolherConta(String pergunta) {
        if (contas.isEmpty()) {
            throw new ContaNaoEncontradaException("Nenhuma conta cadastrada. Crie uma conta primeiro (opção 1).");
        }
        listarContas();
        return buscarConta(lerInt(pergunta));
    }

    // =============== Transações ===============

    private void registrarReceita() {
        System.out.println("== Registrar receita ==");
        Conta conta = escolherConta("Id da conta que vai receber: ");
        String descricao = lerTexto("Descrição: ");
        BigDecimal valor = lerValor("Valor: R$ ");
        LocalDate data = lerData("Data (dd/mm/aaaa, Enter = hoje): ");
        Categoria categoria = escolherCategoria();

        Receita receita = new Receita(Transacao.getProximoId(), data, valor, descricao, categoria);
        conta.adicionarTransacao(receita);
        System.out.println("Receita registrada! Saldo de " + conta.getNome() + ": " + dinheiro(conta.calcularSaldo()));
    }

    private void registrarDespesa() {
        System.out.println("== Registrar despesa ==");
        Conta conta = escolherConta("Id da conta que vai pagar: ");
        String descricao = lerTexto("Descrição: ");
        BigDecimal valor = lerValor("Valor: R$ ");
        LocalDate data = lerData("Data (dd/mm/aaaa, Enter = hoje): ");
        Categoria categoria = escolherCategoria();

        Despesa despesa = new Despesa(Transacao.getProximoId(), data, valor, descricao, categoria);
        conta.adicionarTransacao(despesa);
        System.out.println("Despesa registrada! Saldo de " + conta.getNome() + ": " + dinheiro(conta.calcularSaldo()));
    }

    private void transferir() {
        System.out.println("== Transferir entre contas ==");
        Conta origem = escolherConta("Id da conta de ORIGEM: ");
        Conta destino = buscarConta(lerInt("Id da conta de DESTINO: "));
        String descricao = lerTexto("Descrição: ");
        BigDecimal valor = lerValor("Valor: R$ ");
        LocalDate data = lerData("Data (dd/mm/aaaa, Enter = hoje): ");

        Transferencia t = new Transferencia(data, valor, descricao, null, origem, destino);
        origem.adicionarTransacao(t); // já registra no destino também
        System.out.println("Transferência feita!");
        System.out.println("  " + origem.getNome() + ": " + dinheiro(origem.calcularSaldo()));
        System.out.println("  " + destino.getNome() + ": " + dinheiro(destino.calcularSaldo()));
    }

    private void verExtrato() {
        System.out.println("== Extrato ==");
        Conta conta = escolherConta("Id da conta: ");
        List<Transacao> extrato;

        if (lerSimNao("Filtrar por período/categoria? (s/n): ")) {
            LocalDate inicio = lerDataOpcional("Data inicial (dd/mm/aaaa, Enter = sem limite): ");
            LocalDate fim = lerDataOpcional("Data final (dd/mm/aaaa, Enter = sem limite): ");
            Categoria categoria = escolherCategoria();
            extrato = conta.listarExtrato(inicio, fim, categoria);
        } else {
            extrato = conta.listarExtrato();
        }

        imprimirExtrato(conta, extrato);
    }

    private void imprimirExtrato(Conta conta, List<Transacao> extrato) {
        System.out.println("\nExtrato - " + conta.getNome());
        if (extrato.isEmpty()) {
            System.out.println("  (nenhuma transação)");
        }
        for (Transacao t : extrato) {
            String sinal = t.aplicar(conta).signum() >= 0 ? "+" : "-";
            System.out.println("  " + sinal + " " + t);
        }
        System.out.println("Saldo: " + dinheiro(conta.calcularSaldo())
                + " | Disponível: " + dinheiro(conta.calcularSaldoDisponivel()));
    }

    private void editarTransacao() {
        System.out.println("== Editar transação ==");
        Conta conta = escolherConta("Id da conta: ");
        imprimirExtrato(conta, conta.listarExtrato());
        Transacao t = conta.buscarTransacao(lerInt("Id da transação que quer editar: "));

        System.out.println("Deixe em branco (Enter) para manter o valor atual.");
        String descricao = lerTextoOuManter("Descrição", t.getDescricao());
        BigDecimal valor = lerValorOuManter("Valor", t.getValor());
        LocalDate data = lerDataOuManter("Data", t.getData());
        Categoria categoria = t.getCategoria();
        if (lerSimNao("Mudar a categoria? (s/n): ")) {
            categoria = escolherCategoria();
        }

        conta.editarTransacao(t.getId(), data, valor, descricao, categoria);
        System.out.println("Transação editada! " + t);
        System.out.println("Saldo de " + conta.getNome() + ": " + dinheiro(conta.calcularSaldo()));
    }

    private void excluirTransacao() {
        System.out.println("== Excluir transação ==");
        Conta conta = escolherConta("Id da conta: ");
        imprimirExtrato(conta, conta.listarExtrato());
        int id = lerInt("Id da transação que quer excluir: ");
        Transacao t = conta.buscarTransacao(id);
        if (lerSimNao("Excluir \"" + t.getDescricao() + "\"? (s/n): ")) {
            conta.removerTransacao(id);
            System.out.println("Transação excluída! Saldo de " + conta.getNome() + ": "
                    + dinheiro(conta.calcularSaldo()));
        } else {
            System.out.println("Nada foi excluído.");
        }
    }

    // =============== Envelopes ===============

    private void menuEnvelopes() {
        System.out.println("== Envelopes ==");
        Conta conta = escolherConta("Id da conta dos envelopes: ");

        boolean noSubmenu = true;
        while (noSubmenu) {
            System.out.println("\n--- Envelopes de " + conta.getNome()
                    + " (disponível: " + dinheiro(conta.calcularSaldoDisponivel()) + ") ---");
            System.out.println("1 - Criar envelope");
            System.out.println("2 - Listar envelopes");
            System.out.println("3 - Guardar dinheiro");
            System.out.println("4 - Retirar dinheiro");
            System.out.println("5 - Remover envelope");
            System.out.println("0 - Voltar");
            int opcao = lerInt("Escolha: ");

            try {
                switch (opcao) {
                    case 1 -> {
                        String nome = lerTexto("Nome do envelope (ex.: Viagem): ");
                        BigDecimal meta = lerValor("Meta: R$ ");
                        Envelope e = new Envelope(nome, meta);
                        conta.adicionarEnvelope(e);
                        System.out.println("Envelope criado! " + e);
                    }
                    case 2 -> listarEnvelopes(conta);
                    case 3 -> {
                        listarEnvelopes(conta);
                        int id = lerInt("Id do envelope: ");
                        conta.guardarNoEnvelope(id, lerValor("Quanto guardar: R$ "));
                        System.out.println("Guardado! " + conta.buscarEnvelope(id));
                    }
                    case 4 -> {
                        listarEnvelopes(conta);
                        int id = lerInt("Id do envelope: ");
                        conta.retirarDoEnvelope(id, lerValor("Quanto retirar: R$ "));
                        System.out.println("Retirado! " + conta.buscarEnvelope(id));
                    }
                    case 5 -> {
                        listarEnvelopes(conta);
                        conta.removerEnvelope(lerInt("Id do envelope: "));
                        System.out.println("Envelope removido. O dinheiro voltou a ficar disponível.");
                    }
                    case 0 -> noSubmenu = false;
                    default -> System.out.println("Opção inválida.");
                }
            } catch (SaldoInsuficienteException | ValorInvalidoException e) {
                System.out.println("[Erro] " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("[Erro] " + e.getMessage());
            }
        }
    }

    private void listarEnvelopes(Conta conta) {
        if (conta.getEnvelopes().isEmpty()) {
            System.out.println("  (nenhum envelope)");
        }
        for (Envelope e : conta.getEnvelopes()) {
            System.out.println("  " + e + (e.metaAtingida() ? "  <- meta atingida!" : ""));
        }
    }

    // =============== Relatório ===============

    private void relatorioDoMes() {
        System.out.println("== Relatório do mês ==");
        YearMonth mes = lerMes("Mês (mm/aaaa, Enter = mês atual): ");
        Relatorio relatorio = new Relatorio(contas);

        System.out.println("\nRelatório de " + mes.format(FORMATO_MES) + " (todas as contas)");
        System.out.println("  Receitas:     " + dinheiro(relatorio.totalReceitas(mes)));
        System.out.println("  Despesas:     " + dinheiro(relatorio.totalDespesas(mes)));
        System.out.println("  Saldo do mês: " + dinheiro(relatorio.total(mes)));

        System.out.println("  Por categoria:");
        boolean algumaCategoria = false;
        for (Categoria c : categorias) {
            BigDecimal total = relatorio.total(mes, c);
            if (total.signum() != 0) {
                System.out.printf("    %-15s %s%n", c.getNome(), dinheiro(total));
                algumaCategoria = true;
            }
        }
        if (!algumaCategoria) {
            System.out.println("    (nenhuma movimentação com categoria)");
        }
    }

    // =============== Categorias ===============

    private void menuCategorias() {
        System.out.println("== Categorias ==");
        listarCategorias();
        if (lerSimNao("Criar uma nova categoria? (s/n): ")) {
            criarCategoria();
        }
    }

    private void listarCategorias() {
        for (Categoria c : categorias) {
            System.out.println("  " + c.getId() + " - " + c.getNome());
        }
    }

    private Categoria criarCategoria() {
        String nome = lerTexto("Nome da nova categoria: ");
        Categoria c = new Categoria(proximoIdCategoria++, nome);
        categorias.add(c);
        System.out.println("Categoria criada: " + c.getId() + " - " + c.getNome());
        return c;
    }

    /** Pede uma categoria: id da lista, N para criar nova, Enter para nenhuma. */
    private Categoria escolherCategoria() {
        System.out.println("Categorias:");
        listarCategorias();
        while (true) {
            String resposta = lerLinha("Id da categoria (N = nova, Enter = nenhuma): ");
            if (resposta.isEmpty()) {
                return null;
            }
            if (resposta.equalsIgnoreCase("n")) {
                return criarCategoria();
            }
            try {
                int id = Integer.parseInt(resposta);
                for (Categoria c : categorias) {
                    if (c.getId() == id) {
                        return c;
                    }
                }
                System.out.println("Categoria não encontrada, tente de novo.");
            } catch (NumberFormatException e) {
                System.out.println("Digite um número, N ou Enter.");
            }
        }
    }

    // =============== Leitura do teclado ===============

    private String lerLinha(String pergunta) {
        System.out.print(pergunta);
        return entrada.nextLine().trim();
    }

    private String lerTexto(String pergunta) {
        while (true) {
            String texto = lerLinha(pergunta);
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("Esse campo não pode ficar vazio.");
        }
    }

    private String lerTextoOuManter(String campo, String atual) {
        String texto = lerLinha(campo + " [" + atual + "]: ");
        return texto.isEmpty() ? atual : texto;
    }

    private int lerInt(String pergunta) {
        while (true) {
            try {
                return Integer.parseInt(lerLinha(pergunta));
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro.");
            }
        }
    }

    private boolean lerSimNao(String pergunta) {
        while (true) {
            String r = lerLinha(pergunta).toLowerCase();
            if (r.equals("s") || r.equals("sim")) {
                return true;
            }
            if (r.equals("n") || r.equals("nao") || r.equals("não")) {
                return false;
            }
            System.out.println("Responda s ou n.");
        }
    }

    /** Lê um valor em reais. Aceita "1500", "1500,50", "1.500,50" ou "1500.50". */
    private BigDecimal lerValor(String pergunta) {
        while (true) {
            try {
                return converterValor(lerLinha(pergunta));
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido. Exemplo: 1500,00");
            }
        }
    }

    private BigDecimal lerValorOuManter(String campo, BigDecimal atual) {
        while (true) {
            String texto = lerLinha(campo + " [" + dinheiro(atual) + "]: R$ ");
            if (texto.isEmpty()) {
                return atual;
            }
            try {
                return converterValor(texto);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido. Exemplo: 1500,00");
            }
        }
    }

    private BigDecimal converterValor(String texto) {
        String limpo = texto.replace("R$", "").replace(" ", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(limpo);
    }

    private LocalDate lerData(String pergunta) {
        while (true) {
            String texto = lerLinha(pergunta);
            if (texto.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(texto, FORMATO_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use dd/mm/aaaa, ex.: 18/11/2026");
            }
        }
    }

    private LocalDate lerDataOpcional(String pergunta) {
        while (true) {
            String texto = lerLinha(pergunta);
            if (texto.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(texto, FORMATO_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use dd/mm/aaaa, ex.: 18/11/2026");
            }
        }
    }

    private LocalDate lerDataOuManter(String campo, LocalDate atual) {
        LocalDate nova = lerDataOpcional(campo + " [" + atual.format(FORMATO_DATA) + "]: ");
        return (nova == null) ? atual : nova;
    }

    private YearMonth lerMes(String pergunta) {
        while (true) {
            String texto = lerLinha(pergunta);
            if (texto.isEmpty()) {
                return YearMonth.now();
            }
            try {
                return YearMonth.parse(texto, FORMATO_MES);
            } catch (DateTimeParseException e) {
                System.out.println("Mês inválido. Use mm/aaaa, ex.: 11/2026");
            }
        }
    }

    private static String dinheiro(BigDecimal valor) {
        return MOEDA.format(valor);
    }
}

