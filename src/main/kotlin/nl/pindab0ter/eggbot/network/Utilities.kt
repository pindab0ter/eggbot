package nl.pindab0ter.eggbot.network

import com.github.kittinunf.fuel.core.Body
import java.util.*

fun Body.base64Decoded(): ByteArray = Base64.getDecoder().decode(toByteArray())