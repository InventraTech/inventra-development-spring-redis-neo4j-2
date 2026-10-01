# INV2-25 Fila de cadastro por código de barras

A API recebe os dados de um produto revisados pelo usuário, enfileira o cadastro no Redis e retorna `202 Accepted`. Um consumer grava o produto no PostgreSQL e disponibiliza o resultado para consulta. O processo usa Redis List, `LPUSH` para entrada e `BRPOP` para saída, conforme a INV2-25.

## Uso pela aplicação

1. Ler o código de barras. O endpoint existente `GET /api/products/barcode-lookup?barcode=...` pode auxiliar no preenchimento com Open Food Facts.
2. Solicitar que o usuário revise nome, marca, categoria e unidade. Código de barras não informa lote, quantidade disponível ou validade.
3. Enviar os dados confirmados com um JWT válido:

```http
POST /api/products/barcode-registrations
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Arroz",
  "brand": "Marca revisada",
  "categoryId": 1,
  "unitId": 1,
  "barcode": "7891234567890",
  "photoUrl": null
}
```

`name`, `unitId` e `barcode` são obrigatórios. Nome tem até 150 caracteres, marca até 80 e URL até 255. IDs devem ser positivos; o código aceita de 8 a 14 dígitos. `categoryId`, `brand` e `photoUrl` são opcionais. Unidade e categoria precisam existir no PostgreSQL quando o consumer processar o cadastro.

A resposta inclui `eventId`, `status: QUEUED`, timestamps e um header `Location`:

```http
GET /api/products/barcode-registrations/<eventId>
Authorization: Bearer <token>
```

Apenas o usuário que enviou o pedido pode consultar seu resultado. Os estados são `QUEUED`, `PROCESSING`, `COMPLETED` e `FAILED`. Quando concluído, `productId` identifica o produto no endpoint existente `GET /api/products/{id}`. Em falha, `errorCode` é `REFERENCE_NOT_FOUND`, `BUSINESS_RULE`, `DATABASE_ERROR` ou `PROCESSING_ERROR`. Falhas de conexão Redis e falhas internas do protocolo da fila retornam `503` sem afirmar que o pedido foi aceito.

O cadastro cria o produto, seguindo as regras existentes de `ProductService`. Entrada de estoque e cadastro de lotes continuam sendo operações próprias. Um código de barras já cadastrado retorna o ID existente, sem modificar seus dados; isso também permite retomar um evento cujo commit no PostgreSQL ocorreu antes de uma queda do consumer.

## Estruturas Redis e recuperação

Prefixo padrão: `inventra:product-registration`.

| Sufixo da chave | Estrutura | Responsabilidade |
| --- | --- | --- |
| `ready` / `ready:<token>` | List | IDs aguardando consumer / disponíveis à concessão atual; `LPUSH`/`BRPOP` implementam FIFO |
| `job:<eventId>` | String JSON | Payload, proprietário, estado e resultado |
| `pending` | Sorted Set | Índice de pedidos ainda não confirmados, em ordem de entrada |
| `sequence` | String numérica | Sequência monotônica usada pelo índice |
| `consumer-lock` | String com TTL | Concessão exclusiva de consumo, renovada a cada 5 segundos |
| `errors` | List | Dead-letter com o JSON original e código de erro |

Scripts Lua enfileiram payload, índice e ID de forma atômica. O `BRPOP` aguarda até 2 segundos, abaixo do timeout Redis de 10 segundos. O consumer usa uma thread de processamento e uma thread de renovação da concessão de 30 segundos, sem bloquear a requisição HTTP. Instâncias que compartilham o prefixo disputam a mesma concessão.

Cada concessão consome uma lista própria. Um consumer que perdeu sua concessão não pode retirar itens da lista do sucessor. A lista temporária tem TTL renovado com a concessão e é removida ao encerrar esse consumer; o índice `pending` preserva os pedidos ainda não confirmados.

Pedidos pendentes e seu payload não têm TTL. O `BRPOP` remove o ID apenas da lista temporária da concessão; ele permanece no índice durável `pending`. Ao adquirir a concessão, o consumer reconstrói sua lista a partir desse índice, inclusive para itens retirados por `BRPOP` antes de registrar `PROCESSING` ou antes de uma confirmação terminal. Pedidos só saem de `pending` depois da confirmação atômica de sucesso ou envio à DLQ. Falha de confirmação no Redis mantém o pedido pendente para retomada, mesmo que o PostgreSQL já tenha confirmado a gravação.

