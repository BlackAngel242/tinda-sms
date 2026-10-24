# Règles R8 du paquet de tests instrumentés (minifyDebugAndroidTestWithR8).
# AGP n'applique pas proguard-rules.pro de l'application au APK de
# test : ce fichier est référencé par testProguardFiles du build type
# debug. androidx.test référence des annotations Error Prone absentes
# du classpath ; sans ce -dontwarn, R8 échoue en CI sur des classes
# qui ne servent qu'à l'analyse statique.
-dontwarn com.google.errorprone.annotations.**
