# Validação de aluno e viabilidade da coleta de documentos

Data da análise: 22/10/2026.

**Atualização:** foi implementado posteriormente o fluxo local de testes com cinco etapas, quatro imagens JPEG/PNG obrigatórias, Azure Blob SDK e Azurite no Docker Compose. Consulte o [README](../README.md) para execução e limites efetivos. As seções abaixo registram a proposta original de produção, incluindo PDFs e medidas que não fazem parte da implementação local atual; a estimativa financeira não representa cobrança ou contratação.

## Entrega atual: unicidade pelo CPF do aluno

A unicidade é exclusivamente do CPF do aluno. Pontuação é removida antes da consulta e da gravação: `529.982.247-25` e `52998224725` representam o mesmo aluno. O serviço verifica a existência antes de salvar e uma restrição única no banco impede que duas requisições simultâneas contornem essa verificação.

Um responsável pode aparecer em várias solicitações, com os mesmos CPF, nome, telefone e endereço. Não há restrição de unicidade sobre CPF de responsável. Nomes e datas de nascimento de alunos também não são critérios de duplicidade.

Ao detectar repetição, o formulário preserva os dados e mostra no CPF do aluno: **“Já existe um aluno cadastrado com este CPF. Consulte a solicitação existente ou procure a secretaria.”** O wizard abre a etapa com o erro. Não é criada outra solicitação nem um novo protocolo.

A regra vale independentemente da situação da solicitação existente. Uma futura rematrícula do mesmo aluno deverá reutilizar seu cadastro e ter uma regra própria de período letivo; não deve criar outro aluno com o mesmo CPF. Esse fluxo por período não faz parte da mudança atual.

O banco atual é H2 em memória, recriado a cada execução: a proteção vale para os dados existentes na instância. Para conservar cadastros e a unicidade entre reinícios, será necessária persistência durável. Em uma base persistente já existente, a implantação deverá primeiro normalizar CPFs, identificar duplicados e resolvê-los antes de criar a restrição; não se deve apagar registros automaticamente.

## Proposta de upload: viável, ainda não implementada

