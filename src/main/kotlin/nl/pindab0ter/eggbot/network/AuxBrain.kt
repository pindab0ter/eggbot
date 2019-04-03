package nl.pindab0ter.eggbot.network

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.encodeBase64ToString


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

    private fun firstContactPostRequest(userId: String): Request = FIRST_CONTACT_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "data=" + EggInc.FirstContactRequest.newBuilder()
                .setIdentifier(userId)
                .setPlatform(1)
                .build()
                .toByteString().toStringUtf8().encodeBase64ToString()
        )

    fun firstContact(userId: String, handler: (EggInc.FirstContactResponse) -> Unit) = firstContactPostRequest(userId)
        .response { _, response, _ ->
            handler(EggInc.FirstContactResponse.parseFrom(response.body().decodeBase64()))
        }

    fun firstContact(userId: String): EggInc.FirstContactResponse = EggInc.FirstContactResponse.parseFrom(
        firstContactPostRequest(userId).response().second.body().decodeBase64()
    )
}