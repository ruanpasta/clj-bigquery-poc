(ns poc.bigquery
  "POC: consulta ao BigQuery via interop Java.

  Credenciais: salve o service account JSON na raiz do projeto
  (ex: service-account.json) e passe o caminho para `make-client`.
  O arquivo .gitignore já exclui *.json — a chave não será commitada.

  Exemplo de setup:
    1. Copie o JSON para este diretório:
         cp ~/Downloads/bigquery-key.json service-account.json
    2. Inicie o REPL:
         clojure -M --repl
    3. No REPL:
         (def client (make-client \"service-account.json\"))"
  (:import
   [com.google.cloud.bigquery
    BigQueryOptions
    QueryJobConfiguration]
   [com.google.auth.oauth2 ServiceAccountCredentials]
   [java.io FileInputStream]
   [com.fasterxml.jackson.databind ObjectMapper]))

;; ---------------------------------------------------------------------------
;; Conexão
;; ---------------------------------------------------------------------------

(defn make-client
  "Retorna um cliente BigQuery autenticado.

  Sem argumentos: usa Application Default Credentials (lê
  GOOGLE_APPLICATION_CREDENTIALS automaticamente).

  Com `credentials-path`: lê o service account JSON diretamente."
  ([]
   (-> (BigQueryOptions/getDefaultInstance)
       (.getService)))
  ([credentials-path]
   (let [creds  (with-open [stream (FileInputStream. credentials-path)]
                  (ServiceAccountCredentials/fromStream stream))
         mapper (ObjectMapper.)
         json   (with-open [stream (FileInputStream. credentials-path)]
                  (.readValue mapper stream java.util.Map))
         proj   (get json "project_id")]
     (-> (BigQueryOptions/newBuilder)
         (.setCredentials creds)
         (.setProjectId proj)
         (.build)
         (.getService)))))

;; ---------------------------------------------------------------------------
;; Execução de queries
;; ---------------------------------------------------------------------------

(defn- row->map
  "Converte uma Row do BigQuery em mapa Clojure {:campo valor}."
  [schema row]
  (into {}
        (map (fn [^com.google.cloud.bigquery.Field field]
               (let [name (.getName field)
                     cell (.get row name)]
                 [(keyword name)
                  (when cell (str (.getValue cell)))]))
             (.getFields schema))))

(defn query
  "Executa um SQL no BigQuery e retorna uma sequência de mapas.

  `client` — resultado de make-client
  `sql`    — SQL padrão (não legacy)

  Exemplo:
    (query client \"SELECT word, word_count
                   FROM `bigquery-public-data.samples.shakespeare`
                   ORDER BY word_count DESC
                   LIMIT 5\")"
  [client sql]
  (let [config (-> (QueryJobConfiguration/newBuilder sql)
                   (.setUseLegacySql false)
                   (.build))
        opts   (make-array (Class/forName "com.google.cloud.bigquery.BigQuery$JobOption") 0)
        result (.query client config opts)
        schema (.getSchema result)]
    (mapv (partial row->map schema) (.iterateAll result))))

;; ---------------------------------------------------------------------------
;; REPL — avalie esses forms para testar
;; ---------------------------------------------------------------------------

(comment

  ;; 1. Copie o JSON para a raiz deste projeto:
  ;;    cp ~/Downloads/bigquery-key.json service-account.json

  ;; 2. Crie o cliente passando o caminho do JSON
  (def client (make-client "service-account.json"))

  ;; 3. Consulta simples: 10 palavras mais frequentes em Shakespeare
  (query client
         "SELECT word, SUM(word_count) AS total
          FROM `bigquery-public-data.samples.shakespeare`
          GROUP BY word
          ORDER BY total DESC
          LIMIT 10")
  ;; => [{:word "the" :total "25568"} {:word "I" :total "21028"} ...]

  ;; 4. Consulta com filtro
  (query client
         "SELECT corpus, SUM(word_count) AS palavras
          FROM `bigquery-public-data.samples.shakespeare`
          GROUP BY corpus
          ORDER BY palavras DESC
          LIMIT 5")
  ;; => [{:corpus "hamlet" :palavras "32446"} ...]

  ;; 5. Qualquer tabela pública
  (query client
         "SELECT name, SUM(number) AS total
          FROM `bigquery-public-data.usa_names.usa_1910_2013`
          WHERE year = 2000
          GROUP BY name
          ORDER BY total DESC
          LIMIT 5")
  ;; => [{:name "Jacob" :total "34471"} ...]

  )