Spring MVC/Boot suporta requisições `multipart/form-data` e arquivos `MultipartFile`, mantendo Thymeleaf e o wizard atual. Não é necessário trocar a stack. Referência: [guia oficial do Spring](https://spring.io/guides/gs/uploading-files/).

Fluxo proposto:

1. Dados do aluno.
2. Dados do responsável.
3. Contato de emergência.
4. Documentos do aluno: CPF e certidão de nascimento.
5. Documentos do responsável: CPF e comprovante de residência; revisão e envio final.

Cada campo terá nome do documento, seleção de arquivo, indicação de tamanho/formato, visualização local de imagem ou nome do PDF e opção de substituir/remover antes de enviar. A seleção poderá usar a câmera quando o navegador e o dispositivo permitirem. Os quatro documentos serão obrigatórios para novas solicitações, como premissa inicial a validar com a escola. Cadastros anteriores precisarão de uma regra de complementação, sem bloqueio retroativo automático.

Se um documento tiver várias páginas, a proposta inicial aceita um PDF com todas as páginas por campo. Upload de várias fotos por documento e conversão de formatos como HEIC são escopos adicionais. Receber um arquivo não comprova sua autenticidade nem confere automaticamente o CPF impresso: a conferência continuará sendo da secretaria, sem OCR nesta estimativa.

## Limites e validações propostos

| Item | Proposta inicial |
| --- | --- |
| Formatos | PDF, JPEG/JPG e PNG |
| Quantidade | Um arquivo por tipo: quatro no total |
| Limite por arquivo | 5 MiB |
| Limite da requisição | 21 MiB, incluindo margem para os campos e cabeçalhos |
| Validação no servidor | Arquivo obrigatório e não vazio; tamanho, extensão, tipo e assinatura do conteúdo coerentes |
| Conteúdo | Verificação antimalware; quarentena até liberação; rejeição de conteúdo inválido ou que não possa ser analisado |
| Arquivos recusados | Executáveis, ZIP, SVG e formatos fora da lista permitida |
| Nomes no armazenamento | Identificadores aleatórios; nunca usar nome original ou CPF como caminho |

Configuração inicial proposta, a incluir somente com a implementação do upload:

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=21MB
```

Os mesmos limites devem ser compatibilizados com o servidor/proxy de hospedagem. O atributo HTML `accept` melhora a interface, mas não substitui verificação no servidor. Extensão e `Content-Type` enviados pelo navegador não são suficientes. Arquivos fora dos limites devem gerar mensagem compreensível, sem perda desnecessária dos demais dados. Após uma recarga, o navegador pode exigir nova seleção dos arquivos; preservação entre tentativas exige o armazenamento temporário descrito abaixo.

As medidas propostas seguem a [orientação de upload da OWASP](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html), incluindo limites, autorização, verificação de conteúdo e armazenamento separado da área pública da aplicação.

## Armazenamento e controle de acesso

Para produção, a proposta é um **bucket privado de armazenamento de objetos**, compatível com S3, com criptografia em repouso, comunicação HTTPS e credenciais restritas mantidas no servidor. Não haverá URLs públicas para CPF, certidão ou comprovante. Uma alternativa para instalação pequena é disco persistente privado, fora das pastas estáticas e do repositório Git, com permissões e backup; não usar disco efêmero do contêiner.

O banco deverá ser durável, por exemplo PostgreSQL, guardando apenas metadados dos documentos. Uma entidade `DocumentoMatricula` poderá conter: solicitação, tipo de documento, responsável quando aplicável, chave privada do objeto, nome original para exibição segura, tipo detectado, tamanho, hash, data de envio e situação de verificação. O aluno será identificado pela solicitação. Os arquivos não devem ficar em colunas do H2 em memória.

Os uploads poderão ser enviados com `FormData` para um rascunho vinculado à sessão, com limite de tempo. A finalização valida os quatro tipos, a análise dos arquivos e novamente o CPF do aluno. Apenas depois confirma a solicitação. Como banco e armazenamento não compartilham a mesma transação, será necessário tratamento de falhas e limpeza de arquivos temporários/órfãos, inclusive quando a duplicidade for detectada no envio final.

A leitura será feita por rota autenticada da secretaria, verificando o perfil e a associação do documento à solicitação. Downloads devem usar nomes seguros, respostas sem cache e proteção contra interpretação de conteúdo. Se houver links assinados para download, terão validade curta e só serão emitidos após autorização. A consulta pública por CPF ou protocolo **não dará acesso aos documentos**.

O cadastro hoje é público. Para receber documentos em produção, a proposta inclui sessão de rascunho vinculada a contato verificado, por exemplo link de confirmação por e-mail, além de limite de frequência/volume e proteção CSRF. CPF ou protocolo não podem servir como prova de identidade para editar ou baixar documentos. O envio de e-mail pode exigir um serviço adicional, dependendo da infraestrutura disponível.

Será necessário definir com a escola quem consulta os arquivos, prazo de retenção, rotina de descarte, tratamento de substituições, backups e registro de acessos. Não serão enviados documentos a serviços públicos de análise de arquivos. O antimalware deverá ser privado/local ou um serviço contratado para esse tratamento.

## Esforço e prazo estimados

Os números abaixo são estimativas de engenharia para o escopo proposto, não uma contratação ou garantia de data. Dependem da hospedagem escolhida e da disponibilidade de contas e credenciais.

| Atividade | Esforço estimado |
| --- | --- |
| Novas etapas, interface, prévia e mensagens | 6–10 horas |
| Recepção, validação, metadados e tratamento de falhas | 8–14 horas |
| Armazenamento privado, persistência durável e configuração de ambientes | 8–14 horas |
| Confirmação de contato, autorização, limites e integração antimalware | 8–14 horas |
| Testes integrados, revisão e documentação de operação | 6–12 horas |
| **Total para a coleta de documentos** | **36–64 horas de trabalho** |

Isso corresponde aproximadamente a 5–8 dias úteis de trabalho de uma pessoa dedicada. Espera por contratação de infraestrutura, aprovação de políticas, revisão da escola e implantação pode aumentar o prazo de calendário. O esforço da validação por CPF já entregue é separado dessa estimativa futura.

A estimativa não inclui OCR, reconhecimento de fraude, assinatura eletrônica, aplicativo móvel, digitalização de acervo, atendimento operacional ou importação/saneamento de uma base legada. Multi-responsáveis no formulário e múltiplos anexos por tipo exigem revisão da estimativa.

## Custos e impacto financeiro

Não foi contratado nem ativado nenhum serviço pago nesta entrega. Não há tabela de honorários, contrato de desenvolvimento ou plano de hospedagem informado; portanto não é possível afirmar que o trabalho adicional está incluído ou dar um preço fechado.

- **Desenvolvimento:** se houver cobrança por hora, o orçamento preliminar do upload será `36 a 64 horas × valor/hora acordado`. A inclusão no escopo atual depende do contrato com o fornecedor; esta estimativa não cria cobrança.
- **Armazenamento:** depende do total de documentos retidos, tamanho médio, versões e cópias. Exemplo de dimensionamento: 1.000 solicitações × 4 arquivos × 2 MB ≈ 8 GB de documentos, antes de backups. O custo cresce com o acervo acumulado, não apenas com os novos cadastros do mês.
- **Operação:** banco persistente, hospedagem, cópias de segurança, antimalware, logs e confirmação por e-mail podem gerar custo adicional ou consumir recursos já disponíveis. Um antivírus sem licença comercial paga ainda consome CPU/RAM e exige manutenção.
- **Plano de hospedagem:** nenhuma mudança de plano é obrigatória apenas por usar `MultipartFile`. Pode ser necessária se o plano atual não oferecer disco durável, memória, tráfego ou limites de upload suficientes. O plano atual não foi informado.

Como referência de preço público consultada em 22/10/2026, o **Cloudflare R2 Standard** oferece 10 GB-mês gratuitos e cobra **US$ 0,015 por GB-mês adicional**, com franquias específicas para operações de gravação e leitura. O tráfego direto de saída do R2 não é cobrado. Fonte: [preços oficiais do R2](https://developers.cloudflare.com/r2/pricing/).

| Acervo constante durante o mês | Exemplo de custo apenas do armazenamento R2 Standard |
| --- | --- |
| 8 GB | US$ 0,00, se a franquia da conta estiver disponível |
| 20 GB | Aproximadamente US$ 0,15/mês após os 10 GB gratuitos |
| 100 GB | Aproximadamente US$ 1,35/mês após os 10 GB gratuitos |

Esses valores são exemplos de armazenamento, não o custo total de manter o SCREI. Pressupõem a franquia disponível, volume estável durante o mês e não incluem operações excedentes, impostos, câmbio, backups externos, servidor, banco ou serviços auxiliares. O provedor arredonda unidades faturáveis. R2 é uma referência de dimensionamento, não uma seleção ou contratação; região, condições de uso e requisitos da escola precisam ser avaliados antes da escolha.

Para fechar o orçamento de implantação faltam: número esperado de alunos, tamanho/quantidade de arquivos, prazo de guarda, provedor e plano atuais, disponibilidade de banco/disco persistentes, política de backup, confirmação de contato e valor de desenvolvimento acordado.
