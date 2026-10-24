package tech.dmampro.tinda.sms

/**
 * Empreinte Build utilisée pour décider si le resultCode 0 a une
 * signification prouvée sur cet appareil précis.
 *
 * Tous les champs participent à la correspondance : une famille de
 * modèles, un nom commercial ou un nom de ROM ne constitue pas une
 * preuve pour un appareil donné.
 */
data class BuildFingerprint(
    val manufacturer: String?,
    val brand: String?,
    val model: String?,
    val display: String?,
    val versionIncremental: String?,
)

/**
 * Appareil dont l'empreinte complète a été relevée et pour lequel le
 * comportement du resultCode 0 a été observé séparément à l'émission
 * et à la livraison.
 */
data class ProvenZeroResultDevice(
    val fingerprint: BuildFingerprint,
    val zeroProvesSent: Boolean,
    val zeroProvesDelivered: Boolean,
)

/**
 * Appareils dont le resultCode 0 est actuellement prouvé : aucun.
 *
 * L'unique entrée ci-dessous est une empreinte exacte capturée, pas
 * encore une preuve. Ses deux drapeaux restent à faux tant que les
 * accusés d'émission et de livraison à 0 n'ont pas été observés
 * séparément sur cet appareil pendant la recette.
 *
 * Le 18 octobre 2026, des envois réels ont été observés avec un
 * resultCode 0 sur un Redmi Note 14 sous HyperOS et sur un Tecno sous
 * HiOS. Le modèle du Redmi, 24117RN76G, a été observé lors d'une
 * session adb le 19 octobre 2026, puis son empreinte complète a été
 * capturée par adb le 24 octobre 2026. Aucune empreinte du Tecno
 * n'est consignée : aucune entrée ne peut donc être créée pour lui.
 *
 * Pour activer une entrée : observer séparément un accusé
 * d'émission et un accusé de livraison à 0 sur l'appareil portant
 * exactement cette empreinte, puis passer uniquement le drapeau
 * correspondant à vrai dans un changement documenté et daté.
 */
val PROVEN_ZERO_RESULT_DEVICES: List<ProvenZeroResultDevice> = listOf(
    // Empreinte capturée le 24 octobre 2026 via adb sur le Redmi
    // Note 14 de SmokeSama (appareil DrSmoke_RN14, produit tanzanite).
    // Preuves émission et livraison par observation des accusés sur
    // cet appareil encore à établir pendant la recette : les deux
    // drapeaux restent donc à faux.
    ProvenZeroResultDevice(
        fingerprint = BuildFingerprint(
            manufacturer = "Xiaomi",
            brand = "Redmi",
            model = "24117RN76G",
            display = "BP2A.250605.031.A3",
            versionIncremental = "OS3.0.302.0.WOGMIDC",
        ),
        zeroProvesSent = false,
        zeroProvesDelivered = false,
    ),
)

/** Retourne l'entrée dont l'empreinte correspond exactement, ou null. */
fun provenZeroResultDeviceFor(
    fingerprint: BuildFingerprint,
    devices: List<ProvenZeroResultDevice> = PROVEN_ZERO_RESULT_DEVICES,
): ProvenZeroResultDevice? = devices.firstOrNull { it.fingerprint == fingerprint }

fun zeroProvesSentFor(
    fingerprint: BuildFingerprint,
    devices: List<ProvenZeroResultDevice> = PROVEN_ZERO_RESULT_DEVICES,
): Boolean = provenZeroResultDeviceFor(fingerprint, devices)?.zeroProvesSent == true

fun zeroProvesDeliveredFor(
    fingerprint: BuildFingerprint,
    devices: List<ProvenZeroResultDevice> = PROVEN_ZERO_RESULT_DEVICES,
): Boolean = provenZeroResultDeviceFor(fingerprint, devices)?.zeroProvesDelivered == true

/**
 * Un accusé de livraison est confirmé si le code est RESULT_OK (-1),
 * ou si le code 0 a spécifiquement été prouvé comme preuve de
 * livraison pour cette empreinte exacte. Tout autre cas reste non
 * confirmé : rien n'est enregistré comme livré.
 */
fun isDeliveryConfirmed(resultCode: Int, zeroProvesDelivered: Boolean): Boolean =
    resultCode == -1 || (resultCode == 0 && zeroProvesDelivered)
