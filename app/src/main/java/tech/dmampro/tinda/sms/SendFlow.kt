package tech.dmampro.tinda.sms

/**
 * Frontière d'un envoi : ce qui se passe avant le retour de l'appel
 * radio n'a pas la même signification que ce qui se passe après.
 *
 * Leçon du 24 octobre 2026 : une exception ne dit pas à elle seule si
 * l'appel radio a eu lieu. Si l'appel radio lève, la soumission n'a
 * pas abouti et l'échec est réel. Si l'appel radio est revenu et que
 * l'écriture dans le fournisseur (ou l'association du providerId)
 * échoue ensuite, le SMS est peut-être parti : ce n'est pas une
 * preuve d'échec, le message reste en état incertain jusqu'à l'accusé
 * ou à une réconciliation, et aucun renvoi automatique ne doit être
 * proposé (risque de doublon payé deux fois).
 */
object SendFlow {

    sealed interface Outcome {
        /** Appel radio revenu ; [providerId] vaut null si l'écriture fournisseur a échoué. */
        data class Submitted(val providerId: Long?) : Outcome

        /** L'appel radio lui-même a levé : la soumission n'a pas abouti. */
        data class RadioFailed(val error: Exception) : Outcome
    }

    /**
     * Exécute l'appel radio puis, seulement s'il est revenu,
     * l'écriture fournisseur. Une exception de [writeToProvider] est
     * signalée à [onProviderFailure] mais ne transforme jamais le
     * résultat en échec d'envoi.
     */
    suspend fun execute(
        submitToRadio: suspend () -> Unit,
        writeToProvider: suspend () -> Long?,
        onProviderFailure: (Exception) -> Unit = {},
    ): Outcome {
        try {
            submitToRadio()
        } catch (e: Exception) {
            return Outcome.RadioFailed(e)
        }
        val providerId = try {
            writeToProvider()
        } catch (e: Exception) {
            onProviderFailure(e)
            null
        }
        return Outcome.Submitted(providerId)
    }
}
