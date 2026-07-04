# Michelin Guide — API Backend

Backend Spring Boot d'une plateforme de réservation d'hôtels et de restaurants "façon guide Michelin", enrichie par des assistants conversationnels IA (Groq). Le projet permet de rechercher, réserver et laisser un avis sur des hôtels et restaurants, avec un back-office d'administration intégré.

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Stack technique](#stack-technique)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Configuration](#configuration)
- [Lancer le projet](#lancer-le-projet)
- [Docker](#docker)
- [Documentation API](#documentation-api)
- [Authentification](#authentification)
- [Tests & CI/CD](#tests--cicd)

## Fonctionnalités

### Hôtels
- Recherche d'hôtels avec scoring (note, nombre d'avis, taux d'annulation, ADR, profil famille/business)
- Suggestions : nouveautés, recommandations "famille" et "business"
- Création de réservation, consultation des réservations par email
- Dépôt d'un avis ("avis") une fois la date du séjour passée

### Restaurants
- Recherche géolocalisée (PostGIS) avec scoring
- Création de réservation, consultation par email
- Dépôt d'un avis après la réservation

### Assistants IA (Groq, en français)
- Assistant conversationnel de recherche de restaurants
- Assistant conversationnel de recherche d'hôtels
- Planificateur de voyage (hôtel + restaurants) via chat
- Constructeur de profil utilisateur / préférences de voyage (avec lecture de profils sociaux)
- Réservation assistée par IA (hôtel et restaurant)

### Back-office (Thymeleaf)
- Authentification admin (restreinte à un utilisateur Supabase précis)
- Tableau de bord, import CSV d'hôtels/restaurants, création manuelle de réservations

## Stack technique

| Domaine | Technologie |
|---|---|
| Langage / Runtime | Java 21 |
| Framework | Spring Boot 3.5.13 |
| Build | Maven (wrapper inclus) |
| Base de données | PostgreSQL + PostGIS (hébergée sur Supabase) |
| Accès données | Spring Data JPA (hôtels, restaurants) + appels REST Supabase/PostgREST (users, bookings, reservations) |
| IA | Spring AI + Groq (API compatible OpenAI), fallback multi-modèles |
| Authentification | Supabase Auth, vérification JWT via JWKS (RS256) |
| Vues admin | Thymeleaf |
| Documentation API | springdoc-openapi (Swagger UI) |
| Autres | OpenCSV (import CSV), Pexels API (photos), Lombok |

## Architecture

```
src/main/java/com/example/demo/
├── ApiApplication.java      # point d'entrée
├── admin/                   # back-office (controllers, dto, service) - Thymeleaf
├── config/                  # configuration (CORS, Jackson, propriétés app)
├── controller/               # API REST publique (hôtels, restaurants, IA, auth)
├── dto/                      # objets de requête/réponse
├── entity/                   # entités JPA (Hotel, Restaurant)
├── repository/               # repositories Spring Data JPA (requêtes SQL natives)
├── security/                  # filtre JWT, configuration Spring Security
├── service/                   # logique métier, IA (Groq), Supabase, photos
└── supabaseAuth/               # intégration Supabase Auth + validation JWT
```

Deux modes d'accès aux données coexistent :
- **JPA/Hibernate** pour les tables `hotels` et `restaurants` (recherche, scoring, requêtes géospatiales PostGIS), sans migration automatique (`ddl-auto: none` — le schéma est géré côté Supabase).
- **API REST Supabase (PostgREST)** pour `users`, `hotel_bookings`, `restaurant_reservations`.

## Prérequis

- Java 21
- Maven (ou utiliser le wrapper `mvnw` / `mvnw.cmd` fourni)
- Un projet Supabase (base PostgreSQL + PostGIS, Auth activé)
- Une clé API Groq
- (Optionnel) une clé API Pexels pour les photos

## Configuration

Créer un fichier `.env` à la racine du projet (il est ignoré par git) avec les variables suivantes :

```env
# Supabase
SUPABASE_URL=
SUPABASE_KEY=
SUPABASE_ANON_KEY=
SUPABASE_SERVICE_KEY=

# Base de données (pooler Supabase)
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
DB_PASSWORD=

# IA (Groq)
GROQ_API_KEY=
GROQ_MODEL=llama-3.1-8b-instant   # optionnel

# Photos
PEXELS_API_KEY=

# Divers
API_URL=
PORT=8000
ADMIN_USER_ID=                     # UUID Supabase de l'admin
ADMIN_COOKIE_SECURE=false
APP_COOKIE_SECURE=false
APP_COOKIE_SAME_SITE=Lax
THYMELEAF_CACHE=false
```

`application.yml` importe automatiquement ce fichier (`spring.config.import: optional:file:.env[.properties]`).

## Lancer le projet

```bash
# Installer les dépendances et builder (sans tests)
./mvnw clean install -DskipTests

# Lancer l'application
./mvnw spring-boot:run

# Lancer les tests
./mvnw test
```

L'application démarre sur le port défini par `PORT` (8000 par défaut dans `.env`, 8080 sinon).

## Docker

```bash
docker build -t michelin-guide .
docker run -p 8000:8000 --env-file .env michelin-guide
```

Le `Dockerfile` utilise un build multi-stage (Maven → JRE Alpine).

## Documentation API

- **Swagger UI** : `/swagger-ui.html` (ou `/swagger-ui/index.html`)
- **OpenAPI JSON** : `/v3/api-docs`
- **Collection Postman** : [`hackaton.postman_collection.json`](./hackaton.postman_collection.json), à importer directement dans Postman pour tester tous les endpoints (auth, hôtels, restaurants, assistants IA).

## Authentification

- L'identité est gérée par **Supabase Auth** (inscription/connexion/déconnexion).
- L'API ne génère pas ses propres JWT : elle **vérifie les JWT émis par Supabase** via JWKS (clé publique RS256, cache local).
- Le token est transmis soit via l'en-tête `Authorization: Bearer <token>` (API REST), soit via un cookie httpOnly `admin_token` (back-office).
- Le rôle `ROLE_ADMIN` est attribué si le `sub` du token correspond à `ADMIN_USER_ID`.

> ⚠️ Note : dans la configuration actuelle, `SecurityConfig` autorise toutes les requêtes (`permitAll`) — le contrôle du rôle admin sur le back-office repose sur la logique applicative plutôt que sur des règles Spring Security `authorizeHttpRequests`. À renforcer avant une mise en production.

## Tests & CI/CD

- Tests : un test de démarrage du contexte Spring (`ApiApplicationTests`). La couverture fonctionnelle est à développer.
- **CI** (GitHub Actions, `.github/workflows/ci_cd.yml`) : build Maven + tests contre une base PostGIS de test, puis build de l'image Docker, sur push/PR vers `main`, `develop`, `feature/**`.
- **CD** : déploiement automatique sur [Render](https://render.com) via un webhook, déclenché uniquement sur `main`.
