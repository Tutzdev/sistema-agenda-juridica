# Agenda Jurídica — backend

Backend REST para agenda interna de escritório de advocacia. Permite administrar usuários, autenticar por sessão, criar e acompanhar atividades, controlar prazos e fornecer uma visão semanal e alertas para um dashboard.

O projeto contém somente o backend. Não envia notificações e não possui frontend, WhatsApp, e-mail, SMS, WebSocket, filas ou integrações externas. Os alertas são calculados no momento da consulta para exibição visual pelo futuro frontend.

## Requisitos e tecnologias

- Java 21
- Maven Wrapper
- PostgreSQL
- Spring Boot 4.1.0
- Spring Web MVC, Data JPA, Security e Validation
- Flyway
- JUnit e Mockito

## Configuração

Crie o banco principal no PostgreSQL:

```sql
create database agenda_juridica;
```

Configure as variáveis de ambiente. O arquivo `.env.example` lista todas elas, mas a aplicação não carrega arquivos `.env` automaticamente.

| Variável | Obrigatória | Finalidade |
|---|---:|---|
| `DB_URL` | sim | URL JDBC, por exemplo `jdbc:postgresql://localhost:5432/agenda_juridica` |
| `DB_USERNAME` | sim | Usuário do PostgreSQL |
| `DB_PASSWORD` | sim | Senha do PostgreSQL |
| `APP_ADMIN_NAME` | apenas no primeiro acesso | Nome do administrador inicial |
| `APP_ADMIN_EMAIL` | apenas no primeiro acesso | E-mail do administrador inicial |
| `APP_ADMIN_PASSWORD` | apenas no primeiro acesso | Senha do administrador inicial |
| `APP_DASHBOARD_UPCOMING_DAYS` | não | Janela de próximos vencimentos; padrão `3` |
| `SESSION_COOKIE_SECURE` | não | Use `true` quando a aplicação estiver sob HTTPS |

No PowerShell:

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/agenda_juridica'
$env:DB_USERNAME='postgres'
$env:DB_PASSWORD='sua-senha-local'
$env:APP_ADMIN_NAME='Administrador'
$env:APP_ADMIN_EMAIL='admin@escritorio.com'
$env:APP_ADMIN_PASSWORD='uma-senha-forte'
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/agenda_juridica'
export DB_USERNAME='postgres'
export DB_PASSWORD='sua-senha-local'
export APP_ADMIN_NAME='Administrador'
export APP_ADMIN_EMAIL='admin@escritorio.com'
export APP_ADMIN_PASSWORD='uma-senha-forte'
./mvnw spring-boot:run
```

O Flyway cria e atualiza o schema; o Hibernate apenas o valida. O administrador inicial só é criado quando a tabela de usuários está vazia e as três variáveis `APP_ADMIN_*` estão preenchidas. A senha é armazenada com BCrypt e nunca é registrada ou devolvida pela API. Depois do primeiro acesso, remova essas variáveis do ambiente de execução.

## Autenticação, sessão e CSRF

A autenticação utiliza sessão HTTP. O cookie da sessão é `HttpOnly`, `SameSite=Lax` e pode ser marcado como `Secure`. CSRF permanece ativo.

Antes de qualquer `POST`, `PUT`, `PATCH` ou `DELETE`, obtenha o token:

```http
GET /api/auth/csrf
```

A resposta informa `headerName` e `token`; envie o token nesse cabeçalho e preserve os cookies. Exemplo com `curl`:

```bash
curl -c cookies.txt http://localhost:8080/api/auth/csrf
curl -b cookies.txt -c cookies.txt -H 'Content-Type: application/json' \
  -H 'X-XSRF-TOKEN: TOKEN_DA_RESPOSTA' \
  -d '{"email":"admin@escritorio.com","password":"uma-senha-forte"}' \
  http://localhost:8080/api/auth/login
