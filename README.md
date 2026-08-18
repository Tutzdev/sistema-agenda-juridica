# Agenda Jurídica Backend

Backend de um sistema interno para organização de tarefas, prazos, audiências, reuniões e documentos de um escritório de advocacia.

O sistema permite cadastrar atividades, definir responsáveis, acompanhar vencimentos e exibir alertas de prazos pendentes diretamente no dashboard.

## Tecnologias

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* Jakarta Validation
* PostgreSQL
* Flyway
* Maven
* JUnit
* Mockito

## Funcionalidades

* Autenticação de usuários
* Controle de acesso por perfil
* Cadastro e gerenciamento de usuários
* Cadastro de tarefas e compromissos
* Definição de responsáveis
* Organização semanal das atividades
* Controle de prazos
* Classificação de tarefas por status
* Marcação de tarefas como concluídas
* Dashboard de pendências
* Alertas visuais de prazos:

  * atrasados;
  * vencendo hoje;
  * próximos do vencimento;
  * lembretes ativos.

O sistema não utiliza notificações por WhatsApp, e-mail, SMS ou APIs externas. Os alertas são exibidos apenas no dashboard.

## Categorias de atividades

* Prazos
* Audiências
* Documentos pendentes
* Reuniões com clientes
* Reuniões internas
* Pendências em processos
* Protocolos urgentes
* Levantamento de documentos
* Outras atividades

## Requisitos

Antes de executar o projeto, instale:

* Java 21
* PostgreSQL
* Git

O projeto utiliza o Maven Wrapper, portanto não é necessário instalar o Maven manualmente.

## Configuração do banco de dados

Crie um banco PostgreSQL:

```sql
CREATE DATABASE agenda_juridica;
```

Configure as seguintes variáveis de ambiente:

```env
DB_URL=jdbc:postgresql://localhost:5432/agenda_juridica
DB_USERNAME=postgres
DB_PASSWORD=sua_senha
```

## Administrador inicial

Na primeira execução, o sistema pode criar um administrador inicial utilizando:

```env
APP_ADMIN_NAME=Administrador
APP_ADMIN_EMAIL=admin@exemplo.com
APP_ADMIN_PASSWORD=senha_segura
```

As credenciais não devem ser adicionadas ao repositório.

## Executando o projeto

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux ou macOS

```bash
./mvnw spring-boot:run
```

A aplicação estará disponível em:

```text
http://localhost:8080
```

## Executando os testes

### Windows

```powershell
.\mvnw.cmd test
```

### Linux ou macOS

```bash
./mvnw test
```

## Build do projeto

### Windows

```powershell
.\mvnw.cmd clean verify
```

### Linux ou macOS

```bash
./mvnw clean verify
```

## Principais endpoints

### Autenticação

```http
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

### Usuários

```http
GET   /api/users
GET   /api/users/{id}
POST  /api/users
PUT   /api/users/{id}
PATCH /api/users/{id}/activation
```

### Tarefas

```http
GET   /api/tasks
GET   /api/tasks/{id}
POST  /api/tasks
PUT   /api/tasks/{id}
POST  /api/tasks/{id}/complete
POST  /api/tasks/{id}/reopen
POST  /api/tasks/{id}/cancel
```

### Dashboard

```http
GET /api/dashboard
```

## Estrutura do projeto

```text
src/main/java
├── auth
├── user
├── task
├── dashboard
├── security
└── shared
```

## Segurança

* Autenticação por sessão
* Senhas armazenadas com hash seguro
* Proteção CSRF
* Controle de acesso por perfil
* Cookies de sessão protegidos
* Dados sensíveis não são retornados pela API
* Credenciais configuradas por variáveis de ambiente

## Banco de dados

As alterações no banco são controladas pelo Flyway.

```text
src/main/resources/db/migration
```

O Hibernate valida a estrutura existente, mas não cria automaticamente as tabelas em produção.

## Objetivo do projeto

Fornecer uma solução simples, segura e de fácil manutenção para organizar as atividades semanais e os prazos de um escritório de advocacia.

