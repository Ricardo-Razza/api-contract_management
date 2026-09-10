# Contract Management API

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-green?style=flat-square&logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-blue?style=flat-square&logo=mysql)
![License](https://img.shields.io/badge/License-Proprietary-red?style=flat-square)


Sistema de gerenciamento de contratos e atas de registro de preço com notificações automáticas de vencimento.


## 📋 Descrição

API REST para administração centralizada de contratos, atas de registro de preço (ARP), equipes de trabalho e servidor/departamentos. Inclui agendador automático para notificações de vencimento (90, 60 e 30 dias) com envio de e-mails.

## 🛠 Stack Tecnológico

- **Linguagem**: Java 21 (LTS)
- **Framework**: Spring Boot 4.1.0
- **ORM**: Hibernate / Spring Data JPA
- **Banco de Dados**: MySQL 8.0+
- **Build**: Maven 3.9+
- **Documentação**: SpringDoc OpenAPI / Swagger
- **Email**: Spring Mail
- **Container**: Docker & Docker Compose

## 🚀 Quick Start

### Pré-Requisitos

- Java 21+
- Maven 3.9+
- MySQL 8.0+
- Docker (opcional)

### Instalação Local

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/contract-management-api.git
cd contract-management-api

# Configure o banco de dados
mysql -u root -p
> CREATE DATABASE contract_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Configure variáveis de ambiente
cp .env.example .env
# Edite .env com suas credenciais

# Compile e execute
mvn clean compile
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8081/api`

### Instalação com Docker

```bash
docker-compose up -d
```

## 📁 Estrutura do Projeto

```
src/main/java/com/contract_management/api/
├── controller/           # REST Controllers
├── service/             # Lógica de negócio
├── repository/          # Spring Data JPA
├── model/               # Entidades JPA
├── dto/                 # Data Transfer Objects
├── scheduler/           # Tarefas agendadas
├── exception/           # Tratamento de erros
├── config/              # Configurações
└── ApiApplication.java  # Entrada da aplicação
```

## 📡 API Endpoints

### Base URL
```
http://localhost:8081/api
```

### Autenticação

A API usa JWT no esquema Bearer. Os usuários iniciais são criados automaticamente
quando `AUTH_SEED_DEFAULT_USERS=true` (configuração padrão), usando as credenciais
definidas no `.env`:

| Papel | E-mail padrão | Senha padrão |
|-------|---------------|--------------|
| ADMIN | `admin@example.com` | `Admin@123456` |
| GESTOR | `gestor@example.com` | `Gestor@123456` |

Faça login para obter o token:

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","senha":"Admin@123456"}'
```

Use o valor de `token` retornado em todas as rotas protegidas:

```bash
curl http://localhost:8081/api/contratos \
  -H "Authorization: Bearer SEU_TOKEN"
```

`ADMIN` possui acesso a todos os recursos. `GESTOR` pode ler e alterar
`/contratos` e `/atas`, mas não pode administrar os demais recursos.
O login e o Swagger permanecem públicos.

Usuários e papéis podem ser administrados por um `ADMIN`:

```http
POST /api/usuarios
Authorization: Bearer SEU_TOKEN_ADMIN
Content-Type: application/json
```

```json
{
  "nome": "Nome da pessoa",
  "email": "pessoa@empresa.com",
  "senha": "SenhaForte@123",
  "papel": "GESTOR"
}
```

Para alterar o papel de um usuário existente:

```http
PATCH /api/usuarios/{id}/papel
Authorization: Bearer SEU_TOKEN_ADMIN
Content-Type: application/json
```

```json
{
  "papel": "ADMIN"
}
```

| Recurso | Método | Endpoint | Descrição |
|---------|--------|----------|-----------|
| **Contratos** | GET | `/contratos` | Listar todos |
| | GET | `/contratos/{id}` | Obter por ID |
| | POST | `/contratos` | Criar novo |
| | PUT | `/contratos/{id}` | Atualizar |
| | DELETE | `/contratos/{id}` | Deletar |
| **Atas** | GET | `/atas` | Listar todas |
| | GET | `/atas/{id}` | Obter por ID |
| | POST | `/atas` | Criar nova |
| | PUT | `/atas/{id}` | Atualizar |
| | DELETE | `/atas/{id}` | Deletar |
| **Servidores** | GET | `/servidores` | Listar todos |
| | POST | `/servidores` | Criar novo |
| | PUT | `/servidores/{id}` | Atualizar |
| | DELETE | `/servidores/{id}` | Deletar |
| **Equipes** | GET | `/equipes-contrato` | Listar todas |
| | POST | `/equipes-contrato` | Criar nova |
| | PUT | `/equipes-contrato/{id}` | Atualizar |
| | DELETE | `/equipes-contrato/{id}` | Deletar |

## 📝 Exemplos de Uso

### Criar um Contrato

```bash
curl -X POST http://localhost:8081/api/contratos \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "001/2026",
    "ano": 2026,
    "dataInicio": "2026-01-01",
    "dataFim": "2026-12-31",
    "descricao": "Contrato de fornecimento",
    "valorTotal": 50000.00,
    "fornecedor": "Empresa XYZ LTDA"
  }'
```

**Resposta (201 Created):**
```json
{
  "id": 1,
  "numero": "001/2026",
  "ano": 2026,
  "dataInicio": "2026-01-01",
  "dataFim": "2026-12-31",
  "descricao": "Contrato de fornecimento",
  "valorTotal": 50000.00,
  "fornecedor": "Empresa XYZ LTDA"
}
```

### Listar Contratos

```bash
curl -X GET http://localhost:8081/api/contratos
```

## ⚙️ Configuração

### Variáveis de Ambiente (`.env`)

```env
DB_URL=jdbc:mysql://localhost:3306/contract_management?useSSL=false&serverTimezone=UTC
DB_USERNAME=app_user
DB_PASSWORD=senha_segura

MAIL_USERNAME=seu_email@gmail.com
MAIL_APP_PASSWORD=sua_app_password

SERVER_PORT=8081
SERVER_SERVLET_CONTEXT_PATH=/api

LOGGING_LEVEL_COM_CONTRACT_MANAGEMENT=DEBUG
```

### Configuração Spring (`application.yml`)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_APP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

server:
  port: ${SERVER_PORT:8081}
  servlet:
    context-path: ${SERVER_SERVLET_CONTEXT_PATH:/api}
```

## 📅 Scheduler de Notificações

**Arquivo**: `NotificacaoVencimentoScheduler.java`

Executa automaticamente às **08:00 AM** e **08:30 AM** diariamente.

- Verifica contratos e atas vencendo em **90, 60 e 30 dias**
- Envia e-mail para equipes vinculadas
- Registra notificações para evitar duplicatas

```
Alertas gerados:
├─ 90 dias antes do vencimento
├─ 60 dias antes do vencimento
└─ 30 dias antes do vencimento
```

## 📊 Modelo de Dados

```
CONTRATO ──(1:N)──┐
                  ├──► EQUIPE_CONTRATO ──(1:N)──► EQUIPE_MEMBRO ──(N:1)──► SERVIDOR
                  │                                                             ↓
ATA_REGISTRO_PRECO ┘                                                    SECRETARIA
```

### Principais Tabelas

| Tabela | Descrição |
|--------|-----------|
| `contrato` | Contratos em vigência |
| `ata_registro_preco` | Atas de Registro de Preço |
| `servidor` | Servidores/Funcionários |
| `secretaria` | Departamentos/Secretarias |
| `equipe_contrato` | Equipes dos contratos |
| `equipe_membro` | Membros das equipes |
| `notificacao_vencimento_enviada` | Log de notificações |

## 📚 Documentação

Swagger/OpenAPI disponível em:
```
http://localhost:8081/api/swagger-ui/index.html
```

## 🧪 Testes

```bash
# Executar testes unitários
mvn test

# Cobertura de código
mvn jacoco:report
```

## 📦 Build & Deploy

```bash
# Gerar JAR
mvn clean package

# Executar JAR
java -Duser.timezone=America/Sao_Paulo -jar target/api-0.0.1-SNAPSHOT.jar

# Build Docker
docker build -t contract-management-api:latest .

# Run Docker
docker run -d -p 8081:8081 \
  -e DB_URL=jdbc:mysql://mysql:3306/contract_management \
  contract-management-api:latest
```

## 📋 Scripts Maven

```bash
mvn clean compile     # Compilar
mvn spring-boot:run   # Executar (desenvolvimento)
mvn test              # Testes
mvn package           # Gerar JAR
mvn verify            # Testes de integração
```

## 🔧 Troubleshooting

| Problema | Solução |
|----------|---------|
| Conexão MySQL recusada | Verificar se MySQL está rodando: `mysql -u root -p` |
| Porta 8081 já em uso | `netstat -ano \| findstr :8081` (Windows) ou `lsof -i :8081` (Linux/Mac) |
| E-mail não enviado | Verificar credenciais no `.env` e permissões do Gmail |
| Swagger não carrega | Acessar `http://localhost:8081/api/swagger-ui/index.html` |