```

O frontend deve repetir o cabeçalho CSRF nas operações que alteram dados. `POST /api/auth/logout` também exige o token.

## Endpoints

### Autenticação

- `GET /api/auth/csrf` — obtém o token CSRF
- `POST /api/auth/login` — autentica por e-mail e senha
- `GET /api/auth/me` — retorna id, nome, e-mail e papel do usuário atual
- `POST /api/auth/logout` — invalida a sessão

### Usuários — somente `ADMIN`

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `PATCH /api/users/{id}/activation`

Não é possível duplicar e-mails nem desativar/rebaixar o último administrador ativo.

### Atividades — usuário autenticado

- `GET /api/tasks` — listagem paginada e filtrada
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}/status`
- `POST /api/tasks/{id}/complete`
- `POST /api/tasks/{id}/reopen`
- `POST /api/tasks/{id}/cancel`
- `DELETE /api/tasks/{id}` — somente cancelada, pelo criador ou administrador
- `GET /api/tasks/day?date=2026-08-18`
- `GET /api/tasks/week?referenceDate=2026-08-18`
- `GET /api/tasks/alerts/OVERDUE`
- `GET /api/tasks/alerts/DUE_TODAY`
- `GET /api/tasks/alerts/UPCOMING`

Filtros de `GET /api/tasks`: `startDate`, `endDate`, `scheduledDate`, `dueDate`, `category`, `status`, `priority`, `responsibleUserId`, `search`, `page`, `size` e `sort`. `startDate` e `endDate` delimitam a data agendada. O tamanho padrão é 20 e o máximo é 100.

Exemplo de criação:

```json
{
  "title": "Prazo do processo da cliente",
  "description": "Preparar e protocolar a manifestação.",
  "category": "DEADLINE",
  "priority": "HIGH",
  "scheduledDate": "2026-08-18",
  "scheduledTime": "09:00",
  "dueDate": "2026-08-19",
  "dueTime": "17:00",
  "reminderDate": "2026-08-18",
  "responsibleUserId": 2
}
```

### Dashboard

- `GET /api/dashboard`
- parâmetros opcionais: `referenceDate` e `responsibleUserId`

A semana principal sempre vai de segunda a sexta. Atividades de sábado e domingo permanecem disponíveis nas consultas gerais. A resposta contém totais por status, listas de atrasadas, vencendo hoje, próximas, lembretes ativos e cinco grupos diários para a semana útil.

A classificação obedece esta prioridade:

1. `OVERDUE`
2. `DUE_TODAY`
3. `UPCOMING`
4. `REMINDER_ACTIVE`
5. `SCHEDULED`
6. `NONE`

Atividades concluídas ou canceladas recebem `NONE` e não aparecem nos alertas pendentes. A data de negócio usa `America/Sao_Paulo` e a janela `UPCOMING` é configurável.

## Banco e migrations

- `V1__create_users_table.sql`
- `V2__create_tasks_table.sql`
- `V3__create_indexes.sql`

As migrations criam chaves, constraints de coerência e índices para os filtros usados. Credenciais reais não devem ser versionadas.

## Testes e build

Os testes unitários e de camada web não exigem banco. O teste de repositório usa H2 apenas no escopo de testes, em modo de compatibilidade PostgreSQL, e aplica as migrations reais. Para testes integrados contra PostgreSQL, o perfil `test` aceita `TEST_DB_URL`, `TEST_DB_USERNAME` e `TEST_DB_PASSWORD`.

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

```bash
./mvnw test
./mvnw clean verify
```

## Decisões e limites

- Arquitetura em camadas organizada por funcionalidade, sem interfaces de serviço ou bases genéricas.
- DTOs imutáveis em `record`; entidades JPA não são serializadas pela API.
- `Clock` injetável para regras de data e testes determinísticos.
- Filtros combináveis usam JPA Specifications; o dashboard usa uma consulta dedicada com relacionamentos carregados para evitar N+1.
- Não há cache, histórico completo de alterações, notificações externas nem frontend.

Para conectar o frontend, hospede-o preferencialmente na mesma origem ou configure CORS de forma restrita para a origem real, implemente o fluxo `csrf → login → me`, preserve cookies com credenciais e traduza no cliente os códigos de enum para textos amigáveis.
"# sistema-agenda-juridica" 
