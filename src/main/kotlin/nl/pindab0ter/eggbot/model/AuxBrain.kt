package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.*
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.ResultHandler
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.decodeBase64
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.helpers.decodeBase64
import nl.pindab0ter.eggbot.helpers.encodeBase64ToString


object AuxBrain {
    private const val PERIODICALS_URL = "http://www.auxbrain.com/ei/get_periodicals"
    private const val COOP_STATUS_URL = "http://www.auxbrain.com/ei/coop_status"
    private const val FIRST_CONTACT_URL = "http://www.auxbrain.com/ei/first_contact"

    private fun periodicalsRequest(): Request = PERIODICALS_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=${PeriodicalsRequest {
                clientVersion = EggBot.clientVersion
            }.serialize().encodeBase64ToString()}"
        )

    fun getPeriodicals(handler: (PeriodicalsResponse) -> Unit) = periodicalsRequest()
        .response { _, response, _ ->
            handler(PeriodicalsResponse.deserialize(response.body().decodeBase64()))
        }

    fun getPeriodicals(): PeriodicalsResponse? =
        periodicalsRequest().responseObject(ContractsDeserializer).third.component1()

    fun getContract(contractId: String): Contract? = getPeriodicals()?.contracts?.contracts?.find { contract ->
        contract.id == contractId
    }

    private object ContractsDeserializer : ResponseDeserializable<PeriodicalsResponse> {
        override fun deserialize(content: String): PeriodicalsResponse? {
            return content.decodeBase64()?.let { PeriodicalsResponse.deserialize(it) }
        }
    }

    private fun firstContactRequest(userId: String): Request = FIRST_CONTACT_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=${FirstContactRequest {
                this.userId = userId
            }.serialize().encodeBase64ToString()}"
        )

    private fun coopStatusRequest(contractId: String, coopId: String) = COOP_STATUS_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=${CoopStatusRequest {
                this.contractId = contractId
                this.coopId = coopId
            }.serialize().encodeBase64ToString()}"
        )

    fun getCoopStatus(
        contractId: String,
        coopId: String
    ): CoopStatusResponse? =
        coopStatusRequest(contractId, coopId).responseObject(CoopStatusDeserializer).third.component1()

    private object CoopStatusDeserializer : ResponseDeserializable<CoopStatusResponse> {
        override fun deserialize(content: String): CoopStatusResponse? {
            return content.decodeBase64()?.let { CoopStatusResponse.deserialize(it) }
        }
    }

    /**
     * Retrieve the [Backup] asynchronously, using the [handler]
     *
     * @param handler [ResultHandler<Backup>] the handler to report the [Backup]
     * @return [CancellableRequest] the request in flight
     */
    fun getFarmerBackup(inGameId: String, handler: ResultHandler<Backup>): CancellableRequest =
        firstContactRequest(inGameId).responseObject(BackupDeserializer, handler)

    /**
     * Retrieve the [Backup] synchronously
     *
     * @note this is a synchronous execution and cannot be cancelled
     *
     * @return [Backup] the backup
     */
    fun getFarmerBackup(inGameId: String): Backup? =
        firstContactRequest(inGameId).responseObject(BackupDeserializer).third.component1()

    private object BackupDeserializer : ResponseDeserializable<Backup> {
        override fun deserialize(content: String): Backup? {
            return content.decodeBase64()?.let { FirstContactResponse.deserialize(it).backup }
        }
    }
}
