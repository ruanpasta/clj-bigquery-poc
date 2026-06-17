# BigQuery POC — Clojure

Prova de conceito de consulta ao Google BigQuery via interop Java em Clojure.

## Dependências

- Clojure CLI (`clojure`)
- JDK 11+

## Configuração de credenciais

1. No [Google Cloud Console](https://console.cloud.google.com/), crie uma conta de serviço com o papel **Usuário de jobs do BigQuery**
2. Gere uma chave JSON e salve-a na raiz deste projeto (ex: `service-account.json`)
3. O arquivo `.gitignore` já exclui `*.json` — a chave não será commitada

## Uso

```bash
cd bigquery
clojure -M --repl
```

No REPL:

```clojure
(require 'poc.bigquery)

;; Cria o cliente lendo o project_id do próprio JSON
(def client (poc.bigquery/make-client "service-account.json"))

;; Executa uma query e retorna um vetor de mapas
(poc.bigquery/query client
  "SELECT word, SUM(word_count) AS total
   FROM `bigquery-public-data.samples.shakespeare`
   GROUP BY word ORDER BY total DESC LIMIT 10")
;; => [{:word "the", :total "25568"} ...]
```

## API

### `(make-client)`
Cria cliente usando `GOOGLE_APPLICATION_CREDENTIALS` do ambiente.

### `(make-client path)`
Cria cliente lendo o service account JSON diretamente do caminho informado.

### `(query client sql)`
Executa um SQL padrão (não legacy) e retorna `[{:campo "valor"} ...]`.

## Exemplos incluídos

- Top 10 palavras mais frequentes em Shakespeare
- Total de palavras por obra
- Nomes mais populares nos EUA no ano 2000 (`usa_names`)
