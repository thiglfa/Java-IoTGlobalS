# WellWork

**WellWork** é uma aplicação Spring Boot que oferece um sistema de check-ins de bem-estar, permitindo que usuários registrem seu humor, nível de energia e notas diárias. A aplicação integra inteligência artificial para gerar mensagens de recomendação personalizadas com base nos check-ins e utiliza mensageria (RabbitMQ) para envio assíncrono de mensagens de boas-vindas.

INTEGRANTES:

- Eduardo do Nascimento Barriviera - RM 555309
- Thiago Lima de Freitas - RM 556795
- Bruno Centurion Fernandes - RM 556531

---

## Funcionalidades

- Registro e autenticação de usuários com JWT.
- Criação, atualização parcial e listagem de check-ins.
- Geração automática de mensagens de bem-estar usando IA (Groq LLM).
- Envio de mensagens de boas-vindas via RabbitMQ.
- Controle de acesso baseado em JWT e roles.
- Cache de usuários com Caffeine para otimização de consultas.

---

## Tecnologias Utilizadas

- **Backend:** Spring Boot 3, Spring Data JPA, Spring Security, Spring WebFlux, Spring AI.
- **Banco de dados:** Oracle (via JDBC/ojdbc11).
- **Mensageria:** RabbitMQ.
- **IA:** Groq LLM (`llama-3.1-8b-instant`) para geração de mensagens.
- **Cache:** Caffeine.
- **Segurança:** JWT (JSON Web Tokens) para autenticação.
- **Build:** Maven.

---

## Estrutura do Projeto

- **`model`**: entidades JPA (`User`, `CheckIn`, `GeneratedMessage`) e enums (`Mood`, `EnergyLevel`).
- **`dto`**: objetos de transferência de dados para requests e responses.
- **`repository`**: interfaces de repositório JPA.
- **`service`**: lógica de negócios, incluindo integração com IA e RabbitMQ.
- **`controller`**: endpoints REST.
- **`config`**: configuração de segurança (JWT) e RabbitMQ.
- **`messaging` / `listener`**: consumidores e produtores de mensagens assíncronas.

---

## Endpoints Principais

### Autenticação

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/auth/register` | Cria um novo usuário |
| POST | `/auth/login` | Autentica o usuário e retorna token JWT |


### Usuários

| Método | Endpoint | Descrição | 
|--------|----------|-----------|
| GET | `/api/users/me` | Retorna perfil do usuário autenticado. |
| GET | `/api/users/all` | Lista todos os usuários (paginação). |
| GET | `/api/users/{id}` | Retorna usuário por ID. |
| DELETE | `/api/users/{id}` | Deleta o próprio usuário. |

### Check-ins

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/checkins` | Lista check-ins do usuário autenticado (paginação). |
| GET | `/api/checkins/{id}` | Consulta check-in por ID. |


Metódos POST, PUT e PATCH requerem um body em formato JSON:

| Método | Endpoint | Descrição | Body JSON |
|--------|----------|-----------|-----------|
| POST | `/auth/register` | Cria um novo usuário | `{ "username": "usuario123", "password": "senha123" }` |
| POST | `/auth/login` | Autentica o usuário e retorna token JWT | `{ "username": "usuario123", "password": "senha123" }` |
| PUT | `/api/users/{id}/password` | Atualiza senha do próprio usuário |`{ "password": "novaSenha123" }` |
| POST | `/api/checkins` | Cria um novo check-in | `{ "mood": "0", "energyLevel": "0", "notes": "Hoje estou me sentindo ótimo!" }` |
| PATCH | `/api/checkins/{id}` | Atualiza parcialmente um check-in | `{ "mood": "1", "energyLevel": "1", "notes": "Notas atualizadas" }` |
| POST | `/api/checkins/{id}/generate-message` | Gera mensagem de bem-estar via IA para o check-in | `{ "checkInId": 123 }` (opcional, pois o ID já está na URL) |



Moods (de 0 a 3): HAPPY, NEUTRAL, SAD, STRESSED.


EnergyLevel (de 0 a 2): HIGH, MEDIUM, LOW.

---

## Configuração

### Banco de dados

Configuração padrão para Oracle no `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl
    username: ${DB_USER}/seu usuário Oracle
    password: ${DB_PASS}/sua senha Oracle
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.OracleDialect
```

### RabbitMQ

```yaml
spring:
  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    port: ${RABBIT_PORT:5672}
    username: ${RABBIT_USER:guest}
    password: ${RABBIT_PASS:guest}
```

- Exchange: `user.exchange`
- Queue: `user.welcome.queue`
- Routing key: `user.welcome`

### Groq LLM

```yaml
groq:
  api:
    key: ${GROQ_KEY}
  model: llama-3.1-8b-instant
  temperature: 0.2
```

---

## Como Rodar o Projeto

1. Clonar o repositório: https://github.com/thiglfa/Java-IoTGlobalS

```bash
git clone https://github.com/thiglfa/Java-IoTGlobalS.git
cd Java-IoTGlobalS
```

2. Configurar variáveis de ambiente:

```bash
export DB_USER=seu_usuario
export DB_PASS=sua_senha
export RABBIT_HOST=localhost
export GROQ_KEY=sua_chave_groq
export JWT_SECRET=uma_chave_segura
```

3. Rodar o projeto com Maven:

```bash
mvn clean install
mvn clean spring-boot:run (ou rodar o arquivo Main do projeto)
```

O servidor será iniciado em `http://localhost:8080`.

---

## Observações

- Todas as requisições para endpoints protegidos exigem um token JWT no header `Authorization: Bearer <token>`.
- As mensagens de check-in são geradas de forma assíncrona usando `@Async`, garantindo que a criação do check-in não seja bloqueada.
- RabbitMQ é usado para envio de mensagens de boas-vindas, mas o sistema ignora falhas de conexão para não impactar o fluxo principal.

