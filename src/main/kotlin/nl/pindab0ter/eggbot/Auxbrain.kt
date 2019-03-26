package nl.pindab0ter.eggbot

import com.github.kittinunf.fuel.httpGet
import nl.pindab0ter.EggBot.EggInc

const val GET_SALES_URL = "http://www.auxbrain.com/ei/get_sales"
const val GET_EVENTS_URL = "http://www.auxbrain.com/ei/get_events"
const val GET_CONTRACTS_URL = "http://www.auxbrain.com/ei/get_contracts"
const val FIRST_CONTACT_URL = "http://www.auxbrain.com/ei/first_contact"
const val DAILY_GIFT_URL = "http://www.auxbrain.com/ei/daily_gift_info"

fun getContracts() = GET_CONTRACTS_URL.httpGet().response { request, response, result ->
    val getContractsResponse = EggInc.GetContractsResponse.parseFrom(response.body().base64Decoded())

    println(getContractsResponse.contractsList)
    getContractsResponse.contractsList.forEach { contract: EggInc.Contract? ->
        println(contract)
    }

}
