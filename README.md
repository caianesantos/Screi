# SCREI

Sistema de Cadastro e Rematrícula da Educação Infantil em Java 17+, Spring Boot 3, Thymeleaf, Spring Security e H2.

## Executar

Requisitos: JDK 17 ou superior, `JAVA_HOME` apontando para o JDK e conexão à internet na primeira execução para baixar o Maven e as dependências.

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```sh
./mvnw spring-boot:run
```

Abra [http://localhost:8080](http://localhost:8080). O Bootstrap usa CDN; o wizard e os estilos locais continuam disponíveis sem ela.

## Acesso e uso

- Responsáveis: cadastro público em `/responsavel/novo`, com três etapas e validação de todos os campos no servidor.
- Acompanhamento público em `/responsavel/status`, por protocolo ou CPF do responsável, com ou sem pontuação. A consulta apresenta apenas protocolo, situação, datas e justificativa de pendência.
- Secretaria: entre em `/login` com `secretaria@escola.com` e `admin123`.
- A fila em `/secretaria/triagem` permite abrir todos os dados, atualizar a situação e exportar somente os aprovados.
- Para registrar `COM_PENDENCIA`, a justificativa é obrigatória no formulário, no DTO, no serviço e na persistência.
- A exportação em `/secretaria/exportar-excel` gera XLSX com dados de aluno, responsáveis, endereços, contatos e datas. Os valores são gravados como texto, inclusive CPFs e telefones.
- O CEP deve ter oito dígitos. O navegador consulta `/responsavel/cep/{cep}`; o servidor usa `RestTemplate` para consumir o ViaCEP com limites de tempo. Se a consulta falhar, o endereço pode ser preenchido manualmente.
- As entidades suportam listas de responsáveis e contatos; o formulário cadastra um responsável e um contato por solicitação.
- `ROLE_RESPONSAVEL` está disponível no modelo e não dá acesso à secretaria. O fluxo público de responsáveis dispensa criação de conta.

## Configuração

O banco H2 é **em memória**: os cadastros são perdidos ao encerrar a aplicação. O usuário inicial é recriado em cada banco novo. Para mudar as credenciais antes de iniciar:

```powershell
$env:SCREI_ADMIN_EMAIL='secretaria@minhaescola.com'
$env:SCREI_ADMIN_PASSWORD='uma-senha-definida-pela-escola'
.\mvnw.cmd spring-boot:run
```

As senhas são armazenadas com BCrypt. Os formulários POST têm proteção CSRF e a área da secretaria exige `ROLE_SECRETARIA`. O console H2 fica desabilitado.

Antes de usar com dados reais, configure persistência durável, HTTPS e credenciais próprias. A consulta pública por CPF foi mantida conforme o requisito; revise esse acesso conforme a política de privacidade da escola.

## Testes e empacotamento

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/screi-1.0.0.jar
```

A suíte integra banco H2, MVC, Security, Thymeleaf e leitura do XLSX, verificando cadastro, consulta, autenticação, autorização, CSRF, workflow e rejeição de justificativas ausentes ou vazias.

O arquivo `CODIGO_COMPLETO.md` apresenta o conteúdo integral dos arquivos da aplicação e dos testes, em blocos Markdown identificados por caminho.
