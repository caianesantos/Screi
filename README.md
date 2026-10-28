# SCREI

Sistema de Cadastro e Rematrícula da Educação Infantil com Java 17+, Spring Boot, PostgreSQL e Azure Blob Storage local via Azurite.

## Executar com Docker

```sh
docker compose --profile app up -d --build
```

Abra [http://localhost:8080](http://localhost:8080). Secretaria: `secretaria@escola.com` / `admin123`.

O Compose usa a imagem oficial `postgres:17-alpine`, aguarda o banco e o Azurite ficarem saudáveis e inicia a aplicação. Os volumes `postgres-data` e `azurite-data` preservam cadastros e imagens. `docker compose down` mantém os volumes; `docker compose down -v` apaga os dados.

Para executar o Java pela IDE ou terminal:

```powershell
docker compose up -d --wait postgres azurite
.\mvnw.cmd spring-boot:run
```

## Conexão local com PostgreSQL

- Host: `localhost`; porta: `15432`.
- Banco e usuário: `screi`; senha de desenvolvimento: `screi_local`.
- Entre os contêineres, o host é `postgres` e a porta é `5432`.
- Variáveis opcionais do Compose: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e `POSTGRES_PORT`.
- Na execução pelo host: `SCREI_DATABASE_URL`, `SCREI_DATABASE_USER` e `SCREI_DATABASE_PASSWORD`.
- Credenciais iniciais da secretaria: `SCREI_ADMIN_EMAIL` e `SCREI_ADMIN_PASSWORD`.
- Armazenamento: `AZURE_STORAGE_CONNECTION_STRING` e `SCREI_BLOB_CONTAINER`.

O banco PostgreSQL começa vazio. Os dados antigos do H2 não são importados automaticamente; o volume antigo foi preservado. A aplicação não utiliza mais H2.

## Cadastro e documentos

O CPF do aluno é único, inclusive em envios simultâneos. O mesmo responsável pode cadastrar vários alunos. O formulário exige quatro imagens: CPF do aluno, certidão de nascimento, CPF do responsável e comprovante de residência.

Aceita JPEG/PNG de até 5 MiB, 16 megapixels e 10.000 pixels por lado. As imagens ficam em container privado e só podem ser consultadas pela secretaria autenticada. A secretaria também pode atualizar o status e exportar os aprovados em XLSX.

## Testes

Os testes usam outro PostgreSQL, com banco e porta separados. Esse banco é temporário e não contém dados da aplicação:

```powershell
docker compose --profile tests up -d --wait postgres-test azurite
$env:SCREI_TEST_AZURITE='true'
.\mvnw.cmd test
```

As variáveis `SCREI_TEST_DATABASE_URL`, `SCREI_TEST_DATABASE_USER` e `SCREI_TEST_DATABASE_PASSWORD` permitem alterar a conexão de teste. O padrão é `localhost:5433/screi_test`, usuário e senha `screi_test`.

Para gerar o executável: `.\mvnw.cmd package`. Os testes de cadastro requerem o PostgreSQL de testes em execução; os testes do Azurite são habilitados pela variável acima.
