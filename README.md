# SCREI

Sistema de Cadastro e Rematrícula da Educação Infantil em Java 17+, Spring Boot 3, Thymeleaf, Spring Security e H2.

## Executar

O ambiente de testes usa **Azurite**, emulador local do Azure Blob Storage. Não é preciso conta ou assinatura Azure. Os arquivos e o banco local ficam em volumes persistentes.

Com Docker Desktop/Engine e Compose disponíveis, execute aplicação e armazenamento juntos:

```sh
docker compose --profile app up -d --build
```

Abra [http://localhost:8080](http://localhost:8080). Para acompanhar a inicialização:

```sh
docker compose logs -f app
```

Se preferir executar o Java pela IDE ou terminal, suba apenas o armazenamento:

```sh
docker compose up -d azurite
```

Em seguida, use JDK 17 ou superior, `JAVA_HOME` apontando para o JDK e o perfil `local`:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

No Linux/macOS:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O primeiro build precisa de internet para baixar imagens, Maven e dependências. O Bootstrap usa CDN; o wizard e os estilos locais continuam disponíveis sem ela.

O ambiente foi validado com Docker em 26/10/2026, incluindo upload real, leitura autenticada e persistência após reinício dos dois contêineres. Veja o [registro de validação](docs/VALIDACAO_DOCKER.md).

## Acesso e uso

- Responsáveis: cadastro público em `/responsavel/novo`, com cinco etapas: aluno, responsável, emergência, documentos do aluno e documentos do responsável.
- Quatro imagens obrigatórias: CPF do aluno, certidão de nascimento, CPF do responsável e comprovante de residência. Aceita JPEG/JPG e PNG de até **5 MiB por arquivo**, no máximo **16 megapixels** e **10.000 pixels por lado**. PDFs, HEIC e outros formatos não são aceitos nesta versão.
- Cada campo permite visualizar, remover ou substituir a imagem antes do envio. Em caso de erro no servidor, os campos de texto são mantidos; os arquivos precisam ser selecionados novamente, conforme indicado na tela.
- O servidor verifica e decodifica a imagem e regrava seus pixels, descartando metadados e conteúdo adicional. Arquivos inválidos são rejeitados antes de qualquer upload.
- As imagens são armazenadas em container privado `screi-documentos`, com chaves aleatórias sem CPF ou nome original. Somente a secretaria autenticada pode visualizá-las nos detalhes. A consulta pública não contém links dos documentos.
- Se o upload ou a gravação no banco falhar, o cadastro é desfeito e a aplicação tenta excluir os arquivos já enviados. Se o próprio armazenamento estiver indisponível durante a limpeza, haverá aviso no log para verificação manual de arquivos órfãos.
- O CPF do aluno é normalizado e único: a aplicação verifica a existência antes de salvar e o banco bloqueia duplicidade inclusive em envios simultâneos. O alerta mantém os dados preenchidos no formulário. O CPF do responsável pode se repetir para cadastrar irmãos ou outros alunos sob sua responsabilidade.
- Acompanhamento público em `/responsavel/status`, por protocolo ou CPF do responsável, com ou sem pontuação. A consulta apresenta apenas protocolo, situação, datas e justificativa de pendência.
- Secretaria: entre em `/login` com `secretaria@escola.com` e `admin123`.
- A fila em `/secretaria/triagem` permite abrir todos os dados, atualizar a situação e exportar somente os aprovados.
- Para registrar `COM_PENDENCIA`, a justificativa é obrigatória no formulário, no DTO, no serviço e na persistência.
- A exportação em `/secretaria/exportar-excel` gera XLSX com dados de aluno, responsáveis, endereços, contatos e datas. Os valores são gravados como texto, inclusive CPFs e telefones.
- O CEP deve ter oito dígitos. O navegador consulta `/responsavel/cep/{cep}`; o servidor usa `RestTemplate` para consumir o ViaCEP com limites de tempo. Se a consulta falhar, o endereço pode ser preenchido manualmente.
- As entidades suportam listas de responsáveis e contatos; o formulário cadastra um responsável e um contato por solicitação.
- `ROLE_RESPONSAVEL` está disponível no modelo e não dá acesso à secretaria. O fluxo público de responsáveis dispensa criação de conta.

## Configuração

Com o perfil `local`, o H2 é salvo em `.data/screi` e atualizado sem apagar os cadastros. No Compose, banco e imagens usam volumes `screi-data` e `azurite-data`. Eles permanecem após `docker compose down`; **`docker compose down -v` apaga os dois volumes e todos os dados de teste**. Ao executar Java no host, os metadados ficam na pasta `.data` do projeto, separada do volume do aplicativo em Docker; não alterne os dois modos esperando compartilhar o mesmo banco.

Sem o perfil `local`, continua valendo o H2 em memória original: os cadastros são perdidos ao encerrar, embora os blobs possam continuar no Azurite. Use o perfil local para testar reinícios.

Conexão padrão no host: `UseDevelopmentStorage=true`, com Blob Storage em `http://127.0.0.1:10000/devstoreaccount1`. O Compose configura automaticamente `http://azurite:10000/devstoreaccount1` para o aplicativo no contêiner. A chave que aparece no Compose é a chave pública de desenvolvimento do emulador, não uma credencial de uma conta Azure real. As portas são publicadas somente no endereço local da máquina.

Variáveis opcionais: `AZURE_STORAGE_CONNECTION_STRING` e `SCREI_BLOB_CONTAINER`. Para mudar as credenciais da secretaria antes de iniciar um banco novo:

```powershell
$env:SCREI_ADMIN_EMAIL='secretaria@minhaescola.com'
$env:SCREI_ADMIN_PASSWORD='uma-senha-definida-pela-escola'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

As senhas são armazenadas com BCrypt. Os formulários POST têm proteção CSRF e a área da secretaria exige `ROLE_SECRETARIA`. O console H2 fica desabilitado.

Esta configuração atende ao **ambiente local de testes**. Não inclui antivírus, confirmação de identidade, cotas por usuário ou garantias de produção. Use imagens fictícias. Azurite não gera cobrança de armazenamento Azure; utiliza disco e recursos da máquina. Referência: [documentação oficial do emulador](https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite).

Se já houver um banco H2 persistente anterior à validação de CPF, será necessário normalizar e resolver duplicados antes de aplicar a restrição de unicidade. O perfil `local` foi introduzido nesta versão, por isso não existe migração automática de um banco persistente legado.

## Testes e empacotamento

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/screi-1.0.0.jar --spring.profiles.active=local
```

A suíte integra banco H2, MVC, Security, Thymeleaf e leitura do XLSX, verificando cadastro, consulta, autenticação, autorização, CSRF, workflow e rejeição de justificativas ausentes ou vazias.

Também verifica CPFs de aluno repetidos com e sem pontuação, responsáveis compartilhados por vários alunos, alerta no formulário, restrição no banco, submissões concorrentes, imagens falsas/vazias/excessivas, metadados dos documentos, acesso restrito e limpeza de uploads após falhas. A suíte padrão simula o armazenamento e dispensa Docker.

Para executar também o teste de integração com o Azurite real já iniciado:

```powershell
$env:SCREI_TEST_AZURITE='true'
.\mvnw.cmd test
```

No Linux/macOS: `SCREI_TEST_AZURITE=true ./mvnw test`. Esse teste cria seu próprio container privado com nome aleatório, confirma upload/leitura/exclusão e remove o container no final.

## Diagnóstico

- **Armazenamento indisponível no formulário:** confira `docker compose ps` e `docker compose logs azurite`, porta 10000 e a variável de conexão. O aplicativo pode iniciar antes do emulador, mas precisa dele para concluir o cadastro.
- **Arquivo recusado:** selecione JPEG ou PNG real, dentro dos limites; mudar somente a extensão de outro formato não funciona.
- **Imagem indisponível após reinício:** confira se o banco e o volume usados são os mesmos do cadastro. No modo em memória o vínculo do documento não persiste.
- **Uploads antigos:** solicitações criadas antes desta versão mostram “Esta solicitação não possui imagens anexadas.” Novos cadastros exigem as quatro imagens; não há edição posterior de anexos nesta entrega.

O arquivo `CODIGO_COMPLETO.md` apresenta o conteúdo integral dos arquivos da aplicação e dos testes, em blocos Markdown identificados por caminho.

O estudo [Viabilidade da coleta de documentos](docs/VIABILIDADE_DOCUMENTOS.md) registra a avaliação anterior de produção. A implementação atual de testes usa Azurite e somente imagens; as funcionalidades de produção mencionadas no estudo continuam fora deste escopo.
