# EcoBudget 🌿
Dépôt de base pour le projet du cours de développement mobile avancé.

## Migration Kotlin Multiplatform (KMP)

Ce document détaille la synthès technique de la migration de l'application **EcoBudget** d'une architecture Android native classique vers une architecture **Kotlin Multiplatform (KMP)**.

---

## 1. Contexte & Objectifs Généraux

L'application EcoBudget était initialement développée selon une architecture Android native classique. L'objectif principal de ce projet a été d'extraire l'intégralité de la couche domaine, de la couche données et de la logique de présentation (`ViewModel` & `StateFlow`) pour les migrer au sein d'un module partagé multiplateforme unique : `:shared (commonMain)`.

Cette démarche offre plusieurs avantages majeurs :
* **Base de code métier unique :** 100 % réutilisable sur les cibles Android et iOS.
* **Conservation de l'UI native :** Maintien de Jetpack Compose sur Android et intégration directe avec SwiftUI sur iOS.
* **Réduction de la dette technique :** Centralisation de la logique financière, des modèles et du filtrage des données.

---

## 2. Architecture & Topologie du Module Partagé

La structure du projet a été réorganisée afin d'isoler la logique purement Kotlin de l'interface utilisateur native :

```text
devma-2025-2026-projet-master/
├── app/                             # Application Android Native (Jetpack Compose UI)
└── shared/                          # Module Partagé KMP
    ├── build.gradle.kts
    └── src/
        ├── commonMain/kotlin/com/example/ # Logique métier commune (Kotlin Pur)
        │   ├── model/                  # Modèles de domaine
        │   ├── data/repository/        # Repositories & sources de données
        │   ├── viewmodel/              # ViewModel & État UI
        │   └── util/                   # Abstractions expect
        ├── androidMain/kotlin/com/example/ # Implémentations actual pour Android
        └── iosMain/kotlin/com/example/     # Implémentations actual pour iOS
```

---

## 3. Documentation Technique des Fichiers Migrés vers `commonMain`

Chaque composant migré vers `commonMain` a fait l'objet d'un audit et d'un refactoring technique strict afin de garantir l'absence de tout couplage avec le SDK Android ou le runtime JVM Java.

### A. Modèle `Category.kt`
* **Emplacement cible :** `shared/src/commonMain/kotlin/com/example/model/Category.kt`
* **🔴 Problème rencontré :** Aucune dépendance spécifique à Android ou à la JVM n'a été identifiée dans ce modèle. Category.kt repose uniquement sur des fonctionnalités Kotlin compatibles avec les différentes cibles de Kotlin Multiplatform.
* **🟢 Choix technique appliqué :** Le fichier a été déplacé directement vers : shared/src/commonMain/kotlin/com/example/model/Category.kt. Aucune abstraction expect / actual n'est nécessaire.
* **🔵 Justification :** Le modèle étant constitué exclusivement de code Kotlin portable, il peut être compilé aussi bien pour la cible Android/JVM que pour Kotlin/Native/iOS. Le déplacement direct dans commonMain permet donc de partager le modèle sans introduire de dépendance spécifique à une plateforme.

---

### B. Modèle `Transaction.kt`
* **Emplacement cible :** `shared/src/commonMain/kotlin/com/example/model/Transaction.kt`
* **🔴 Problème rencontré :** La classe de données native utilisait `java.util.UUID.randomUUID().toString()`. Le package `java.util.UUID` est issu du JDK (JVM) et n'existe pas dans le moteur d'exécution Native / LLVM d'iOS, provoquant une erreur de compilation bloquante dans `commonMain`.
* **🟢 Choix technique appliqué :** Création d'une fonction d'abstraction `expect fun generateUUID(): String` dans `commonMain/util/UUID.kt`. L'implémentation `actual` utilise `java.util.UUID.randomUUID().toString()` dans `androidMain` et `platform.Foundation.NSUUID().UUIDString()` dans `iosMain`.
* **🔵 Justification :** Délègue la génération d'identifiants aux API système natives de chaque plateforme sans ajouter de dépendances lourdes, garantissant une compatibilité à 100 % sans violer les contraintes de `commonMain`.

---

### C. Modèle `YearMonth.kt`
* **Emplacement cible :** `shared/src/commonMain/kotlin/com/example/model/YearMonth.kt`
* **🔴 Problème rencontré :** S'appuyait sur `java.util.Calendar` pour calculer la date courante (`YearMonth.now()`) et effectuer la navigation mensuelle (`previous()` et `next()`). Classe JVM incompatible avec Kotlin/Native iOS.
* **🟢 Choix technique appliqué :** Réécriture complète du modèle à l'aide de la bibliothèque officielle `kotlinx-datetime` (version 0.6.1) fournie par JetBrains. L'instanciation s'effectue désormais via `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())`.
* **🔵 Justification :** `kotlinx-datetime` est la bibliothèque multiplateforme de référence. Elle offre des abstractions temporelles légères, immuables et identiques sur JVM, iOS et JS, évitant le recours à des blocs `expect`/`actual` faits maison.

