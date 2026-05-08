# Auth Service TDD Plan

Este documento guia a implementacao incremental do `auth-service` usando o ciclo Red, Green, Refactor. Cada fatia deve ser implementada em commits pequenos, com testes escritos antes do codigo de producao.

## Principios

- Escrever primeiro o menor teste que expresse o comportamento desejado.
- Ver o teste falhar pelo motivo esperado.
- Implementar apenas o suficiente para passar.
- Refatorar nomes, duplicacoes e limites arquiteturais mantendo os testes verdes.
- Evitar expor senhas, access tokens ou refresh tokens em logs, eventos Kafka ou respostas de erro.
- Preferir dominio puro para regras de negocio e Spring apenas nas bordas.
- Usar eventos de dominio e outbox para publicar no Kafka apos commit da transacao.

## Arquitetura Alvo

Pacotes sugeridos:

```text
com.jobson.market.auth
  application
    usecase
    port
  domain
    event
    model
    service
  infrastructure
    crypto
    jwt
    kafka
    oauth
    persistence
    web
```

Responsabilidades:

- `domain`: entidades, value objects, regras puras e eventos.
- `application`: casos de uso e portas para repositorios, token, hash e eventos.
- `infrastructure`: JPA, Kafka, JWT, Spring Security, OAuth2 Google e controllers.

## Fatia 1: Dominio Puro

Objetivo: modelar os conceitos centrais sem Spring.

Classes iniciais:

- `Email`
- `Password`
- `User`
- `RefreshToken`
- `TokenFamily`

Red:

- `EmailTest`: aceita email valido e rejeita nulo, vazio e formato invalido.
- `PasswordTest`: rejeita senha fraca e aceita senha conforme politica.
- `UserTest`: cria usuario pendente/ativo com email normalizado.
- `RefreshTokenTest`: calcula expiracao, sabe se esta expirado ou revogado.
- `TokenFamilyTest`: revoga familia inteira quando detecta reuso.

Green:

- Implementar value objects e entidades sem persistencia.
- Usar mensagens de erro estaveis o suficiente para os testes.

Refactor:

- Remover duplicacao nas validacoes.
- Separar regras de politica de senha se comecar a crescer.

## Fatia 2: Registro

Objetivo: registrar usuario local com email e senha.

Use case:

- `RegisterUserUseCase`

Red:

- Deve registrar usuario com email e senha validos.
- Deve rejeitar email ja cadastrado.
- Deve salvar hash da senha, nunca senha em texto puro.
- Deve gravar evento `UserRegistered`.

Green:

- Criar portas:
  - `UserRepository`
  - `PasswordHasher`
  - `OutboxEventRepository`
- Implementar primeiro com fakes em testes unitarios.
- Depois adicionar adaptadores JPA e teste de integracao.

Refactor:

- Introduzir DTOs de command/result se o caso de uso estiver acoplado ao controller.
- Garantir transacao unica para usuario, credencial e outbox.

Evento:

```text
auth.user.registered.v1
```

Payload minimo:

```json
{
  "userId": "uuid",
  "email": "john@example.com"
}
```

## Fatia 3: Login Email/Senha

Objetivo: autenticar credenciais locais.

Use case:

- `LoginWithPasswordUseCase`

Red:

- Deve autenticar com senha valida.
- Deve rejeitar senha invalida.
- Deve rejeitar usuario inexistente sem revelar se o email existe.
- Deve publicar `LoginSucceeded` em sucesso.
- Deve publicar `LoginFailed` em falha.

Green:

- Criar porta `PasswordVerifier`.
- Retornar par de tokens por uma porta `TokenIssuer`, ainda fake no primeiro passo.
- Persistir metadados de sessao apenas quando necessario para refresh token.

Refactor:

- Normalizar erros para resposta HTTP generica.
- Preparar ponto de extensao para lockout e rate limit.

Depois:

- Adicionar lockout temporario por muitas falhas.
- Adicionar rate limit por IP/email usando Redis.

Eventos:

```text
auth.session.login_succeeded.v1
auth.session.login_failed.v1
auth.account.locked.v1
```

## Fatia 4: Emissao JWT

Objetivo: emitir access tokens JWT assinados e validaveis pelos outros servicos.

Red:

- Deve emitir token com `iss` correto.
- Deve emitir token com `aud` esperado.
- Deve incluir `sub`, `email`, `roles` ou `scope`.
- Deve incluir `iat`, `exp` e `jti`.
- Deve assinar usando chave com `kid`.
- Deve expirar em janela curta configuravel.

Green:

