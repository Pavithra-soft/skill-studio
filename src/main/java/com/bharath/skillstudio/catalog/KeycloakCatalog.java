package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class KeycloakCatalog {

    private KeycloakCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("keycloak", "Keycloak",
                "Keycloak is the identity box: realms, clients, tokens, and the flows your apps should not reinvent.",
                Concepts.of("Realms and clients",
                                "A realm is a tenant of users. A client is an app. Mixing them is how tokens leak across products.",
                                "A personal job app and a team dashboard should not share a realm secret. Each client has its own id, secret or PKCE, and redirect URIs.")
                        .depth("""
                                Realms isolate users, roles, and identity providers. Clients are applications in a realm: confidential (server-side secret) or public (PKCE). Redirect URIs are a security boundary — wildcards are how tokens get stolen.

                                A token issued for client A should fail audience checks on API B. Separate clients even if the users are the same people.

                                In interviews, realm versus client versus user, and why one realm for two products is a leak path.
                                """)
                        .qa("Realm or client for a second app?",
                                "Same company, same users: often the same realm, new client, mapped roles. Different product/tenancy: new realm. Never share client secrets.",
                                "What is a confidential client?")
                        .qa("Why pin redirect URIs?",
                                "The authorization code is useless without a matching redirect. A wildcard URI is an open redirect into a token theft.",
                                "Can two clients share a secret?")
                        .sample("Names as documentation",
                                """
                                        realm=job-monitor
                                        client=job-monitor-ui
                                        """,
                                "A future dashboard gets its own client."),
                Concepts.of("Access versus refresh tokens",
                                "Access tokens are short and presented to APIs. Refresh tokens stay with a confidential client. Browsers should not hold refresh tokens in localStorage.",
                                "A SPA on the public internet uses Authorization Code with PKCE. A server BFF holds the refresh token in a server session.")
                        .depth("""
                                Access tokens (often JWT) go to resource servers. Minutes of life. Refresh tokens get new access tokens; they are long-lived and precious. Public clients use PKCE and typically avoid long-lived refresh in JS storage.

                                Resource servers never see the refresh token. Password grant is obsolete. Implicit grant is obsolete. Authorization code is the default.

                                In interviews, which token goes to the API, where refresh lives, and why localStorage is a XSS gift.
                                """)
                        .qa("Where does the refresh token live in a SPA?",
                                "Prefer a BFF cookie session. If the SPA must, use careful PKCE and short rotation — not localStorage as a default.",
                                "Does the API validate the refresh token?")
                        .qa("Why is the password grant a bad idea?",
                                "The app sees the password. You skip SSO, MFA, and consent. Authorization code plus PKCE is the replacement.",
                                "How short should access TTL be?")
                        .sample("Code plus PKCE",
                                """
                                        // public client: authorization_code + PKCE
                                        // confidential BFF: authorization_code + secret, refresh in server session
                                        """,
                                "No password grant."),
                Concepts.of("Roles and groups",
                                "Realm roles are global. Client roles are per app. Map them once in the token mapper so Spring Security sees ROLE_ names.",
                                "An apply-queue admin is a client role, not a new user table in the app's database.")
                        .depth("""
                                Groups are collections of users; roles are permissions. Keycloak can map groups to roles. Spring's hasRole looks for ROLE_ prefix unless you change it. Token mappers add realm_access.roles or resource_access.<client>.roles into the JWT.

                                Do not invent a parallel user table that copies roles. Look up profile data if you must; authorization data belongs in the token or in the IdP.

                                In interviews, realm versus client roles, and the mapper.
                                """)
                        .qa("Where should an app-specific admin role live?",
                                "A client role on that app, mapped into the access token. Not a realm-wide god role unless it truly is.",
                                "How does Spring see it?")
                        .qa("Why not store roles only in your DB?",
                                "You will drift from the IdP, and every service will disagree. The token is the ticket. App-specific profiles can still live in your DB.",
                                "Groups versus roles?")
                        .sample("JWT claim path",
                                """
                                        realm_access.roles  // e.g. ["admin"]
                                        resource_access.job-monitor-api.roles
                                        """,
                                "Mappers decide which of these exist."),
                Concepts.of("Identity brokering",
                                "Keycloak can sit in front of Google or GitHub. Your app still sees one JWT shape.",
                                "Candidates sign in with Google. Recruiters stay on the realm. One resource server.")
                        .depth("""
                                An identity provider (IdP) in Keycloak federates. Users arrive with an upstream token; Keycloak issues its own. Mappers copy email, name, and maybe groups. First-broker login can link accounts by email — that is a product decision with account-takeover risk if email is unverified.

                                Your Spring resource server still validates Keycloak as issuer. It should not learn Google's token format.

                                In interviews, one JWT shape, and the linking pitfall.
                                """)
                        .qa("Does the API validate Google's JWT?",
                                "No. It validates Keycloak's. Brokering is Keycloak's job.",
                                "What is first-broker login?")
                        .qa("Why not call Google from every microservice?",
                                "Each would need client config and would see different claims. One broker, one issuer, many clients.",
                                "Unverified email linking risk?")
                        .sample("Issuer stays Keycloak",
                                """
                                        spring.security.oauth2.resourceserver.jwt.issuer-uri=https://keycloak/realms/job-monitor
                                        """,
                                "Google is behind the broker."),
                Concepts.of("Token hygiene",
                                "Rotate client secrets. Short access TTL. Revoke refresh on logout. Never log the token.",
                                "A leaked JWT from a log aggregator should already be expired before anyone finds it.")
                        .depth("""
                                accessTokenLifespan in minutes. Refresh rotation (reuse detection) kills stolen refresh tokens. Admin events and user logout revoke sessions. Offline tokens are extra-long refresh — treat them as credentials.

                                Logs, APM, and support tickets: redact Bearer. Actuator dumps should not include Authorization headers.

                                In interviews, TTL, rotation, revocation, and a log leak.
                                """)
                        .qa("The access token leaked in logs. Now what?",
                                "It should already be close to expiry. Rotate the client if it was a refresh or a secret. Short TTL is the mitigation you already wanted.",
                                "What is refresh token reuse detection?")
                        .qa("Logout options?",
                                "Keycloak end-session endpoint, back-channel logout to clients, and local session clear. Doing only local clear leaves the refresh token alive.",
                                "Offline tokens?")
                        .sample("Minutes, not hours",
                                """
                                        // realm settings: Access Token Lifespan = 5 minutes
                                        """,
                                "Refresh lives in the BFF."),
                Concepts.of("PKCE",
                                "Proof Key for Code Exchange stops an intercepted authorization code from being exchanged without the verifier. Public clients must use it.",
                                "A desktop or SPA client without a secret uses PKCE. A stolen code from a redirect is not enough.")
                        .depth("""
                                The client creates a verifier, sends a challenge (S256 hash) with the auth request, and sends the verifier on the token request. The broker checks they match. Authorization code intercept (custom URL schemes, loose redirects) is the threat.

                                Confidential clients can use PKCE too; it is not harmful. Skipping PKCE on a public client is a finding.

                                In interviews, challenge versus verifier, and public clients.
                                """)
                        .qa("What does PKCE protect?",
                                "An attacker who steals the authorization code but not the verifier cannot exchange it for tokens.",
                                "Do confidential clients need it?")
                        .qa("plain versus S256?",
                                "S256. plain is only for clients that cannot hash, which is not your Java or modern SPA client.",
                                "Where is the verifier stored?")
                        .sample("S256 challenge",
                                """
                                        // code_verifier: 43-128 chars
                                        // code_challenge: BASE64URL(SHA256(verifier))
                                        """,
                                "Sent on authorize, proven on token."),
                Concepts.of("Token mappers and claims",
                                "Mappers decide what lands in the JWT. Too many claims bloat the token. Too few force extra lookups. Audience must include the API.",
                                "Spring Security ignored roles until a mapper added them. A second API rejected the token until aud included it.")
                        .depth("""
                                Built-in mappers: audience, roles, username, protocol mappers for custom claims. Keep tokens small; they ride on every request. Do not put PII you do not need. Audience mapper is how a token meant for the UI fails on the API if you configured it that way — or succeeds if you add the API as audience.

                                Changing claims is a contract with resource servers. Version thoughtfully.

                                In interviews, aud, roles, and token size.
                                """)
                        .qa("API returns 401 with a valid-looking JWT. First check?",
                                "iss, aud, exp, and signature (JWKs). Wrong audience is the usual 'looks valid in jwt.io' miss — jwt.io does not know your aud.",
                                "Should you put the email in every token?")
                        .qa("Realm roles not showing in Spring?",
                                "Mapper missing, or prefix mismatch (ROLE_). Inspect the access token claims, do not guess the Security config first.",
                                "Why keep tokens small?")
                        .sample("Audience for the API",
                                """
                                        // Keycloak client scope: audience mapper -> job-monitor-api
                                        """,
                                "Resource server checks aud."),
                Concepts.of("Sessions and logout",
                                "Keycloak sessions are the source of SSO. Logout must hit the broker, not only delete a local cookie.",
                                "A user logged out of the UI and still had a live refresh token until end-session was called.")
                        .depth("""
                                SSO is a Keycloak session plus client sessions. Remember-me and offline sessions extend life. Logout: OIDC end-session, optional id_token_hint, post_logout_redirect_uri that must be registered.

                                Back-channel logout notifies other clients. Front-channel is a browser hop. Local cookie delete without broker logout is 'looks logged out' until the next silent refresh.

                                In interviews, SSO session versus app session, and a complete logout.
                                """)
                        .qa("I deleted the app cookie. Are they logged out?",
                                "Not from Keycloak. The next visit can SSO silently. Call end-session.",
                                "What is back-channel logout?")
                        .qa("Why register post_logout_redirect_uri?",
                                "Same reason as login redirects: do not send the browser (and leftover query params) to an attacker.",
                                "Remember-me risk?")
                        .sample("End session",
                                """
                                        GET {issuer}/protocol/openid-connect/logout
                                            ?id_token_hint=...
                                            &post_logout_redirect_uri=https://app/example
                                        """,
                                "URI must be pre-registered.")
        );
    }
}
