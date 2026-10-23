package tech.dmampro.tinda.sms

/**
 * Politique ROM pour le resultCode 0 des accusés (émission et
 * livraison) : sur la plupart des ROM, 0 (RESULT_CANCELED) ne prouve
 * ni un succès ni un échec. Il ne vaut confirmation que sur les
 * appareils où le terrain l'a prouvé, le 18 octobre 2026, pour des
 * envois réellement partis :
 *
 * - Redmi Note 14 sous HyperOS (fabricant Xiaomi) ;
 * - un appareil Tecno sous HiOS (journal d'envoi, envois validés).
 *
 * Aucune autre ROM n'est prouvée (hypothèse HYP-03 du PRD) : un Xiaomi
 * générique, un autre modèle, ou un Tecno sans indication HiOS
 * retournent faux et le code 0 y reste non confirmé. Fonction pure,
 * testable en JVM à partir des champs de Build.
 */
fun isZeroResultCodeProvenRom(
    manufacturer: String?,
    brand: String?,
    model: String?,
    display: String?,
    versionIncremental: String?,
): Boolean {
    val maker = manufacturer.orEmpty().lowercase()
    val brandNorm = brand.orEmpty().lowercase()
    val modelNorm = model.orEmpty().lowercase()
    val displayNorm = display.orEmpty().lowercase()
    val incrementalNorm = versionIncremental.orEmpty().lowercase()

    // Redmi Note 14 sous HyperOS : famille Xiaomi, modèle marketing
    // « Note 14 » exposé, et indication HyperOS vérifiable. Sans le
    // modèle marketing ou sans indication HyperOS, pas de preuve.
    val xiaomiFamily = "xiaomi" in maker || "redmi" in brandNorm || "xiaomi" in brandNorm
    val hyperOsIndication = "hyperos" in displayNorm ||
        "hyperos" in incrementalNorm ||
        incrementalNorm.startsWith("os1.") ||
        incrementalNorm.startsWith("os2.") ||
        incrementalNorm.startsWith("os3.")
    if (xiaomiFamily && "note 14" in modelNorm && hyperOsIndication) return true

    // Tecno sous HiOS : fabricant Tecno et indication HiOS explicite.
    val hiosIndication = "hios" in displayNorm || "hios" in incrementalNorm
    if ("tecno" in maker && hiosIndication) return true

    return false
}

/**
 * Un accusé de livraison est confirmé si le code est RESULT_OK (-1),
 * ou si le code 0 provient d'une ROM prouvée par [isZeroResultCodeProvenRom].
 * Tout autre cas reste non confirmé : rien n'est enregistré comme livré.
 */
fun isDeliveryConfirmed(resultCode: Int, zeroMeansConfirmedOnThisRom: Boolean): Boolean =
    resultCode == -1 || (resultCode == 0 && zeroMeansConfirmedOnThisRom)
