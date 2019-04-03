package nl.pindab0ter.eggbot.network

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.encodeBase64ToString


object AuxBrain {
    private const val GET_SALES_URL = "http://www.auxbrain.com/ei/get_sales"
    private const val GET_EVENTS_URL = "http://www.auxbrain.com/ei/get_events"
    private const val GET_CONTRACTS_URL = "http://www.auxbrain.com/ei/get_contracts"
    private const val COOP_STATUS_URL = "http://www.auxbrain.com/ei/coop_status"
    private const val FIRST_CONTACT_URL = "http://www.auxbrain.com/ei/first_contact"
    private const val DAILY_GIFT_URL = "http://www.auxbrain.com/ei/daily_gift_info"

    fun getContracts(handler: (EggInc.GetContractsResponse) -> Unit) = GET_CONTRACTS_URL.httpPost()
        .response { _, response, _ ->
            handler(EggInc.GetContractsResponse.parseFrom(response.body().base64Decoded()))
        }
