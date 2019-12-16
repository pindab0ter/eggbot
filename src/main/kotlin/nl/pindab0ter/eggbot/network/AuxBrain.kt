package nl.pindab0ter.eggbot.network

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.ResultHandler
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.decodeBase64
import com.github.kittinunf.fuel.util.encodeBase64ToString
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.utilities.decodeBase64


object AuxBrain {
    private const val SALES_URL = "http://www.auxbrain.com/ei/get_sales"
    private const val EVENTS_URL = "http://www.auxbrain.com/ei/get_events"
    private const val CONTRACTS_URL = "http://www.auxbrain.com/ei/get_contracts" // Deprecated
    private const val PERIODICALS_URL = "http://www.auxbrain.com/ei/get_periodicals"
    private const val COOP_STATUS_URL = "http://www.auxbrain.com/ei/coop_status"
    private const val FIRST_CONTACT_URL = "http://www.auxbrain.com/ei/first_contact"
    private const val DAILY_GIFT_URL = "http://www.auxbrain.com/ei/daily_gift_info"

    private fun periodicalsRequest(): Request = PERIODICALS_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=${EggInc.PeriodicalsRequest
                .newBuilder()
                .setClientVersion(EggBot.clientVersion)
                .build()
                .toByteString()
                .toStringUtf8()
                .encodeBase64ToString()}"
        )

    fun getPeriodicals(handler: (EggInc.PeriodicalsResponse) -> Unit) = periodicalsRequest()
        .response { _, response, _ ->
            handler(EggInc.PeriodicalsResponse.parseFrom(response.body().decodeBase64()))
        }

    fun getPeriodicals(): EggInc.PeriodicalsResponse? =
        periodicalsRequest().responseObject(ContractsDeserializer).third.component1()

    object ContractsDeserializer : ResponseDeserializable<EggInc.PeriodicalsResponse> {
        override fun deserialize(content: String): EggInc.PeriodicalsResponse? {
            return EggInc.PeriodicalsResponse.parseFrom(content.decodeBase64())
        }
    }

    private fun firstContactRequest(userId: String): Request = FIRST_CONTACT_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=${EggInc.FirstContactRequest
                .newBuilder()
                .setUserId(userId)
                .build()
                .toByteString()
                .toStringUtf8()
                .encodeBase64ToString()}"
        )

    private fun coopStatusRequest(contractId: String, coopId: String) = COOP_STATUS_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=${EggInc.CoopStatusRequest
                .newBuilder()
                .setContractId(contractId)
                .setCoopId(coopId)
                .build()
                .toByteString()
                .toStringUtf8()
                .encodeBase64ToString()}"
        )

    fun getCoopStatus(
        contractId: String,
        coopId: String
    ): EggInc.CoopStatusResponse? =
        coopStatusRequest(contractId, coopId).responseObject(CoopStatusDeserializer).third.component1()

    object CoopStatusDeserializer : ResponseDeserializable<EggInc.CoopStatusResponse> {
        override fun deserialize(content: String): EggInc.CoopStatusResponse? {
            return EggInc.CoopStatusResponse.parseFrom(content.decodeBase64())
        }
    }

    /**
     * Retrieve the [EggInc.Backup] asynchronously, using the [handler]
     *
     * @param handler [ResultHandler<EggInc.Backup>] the handler to report the [EggInc.Backup]
     * @return [CancellableRequest] the request in flight
     */
    fun getFarmerBackup(inGameId: String, handler: ResultHandler<EggInc.Backup>): CancellableRequest =
        firstContactRequest(inGameId).responseObject(BackupDeserializer, handler)

    /**
     * Retrieve the [EggInc.Backup] synchronously
     *
     * @note this is a synchronous execution and cannot be cancelled
     *
     * @return [EggInc.Backup] the backup
     */
    fun getFarmerBackup(inGameId: String): EggInc.Backup? =
        firstContactRequest(inGameId).responseObject(BackupDeserializer).third.component1()

    object BackupDeserializer : ResponseDeserializable<EggInc.Backup> {
        override fun deserialize(content: String): EggInc.Backup? {
            return EggInc.FirstContactResponse.parseFrom(content.decodeBase64()).backup
        }
    }
}