- Implementar `JwtTokenIssuer`.
- Usar chaves assimetricas e expor JWKS.
- Configurar propriedades:
  - `auth.jwt.issuer`
  - `auth.jwt.audience`
  - `auth.jwt.access-token-ttl`
  - `auth.jwt.key-id`

Refactor:

- Isolar relogio com `Clock` para testes deterministas.
- Separar claims publicas de detalhes internos.

Endpoints esperados:

```text
GET /.well-known/jwks.json
```

## Fatia 5: Refresh Token Rotation

Objetivo: permitir renovacao segura de sessoes.

Red:

- Refresh valido gera novo access token e novo refresh token.
- Refresh usado anteriormente nao pode ser usado de novo.
- Reuso de refresh antigo revoga a familia inteira.
- Refresh expirado e rejeitado.
- Refresh revogado e rejeitado.
- Tokens devem ser armazenados apenas como hash.

Green:

- Implementar `RefreshTokenService`.
- Criar repositorio para tokens/familias.
- Salvar `familyId`, `previousTokenId`, expiracao, revogacao e metadados.

Refactor:

- Separar token opaco bruto do registro persistido.
- Padronizar resposta de erro para token invalido.

Eventos:

```text
auth.session.refresh_token_rotated.v1
auth.session.refresh_token_reused.v1
```

## Fatia 6: Logout

Objetivo: encerrar sessao atual ou todas as sessoes do usuario.

Red:

- Logout da sessao atual revoga a familia informada.
- Logout global revoga todas as familias ativas do usuario.
- Logout deve ser idempotente.
- Deve publicar evento de sessao revogada.

Green:

- Implementar `LogoutUseCase`.
- Adicionar endpoints protegidos por JWT.

Refactor:

- Reutilizar servico de revogacao de token family.

Evento:

```text
auth.session.revoked.v1
```

## Fatia 7: RBAC e Perfis de Conta

Objetivo: suportar clientes e administradores com autorizacao clara, mantendo o `auth-service` responsavel por identidade e acesso, nao por regras detalhadas de assinatura ou perfil comercial.

Decisao de dominio:

- `auth-service` deve possuir roles, permissoes derivadas, status de conta e classificacao leve de conta.
- `customer-service` deve possuir dados ricos de perfil do cliente, como nome, telefone, endereco e preferencias.
- `subscription-service` deve possuir plano, ciclo de entrega, cesta, renovacao, pausas e estados de assinatura.
- O JWT pode carregar claims estaveis e pequenas para autorizacao, mas nao deve carregar detalhes mutaveis da assinatura.

Modelo inicial:

- `Role`: `CUSTOMER`, `ADMIN`, `SUPER_ADMIN`.
- `Permission`: acoes derivadas das roles para endpoints sensiveis.
- `AccountProfile`: `CUSTOMER`, `ADMIN`.
- `CustomerProfileType`: `INDIVIDUAL`, `HOUSEHOLD`, `BUSINESS`.

Regras:

- Registro publico cria usuario com role `CUSTOMER`, account profile `CUSTOMER` e customer profile type `INDIVIDUAL` por padrao.
- `SUPER_ADMIN` pode conceder e revogar roles administrativas.
- `ADMIN` pode acessar funcoes administrativas permitidas, mas nao pode conceder `ADMIN` ou `SUPER_ADMIN`.
- Um usuario pode ter multiplas roles; a role `CUSTOMER` nao precisa ser removida quando o usuario vira administrador.
- Um usuario nunca deve ficar sem role efetiva.
- Perfis de assinatura nao concedem permissoes administrativas. Eles apenas ajudam outros servicos a aplicar regras de produto.
- Tokens devem incluir `roles`, `permissions`, `account_profile` e, para clientes, `customer_profile_type`.
- Downstream services devem validar `roles` para autorizacao grosseira e consultar o servico dono quando precisarem de estado atualizado da assinatura.

Permissoes iniciais:

```text
AUTH_USER_READ
AUTH_USER_ROLE_ASSIGN
AUTH_USER_ROLE_REVOKE
AUTH_ADMIN_ACCESS
CUSTOMER_PROFILE_ACCESS
SUBSCRIPTION_MANAGE_OWN
```

Red:

