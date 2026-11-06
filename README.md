# 👻 Money Buu 💰

### Seu dinheiro organizado, sem complicação!

O **Money Buu** é um projeto de aplicativo de finanças pessoais desenvolvido em **Java**, com o objetivo de tornar a organização financeira mais simples, acessível e intuitiva.

A proposta é oferecer uma ferramenta gratuita para pessoas de diferentes idades acompanharem suas receitas, despesas e transferências, além de organizarem suas economias por meio de caixinhas virtuais, chamadas de **envelopes financeiros**.

O projeto está sendo desenvolvido como atividade acadêmica da disciplina de **Modelagem e Programação Orientada a Objetos**, da Universidade Federal Rural de Pernambuco (UFRPE/UABJ).

---

## 🎯 Objetivo

Desenvolver uma aplicação que permita aos usuários controlar suas finanças pessoais de maneira prática, mantendo um histórico de movimentações e oferecendo recursos para planejamento financeiro.

O sistema busca combinar funcionalidades essenciais com uma interface amigável e uma identidade visual própria.

## ✨ Funcionalidades planejadas

- 💳 **Gerenciamento de contas:** cadastro de contas financeiras com nome e saldo inicial.
- 💚 **Receitas:** registro das entradas de dinheiro.
- ❤️ **Despesas:** registro e categorização dos gastos.
- 🔄 **Transferências:** movimentação de valores entre contas.
- 📊 **Saldo e extrato:** consulta do saldo atualizado e histórico de transações.
- ✏️ **Edição de movimentações:** alteração e exclusão de registros financeiros.
- 📅 **Relatórios mensais:** acompanhamento das receitas, despesas e resultados do mês.
- 🏷️ **Categorias:** organização das transações por finalidade.
- 🐷 **Envelopes financeiros:** criação de caixinhas virtuais para separar valores destinados a objetivos específicos.
- 💾 **Persistência:** armazenamento das informações em arquivos JSON.
- 📁 **Exportação CSV:** exportação de extratos financeiros como funcionalidade adicional.

## 🛠️ Tecnologias

| Tecnologia | Finalidade |
|---|---|
| Java 17 ou superior | Linguagem de programação |
| Java Swing | Desenvolvimento da interface gráfica |
| JSON | Armazenamento dos dados |
| Git e GitHub | Versionamento e colaboração |
| diagrams.net (draw.io) | Modelagem do diagrama UML |

A versão exata do Java e a ferramenta de build serão definidas durante o desenvolvimento.

## 🧩 Modelagem orientada a objetos

O Money Buu utiliza conceitos de Programação Orientada a Objetos para organizar as responsabilidades do sistema.

O modelo planejado contempla as seguintes classes:

| Classe | Responsabilidade |
|---|---|
| `Conta` | Gerenciar contas, transações e envelopes financeiros |
| `Transacao` | Classe abstrata que representa movimentações financeiras |
| `Receita` | Representar entradas de dinheiro |
| `Despesa` | Representar saídas de dinheiro |
| `Transferencia` | Representar movimentações entre contas |
| `Categoria` | Classificar as transações |
| `Envelope` | Organizar valores reservados para objetivos financeiros |
| `Relatorio` | Calcular e apresentar resultados financeiros |

### Conceitos de POO utilizados na modelagem

**Herança:** as classes `Receita`, `Despesa` e `Transferencia` especializam a classe abstrata `Transacao`.

**Polimorfismo:** os diferentes tipos de transação poderão apresentar comportamentos específicos utilizando operações definidas na estrutura comum.

**Encapsulamento:** os atributos financeiros serão protegidos, com acesso e alterações controlados por métodos.

**Agregação:** a classe `Conta` reúne transações financeiras.

**Composição:** os envelopes financeiros pertencem às contas e dependem delas para existir.

**Associação:** relacionamentos entre transações, categorias, contas e relatórios.

O diagrama UML completo está previsto para a pasta `docs` do repositório.

## 🎨 Identidade visual

O Money Buu possui uma identidade visual própria, inspirada em ilustrações artesanais, com a presença de um fantasminha como mascote.

A proposta visual combina:

- Azul-marinho escuro como cor predominante.
- Marrom-avermelhado para botões e elementos de destaque.
- Branco para textos e informações.
- Amarelo e verde em detalhes relacionados ao dinheiro.
- Elementos e tipografia inspirados em desenhos feitos à mão.

Os protótipos iniciais incluem telas de login, cadastro, página principal e formulários de receitas e despesas.

## 📂 Organização planejada

A estrutura de pacotes abaixo é uma proposta inicial e poderá ser atualizada durante a implementação.

```text
Money-Buu/
├── docs/
│   ├── uml/
│   └── prototipos/
├── src/
│   └── main/
│       └── java/
│           └── moneybuu/
│               ├── modelo/
│               ├── arquivo/
│               ├── gui/
│               └── app/
├── README.md
└── .gitignore
```

- `modelo`: classes e regras de negócio.
- `arquivo`: leitura e gravação dos dados JSON.
- `gui`: interface gráfica desenvolvida com Swing.
- `app`: inicialização e integração da aplicação.
- `docs`: diagramas, protótipos e documentação acadêmica.

## 📅 Cronograma

| Entrega | Descrição | Prazo |
|---|---|---|
| 1 | Modelagem UML, relacionamentos, armazenamento e protótipos | 06/11/2026 |
| 2 | Implementação das classes e regras de negócio | 13/11/2026 |
| 3 | Persistência dos dados e funcionalidades principais | 30/11/2026 |
| 4 | Aplicação completa, documentação e apresentação | 09/12/2026 |

**Etapa atual:** Entrega 1 — Modelagem.

## 👥 Equipe

Projeto desenvolvido por estudantes da UFRPE/UABJ:

- **Maria Luiza Rodrigues Guisande da Silva**
- **Manoel**
- **Breno**

**Disciplina:** Modelagem e Programação Orientada a Objetos

**Professor:** Michael Oliveira da Cruz

**Instituição:** Universidade Federal Rural de Pernambuco — Unidade Acadêmica de Belo Jardim.

## 📌 Situação do projeto

O Money Buu está em desenvolvimento acadêmico.

A primeira etapa é dedicada ao levantamento dos requisitos, criação do diagrama UML, definição dos relacionamentos e elaboração dos protótipos visuais.

As etapas seguintes compreenderão a implementação em Java, o armazenamento dos dados e a integração da interface gráfica.

---

**Money Buu 👻💰 — Cuidando do seu dinheiro de um jeito simples!**
