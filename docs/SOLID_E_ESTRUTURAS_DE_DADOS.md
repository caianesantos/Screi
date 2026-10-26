# SOLID e estruturas de dados do SCREI

Revisão e ajustes realizados em 26/10/2026.

## O que foi ajustado

1. `DocumentoService` passou a receber `ValidadorDocumento`, em vez de depender diretamente de `ImagemDocumentoValidator`. O resultado é o DTO `DocumentoValidado`, independente da classe concreta do validador.
2. O tratamento da restrição única do Hibernate saiu de `SolicitacaoService` e foi para `JpaRegistroSolicitacao`, atrás da interface `RegistroSolicitacao`.
3. Os serviços passaram a lançar `RecursoNaoEncontradoException`, sem escolher códigos HTTP. A conversão para 404 fica em `RecursoExceptionHandler`, na camada web.
4. A construção/configuração do cliente Azure saiu de `AzureBlobDocumentoStorage` e foi para `AzureBlobConfig`. O adaptador concentra as operações de armazenamento.
5. O contrato de `DocumentoStorage` foi documentado e recebeu verificações comportamentais compartilhadas por uma implementação de teste em memória e pelo adaptador Azure/Azurite.

O fluxo, os limites dos arquivos, as permissões e a regra de unicidade do CPF do aluno foram preservados.

## Princípios SOLID: onde e por quê

| Princípio | Evidência concreta | Motivo |
| --- | --- | --- |
| **S — Responsabilidade única (SRP)** | [ImagemDocumentoValidator](../src/main/java/com/escola/screi/service/ImagemDocumentoValidator.java), [AzureBlobDocumentoStorage](../src/main/java/com/escola/screi/service/AzureBlobDocumentoStorage.java), [AzureBlobConfig](../src/main/java/com/escola/screi/config/AzureBlobConfig.java), [JpaRegistroSolicitacao](../src/main/java/com/escola/screi/repository/JpaRegistroSolicitacao.java) e [RecursoExceptionHandler](../src/main/java/com/escola/screi/controller/RecursoExceptionHandler.java). | Regras de imagem, armazenamento, configuração de infraestrutura, tradução de falhas de persistência e representação HTTP têm responsáveis diferentes. Mudanças nesses detalhes não exigem concentrar tudo no serviço de matrícula. |
| **O — Aberto para extensão, fechado para modificação (OCP)** | [DocumentoStorage](../src/main/java/com/escola/screi/service/DocumentoStorage.java), [ValidadorDocumento](../src/main/java/com/escola/screi/service/ValidadorDocumento.java) e [RegistroSolicitacao](../src/main/java/com/escola/screi/service/RegistroSolicitacao.java). | Outro provedor de armazenamento, outro algoritmo de validação que preserve a política atual ou outro adaptador de registro podem implementar os contratos e ser selecionados pela configuração, sem reescrever a orquestração do cadastro. |
| **L — Substituição de Liskov (LSP)** | [ContratoDocumentoStorage](../src/test/java/com/escola/screi/ContratoDocumentoStorage.java), [DocumentoStorageContratoTests](../src/test/java/com/escola/screi/DocumentoStorageContratoTests.java) e [AzuriteIntegrationTests](../src/test/java/com/escola/screi/AzuriteIntegrationTests.java). | As mesmas verificações são executadas em memória e no Azure/Azurite: preservar o conteúdo enviado, não permitir que alterações nos arrays do cliente modifiquem o objeto guardado e aceitar exclusão repetida. A troca de implementação mantém o comportamento esperado pelo consumidor. |
| **I — Segregação de interfaces (ISP)** | `ValidadorDocumento` tem somente `validar`; `RegistroSolicitacao` tem somente `salvar`; `DocumentoStorage` tem somente `enviar`, `ler` e `excluir`. | Validação não exige implementar operações de banco ou nuvem. O registro da solicitação não exige operações de segurança ou imagem. O armazenamento não expõe criação de contas Azure, configuração de redes ou exportação de planilhas. |
| **D — Inversão de dependência (DIP)** | Construtores de [DocumentoService](../src/main/java/com/escola/screi/service/DocumentoService.java) e [SolicitacaoService](../src/main/java/com/escola/screi/service/SolicitacaoService.java). | A orquestração recebe contratos por injeção: `DocumentoStorage`, `ValidadorDocumento` e `RegistroSolicitacao`. As classes concretas são conectadas pelo Spring. Conhecimento específico do SDK Azure e do Hibernate fica nos adaptadores/configuração. |

