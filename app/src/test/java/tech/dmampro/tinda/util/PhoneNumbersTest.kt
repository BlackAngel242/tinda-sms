package tech.dmampro.tinda.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Normalisation et équivalence des adresses (P0-7).
 * Le contexte régional du projet est le Congo (+242) : l'équivalence
 * local/international est testée dans ce contexte explicite, et un numéro
 * d'un autre pays partageant le même suffixe ne doit pas correspondre.
 */
class PhoneNumbersTest {

    @Test
    fun `format local et international du meme numero congolais sont equivalents`() {
        assertTrue(PhoneNumbers.equivalent("06 12 34 56 78", "+242612345678"))
        assertTrue(PhoneNumbers.equivalent("612345678", "+242 612 345 678"))
        assertTrue(PhoneNumbers.equivalent("00242 612345678", "0612345678"))
    }

    @Test
    fun `un autre pays avec le meme suffixe n est pas equivalent`() {
        assertFalse(PhoneNumbers.equivalent("+243612345678", "0612345678"))
        assertFalse(PhoneNumbers.equivalent("+243612345678", "+242612345678"))
    }

    @Test
    fun `deux numeros differents ne sont pas equivalents`() {
        assertFalse(PhoneNumbers.equivalent("+242699887766", "0612345678"))
        assertFalse(PhoneNumbers.equivalent("0612345678", "0699887766"))
    }

    @Test
    fun `les codes courts ne correspondent que s ils sont identiques`() {
        assertTrue(PhoneNumbers.equivalent("1234", "1234"))
        assertFalse(PhoneNumbers.equivalent("1234", "612345678"))
        assertFalse(PhoneNumbers.equivalent("1234", "+242612341234"))
    }

    @Test
    fun `les expediteurs alphabetiques se comparent en texte, jamais a un numero`() {
        assertTrue(PhoneNumbers.equivalent("MOMO", "momo"))
        assertTrue(PhoneNumbers.equivalent(" Airtel ", "AIRTEL"))
        assertFalse(PhoneNumbers.equivalent("MOMO", "Airtel"))
        assertFalse(PhoneNumbers.equivalent("MOMO", "0612345678"))
    }

    @Test
    fun `une adresse vide n est equivalente a rien`() {
        assertFalse(PhoneNumbers.equivalent("", "0612345678"))
        assertFalse(PhoneNumbers.equivalent("  ", "MOMO"))
        assertFalse(PhoneNumbers.equivalent("", ""))
    }

    @Test
    fun `isBlocked applique l equivalence a la liste stockee`() {
        val blocked = listOf("0612345678", "MOMO")
        assertTrue(PhoneNumbers.isBlocked("+242612345678", blocked))
        assertTrue(PhoneNumbers.isBlocked("momo", blocked))
        assertFalse(PhoneNumbers.isBlocked("+243612345678", blocked))
        assertFalse(PhoneNumbers.isBlocked("+242699887766", blocked))
        assertFalse(PhoneNumbers.isBlocked("0612345678", emptyList()))
    }

    @Test
    fun `storageKey canonise sans perdre le type d adresse`() {
        assertTrue(PhoneNumbers.storageKey("+242 612 345 678") == "+242612345678")
        assertTrue(PhoneNumbers.storageKey("06 12 34 56 78") == "0612345678")
        assertTrue(PhoneNumbers.storageKey(" momo ") == "MOMO")
    }
}
