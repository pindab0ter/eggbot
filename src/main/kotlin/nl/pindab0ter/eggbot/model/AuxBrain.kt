package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.*
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.decodeBase64
import com.github.kittinunf.result.getOrNull
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.encodeBase64ToString
import org.joda.time.Instant
import org.joda.time.Period.minutes


object AuxBrain {
    private val logger = KotlinLogging.logger("AuxBrain")

    private const val PERIODICALS_URL = "http://www.auxbrain.com/ei/get_periodicals"
    private const val PERIODICALS_BETA_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/get_periodicals"
    private const val COOP_STATUS_URL = "http://www.auxbrain.com/ei/coop_status"
    private const val COOP_STATUS_BETA_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/coop_status"
    private const val FIRST_CONTACT_URL = "http://www.auxbrain.com/ei/first_contact"
    private const val FIRST_CONTACT_BETA_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/first_contact"
    private const val ARTIFACTS_CONFIGURATION_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei_afx/config"

    ///////////////
    // Contracts //
    ///////////////

    private var contracts: Map<String, Contract> = emptyMap()
    private var lastContractsUpdate: Instant = Instant.EPOCH

    private fun periodicalsRequest(): Request {
        val data = PeriodicalsRequest {
            clientVersion = Config.clientVersion
            // TODO: Dynamically update the clientVersion
            // clientVersion = EggBot.clientVersion
            userId = Config.userId
        }.serialize().encodeBase64ToString()

        return PERIODICALS_BETA_URL.httpPost()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("data=$data")
    }

    private fun getCachedContracts(): Map<String, Contract> {
        if (lastContractsUpdate.isBefore(Instant.now().minus(minutes(5).toStandardDuration()))) {
            logger.info { "Fetching contracts..." }

            periodicalsRequest().responseObject(ContractsDeserializer).third.component1()?.let { contracts ->
                this.contracts = contracts
                    .filter { contract -> contract.id != "first-contract" }
                    .associateBy { contract -> contract.id }
                lastContractsUpdate = Instant.now()
            }
        } else {
            logger.info { "Using cached contracts..." }
        }

        return contracts
    }

    fun getContracts(): List<Contract> = getCachedContracts().values.toList()

    fun getContract(contractId: String): Contract? = getCachedContracts()[contractId]

    private object ContractsDeserializer : ResponseDeserializable<List<Contract>> {
        override fun deserialize(content: String): List<Contract>? {
            return content.decodeBase64()?.let { PeriodicalsResponse.deserialize(it).periodicals?.contracts?.contracts }
        }
    }

    /////////////
    // Backups //
    /////////////

    private fun firstContactRequest(userId: String): Request {
        val data = FirstContactRequest {
            eiUserId = userId
            deviceId = Config.deviceId
            clientVersion = Config.clientVersion
        }.serialize().encodeBase64ToString()

        return FIRST_CONTACT_BETA_URL.httpPost()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("data=$data")
    }

    fun getFarmerBackup(userId: String): Backup? {
        return firstContactRequest(userId).responseObject(BackupDeserializer).third.component1()
    }

    private object BackupDeserializer : ResponseDeserializable<Backup> {
        override fun deserialize(content: String): Backup? {
            return content.decodeBase64()?.let { FirstContactResponse.deserialize(it).firstContact?.backup }
        }
    }

    ////////////
    // Co-ops //
    ////////////

    private fun coopStatusRequest(contractId: String, coopId: String): Request {
        val data = CoopStatusRequest {
            this.contractId = contractId
            this.coopId = coopId
        }.serialize().encodeBase64ToString()

        return COOP_STATUS_BETA_URL.httpPost()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("data=$data")
    }

    fun getCoopStatus(contractId: String, coopId: String): CoopStatus? = coopStatusRequest(contractId, coopId)
        .responseObject(CoopStatusDeserializer)
        .third
        .getOrNull()
        ?.takeUnless { coopStatus ->
            coopStatus.totalAmount == 0.0 && coopStatus.contributors.isEmpty() && coopStatus.secondsRemaining == 0.0
        }

    private object CoopStatusDeserializer : ResponseDeserializable<CoopStatus> {
        override fun deserialize(content: String): CoopStatus? {
            return content.decodeBase64()?.let { CoopStatusResponse.deserialize(it).coopStatus }
        }
    }
}