---

### D. Dépôt de Données `TransactionRepositorykt`
* **Emplacement cible :** `shared/src/commonMain/kotlin/com/example/data/repository/TransactionRepository.kt`
* **🔴 Problème rencontré :** Dépendances et comportements spécifiques à Android incompatibles avec `commonMain`.
* **🟢 Choix technique appliqué :** Déplacement du contrat TransactionRepository dans commonMain avec `Flow`, `StateFlow` et `MutableStateFlow` fournies par `kotlinx-coroutines-core`.
* **🔵 Justification :** Le repository devient multiplateforme et découplé des API Android..

---

### E. Dépôt de Données `FakeTransactionRepository.kt`
* **Emplacement cible :** `shared/src/commonMain/kotlin/com/example/data/repository/FakeTransactionRepository.kt`
* **🔴 *Problème rencontré :** Utilisait `System.currentTimeMillis()` pour dater les transactions de démonstration, ainsi que `java.util.Calendar` pour le filtrage par année et mois.
* **🟢 Choix technique appliqué :** Remplacement de `System.currentTimeMillis()` par une fonction `expect fun getCurrentTimeMillis(): Long` (implémentée via `System.currentTimeMillis()` sur Android et `NSDate().timeIntervalSince1970 * 1000` sur iOS). Utilisation des objets `Instant` et `LocalDateTime` de `kotlinx-datetime` pour le filtrage par mois/année.
* **🔵 Justification :** Le filtrage et la génération du timestamp s'appuient désormais exclusivement sur du code Kotlin pur et des appels système portables. La réactivité avec `kotlinx.coroutines.flow.Flow` et `MutableStateFlow` reste entièrement fonctionnelle.

---

### F. Couche de Présentation `EcoBudgetViewModel.kt` & `EcoBudgetUiState.kt`
* **Emplacement cible :** `shared/src/commonMain/kotlin/com/example/viewmodel/`
* **🔴 Problème rencontré :** `EcoBudgetViewModel` dérivait directement de `androidx.lifecycle.ViewModel` issu du SDK Android, ce qui empêchait son déplacement vers `commonMain`.
* **🟢 Choix technique appliqué :** Intégration de la bibliothèque officielle `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` (version 2.8.6) dans les dépendances `commonMain` du module `:shared`.
* **🔵 Justification :** Cette bibliothèque unifie l'API ViewModel et la portée `viewModelScope` pour KMP. Sur Android, elle s'intègre nativement au cycle de vie Jetpack, tandis que sur iOS, l'état exposed via `StateFlow` est consommé réactivement par SwiftUI.

---

## 4. Synthèse des Bibliothèques & Outils Retenus

| Composant / Fonctionnalité | Ancienne implémentation (Android Native) | Nouvelle solution KMP (`commonMain`) | Justification Technique |
| :--- | :--- | :--- | :--- |
| **Génération d'UUID** | `java.util.UUID` | `expect` / `actual` (`generateUUID`) | Maintien des API natives sans dépendance tierce. |
| **Horodatage système** | `System.currentTimeMillis()` | `expect` / `actual` (`getCurrentTimeMillis`) | Abstraction portable du temps système. |
| **Gestion du temps / Dates** | `java.util.Calendar` | `org.jetbrains.kotlinx:kotlinx-datetime` (0.6.1) | Standard officiel KMP pour la manipulation temporelle. |
| **ViewModel & Scope** | `androidx.lifecycle:lifecycle-viewmodel-compose` | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` (2.8.6) | Gestion d'état unifiée et compatible Android & iOS. |
| **Programmation Réactive** | Kotlin Coroutines Android (`kotlinx-coroutines-android`) | Kotlin Coroutines Core (`kotlinx-coroutines-core`) | Gestion asynchrone multiplateforme via `StateFlow` / `Flow`. |

---

## 5. Bilan et Conformité des Évaluations

* **Isolation Métier (100 %) :** L'intégralité du calcul des budgets (Solde, Total Revenus, Total Dépenses), des règles de filtrage mensuel et du stockage en mémoire réside exclusivement dans `:shared:commonMain`.
* **Absence de régression :** L'application Android native conserve son interface d'origine (Jetpack Compose) tout en consommant de manière transparente les données et le ViewModel issus du module partagé `:shared`.
* **Préparation iOS :** Le module `:shared` compile avec succès pour les cibles iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`), produisant un framework statique directement intégrable dans un projet Xcode.