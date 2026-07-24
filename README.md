# 📚 Manguys

**Manguys** est une application Android moderne et intuitive conçue pour tous les passionnés de mangas, d'animés, de séries TV, de films et de bandes dessinées. Elle vous permet de suivre l'avancement de vos lectures et visionnages facilement, en conservant l'intégralité de vos données en local sur votre appareil.

---

## ✨ Fonctionnalités Principales

### 📖 Suivi Complet de vos Média
* **Multi-catégories** : Gérez vos Mangas, Animés, Séries TV, Films, Books/Webtoons et plus encore.
* **Gestion des statuts** : *En cours*, *Terminé*, *En pause*, *Abandonné*, *À lire / voir*.
* **Compteurs de progression** : Suivi précis des chapitres, tomes, épisodes et saisons lus ou visionnés.
* **Évaluations et Notes** : Atribuez des notes et des appréciations personnelles à vos œuvres préférées.

### 📊 Statistiques & Analyses Visuelles
* Dashboard interactif récapitulant vos habitudes de consommation.
* Graphiques de répartition par catégorie, statut et niveau de complétion.
* Indicateurs clés sur le nombre total d'œuvres suivies et complétées.

### 📰 Actualités Otaku & Pop Culture
* Fil d'actualités intégré pour vous tenir informé des dernières nouveautés, sorties de chapitres et annonces d'animés.

### 🔒 Stockage Hors-Ligne & Confidentialité
* **100% Local (Room Database)** : Vos données restent sur votre smartphone, accessibles même sans connexion Internet.
* **Sauvegarde & Restauration** : Exportez et importez votre bibliothèque au format CSV ou JSON en toute simplicité.

### 🎨 Interface Soignée & Moderne
* Conçue avec **Jetpack Compose** et **Material Design 3**.
* Thème fluide, moderne et réactif adapté à toutes les tailles d'écran.

---

## 📲 Installation

### 🚀 Google Play Store
> 📢 **Prochainement disponible !**  
> L'application **Manguys** sera **bientôt disponible sur le Google Play Store** pour le grand public. Restez à l'affût des prochaines mises à jour du dépôt !

### 🛠️ Compilation à partir des sources (Dépôt GitHub)

Pour les développeurs ou les utilisateurs souhaitant tester l'application directement depuis le code source :

1. **Cloner le dépôt GitHub :**
   ```bash
   git clone https://github.com/votre-utilisateur/manguys.git
   cd manguys
   ```

2. **Ouvrir le projet dans Android Studio :**
   * Ouvrez Android Studio (version Ladybug ou plus récente recommandée).
   * Choisissez **Open** et sélectionnez le dossier du projet.

3. **Compiler et Lancer l'application :**
   * Attendez la fin de la synchronisation Gradle.
   * Connectez un appareil Android en mode débogage USB ou lancez un émulateur.
   * Cliquez sur **Run** (`Shift + F10`) ou exécutez la commande Gradle suivante :
     ```bash
     ./gradlew assembleDebug
     ```

---

## 🛠️ Technologies Utilisées

* **Langage** : [Kotlin](https://kotlinlang.org/)
* **UI Framework** : [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Architecture** : MVVM (Model-View-ViewModel) + Clean Architecture
* **Base de données** : [Room Database](https://developer.android.com/training/data-storage/room) (SQLite local)
* **Asynchronisme** : Kotlin Coroutines & StateFlow
* **Réseau** : Retrofit / Ktor (pour le fil d'actualités)

---

## 📄 Licence

Ce projet est sous licence open-source. Consultez le fichier `LICENSE` pour plus de détails.
