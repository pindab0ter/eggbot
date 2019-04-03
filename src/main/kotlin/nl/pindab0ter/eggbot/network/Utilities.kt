package nl.pindab0ter.eggbot.network

import com.github.kittinunf.fuel.core.Body
import java.util.*

fun Body.decodeBase64(): ByteArray = Base64.getDecoder().decode(toByteArray())