# Règles ProGuard spécifiques à Tinda SMS (lot 0 : base).
# SQLCipher : conserver l'initialisation native.
-keep class net.zetetic.database.sqlcipher.* { *; }
