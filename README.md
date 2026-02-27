# 🌐 API Gateway

<div align="center">
  <img src="https://img.shields.io/badge/Spring_Cloud-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Cloud" />
  <img src="https://img.shields.io/badge/Spring_WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring WebFlux" />
  <img src="https://img.shields.io/badge/Java_21-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens" alt="JWT" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
</div>

<br />

Ce dépôt contient le code source de l'**API Gateway** de l'écosystème **Product Trial**. Construit de manière réactive avec **Spring Cloud Gateway** et **WebFlux**, il sert de point d'entrée unique pour le frontend, route intelligemment les requêtes vers les microservices sous-jacents, et agit comme une barrière de sécurité globale.

## 📋 Table des matières
- [Fonctionnalités](#-fonctionnalités)
- [Architecture & Routage](#-architecture--routage)
- [Prérequis](#-prérequis)
- [Installation et Lancement (Local)](#-installation-et-lancement-local)
- [Lancement avec Docker](#-lancement-avec-docker)
- [Tests](#-tests)

---

## ✨ Fonctionnalités
- **Routage Dynamique** : Redirection des requêtes HTTP vers les bons microservices (`ms-auth` ou `product-backend`) en fonction de l'URI.
- **Sécurité Centralisée (Global Filter)** : Validation des tokens JWT à la volée via le `JwtValidationFilter`. Bloque les requêtes non autorisées avant même qu'elles n'atteignent les microservices.
- **Gestion des CORS** : Configuration globale des règles Cross-Origin (CORS) pour autoriser le frontend (`product-app.local`) à communiquer avec l'API.
- **Haute Performance (Réactif)** : Utilisation de Spring WebFlux (non-bloquant) pour gérer un grand nombre de requêtes simultanées avec une faible empreinte mémoire.
- **Monitoring** : Points de terminaison Actuator (`/actuator/health`, `/actuator/info`) activés pour la surveillance de l'état de la passerelle.

---

## 🗺 Architecture & Routage

L'API Gateway écoute sur le port `8080` et dispatche le trafic de la manière suivante (via `application.yaml`) :

| Chemin entrant | Microservice de destination | Service Kubernetes cible |
| :--- | :--- | :--- |
| `/api/ms-auth/**` | Microservice d'Authentification | `http://ms-auth-service:8082` |
| `/api/product-backend/**` | Backend principal (Produits/Paniers) | `http://product-backend-service:8080` |

> **Note sur la sécurité :** Toutes les routes exigent un token JWT valide (`Authorization: Bearer <token>`), à l'exception des routes publiques d'authentification définies dans le filtre (ex: `/v1/auth/login`, `/v1/accounts/register`).

---

## 🛠 Prérequis

Pour exécuter ce projet localement, assurez-vous d'avoir :
- **Java 21** (JDK 21)
- **Docker** (pour construire ou exécuter l'image)

---

## 🚀 Installation et Lancement (Local)

### 1. Cloner le projet
```bash
git clone [https://github.com/wang-tu-94/api-gateway.git](https://github.com/wang-tu-94/api-gateway.git)
cd api-gateway
```

### 2. Démarrer l'application avec Gradle
Utilisez le wrapper Gradle inclus pour démarrer la passerelle :
```bash
./gradlew bootRun
```
L'API Gateway sera lancée et écoutera sur `http://localhost:8080`. 
*Assurez-vous que vos microservices cibles tournent également pour que le routage aboutisse.*

---

## 🐳 Lancement avec Docker

Le projet inclut un `Dockerfile` basé sur Eclipse Temurin 21 (Alpine) et un workflow GitHub Actions qui pousse automatiquement l'image sur Docker Hub (`magnomos/api-gateway:latest`).

**1. Construire l'image localement :**
```bash
# Générer le JAR via Gradle
./gradlew bootJar

# Construire l'image Docker
docker build -t magnomos/api-gateway:latest .
```

**2. Exécuter le conteneur :**
```bash
docker run -d -
