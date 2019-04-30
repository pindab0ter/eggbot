package nl.pindab0ter.eggbot.network

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.ResultHandler
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.decodeBase64
import com.github.kittinunf.fuel.util.encodeBase64ToString
import com.github.kittinunf.result.Result
import nl.pindab0ter.eggbot.decodeBase64


object AuxBrain {
    private const val GET_SALES_URL = "http://www.auxbrain.com/ei/get_sales"
    private const val GET_EVENTS_URL = "http://www.auxbrain.com/ei/get_events"
    private const val GET_CONTRACTS_URL = "http://www.auxbrain.com/ei/get_contracts"
    private const val COOP_STATUS_URL = "http://www.auxbrain.com/ei/coop_status"
    private const val FIRST_CONTACT_URL = "http://www.auxbrain.com/ei/first_contact"
    private const val DAILY_GIFT_URL = "http://www.auxbrain.com/ei/daily_gift_info"

    fun getContracts(handler: (EggInc.GetContractsResponse) -> Unit) = GET_CONTRACTS_URL.httpGet()
        .response { _, response, _ ->
            handler(EggInc.GetContractsResponse.parseFrom(response.body().decodeBase64()))
        }

    fun getContracts(): EggInc.GetContractsResponse = EggInc.GetContractsResponse.parseFrom(
        GET_CONTRACTS_URL.httpGet().response().second.body().decodeBase64()
    )

    private fun firstContactPostRequest(userId: String): Request = FIRST_CONTACT_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=${EggInc.FirstContactRequest
                .newBuilder()
                .setIdentifier(userId)
                .build()
                .toByteString()
                .toStringUtf8()
                .encodeBase64ToString()}"
        )

    private fun getCoopStatusPostRequest(contractId: String, coopName: String) = COOP_STATUS_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=${EggInc.CoopStatusRequest
                .newBuilder()
                .setContractId(contractId)
                .setCoopName(coopName)
                .build()
                .toByteString()
                .toStringUtf8()
                .encodeBase64ToString()}"
        )

    fun getCoopStatus(
        contractId: String,
        coopName: String
    ): Result<EggInc.CoopStatusResponse, FuelError> =
        getCoopStatusPostRequest(contractId, coopName).responseObject(ContractDeserializer).third

    fun getCoopStatus(
        contractId: String,
        coopName: String,
        handler: ResultHandler<EggInc.CoopStatusResponse>
    ): CancellableRequest =
        getCoopStatusPostRequest(contractId, coopName).responseObject(ContractDeserializer, handler)

    object ContractDeserializer : ResponseDeserializable<EggInc.CoopStatusResponse> {
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
        firstContactPostRequest(inGameId).responseObject(BackupDeserializer, handler)

    /**
     * Retrieve the [EggInc.Backup] synchronously
     *
     * @note this is a synchronous execution and can not be cancelled
     *
     * @return [EggInc.Backup] the backup
     */
    fun getFarmerBackup(inGameId: String): Result<EggInc.Backup, FuelError> =
        firstContactPostRequest(inGameId).responseObject(BackupDeserializer).third

    object BackupDeserializer : ResponseDeserializable<EggInc.Backup> {
        override fun deserialize(content: String): EggInc.Backup? {
            return EggInc.FirstContactResponse.parseFrom(content.decodeBase64()).backup
        }
    }
}