Injeção por construtor, isoladamente, não prova DIP: o ponto relevante é a dependência de abstrações nas fronteiras em que a implementação pode variar. Também não é necessário criar uma interface para toda classe.

O teste de contrato oferece evidência de LSP para as operações exercitadas; não constitui prova formal de qualquer implementação futura. Uma nova implementação deve respeitar o contrato e passar pelas mesmas verificações.

## Limites da aplicação dos princípios

- Os cinco princípios estão aplicados nos pontos descritos. SOLID é uma orientação de projeto contextual, não um selo que torne toda classe automaticamente extensível ou independente de frameworks.
- O formulário ainda contém entidades JPA e `MultipartFile`; isso mantém acoplamento entre a entrada web e o modelo. Uma evolução possível é ter DTOs de entrada próprios e fazer a conversão na fronteira. Essa separação adicional não foi necessária para as mudanças atuais.
- Os repositórios de consulta continuam sendo interfaces Spring Data JPA. Não foi acrescentada uma abstração idêntica sobre cada consulta apenas para aumentar o número de interfaces.
- A lista de documentos obrigatórios é um catálogo fechado em `TipoDocumento`, no DTO e no HTML. Incluir outro tipo de documento exige ajustar esse catálogo e a interface. O ponto de extensão do validador não significa que PDF, OCR ou qualquer novo formato passem a funcionar sem mudanças de requisitos e apresentação.
- Para outro armazenamento, além de implementar a interface, é necessário fornecer credenciais/configuração e selecionar o bean adequado. A aplicação atual continua usando Azure/Azurite.

## Estruturas de dados da aplicação

| Estrutura/tipo | Onde aparece | Uso e justificativa |
| --- | --- | --- |
| **`List<T>` e `ArrayList<T>`** | [SolicitacaoMatricula](../src/main/java/com/escola/screi/model/SolicitacaoMatricula.java): responsáveis, contatos e documentos. | Representam relações com vários elementos, com tamanho variável e iteração simples. As listas são inicialmente `ArrayList`; o Hibernate pode substituir a implementação ao carregar as relações. Sem `@OrderColumn`/`@OrderBy`, não se deve assumir que a ordem das relações será preservada pelo banco. |
| **`List<SolicitacaoMatricula>`** | [SolicitacaoMatriculaRepository](../src/main/java/com/escola/screi/repository/SolicitacaoMatriculaRepository.java) e `SolicitacaoService`. | Reúnem resultados de consultas para triagem, acompanhamento e exportação. As consultas `OrderByDataSubmissaoDesc` definem explicitamente a ordem das solicitações. |
| **`List<DocumentoValidado>`** | `DocumentoService.validar`. | Agrupa os quatro resultados de validação antes de enviar qualquer arquivo. É produzida por `Stream.toList()`, que retorna uma lista não modificável; seus elementos e os arrays contidos não se tornam imutáveis por isso. |
| **`List<String>` / `ArrayList<String>`** | `DocumentoService.anexar`, variável `chaves`. | Guarda chaves de blobs já tentados para limpeza em caso de rollback. É uma sequência de controle local, não uma fila assíncrona de mensagens. |
| **`byte[]`** | `DocumentoValidado`, `DocumentoStorage`, processamento de imagens, respostas HTTP e retorno do XLSX. | Vetor de bytes para conteúdo binário. Integra diretamente com ImageIO, SDK Azure e resposta HTTP. Os uploads têm limite para controlar uso de memória. Um array continua mutável, mesmo quando está dentro de um `record`. |
| **`String[]`** | [ExcelExportService](../src/main/java/com/escola/screi/service/ExcelExportService.java): `headers` e `values`. | Vetores posicionais para alinhar cada cabeçalho e valor à coluna correspondente da planilha; a quantidade de colunas é conhecida. |
| **`Map<String, String>` via `Map.of`** | [ResponsavelController](../src/main/java/com/escola/screi/controller/ResponsavelController.java), respostas de erro de CEP. | Associação chave/valor que o Spring transforma em JSON, por exemplo `mensagem → texto`. `Map.of` cria um mapa não modificável; o código não escolhe uma implementação `HashMap` para essas respostas. |
| **Arrays e objetos JavaScript** | [wizard.js](../src/main/resources/static/js/wizard.js): `steps`, resultados convertidos de `querySelectorAll` e mapeamento de campos do CEP. | Os arrays permitem percorrer e localizar etapas. O objeto literal associa o nome do campo da tela à propriedade retornada pelo ViaCEP. `Object.entries` fornece pares para preencher os campos. |
| **Imagem matricial em `BufferedImage`** | `ImagemDocumentoValidator`. | Representa pixels em memória para validar dimensões, decodificar e regravar a imagem. Não há matriz `int[][]` implementada manualmente. |