- `UserTest`: registro publico deve criar usuario com role `CUSTOMER`.
- `UserTest`: usuario admin deve ser criado somente por factory explicita, nao pelo fluxo publico.
- `UserTest`: promover usuario para `ADMIN` deve adicionar role admin sem remover identidade original.
- `UserTest`: remover a ultima role efetiva deve ser rejeitado.
- `UserTest`: usuario suspenso nao deve ser elegivel para login.
- `PermissionPolicyTest`: `CUSTOMER` deve mapear para permissoes de cliente e assinatura propria.
- `PermissionPolicyTest`: `ADMIN` deve mapear para acesso administrativo sem permissao de conceder `SUPER_ADMIN`.
- `PermissionPolicyTest`: `SUPER_ADMIN` deve mapear para concessao e revogacao de roles.
- `JwtTokenIssuerTest`: access token deve incluir claim `roles` como lista.
- `JwtTokenIssuerTest`: access token deve incluir claim `permissions` como lista.
- `JwtTokenIssuerTest`: access token deve incluir claim `account_profile`.
- `JwtTokenIssuerTest`: access token de cliente deve incluir `customer_profile_type`.
- `RegisterUserUseCaseTest`: resultado de registro deve retornar role `CUSTOMER`.
- `LoginWithPasswordUseCaseTest`: login de usuario suspenso deve falhar com erro generico.
- `AdminUserManagementUseCaseTest`: `SUPER_ADMIN` pode promover cliente para admin.
- `AdminUserManagementUseCaseTest`: `ADMIN` nao pode promover cliente para admin.
- `AdminUserManagementUseCaseTest`: cliente nao pode promover outro usuario.
- `AuthControllerTest`: endpoint admin deve responder `403` para cliente autenticado.

Green:

- Adicionar value objects/enums:
  - `Role`
  - `Permission`
  - `AccountProfile`
  - `CustomerProfileType`
- Estender `User` com roles, account profile e customer profile type opcional.
- Criar `PermissionPolicy` para derivar permissoes a partir das roles.
- Persistir roles em tabela separada `user_roles` ou coluna JSON/array. Preferir `user_roles` para consultas e constraints simples em PostgreSQL.
- Adicionar colunas pequenas em `users`:
  - `account_profile`
  - `customer_profile_type`
- Atualizar `JwtTokenIssuer` para emitir claims:
  - `roles: ["CUSTOMER"]`
  - `permissions: ["CUSTOMER_PROFILE_ACCESS", "SUBSCRIPTION_MANAGE_OWN"]`
  - `scope: "auth:user"` inicialmente, mantendo compatibilidade
  - `account_profile: "CUSTOMER"`
  - `customer_profile_type: "INDIVIDUAL"`
- Configurar Spring Security com `JwtAuthenticationConverter` mapeando `roles` para authorities `ROLE_CUSTOMER`, `ROLE_ADMIN` e `ROLE_SUPER_ADMIN`, alem de mapear `permissions` para authorities sem prefixo.
- Criar `AdminUserManagementUseCase` com portas existentes de usuario e evento.
- Proteger endpoints administrativos com `hasAuthority("AUTH_USER_ROLE_ASSIGN")`, `hasAuthority("AUTH_USER_ROLE_REVOKE")` ou `hasRole("ADMIN")` apenas quando a regra for ampla.

Refactor:

- Separar permissao de papel: roles ficam persistidas; permissoes sao derivadas por politica e entram no token.
- Evitar strings soltas em claims criando constantes de nomes de claims.
- Evitar que controllers decidam regras de negocio; controller aplica protecao HTTP, use case valida invariantes.
- Revisar BDD features para alinhar `role "CUSTOMER"` com o contrato real de resposta.

Eventos:

```text
auth.user.role_assigned.v1
auth.user.role_removed.v1
auth.user.account_suspended.v1
auth.user.account_reactivated.v1
auth.user.customer_profile_type_changed.v1
```

Payload minimo para role:

```json
{
  "userId": "uuid",
  "role": "ADMIN",
  "changedBy": "admin-user-uuid"
}
```

Endpoints administrativos alvo:

```text
POST /auth/admin/users/{userId}/roles
DELETE /auth/admin/users/{userId}/roles/{role}
POST /auth/admin/users/{userId}/suspend
POST /auth/admin/users/{userId}/reactivate
```

Bootstrap administrativo:

- Nao criar endpoint publico para cadastrar admin.
- Registrar o primeiro usuario pelo fluxo normal.
- Promover o primeiro administrador por comando operacional auditavel, habilitado apenas quando `AUTH_BOOTSTRAP_ADMIN_EMAIL` estiver definido.
- O comando deve ser idempotente, falhar se o usuario nao existir e publicar evento de role atribuida.

Endpoint de perfil de cliente alvo:

```text
PATCH /auth/me/customer-profile-type
```

Observacao: esse endpoint so altera a classificacao leve usada por auth e clientes. A assinatura real continua em `subscription-service`.

## Fatia 8: Google Login

Objetivo: autenticar usuarios via conta Google usando OIDC.

Red:

- Dado um `OidcUser` com `sub` do Google, deve criar usuario local se nao existir.
- Deve vincular conta Google a usuario existente quando email verificado combinar.
- Deve autenticar usuario ja vinculado pelo `providerSubject`.
- Deve rejeitar email nao verificado quando a politica exigir.
- Deve publicar evento de conta Google vinculada.

Green:

- Criar modelo `OAuthAccount`.
- Criar porta `OAuthAccountRepository`.
- Implementar servico de vinculacao usando `provider = google` e `providerSubject = sub`.
- Depois integrar com Spring OAuth2 Client.

Refactor:

- Nunca usar email como identificador principal do Google; usar `sub`.
- Isolar mapeamento de claims OIDC.

Eventos:

```text
auth.user.google_account_linked.v1
auth.session.login_succeeded.v1
```

Endpoints Spring esperados:

```text
GET /oauth2/authorization/google
GET /login/oauth2/code/google
```

## Fatia 9: Outbox e Kafka

Objetivo: publicar eventos de auth com confiabilidade.

Red:

- Cada use case deve gravar evento na outbox na mesma transacao.
- Publisher deve publicar eventos pendentes no topico correto.
- Publisher deve marcar evento como publicado apos sucesso.
- Publisher deve preservar evento pendente em falha.

Green:

- Criar tabela `outbox_events`.
- Criar `OutboxEventRepository`.
- Criar `KafkaOutboxPublisher`.
- Usar payload JSON versionado.

Refactor:

- Padronizar envelope de evento.
- Centralizar nomes de topicos.
- Adicionar correlation id.

Envelope:

```json
{
  "eventId": "uuid",
  "eventType": "UserRegistered",
  "version": 1,
  "occurredAt": "2026-05-05T18:00:00Z",
  "correlationId": "uuid",
  "payload": {}
}
```

Topicos iniciais:

```text
auth.user.registered.v1
auth.user.email_verified.v1
auth.user.role_assigned.v1
auth.user.role_removed.v1
auth.user.account_suspended.v1
auth.user.account_reactivated.v1
auth.user.customer_profile_type_changed.v1
auth.user.password_changed.v1
auth.user.password_reset_requested.v1
auth.user.password_reset_completed.v1
auth.user.google_account_linked.v1
auth.session.login_succeeded.v1
auth.session.login_failed.v1
auth.session.refresh_token_rotated.v1
auth.session.refresh_token_reused.v1
auth.session.revoked.v1
auth.account.locked.v1
auth.account.unlocked.v1
```

## Banco de Dados Inicial

Tabelas candidatas:

```text
users
user_roles
password_credentials
oauth_accounts
refresh_token_families
refresh_tokens
outbox_events
```

Regras:

- Email deve ser unico por usuario local.
- `users.account_profile` deve indicar `CUSTOMER` ou `ADMIN`.
- `users.customer_profile_type` deve ser preenchido para clientes e nulo para administradores puros.
- `user_roles(user_id, role)` deve ser unico.
- `oauth_accounts(provider, provider_subject)` deve ser unico.
- Refresh token deve ser persistido como hash.
- Eventos da outbox nao devem conter segredos.

## Endpoints Alvo

```text
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/logout
POST /auth/logout-all
GET  /auth/me
POST /auth/admin/users/{userId}/roles
DELETE /auth/admin/users/{userId}/roles/{role}
POST /auth/admin/users/{userId}/suspend
POST /auth/admin/users/{userId}/reactivate
PATCH /auth/me/customer-profile-type
GET  /.well-known/jwks.json
GET  /oauth2/authorization/google
GET  /login/oauth2/code/google
```

## Checklist de Seguranca

- Hash de senha com `PasswordEncoder` do Spring Security.
- Politica minima de senha no dominio.
- Access token curto.
- Claims de roles pequenas, estaveis e sem dados sensiveis de assinatura.
- Validar permissoes no gateway/servico usando authorities derivadas de `permissions`; usar roles para autorizacao ampla.
- Refresh token opaco, rotacionavel e armazenado como hash.
- Reuso de refresh token revoga a familia inteira.
- Cookies de refresh com `HttpOnly`, `Secure` e `SameSite`.
- Erros de login genericos.
- Rate limit em login, refresh e reset de senha.
- Logs sem tokens, senhas ou dados sensiveis.
- Eventos Kafka sem segredos.
- Chaves JWT com `kid` e plano de rotacao.
- Validar `iss`, `aud`, expiracao e assinatura nos demais servicos.

## Ordem Recomendada de Implementacao

1. Dominio puro.
2. Registro com outbox fake.
3. Login email/senha com token fake.
4. JWT real e JWKS.
5. Refresh token rotation.
6. Logout.
7. RBAC, permissoes e perfis de conta nos tokens.
8. Google login.
9. Outbox/Kafka real.