A entrega é **pelo menos uma vez**: reinícios podem repetir o processamento. A verificação por código de barras e sua restrição única no PostgreSQL evitam cadastro duplicado. Não há transação distribuída entre os dois bancos. O resultado final tem TTL de 7 dias; a DLQ preserva o payload sem expiração automática para investigação e tratamento operacional. A retenção da DLQ deve ser acompanhada pelo responsável pelo serviço. Não há endpoint público de leitura ou reprocessamento da DLQ nesta tarefa.

Logs registram início e fim com `eventId`, estado, código de erro e timestamp; não incluem payload, credenciais nem mensagens internas de exceção.

## Configuração e operação

As credenciais Redis continuam vindo das variáveis `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD`, `REDIS_DATABASE` e `REDIS_SSL`. A Aiven é o serviço principal; manter SSL habilitado nela.

Variáveis opcionais adicionadas ao `.env.example`:

```dotenv
REDIS_PRODUCT_QUEUE_CONSUMER_ENABLED=true
REDIS_PRODUCT_QUEUE_KEY_PREFIX=inventra:product-registration
```

Desabilitar o consumer mantém o producer disponível, permitindo enfileirar enquanto o processamento estiver pausado. Ambientes independentes devem usar prefixos diferentes. A implementação atende Redis/Valkey standalone, como o ambiente atual; Redis Cluster não é coberto nesta entrega.

Reiniciar apenas a API não remove as chaves Redis. Sobreviver a reinícios ou perdas do próprio servidor Redis exige persistência configurada no serviço (Aiven ou AOF + volume no Docker local). Nenhum teste desta tarefa reinicia o serviço Aiven compartilhado.

## Testes e evidências

Usar Java 21 e fornecer as variáveis Redis ao processo Maven, inclusive localmente. O teste de conectividade existente também exige essas variáveis no ambiente, não somente no arquivo `.env`.

```bash
./mvnw -B -ntp test
```

No Windows, usar `mvnw.cmd` ou a instalação Maven já disponível. O CI reutilizável existente já fornece os secrets Redis; esta tarefa não exige novos secrets.

Se a execução Java no Windows apresentar `Unable to establish loopback connection` em `UnixDomainSockets`, foi validado o fallback TCP somente no comando de testes: `-DargLine=-Djdk.net.unixdomain.tmpdir=target/java-nio-tcp`, indicando um diretório inexistente. Não é necessário alterar a configuração da aplicação nem usar esse ajuste no CI Linux.

| Critério da INV2-25 | Evidência automatizada |
| --- | --- |
| Producer enfileira corretamente | JSON preservado e IDs na lista com Redis real |
| Consumer processa FIFO | Múltiplos eventos com resultados em ordem |
| Falhas vão para dead-letter | Primeiro evento falha, próximo conclui e payload permanece na DLQ |
| Logs de início e fim | Captura dos logs com timestamps e eventId |
| Reinício da aplicação preserva pedidos | Nova instância recupera evento retirado antes de `PROCESSING` e evento em processamento, ambos na ordem original |
| Sem cadastro duplicado em retomadas | Processamento real com H2 e repetição do mesmo código |
| Proteção do resultado e validação | Testes do serviço e dos endpoints |
| Confirmação Redis perdida | Pedido permanece pendente e não vira DLQ indevidamente |

Os testes Redis usam prefixos `inventra:test:product-queue:<UUID>` e removem apenas suas próprias chaves. O processamento relacional dos testes usa H2, sem alterar o PostgreSQL compartilhado. Os relatórios Maven ficam em `target/surefire-reports`.

O Redis da Aiven configurado no `.env` foi validado em 01/10/2026: conexão autenticada, fila FIFO, recuperação, DLQ e suíte completa executaram com sucesso. O `.env` real não é versionado nem alterado pelos testes.

Referências técnicas: [LPUSH](https://redis.io/docs/latest/commands/lpush/), [BRPOP](https://redis.io/docs/latest/commands/brpop/) e [scripts no Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/redis/scripting.html).

## DevOps

Branch: `feat/fila-processamento-redis`.

Commit sugerido: `feat(redis): implementa fila de cadastro por codigo de barras`.

Título da PR: `INV2-25: Implementar fila de processamento com Redis`.

Vincular à tarefa os testes, exemplos de requisição/resposta e evidências de FIFO/DLQ. Não versionar `.env` nem credenciais.
