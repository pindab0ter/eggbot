package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.*
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.decodeBase64
import com.github.kittinunf.result.getOrNull
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.encodeBase64ToString
import nl.pindab0ter.eggbot.model.database.Farmer
import org.joda.time.Instant
import org.joda.time.Period.minutes


object AuxBrain {
    private val logger = KotlinLogging.logger {}

    private const val PERIODICALS_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/get_periodicals"
    private const val COOP_STATUS_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/coop_status"
    private const val FIRST_CONTACT_URL = "http://afx-2-dot-auxbrainhome.appspot.com/ei/first_contact"

    ///////////////
    // Contracts //
    ///////////////

    private var contractsUpdateValidUntil: Instant = Instant.EPOCH
    private val contracts: MutableMap<String, Contract> = mutableMapOf()
        get() = if (contractsUpdateValidUntil.isBefore(Instant.now())) {
            logger.trace { "Contracts cache miss" }

            val contracts = periodicalsRequest()
                .responseObject(ContractsDeserializer)
                .third
                .component1()
                .orEmpty()

            if (contracts.isEmpty()) {
                logger.warn { "Could not get contracts from AuxBrain" }
            }

            contracts
                .filter { contract -> contract.id != "first-contract" }
                .forEach { contract ->
                    field[contract.id] = contract
                }
            contractsUpdateValidUntil = Instant.now().plus(minutes(5).toStandardDuration())
            field
        } else {
            logger.trace { "Contracts cache hit" }
            field
        }

    private fun periodicalsRequest(): Request {
        val data = PeriodicalsRequest {
            clientVersion = Config.clientVersion
            userId = Config.userId
        }.serialize().encodeBase64ToString()

        return PERIODICALS_URL.httpPost()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("data=$data")
    }

    fun getContracts(): List<Contract> = contracts.values.toList()

    fun getContract(contractId: String): Contract? = contracts[contractId]

    private object ContractsDeserializer : ResponseDeserializable<List<Contract>> {
        override fun deserialize(content: String): List<Contract>? {
            return content.decodeBase64()?.let { PeriodicalsResponse.deserialize(it).periodicals?.contracts?.contracts }
        }
    }

    /////////////
    // Backups //
    /////////////

    data class FarmerBackupCache(
        val validUntil: Instant,
        val farmerBackup: Backup
    )

    private val cachedFarmerBackups: MutableMap<String, FarmerBackupCache> = mutableMapOf()

    private fun firstContactRequest(eggIncId: String): Request {
        val data = FirstContactRequest {
            eiUserId = eggIncId
            deviceId = Config.deviceId
            clientVersion = Config.clientVersion
        }.serialize().encodeBase64ToString()

        return FIRST_CONTACT_URL.httpPost()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("data=$data")
    }

    fun getFarmerBackup(eggIncId: String): Backup? = cachedFarmerBackups[eggIncId]
        ?.takeIf { cachedFarmerBackup -> cachedFarmerBackup.validUntil.isAfterNow }
        ?.let { cachedFarmerBackup ->
            logger.trace { "Farmer backup cache hit" }
            return cachedFarmerBackup.farmerBackup
        } ?: run {

        logger.trace { "Farmer backup cache miss" }

        val retrievedFarmerBackup = firstContactRequest(eggIncId)
            .responseObject(BackupDeserializer)
            .third
            .component1()

        if (retrievedFarmerBackup == null) logger.warn { "Could not get backup for ID `$eggIncId` from AuxBrain" }
        else {
            cachedFarmerBackups[eggIncId] = FarmerBackupCache(
                validUntil = Instant.now().plus(minutes(5).toStandardDuration()),
                farmerBackup = retrievedFarmerBackup,
            )

            if (retrievedFarmerBackup.clientVersion > Config.clientVersion) {
                Farmer.logger.info { "Updated to client version ${retrievedFarmerBackup.clientVersion}." }
                Config.clientVersion = retrievedFarmerBackup.clientVersion
            }
        }

        retrievedFarmerBackup
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

        return COOP_STATUS_URL.httpPost()
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
