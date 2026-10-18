pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Dépôt local pré-rempli (optionnel) : utilisé dans l'environnement de build
        // sandbox où le proxy est peu fiable pour les gros téléchargements.
        // Absent ailleurs : Gradle bascule alors sur les dépôts distants.
        // Voir ~/workspace/fetch_deps.py (non versionné).
        val localDeps = file("/home/hatch/workspace/.deps-m2")
        if (localDeps.isDirectory) {
            maven {
                name = "LocalDeps"
                url = localDeps.toURI()
            }
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "Tinda SMS"
include(":app")
