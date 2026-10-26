# Validação do ambiente Docker local

Verificação realizada em 26/10/2026, com Docker Desktop e mecanismo Linux/WSL 2.

## Resultado

- `docker compose --profile app config --quiet`: configuração válida.
- `docker compose --profile app up -d --build`: build concluído com Java 17 e contêineres iniciados.
- Azurite com healthcheck saudável, acessível pelo aplicativo em `http://azurite:10000/devstoreaccount1`.
- Aplicação disponível em `http://localhost:8080`, com perfil `local` e banco H2 em arquivo.
- Envio HTTP real de cadastro fictício com token CSRF e quatro imagens PNG: concluído.
- Login da secretaria, tela de detalhes e leitura das quatro imagens: concluídos.
- Respostas das imagens com `Content-Type: image/png` e `Cache-Control: no-store`.
- Reinício de `app` e `azurite`: protocolo, vínculo dos documentos e arquivos preservados nos volumes.
- Acesso às imagens sem login: redirecionado para autenticação.
- Suíte de regressão executada no host: 25 testes passaram; os dois testes condicionais de SDK/Azurite foram ignorados nessa execução. A integração em Docker foi verificada separadamente pelo fluxo HTTP real descrito acima.

O cadastro fictício usado na validação está identificado como `Aluno ficticio HTTP`, com imagens de teste de um pixel. Não foram utilizados documentos pessoais reais. O ambiente foi mantido em execução para teste do usuário.

## Falha de infraestrutura encontrada e recuperação

Na primeira construção, o disco C: ficou sem espaço e o Docker falhou ao gravar um snapshot da imagem. Os logs registraram erros de I/O no ext4, journal interrompido e posteriormente SIGBUS no containerd.

Foram removidos somente os caches temporários do emulador npm e do Maven em `.cache` deste projeto. Depois, os processos travados do Docker e apenas sua distribuição `docker-desktop` foram reiniciados. A distribuição Ubuntu não foi encerrada nem alterada. Não houve reset de fábrica, exclusão de disco virtual ou remoção de volumes.

Após a recuperação, o build e os testes funcionaram. O espaço disponível continua limitado: libere espaço adicional antes de novos downloads ou construções grandes. Não apague os volumes se quiser preservar os cadastros de teste. `docker compose down` mantém os dados; `docker compose down -v` os apaga.

## Uso

```sh
docker compose --profile app up -d --build
docker compose --profile app ps
docker compose logs --tail 50 app azurite
```

Abra [http://localhost:8080](http://localhost:8080). Secretaria: `secretaria@escola.com` / `admin123`, caso as credenciais iniciais não tenham sido alteradas.

Os detalhes de configuração, limites e execução pela IDE estão no [README](../README.md).
