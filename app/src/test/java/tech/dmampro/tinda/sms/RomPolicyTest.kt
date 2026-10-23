package tech.dmampro.tinda.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Politique ROM du resultCode 0 : seuls les appareils prouvés sur le
 * terrain le 18 octobre 2026 acceptent 0 comme confirmation, pour
 * l'émission comme pour la livraison (revue PR nº 2, point 4).
 */
class RomPolicyTest {

    @Test
    fun `redmi note 14 hyperos est prouve par l affichage`() {
        assertTrue(
            isZeroResultCodeProvenRom(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 14",
                display = "HyperOS 2.0",
                versionIncremental = "V816.0.1.0",
            ),
        )
    }

    @Test
    fun `redmi note 14 hyperos est prouve par le prefixe d incremental`() {
        assertTrue(
            isZeroResultCodeProvenRom(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "24117RN76G Note 14",
                display = "AP3A.240905.015",
                versionIncremental = "OS2.0.201.0.VNCMIXM",
            ),
        )
    }

    @Test
    fun `tecno hios est prouve`() {
        assertTrue(
            isZeroResultCodeProvenRom(
                manufacturer = "TECNO",
                brand = "TECNO",
                model = "TECNO Spark",
                display = "HiOS 14.0.0",
                versionIncremental = "14.0",
            ),
        )
    }

    @Test
    fun `xiaomi autre modele sans preuve est refuse`() {
        assertFalse(
            isZeroResultCodeProvenRom(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi 13C",
                display = "HyperOS 1.0",
                versionIncremental = "OS1.0.1.0",
            ),
        )
        assertFalse(
            isZeroResultCodeProvenRom(
                manufacturer = "Xiaomi",
                brand = "Xiaomi",
                model = "Xiaomi 14",
                display = "HyperOS 2.0",
                versionIncremental = "OS2.0.1.0",
            ),
        )
    }

    @Test
    fun `redmi note 14 sans indication hyperos est refuse`() {
        assertFalse(
            isZeroResultCodeProvenRom(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 14",
                display = "MIUI 14.0.3",
                versionIncremental = "V816.0.1.0",
            ),
        )
    }

    @Test
    fun `tecno sans indication hios est refuse`() {
        assertFalse(
            isZeroResultCodeProvenRom(
                manufacturer = "TECNO",
                brand = "TECNO",
                model = "TECNO Spark",
                display = "Android 14",
                versionIncremental = "14.0",
            ),
        )
    }

    @Test
    fun `autres fabricants sont refuses`() {
        assertFalse(
            isZeroResultCodeProvenRom(
                manufacturer = "samsung",
                brand = "samsung",
                model = "Galaxy Note 14",
                display = "HyperOS like",
                versionIncremental = "OS2.0",
            ),
        )
        assertFalse(isZeroResultCodeProvenRom(null, null, null, null, null))
    }

    @Test
    fun `livraison confirmee seulement par ok ou zero sur rom prouvee`() {
        assertTrue(isDeliveryConfirmed(-1, zeroMeansConfirmedOnThisRom = false))
        assertTrue(isDeliveryConfirmed(0, zeroMeansConfirmedOnThisRom = true))
        assertFalse(isDeliveryConfirmed(0, zeroMeansConfirmedOnThisRom = false))
        assertFalse(isDeliveryConfirmed(1, zeroMeansConfirmedOnThisRom = true))
    }
}
