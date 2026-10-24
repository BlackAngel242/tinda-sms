# Règles ProGuard spécifiques à Tinda SMS (lot 0 : base).
# SQLCipher : conserver l'initialisation native (le code natif
# libsqlcipher.so recherche les méthodes par leur nom exact via JNI ;
# sans cette règle, R8 les renomme et le chargement plante).
-keep class net.sqlcipher.** { *; }

# Tests instrumentés (CI) : androidx.test référence des annotations
# Error Prone absentes du classpath ; R8 ne doit pas échouer dessus.
-dontwarn com.google.errorprone.annotations.**
