package nl.pindab0ter.eggbot.helpers

import com.github.kittinunf.fuel.core.Body
import com.github.kittinunf.fuel.util.encodeBase64
import java.util.*


fun Body.decodeBase64(): ByteArray = Base64.getDecoder().decode(toByteArray())
fun ByteArray.encodeBase64ToString(): String = String(encodeBase64())
