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
import nl.pindab0ter.eggbot.helpers.discard
import nl.pindab0ter.eggbot.helpers.encodeBase64ToString


object AuxBrain {
    private const val PERIODICALS_URL = "http://www.auxbrain.com/ei/get_periodicals"
    private const val PERIODICALS_BETA_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/get_periodicals"
    private const val COOP_STATUS_URL = "http://www.auxbrain.com/ei/coop_status"
    private const val COOP_STATUS_BETA_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/coop_status"
    private const val FIRST_CONTACT_URL = "http://www.auxbrain.com/ei/first_contact"
    private const val FIRST_CONTACT_BETA_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/first_contact"

    fun periodicalsRequest(): Request = PERIODICALS_BETA_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body("data=${
            PeriodicalsRequest {
                clientVersion = EggBot.clientVersion
                userId = "0"
            }.serialize().encodeBase64ToString()
        }")

    private fun firstContactRequest(userId: String): Request = FIRST_CONTACT_BETA_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body("data=${
            FirstContactRequest {
                eiUserId = userId
                deviceId = "UNKNOWN"
                clientVersion = Config.clientVersion
            }.serialize().encodeBase64ToString()
        }")

    private fun oldFirstContactRequest(userId: String): Request = FIRST_CONTACT_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body("data=${
            FirstContactRequest {
                this.userId = userId
            }.serialize().encodeBase64ToString()
        }")

    private fun coopStatusRequest(contractId: String, coopId: String) = COOP_STATUS_BETA_URL.httpPost()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body("data=${
            CoopStatusRequest {
                this.contractId = contractId
                this.coopId = coopId
            }.serialize().encodeBase64ToString()
        }")

    fun getContracts(handler: (List<Contract>?) -> Unit) = periodicalsRequest()
        .response { _, response, _ ->
            handler(PeriodicalsResponse.deserialize(response.body().decodeBase64()).periodicals?.contracts?.contracts)
        }.discard()

    fun getContracts(): List<Contract>? =
        periodicalsRequest().responseObject(ContractsDeserializer).third.component1()

    fun getContract(contractId: String): Contract? =
        getContracts()?.find { contract ->
            contract.id == contractId
        }

    fun getCoopStatus(contractId: String, coopId: String): CoopStatus? =
        coopStatusRequest(contractId, coopId).responseObject(CoopStatusDeserializer).third.component1()

    fun getFarmerBackup(userId: String, handler: ResultHandler<Backup>): CancellableRequest = when {
        userId.startsWith("EI") -> firstContactRequest(userId).responseObject(BackupDeserializer, handler)
        else -> oldFirstContactRequest(userId).responseObject(OldBackupDeserializer, handler)
    }

    fun getFarmerBackup(userId: String): Backup? = when {
        userId.startsWith("EI") -> firstContactRequest(userId).responseObject(BackupDeserializer).third.component1()
        else -> oldFirstContactRequest(userId).responseObject(OldBackupDeserializer).third.component1()
    }

    private object ContractsDeserializer : ResponseDeserializable<List<Contract>> {
        override fun deserialize(content: String): List<Contract>? {
            return content.decodeBase64()?.let { PeriodicalsResponse.deserialize(it).periodicals?.contracts?.contracts }
        }
    }

    private object CoopStatusDeserializer : ResponseDeserializable<CoopStatus> {
        override fun deserialize(content: String): CoopStatus? {
            return content.decodeBase64()?.let { CoopStatusResponse.deserialize(it).coopStatus }
        }
    }

    private object BackupDeserializer : ResponseDeserializable<Backup> {
        override fun deserialize(content: String): Backup? {
            return content.decodeBase64()?.let { FirstContactResponse.deserialize(it).firstContact?.backup }
        }
    }

    private object OldBackupDeserializer : ResponseDeserializable<Backup> {
        override fun deserialize(content: String): Backup? {
            return content.decodeBase64()?.let { FirstContact.deserialize(it).backup }
        }
    }
}
