# Règles ProGuard spécifiques à Tinda SMS (lot 0 : base).
# SQLCipher : conserver l'initialisation native (le code natif
# libsqlcipher.so recherche les méthodes par leur nom exact via JNI ;
# sans cette règle, R8 les renomme et le chargement plante).
-keep class net.sqlcipher.** { *; }
