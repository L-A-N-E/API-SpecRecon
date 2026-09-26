# 🩹 Troubleshooting

Guia de apoio para quem já seguiu o [README](../README.md) e encontrou algum erro. Leia a seção 1 antes de reportar um bug - ela explica a causa da maioria dos problemas de configuração do projeto.

## 1. Como o Spring Boot decide a configuração

O resultado final de qualquer execução é a soma de três camadas, aplicadas nesta ordem:

**1) `application.properties` (base, sempre carregado primeiro)**
Define HTTPS na porta `8443` por padrão (pensado para o cenário Docker/produção):
```properties
server.ssl.enabled=true
server.port=8443
```

**2) O profile ativo sobrescreve a base**
O profile `local` (`application-local.properties`) desliga o SSL e usa a porta `8080`:
```properties
server.ssl.enabled=false
server.port=8080
```
Sem ativar esse profile, a aplicação sempre volta ao padrão HTTPS/8443 - **é por isso que abrir `http://localhost:8443` no navegador dá o erro "Bad Request: this combination of host and port requires TLS"**: a porta 8443 só existe em modo HTTPS quando o profile `local` não está ativo.

Para ativar o profile `local` (use uma das duas formas, não as duas):
```powershell
# Opção A - flag na hora de rodar (vale só para essa execução)
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

# Opção B - variável de ambiente (vale para toda a sessão do terminal)
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

**3) Variáveis de ambiente sempre "ganham" dos valores nos arquivos `.properties`**
Um valor como `${DB_USER:root}` significa "use a variável de ambiente `DB_USER`; se ela não existir, use `root`". Ou seja:
- Sem a variável definida → vale o padrão após os dois-pontos.
- Com a variável definida no PowerShell → ela **continua valendo em qualquer comando seguinte na mesma janela**, mesmo em contextos diferentes (teste, run, outro profile). Essa é a causa mais comum de confusão: uma `DB_USER`/`DB_PASS`/`DB_URL` definida há vários comandos atrás "vaza" para a execução atual.

**Resumo:** `application.properties` + arquivo do profile ativo + variáveis de ambiente da sessão (que sempre vencem). Se algo não bate com o esperado, confira essas três camadas nessa ordem.

> O `.env` na raiz de `API/` tem prioridade **menor** que variáveis já definidas na sessão do terminal - se os dois existirem com valores diferentes, vale o do terminal.

## 2. Tabela de problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Bad Request: this combination of host and port requires TLS` no navegador | Acesso a `http://localhost:8443` sem o profile `local` ativo (8443 só existe em HTTPS) | Acesse com `https://` na porta 8443, **ou** ative o profile `local` e use `http://localhost:8080` |
| `Access denied for user 'X'@'localhost' (using password: YES)` no log, app encerra sozinha | `DB_USER`/`DB_PASS` (padrão ou variável de ambiente) não batem com o usuário real do MySQL | Confira com `echo $env:DB_USER` / `echo $env:DB_PASS` e alinhe com um usuário que exista de fato (crie um se necessário - ver README) |
| `UnknownHostException` ou `Communications link failure` ao conectar no banco | `DB_URL` aponta para um host de container (ex.: `mysql`, `db_test`) em vez de `localhost`, geralmente por ter sido definida numa sessão anterior com Docker | `echo $env:DB_URL` para confirmar e redefina para `localhost` antes de rodar localmente |
| Log mostra `BUILD SUCCESS`, mas a API não responde | O processo Java terminou porque o Spring falhou ao subir o contexto (erro de banco, bean, etc.) - `BUILD SUCCESS` só indica sucesso do build, não da aplicação | Role o log até achar a primeira linha `Caused by:` - ela mostra o erro real |
| Credenciais de uma execução "vazam" para a próxima | `DB_USER`/`DB_PASS`/`DB_URL`/`SPRING_PROFILES_ACTIVE` definidas com `$env:` continuam valendo em todos os comandos seguintes na mesma janela do PowerShell | Abra um terminal novo para "resetar", ou redefina as variáveis explicitamente antes de cada execução |

## 3. Rodando os testes automatizados contra um MySQL local

Os testes usam o schema `specrecon_test` (separado do banco de desenvolvimento) e leem a conexão de três variáveis de ambiente, com padrões definidos em `application-test.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/specrecon_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASS:specreconpass}
```

Por padrão, tentam conectar em `localhost:3306` com usuário `root` e senha `specreconpass`. Se o seu MySQL usa outras credenciais, defina as variáveis antes de rodar:

```powershell
# PowerShell
$env:DB_URL="jdbc:mysql://localhost:3306/specrecon_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="seu_usuario"
$env:DB_PASS="sua_senha"
```
```bash
# Linux/macOS
export DB_URL="jdbc:mysql://localhost:3306/specrecon_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER=seu_usuario
export DB_PASS=sua_senha
```

O usuário precisa de permissão para criar o schema (`createDatabaseIfNotExist=true` exige privilégio `CREATE`) e para criar/alterar tabelas, já que o Flyway roda as migrações no boot dos testes. No MySQL Workbench: **Users and Privileges → Schema Privileges → Add Entry**, concedendo ao menos `CREATE, ALTER, DROP, SELECT, INSERT, UPDATE, DELETE, INDEX, REFERENCES`.

> ⚠️ **Alternando entre Docker e MySQL local:** o `docker-compose.test.yml` define suas próprias `DB_URL`/`DB_USER`/`DB_PASS` apontando para o container (host `db_test`), mas isso vale **dentro** do container - não afeta seu terminal. O problema comum é o oposto: se essas variáveis já estiverem definidas manualmente no seu sistema apontando para um host de container ou credenciais antigas, elas sobrescrevem os padrões do `application-test.properties` e os testes falham com `UnknownHostException` ou `Communications link failure` ao rodar contra o MySQL local.
>
> Se isso acontecer: confira com `echo $env:DB_URL` (PowerShell) ou `echo $DB_URL` (bash); se apontar para um host de container, redefina para `localhost` ou remova a variável.