### Tipos estruturados complementares

Estes também organizam os dados, mas não são coleções clássicas como listas e mapas:

- **`Optional<T>`:** em `UsuarioRepository`, `DocumentoMatriculaRepository` e `findById` herdado. Representa presença ou ausência de um resultado; o código trata a ausência antes de utilizar o objeto.
- **Enums:** `StatusMatricula`, `TipoPapel` e `TipoDocumento`. Restringem categorias a valores conhecidos e evitam textos arbitrários para status, papel e tipo de documento.
- **Records:** `DocumentoValidado`, `DocumentoService.ImagemDocumento` e `ViaCepService.Endereco`. Agrupam dados de transporte com acesso nominal aos campos e menos código repetitivo. São superficialmente imutáveis: os componentes que são arrays continuam mutáveis.
- **Entidades e DTOs:** organizam os atributos e as relações do domínio; as associações JPA não significam que o projeto implementa um algoritmo ou uma estrutura própria de grafos.

### Persistência e bibliotecas

- **Tabelas relacionais no H2:** armazenam cadastros e metadados. As restrições únicas de CPF do aluno e protocolo garantem unicidade no banco, inclusive contra concorrência. A prevenção de duplicidade não depende de uma busca em `ArrayList` ou de um `Set` em memória. O CPF de responsável pode se repetir.
- **Objetos no Azure Blob/Azurite:** arquivos são recuperados por uma chave textual; o banco mantém chave, tipo, tamanho e vínculo com a solicitação. Isso é armazenamento de objetos, não um `HashMap` Java mantido pelo aplicativo.
- **Planilha Apache POI:** `Workbook → Sheet → Row → Cell` organiza a saída XLSX em abas, linhas e colunas. É a estrutura da biblioteca para o documento, não um banco de dados do sistema.

### Somente nos testes

- `HashMap<String, Objeto>` no `StorageEmMemoria` de `DocumentoStorageContratoTests`: simula o armazenamento por chave. Não é o armazenamento efetivo do sistema.
- `CountDownLatch`, executores e `AtomicInteger` controlam os testes concorrentes e a simulação de falhas. São mecanismos de sincronização/controle, não a estrutura de dados da triagem.

## O que não está implementado como estrutura própria

A “fila de triagem” é uma funcionalidade de listagem, implementada com consulta e `List`, ordenada por submissão mais recente. **Não usa `Queue` nem garante processamento FIFO.**

Não há implementação própria de pilha, lista encadeada, árvore, grafo ou tabela hash no código de negócio. Estruturas internas da JVM, do Hibernate, do H2, do SDK ou do navegador não devem ser confundidas com estruturas escolhidas explicitamente pelo projeto.

Para os volumes atuais de teste, as listas e os vetores são escolhas simples e suficientes. Se o número de matrículas crescer, a listagem completa deverá evoluir para paginação; imagens e XLSX maiores também poderão exigir processamento por fluxo, em vez de manter todo o conteúdo em memória.
