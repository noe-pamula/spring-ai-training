# Support technique — point de départ des TP Spring AI

Une seule application Java : Spring MVC REST + Thymeleaf + CSS/JavaScript locaux, servie sur le même port. Aucun serveur front, aucune base de données et aucune clé API nécessaires.

## Versions

- Spring Boot **4.1.1**, dernière version stable vérifiée le 1er octobre 2026.
- Java **25 LTS**, dernière version LTS. Java 27 est plus récent mais la compatibilité publiée de Boot 4.1.1 s’arrête à Java 26 : ce socle retient donc Java 25.
- Dépendances Spring Web MVC, Thymeleaf et validation gérées par le BOM Spring Boot.
- Maven 3.9.11 via le wrapper officiel fourni.

Sources : https://spring.io/projects/spring-boot ; https://docs.spring.io/spring-boot/system-requirements.html ; https://www.oracle.com/java/technologies/downloads/

## Démarrage

Installer un JDK 25 et configurer JAVA_HOME. Une connexion Internet est nécessaire au premier lancement pour télécharger Maven et les dépendances.

Linux / macOS :
```sh
./mvnw spring-boot:run
```
Windows PowerShell :
```powershell
.\mvnw.cmd spring-boot:run
```
Puis ouvrir http://localhost:8080.

## Démarrage avec Docker

Prérequis : installer Docker Desktop, ou un moteur Docker compatible, puis démarrer le moteur.

Depuis la racine du projet, construire l’image :

```sh
docker build -t spring-ai-training .
```

La construction utilise le Maven Wrapper du projet et exécute les tests avant de créer l’image finale. Elle nécessite une connexion Internet lors du premier lancement pour télécharger les images de base et les dépendances Maven.

Lancer ensuite l’application dans un conteneur :

```sh
docker run --rm --name spring-ai-training -p 8080:8080 spring-ai-training
```

Puis ouvrir http://localhost:8080. Les requêtes de `requests.http` peuvent également être exécutées contre l’application conteneurisée.

Pour utiliser un autre port sur la machine, par exemple `8081`, sans modifier le port interne de l’application :

```sh
docker run --rm --name spring-ai-training -p 8081:8080 spring-ai-training
```

L’application est alors accessible sur http://localhost:8081.

Arrêter le conteneur lancé au premier plan avec `Ctrl+C`. S’il tourne dans un autre terminal, utiliser :

```sh
docker stop spring-ai-training
```

Créer le JAR contenant toute l’application :
```sh
./mvnw clean verify
java -jar target/spring-ai-starter-1.0.0.jar
```
Sous Windows, remplacer `./mvnw` par `.\mvnw.cmd`.
Le port est configurable : `java -jar target/spring-ai-starter-1.0.0.jar --server.port=8081`.

## Parcours disponible

- Afficher, rechercher et filtrer les tickets issus du web service.
- Créer un ticket et changer son statut depuis l’interface.
- Consulter trois articles d’assistance issus du web service.
- Envoyer un message au chat : navigateur → `POST /api/chat`.

Les données fictives sont écrites directement dans `SupportService.java`. Les tickets créés/modifiés restent seulement en mémoire et sont réinitialisés au redémarrage. Les articles sont immuables. Le chat ne conserve pas d’historique côté serveur et n’appelle aucun modèle.

## API

| Méthode | Route | Usage |
|---|---|---|
| GET | `/api/tickets?status=OPEN&q=VPN` | Liste / filtres facultatifs |
| GET | `/api/tickets/{id}` | Détail |
| POST | `/api/tickets` | Création (201 + Location) |
| PATCH | `/api/tickets/{id}/status` | Changement de statut |
| GET | `/api/articles` | Liste des articles |
| GET | `/api/articles/{id}` | Détail d’un article |
| POST | `/api/chat` | Message et réponse simulée |

Statuts : `OPEN`, `IN_PROGRESS`, `RESOLVED`. Priorités : `LOW`, `NORMAL`, `HIGH`.
Corps JSON d’exemple : voir `requests.http` (IntelliJ / extension REST Client VS Code).
Les entrées sont validées : une requête invalide retourne 400, une ressource absente 404, avec ProblemDetail. Les textes du front sont insérés via `textContent`.

## Repères pour les TP

```text
src/main/java/fr/talosi/formation/support/
  SupportApplication.java
  model/       Ticket, Article
  service/     SupportService
  web/         PageController, SupportController, ApiExceptionHandler
src/main/resources/
  templates/index.html
  static/css/app.css
  static/js/app.js
  application.yml
```

Le projet ne préinstalle pas Spring AI : il doit fonctionner avant le premier TP, sans compte fournisseur. Les versions de Spring AI et leur matrice de compatibilité devront être vérifiées au moment de l’ajout.

Application pédagogique locale sans authentification : les données sont fictives. Ne pas la publier telle quelle avec des données réelles.

## Vérification

`./mvnw verify` exécute les tests HTTP avec serveur réel : page Thymeleaf, listes, filtre, création, changement de statut, erreurs de validation, 404 et chat simulé.

Validation de cette archive : compilation, packaging et test HTTP réussis avec Spring Boot 4.1.1 sur le JDK 17 disponible, via `mvn -Djava.version=17 clean verify`. La configuration livrée cible Java 25 ; l’exécution sur JDK 25 n’a pas pu être vérifiée ici. Le JavaScript passe `node --check`.
