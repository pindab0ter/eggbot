package nl.pindab0ter.eggbot.network

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.encodeBase64ToString
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

    private fun Response.parseFirstContactResponse(inGameId: String): EggInc.Backup? = EggInc.FirstContactResponse
        .parseFrom(body().decodeBase64())
        .takeIf { it.hasBackup() }
        ?.backup
        .takeIf { it?.userid == inGameId }

    fun getFarmerBackup(inGameId: String, handler: (EggInc.Backup?) -> Unit): CancellableRequest =
        firstContactPostRequest(inGameId).response { _, response, _ ->
            handler(response.parseFirstContactResponse(inGameId))
        }

    fun getFarmerBackup(inGameId: String): EggInc.Backup? =
        firstContactPostRequest(inGameId).response().second.parseFirstContactResponse(inGameId)
}
