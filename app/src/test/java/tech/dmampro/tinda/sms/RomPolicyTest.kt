package tech.dmampro.tinda.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Politique du resultCode 0 : une famille d'appareils n'est pas une
 * preuve. Seule une empreinte Build complète, exactement égale à une
 * entrée documentée, peut activer séparément les drapeaux d'émission
 * et de livraison.
 */
class RomPolicyTest {

    private val xiaomiFamilyFingerprint = BuildFingerprint(
        manufacturer = "Xiaomi",
        brand = "Redmi",
        model = "Redmi Note 14",
        display = "HyperOS 2.0",
        versionIncremental = "OS2.0.201.0.VNCMIXM",
    )

    private val tecnoFamilyFingerprint = BuildFingerprint(
        manufacturer = "TECNO",
        brand = "TECNO",
        model = "TECNO Spark",
        display = "HiOS 14.0.0",
        versionIncremental = "14.0",
    )

    private val capturedRedmiFingerprint = BuildFingerprint(
        manufacturer = "Xiaomi",
        brand = "Redmi",
        model = "24117RN76G",
        display = "BP2A.250605.031.A3",
        versionIncremental = "OS3.0.302.0.WOGMIDC",
    )

    @Test
    fun `la famille xiaomi note 14 hyperos ne prouve rien`() {
        assertNull(provenZeroResultDeviceFor(xiaomiFamilyFingerprint))
        assertFalse(zeroProvesSentFor(xiaomiFamilyFingerprint))
        assertFalse(zeroProvesDeliveredFor(xiaomiFamilyFingerprint))
    }

    @Test
    fun `la famille tecno hios ne prouve rien`() {
        assertNull(provenZeroResultDeviceFor(tecnoFamilyFingerprint))
        assertFalse(zeroProvesSentFor(tecnoFamilyFingerprint))
        assertFalse(zeroProvesDeliveredFor(tecnoFamilyFingerprint))
    }

    @Test
    fun `l empreinte redmi capturee est presente mais inactive`() {
        val device = provenZeroResultDeviceFor(capturedRedmiFingerprint)

        assertEquals(capturedRedmiFingerprint, device?.fingerprint)
        assertFalse(device?.zeroProvesSent ?: true)
        assertFalse(device?.zeroProvesDelivered ?: true)
        assertFalse(zeroProvesSentFor(capturedRedmiFingerprint))
        assertFalse(zeroProvesDeliveredFor(capturedRedmiFingerprint))
    }

    @Test
    fun `une empreinte quelconque ne prouve rien`() {
        val arbitrary = BuildFingerprint("Maker", "Brand", "Model", "Display", "Incremental")

        assertNull(provenZeroResultDeviceFor(arbitrary))
        assertFalse(zeroProvesSentFor(arbitrary))
        assertFalse(zeroProvesDeliveredFor(arbitrary))
        assertFalse(zeroProvesSentFor(BuildFingerprint(null, null, null, null, null)))
    }

    @Test
    fun `une entree synthetique exacte active seulement le drapeau d emission`() {
        val fingerprint = BuildFingerprint("A", "B", "C", "D", "E")
        val devices = listOf(
            ProvenZeroResultDevice(
                fingerprint = fingerprint,
                zeroProvesSent = true,
                zeroProvesDelivered = false,
            ),
        )

        assertTrue(zeroProvesSentFor(fingerprint, devices))
        assertFalse(zeroProvesDeliveredFor(fingerprint, devices))
    }

    @Test
    fun `une entree synthetique exacte active seulement le drapeau de livraison`() {
        val fingerprint = BuildFingerprint("A", "B", "C", "D", "E")
        val devices = listOf(
            ProvenZeroResultDevice(
                fingerprint = fingerprint,
                zeroProvesSent = false,
                zeroProvesDelivered = true,
            ),
        )

        assertFalse(zeroProvesSentFor(fingerprint, devices))
        assertTrue(zeroProvesDeliveredFor(fingerprint, devices))
    }

    @Test
    fun `un seul champ different suffit a refuser une entree synthetique`() {
        val fingerprint = BuildFingerprint("A", "B", "C", "D", "E")
        val devices = listOf(
            ProvenZeroResultDevice(
                fingerprint = fingerprint,
                zeroProvesSent = true,
                zeroProvesDelivered = true,
            ),
        )
        val variants = listOf(
            fingerprint.copy(manufacturer = "A2"),
            fingerprint.copy(brand = "B2"),
            fingerprint.copy(model = "C2"),
            fingerprint.copy(display = "D2"),
            fingerprint.copy(versionIncremental = "E2"),
            fingerprint.copy(manufacturer = null),
        )

        variants.forEach { variant ->
            assertNull(provenZeroResultDeviceFor(variant, devices))
            assertFalse(zeroProvesSentFor(variant, devices))
            assertFalse(zeroProvesDeliveredFor(variant, devices))
        }
    }

    @Test
    fun `le code zero non prouve reste non confirme a l emission et a la livraison`() {
        assertEquals(
            PartSentState.UNCONFIRMED,
            sentVerdictForResultCode(
                0,
                zeroProvesSentForExactDevice = zeroProvesSentFor(capturedRedmiFingerprint),
            ).first,
        )
        assertFalse(
            isDeliveryConfirmed(
                0,
                zeroProvesDelivered = zeroProvesDeliveredFor(capturedRedmiFingerprint),
            ),
        )
    }

    @Test
    fun `le code ok confirme l emission et la livraison sans derogation`() {
        assertEquals(
            PartSentState.CONFIRMED,
            sentVerdictForResultCode(-1, zeroProvesSentForExactDevice = false).first,
        )
        assertTrue(isDeliveryConfirmed(-1, zeroProvesDelivered = false))
        assertFalse(isDeliveryConfirmed(1, zeroProvesDelivered = true))
    }
}
