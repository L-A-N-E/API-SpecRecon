# 🔍 SpecRecon API

> **Sistema de Gerenciamento de Veículos para Inteligência Estratégica da Ford**

API RESTful desenvolvida em **Java + Spring Boot** para gerenciamento de veículos, especificações técnicas, unidades de medida e usuários, com autenticação, autorização, segurança e testes automatizados.

## 👥 Integrantes

| Integrante | RM |
|---|---:|
| Alice Santos Bulhões | RM554499 |
| Eduardo Oliveira Cardoso Madid | RM556349 |
| Nicolas Haubricht Hainfellner | RM556259 |
| Lucas Henzo Ide Yuki | RM554865 |
| Guilherme da Cunha Melo | RM555310 |

## 📚 Índice

- [🛠️ Tecnologias e Componentes](#️-tecnologias-e-componentes)
- [🏗️ Arquitetura da Aplicação](#️-arquitetura-da-aplicação)
  - [Fluxo de Requisição](#fluxo-de-requisição)
  - [Arquitetura](#arquitetura)
  - [Fluxo de Comunicação e Autenticação](#fluxo-de-comunicação-e-autenticação)
  - [Perfis de Acesso (RBAC)](#perfis-de-acesso-rbac)
  - [Arquitetura Orientada a Serviços (SOA)](#arquitetura-orientada-a-serviços-soa)
- [🛡️ Segurança](#️-segurança)
  - [Mecanismos de Segurança](#mecanismos-de-segurança)
  - [Tratamento de Erros](#tratamento-de-erros)
- [🚀 API RESTful](#-api-restful)
  - [Swagger](#swagger)
  - [Veículos](#veículos)
  - [Usuários e Autenticação](#usuários-e-autenticação)
  - [Tipos de Especificação](#tipos-de-especificação)
  - [Unidades de Medida](#unidades-de-medida)
- [💾 Banco de Dados e Migrações](#-banco-de-dados-e-migrações)
- [▶️ Como Executar](#️-como-executar)
  - [Execução Local - HTTP](#execução-local--http)
  - [Execução com Docker - HTTPS](#execução-com-docker--https)
- [🧪 Como Testar](#-como-testar)
  - [Testes via Swagger](#testes-via-swagger)
  - [Testes Automatizados com JUnit](#testes-automatizados-com-junit)
  - [Script de Cybersecurity](#script-de-cybersecurity)
- [🎥 Demonstração](#-demonstração)
- [🔗 Links Úteis](#-links-úteis)

# 🛠️ Tecnologias e Componentes

A aplicação utiliza uma arquitetura baseada em camadas e os seguintes componentes:

| Tecnologia / Componente | Utilização |
|---|---|
| **Spring Boot 3.x** | Framework base para construção da API Java |
| **Spring Security + JWT** | Autenticação e autorização stateless com RBAC |
| **Spring Data JPA / Hibernate** | Persistência e ORM |
| **MySQL** | Banco de dados relacional |
| **Flyway/Liquibase** | Controle de versionamento e migrações do banco |
| **Bucket4j** | Rate Limiting para proteção contra abusos |
| **Jakarta Validation** | Validação dos dados de entrada |
| **Swagger / OpenAPI 3** | Documentação interativa da API |
| **JUnit 5** | Testes unitários |

# 🏗️ Arquitetura da Aplicação

A aplicação segue o modelo de **Arquitetura em Camadas**, promovendo separação de responsabilidades e facilitando manutenção e escalabilidade.

## Fluxo de Requisição

O fluxo principal de uma requisição é:

```text
Client
  ↓
Controller (REST)
  ↓
DTO
  ↓
Service (Business Logic)
  ↓
Repository
  ↓
Database
```

## Arquitetura

![Arquitetura SpecRecon](Arquitetura/specrecon_architecture.drawio.png)

## Fluxo de Comunicação e Autenticação

Todo endpoint protegido segue o fluxo abaixo, desde o login até a liberação ou negação de acesso ao recurso:

```mermaid
sequenceDiagram
    participant C as Cliente
    participant Auth as AuthController
    participant JWT as JwtTokenProvider
    participant Filter as JwtAuthenticationFilter
    participant Sec as Spring Security (RBAC)
    participant Ctrl as Controller (ex: VehicleController)

    C->>Auth: POST /auth/login (email, password)
    Auth->>Auth: valida credenciais (BCrypt)
    Auth->>JWT: gera token (HS512) + refresh token
    JWT-->>Auth: token, refreshToken, role
    Auth-->>C: 200 OK { token, refreshToken, role }

    Note over C: Cliente guarda o token e passa a enviá-lo<br/>no header Authorization em toda requisição

    C->>Filter: GET/POST /vehicles (Authorization: Bearer <token>)
    Filter->>JWT: valida assinatura e expiração do token

    alt token ausente, inválido ou expirado
        Filter-->>C: 401 Unauthorized
    else token válido
        Filter->>Sec: popula SecurityContext (usuário + role)
        Sec->>Sec: authorizeHttpRequests / @PreAuthorize

        alt role sem permissão para o recurso
            Sec-->>C: 403 Forbidden
        else role autorizada
            Sec->>Ctrl: encaminha requisição
            Ctrl-->>C: 200/201/204 + corpo (quando houver)
        end
    end
```

### Resumo do fluxo de autenticação

1. **Login - `POST /auth/login`**
   - As credenciais são validadas com BCrypt.
   - Se estiverem corretas, o `JwtTokenProvider` emite um **JWT (HS512)** válido por 1 hora.
   - Também é emitido um **refresh token** válido por 24 horas.
   - Ambos contêm `userId` e `role` nas claims.

2. **Requisições autenticadas**
   - O cliente envia o token no header:
     ```text
     Authorization: Bearer <token>
     ```
   - O `JwtAuthenticationFilter` intercepta a requisição.
   - A assinatura e a expiração são validadas.
   - Se o token for válido, o `SecurityContext` do Spring Security é preenchido com o usuário e sua `role`.

3. **Autorização**
   - O `SecurityFilterChainConfig` controla permissões por URL através de `authorizeHttpRequests`.
   - Permissões também podem ser controladas por método utilizando `@PreAuthorize`.
   - Quando aplicável:
     - `401 Unauthorized`: token ausente, inválido ou expirado.
     - `403 Forbidden`: token válido, mas sem permissão.

4. **Renovação**
   - Quando o token expira, o cliente pode utilizar:
     ```text
     POST /auth/refresh
     ```
   - O refresh token permite obter um novo par de tokens sem realizar login novamente.

## Perfis de Acesso (RBAC)

| Perfil (`role`) | Permissões |
|---|---|
| `ADMIN` | Todos os endpoints, incluindo criação, edição e exclusão de usuários (`/users`) |
| `ANALYST` | Leitura de usuários (`GET /users`, `GET /users/{id}`) e acesso completo a veículos, unidades e tipos de especificação |
| `USER` | Acesso completo a veículos, unidades e tipos de especificação, sem acesso a `/users` |

### Endpoints públicos

Não exigem token:

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/refresh`
- `/swagger-ui/**`
- `/v3/api-docs/**`

## Arquitetura Orientada a Serviços (SOA)

O projeto foi construído com foco em **modularidade** e **reutilização**.

### Organização Modular

Cada entidade de domínio possui seu próprio serviço:

- `VehicleService`
- `UserService`
- `UnitService`

Isso mantém a lógica de negócio independente e permite que os módulos sejam expostos ou reutilizados por outros sistemas.

### Separação de Camadas

| Camada | Responsabilidade |
|---|---|
| **Apresentação** | Controllers lidam com HTTP e conversão de DTOs |
| **Serviço** | Service Layer centraliza regras de negócio |
| **Dados** | Repositories isolam o acesso ao banco via Spring Data JPA |

Exemplos de regras de negócio incluem validação de nomes únicos e validação de tipos de dados.

# 🛡️ Segurança

Os mecanismos de segurança da aplicação estão detalhados no arquivo [`SECURITY.md`](https://github.com/L-A-N-E/API-SpecRecon/blob/main/API/SECURITY.md).

## Mecanismos de Segurança

| Mecanismo | Descrição |
|---|---|
| **JWT + RBAC** | Autenticação stateless e controle de acesso por perfil |
| **BCrypt** | Validação e proteção de senhas |
| **HMAC Payload Signature** | Verificação de integridade através do header `X-Signature` |
| **AES-256** | Criptografia de dados sensíveis em repouso no banco |
| **Rate Limiting** | Limite de 100 requisições por minuto por IP |
| **Sanitização** | Proteção contra XSS e SQL Injection através do `SafeStringValidator` |
| **HTTPS / TLS 1.2+** | Comunicação criptografada utilizando certificado PKCS12 |
| **Auditoria** | Registro de operações e trilha de auditoria |
| **Honeypot** | Mecanismo adicional contemplado no script de cybersecurity |

## Tratamento de Erros

A API utiliza um **Global Exception Handler** (`@ControllerAdvice`) para capturar falhas e retornar respostas padronizadas.

Exemplo:

```json
{
  "timestamp": "2023-10-27T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Unidade padrão é recomendada para NUMBER",
  "path": "/specification-types"
}
```

Essa abordagem evita a exposição de stack traces internos ao cliente.

# 🚀 API RESTful

A API segue os princípios **RESTful**, utilizando métodos HTTP e códigos de status apropriados para representar as operações sobre os recursos.

## Swagger

A documentação interativa está disponível através do Swagger:

- **HTTP:** `http://localhost:8080/swagger-ui/index.html`
- **HTTPS / Docker:** `https://localhost:8443/swagger-ui/index.html`

## Veículos

Base URL:

```text
/vehicles
```

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/vehicles` | Lista todos os veículos cadastrados |
| `GET` | `/vehicles/{id}` | Busca detalhes e especificações de um veículo |
| `POST` | `/vehicles` | Cria um novo veículo - requer `X-Signature` |
| `PUT` | `/vehicles/{id}` | Atualiza dados de um veículo |
| `DELETE` | `/vehicles/{id}` | Remove um veículo |

## Usuários e Autenticação

Bases:

```text
/auth
/users
```

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/auth/login` | Autentica usuário e retorna JWT + Refresh Token |
| `POST` | `/auth/register` | Registra um novo usuário com validação de senha forte |
| `GET` | `/users` | Lista usuários - acesso restrito a `ADMIN`/`ANALYST` |
| `PUT` | `/users/{id}` | Atualiza perfil e permissões do usuário |

## Tipos de Especificação

Base URL:

```text
/specification-types
```

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/specification-types` | Lista categorias como Motor, Transmissão etc. |
| `POST` | `/specification-types` | Cria novos tipos com validação de unidade |

> Exemplo: tipos `NUMBER` podem exigir uma unidade de medida padrão.

## Unidades de Medida

Base URL:

```text
/units
```

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/units` | Lista todas as unidades de medida |
| `POST` | `/units` | Cria uma nova unidade, como `km/h` ou `kg` |

# 💾 Banco de Dados e Migrações

## Configuração da Conexão

A conexão com o banco é configurada através do `application.properties`, utilizando variáveis de ambiente para evitar o armazenamento direto de credenciais.

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.jpa.hibernate.ddl-auto=validate
```

## Migrações

Os scripts SQL ficam em:

```text
src/main/resources/db/migration/
```

As versões descritas no projeto são:

| Versão | Alterações |
|---|---|
| **V1** | Criação do esquema base: veículos, usuários e especificações |
| **V2** | Adição de tabelas de auditoria e políticas de retenção de dados |

O versionamento permite que o banco evolua de forma consistente entre os ambientes de desenvolvimento, staging e produção.

# ▶️ Como Executar

Existem duas formas principais de executar a aplicação:

1. **Execução local utilizando HTTP**
2. **Execução através do Docker utilizando HTTPS**

## Execução Local - HTTP

### Pré-requisitos

- Java 17+
- Maven
- MySQL configurado

### 1. Instalar dependências

Execute:

```bash
mvn clean install
```

### 2. Configurar variáveis de ambiente

Configure o arquivo `.env` com as chaves necessárias, incluindo:

```text
JWT_SECRET
DB_URL
DB_USER
DB_PASS
```

### 3. Ativar o profile local

No ambiente de execução, selecione o profile:

```text
local
```

Isso fará com que seja utilizado o arquivo:

```text
application-local.properties
```

### 4. Executar a aplicação

Pelo IntelliJ, execute:

```text
SpecReconApplication.java
```

Ou utilize o Maven:

```bash
mvn spring-boot:run
```

### 5. Acessar o Swagger

Após iniciar a aplicação:

```text
http://localhost:8080/swagger-ui/index.html
```

## Execução com Docker - HTTPS

### Pré-requisitos

- Docker Desktop instalado e em execução

### 1. Configurar o `.env`

Configure as variáveis necessárias:

```text
JWT_SECRET
DB_URL
DB_USER
DB_PASS
```

### 2. Verificar a porta do MySQL

Se houver um MySQL instalado diretamente na máquina, interrompa o serviço local antes de iniciar o Docker para evitar conflito na porta `3306`.

No Windows:

```text
Win + R
→ services.msc
→ localizar o serviço MySQL
→ Parar
```

Ou, em um terminal executado como Administrador:

```bash
net stop MySQL80
```

### 3. Subir os containers

```bash
docker compose up --build
```

### 4. Acessar o Swagger

```text
https://localhost:8443/swagger-ui/index.html
```

# 🧪 Como Testar

A aplicação possui três formas principais de validação:

1. Testes manuais através do Swagger
2. Testes automatizados com JUnit 5
3. Script automatizado de cybersecurity

## Testes via Swagger

### 1. Autenticação

Acesse:

```text
https://localhost:8443/swagger-ui/index.html
```

Depois:

1. Expanda o grupo **Autenticação**.
2. Execute `POST /auth/register` para criar um usuário.
3. Execute `POST /auth/login` utilizando as mesmas credenciais.
4. Copie o `token` retornado.
5. Clique em **Authorize** 🔒 no topo da página.
6. Cole o token.
7. Clique em **Authorize** e depois em **Close**.

A partir desse momento, os endpoints protegidos serão chamados com autenticação.

## Endpoints com `X-Signature`

Alguns endpoints exigem o header `X-Signature`, incluindo:

- `POST /vehicles`
- `POST /units`
- `POST /specification-types`
- `POST /users`
- Operações `PUT`
- Operações `DELETE`

A assinatura é gerada a partir do body da requisição.

No Git Bash:

```bash
echo -n 'COLE_O_BODY_AQUI' | openssl dgst -sha256 -hmac "ixe9zAIWWX2xb0x92Vh2saWOWPOMnj0/OO5MONBvYlspovQ+ZvBoMJyq0btR0yOq" -binary | base64
```

Substitua `COLE_O_BODY_AQUI` pelo JSON exatamente como será enviado pelo Swagger.

> ⚠️ O body utilizado para gerar a assinatura deve ser **exatamente igual** ao body enviado na requisição. Diferenças de espaços ou ordem dos campos podem invalidar a assinatura.

Depois, cole o resultado no campo **X-Signature** do Swagger e execute o endpoint.

## Fluxo recomendado

Para realizar uma operação autenticada:

```text
1. Registrar usuário ADMIN
        ↓
2. Fazer login
        ↓
3. Copiar o token
        ↓
4. Clicar em Authorize 🔒
        ↓
5. Colar o token
        ↓
6. Para endpoints com X-Signature:
   copiar body → gerar assinatura → colar assinatura
        ↓
7. Executar endpoint
```

## Exemplo Completo - Criar um Veículo

### Passo 1 - Registrar usuário

Utilize `POST /auth/register`:

```json
{
  "email": "admin@ford.com",
  "password": "@Securepassword123",
  "role": "ADMIN"
}
```

### Passo 2 - Fazer login

Utilize `POST /auth/login`:

```json
{
  "email": "admin@ford.com",
  "password": "@Securepassword123"
}
```

Copie o `token` retornado.

### Passo 3 - Autorizar no Swagger

Clique em **Authorize 🔒**, cole o token e confirme.

### Passo 4 - Gerar a assinatura

Para o seguinte body:

```json
{"brand":"Ford","model":"Ranger","version":"Raptor"}
```

Execute:

```bash
echo -n '{"brand":"Ford","model":"Ranger","version":"Raptor"}' | openssl dgst -sha256 -hmac "ixe9zAIWWX2xb0x92Vh2saWOWPOMnj0/OO5MONBvYlspovQ+ZvBoMJyq0btR0yOq" -binary | base64
```

### Passo 5 - Enviar a requisição

No Swagger, informe:

**Header `X-Signature`:**

```text
<assinatura-gerada>
```

**Body:**

```json
{
  "brand": "Ford",
  "model": "Ranger",
  "version": "Raptor"
}
```

> ⚠️ O body enviado deve ser exatamente igual ao utilizado para gerar a assinatura.

### Passo 6 - Executar

Clique em **Execute**.

# 🧪 Testes Automatizados com JUnit

A API possui testes de integração utilizando **JUnit 5**.

Os testes sobem o contexto completo da aplicação, incluindo:

- Security
- JWT
- Interceptors
- Banco de dados
- Endpoints HTTP

Os cenários cobrem:

- Sucesso
- Erros de validação
- Recursos inexistentes
- Falhas de autenticação
- Falhas de autorização

Existem duas formas de executar os testes:

- Docker
- MySQL local

## Opção 1 - Docker

> **Recomendado**

O arquivo `docker-compose.test.yml` cria um ambiente isolado para execução dos testes.

### Serviços

#### `db_test`

Executa uma instância do MySQL 8.0 e cria automaticamente o banco:

```text
specrecon_test
```

#### `junit_runner`

Container Maven que aguarda o banco de testes ficar disponível através do healthcheck e executa:

```bash
mvn clean test
```

### Executar

```bash
docker compose -f docker-compose.test.yml up --build --abort-on-container-exit
```

## Opção 2 - MySQL Local

### Pré-requisito

É necessário possuir um MySQL acessível em:

```text
localhost:3306
```

Os testes utilizam o schema:

```text
specrecon_test
```

separado do banco de desenvolvimento.

### Executar

Dentro da pasta `API/`:

```bash
cd API
mvn test
```

## Classes e Cenários Cobertos

| Classe | Cenários |
|---|---|
| `AuthFlowTest` | Registro com sucesso (`201`), e-mail duplicado (`409`), senha fraca (`400`), login válido (`200` + token/refreshToken) e login com senha incorreta (`401`) |
| `VehicleAccessAndCrudTest` | Listagem sem token (`401`), listagem autenticada (`200`), criação de veículo (`201`), corpo inválido (`400`), veículo inexistente (`404`) e criação + exclusão (`204`) |
| `UserAccessControlTest` | Listagem de usuários sem token (`401`), perfil `USER` tentando acessar `/users` (`403` - RBAC) e perfil `ADMIN` acessando `/users` (`200`) |

# 🔐 Script de Cybersecurity

> Se for rodar, não esqueça de configurar o arquivo `.env` com as variáveis de ambiente necessárias e subir via Docker o banco de dados MySQL

Um script Bash automatizado está disponível para testar os principais eixos de cybersecurity da aplicação.

Execute dentro da pasta `API/`:

```bash
bash test_specrecon.sh
```

O script cobre:

- ✅ Autenticação JWT
- ✅ Refresh Token
- ✅ RBAC
- ✅ Validação de entrada
- ✅ Proteção contra XSS
- ✅ Proteção contra SQL Injection
- ✅ Validação de senha fraca
- ✅ X-Signature HMAC
- ✅ Proteção das APIs
- ✅ Criptografia AES-256 no banco
- ✅ Trilha de auditoria
- ✅ Honeypot
- ✅ Rate Limiting

# 🎥 Demonstração

Foi desenvolvido um vídeo tutorial demonstrando como executar a API com os mecanismos de segurança aplicados.

▶️ **[Assistir ao vídeo da API no YouTube](https://www.youtube.com/watch?v=pPPahV5tn5Y)**

# 🔗 Links Úteis

| Recurso | Link |
|---|---|
| 📖 Swagger - HTTP | [Abrir Swagger](http://localhost:8443/swagger-ui/index.html) |
| 💻 Repositório | [GitHub - API-SpecRecon](https://github.com/L-A-N-E/API-SpecRecon) |
| 🛡️ Segurança | [SECURITY.md](https://github.com/L-A-N-E/API-SpecRecon/blob/main/API/SECURITY.md) |
| 🎥 Vídeo | [Tutorial da API no YouTube](https://www.youtube.com/watch?v=pPPahV5tn5Y) |


## 📄 Documentação de Segurança

Para informações detalhadas sobre os mecanismos de segurança implementados no projeto, consulte:

[**SECURITY.md - Política de Segurança**](https://github.com/L-A-N-E/API-SpecRecon/blob/main/API/SECURITY.md)

