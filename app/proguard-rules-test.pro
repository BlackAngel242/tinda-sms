# Règles R8 du paquet de tests instrumentés (minifyDebugAndroidTestWithR8).
# AGP n'applique pas proguard-rules.pro de l'application au APK de
# test : ce fichier est référencé par testProguardFiles du build type
# debug. androidx.test référence des annotations Error Prone absentes
# du classpath ; sans ce -dontwarn, R8 échoue en CI sur des classes
# qui ne servent qu'à l'analyse statique.
-dontwarn com.google.errorprone.annotations.**

# Le runner d'instrumentation n'est référencé que par le manifeste
# du paquet de test : sans ces keeps, R8 le supprime et l'émulateur
# démarre « 0 test » avant de planter en ClassNotFoundException
# (AndroidJUnitRunner introuvable, lu dans le journal CI du run
# 40094925424). Les classes de test sont trouvées par réflexion :
# elles restent des racines, ainsi que le socle androidx.test.
-keep class androidx.test.runner.AndroidJUnitRunner { *; }
-keep class androidx.test.internal.** { *; }
-keep class androidx.test.ext.** { *; }
-keep class tech.dmampro.tinda.** { *; }
