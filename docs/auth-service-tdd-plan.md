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

## Fatia 7: Google Login

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

## Fatia 8: Outbox e Kafka

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
password_credentials
oauth_accounts
refresh_token_families
refresh_tokens
outbox_events
```

Regras:

- Email deve ser unico por usuario local.
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
GET  /.well-known/jwks.json
GET  /oauth2/authorization/google
GET  /login/oauth2/code/google
```

## Checklist de Seguranca

- Hash de senha com `PasswordEncoder` do Spring Security.
- Politica minima de senha no dominio.
- Access token curto.
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
7. Google login.
8. Outbox/Kafka real.

