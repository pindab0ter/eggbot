package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.*
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.util.decodeBase64
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrNull
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.joda.time.Instant
import org.joda.time.Period.minutes


private const val CACHE_TTL_MINUTES = 10

object AuxBrain {
    private val logger = KotlinLogging.logger {}

    private const val PERIODICALS_URL = "https://afx-2-dot-auxbrainhome.appspot.com/ei/get_periodicals"
    private const val COOP_STATUS_URL = "https://afx-2-dot-auxbrainhome.appspot.com/ei/coop_status"
    private const val FIRST_CONTACT_URL = "https://afx-2-dot-auxbrainhome.appspot.com/ei/bot_first_contact"

    //region Contracts
    private var contractsCacheUpdateValidUntil: Instant = Instant.EPOCH
    private var contractsCache: Set<Contract> = emptySet()

    private fun periodicalsRequest(): Request {
        val data = PeriodicalsRequest(
            clientVersion = Int.MAX_VALUE,
            userId = config.eggIncId,
        ).encode().encodeBase64()

        return PERIODICALS_URL.httpPost()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("data=$data")
    }

    private fun updateContractsCache() = when (val result = runBlocking { periodicalsRequest().awaitResult(ContractsDeserializer) }) {
        is Result.Success -> {
            contractsCache = result.value.filter { contract -> contract.id != "first-contract" }.toSet()
            contractsCacheUpdateValidUntil = Instant.now().plus(minutes(CACHE_TTL_MINUTES).toStandardDuration())
        }
        is Result.Failure -> {
            logger.error { "Could not get contracts from AuxBrain." }
            logger.error { result.error }
        }
    }

    fun getContracts(): Set<Contract> {
        if (contractsCacheUpdateValidUntil.isBeforeNow) updateContractsCache()
        return contractsCache
    }

    private object ContractsDeserializer : ResponseDeserializable<List<Contract>> {
        override fun deserialize(content: String): List<Contract>? {
            return content.decodeBase64()?.let { PeriodicalsResponse.ADAPTER.decode(it).periodicals.contracts.contracts }
        }
    }
    //endregion

    //region Farmer backups
    data class FarmerBackupCache(val validUntil: Instant, val farmerBackup: Backup)

    private val farmerBackupsCache: MutableMap<String, FarmerBackupCache> = mutableMapOf()

    private fun firstContactRequest(eggIncId: String): Request {
        val data = FirstContactRequest(
            eiUserId = eggIncId,
            deviceId = config.deviceId,
        ).encode().encodeBase64()

        return FIRST_CONTACT_URL.httpPost()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("data=$data")
    }

    private fun updateFarmerBackupsCache(
        eggIncId: String,
        database: Database? = null
    ): Unit = when (val result = runBlocking { firstContactRequest(eggIncId).awaitResult(BackupDeserializer) }) {
        is Result.Success -> runBlocking {
            if (database != null) suspendedTransactionAsync(null, database) {
                Farmer.findById(eggIncId)?.update(result.value)
            }.start()
            farmerBackupsCache[eggIncId] = FarmerBackupCache(
                validUntil = Instant.now().plus(minutes(CACHE_TTL_MINUTES).toStandardDuration()),
                farmerBackup = result.value
            )
        }
        is Result.Failure -> {
            logger.error { "Could not get backup for ID $eggIncId from AuxBrain." }
            logger.error { result.error }
        }
    }

    fun getFarmerBackup(eggIncId: String, database: Database? = null): Backup? = farmerBackupsCache[eggIncId]
        ?.takeIf { cachedFarmerBackup -> cachedFarmerBackup.validUntil.isAfterNow }?.farmerBackup
        ?: updateFarmerBackupsCache(eggIncId, database).run { farmerBackupsCache[eggIncId]?.farmerBackup }

    object BackupDeserializer : ResponseDeserializable<Backup> {
        override fun deserialize(content: String): Backup? {
            return content.decodeBase64()?.let { FirstContact.ADAPTER.decode(it).backup }
        }
    }
    //endregion

    //region Co-ops
    private fun coopStatusRequest(contractId: String, coopId: String): Request {
        val data = CoopStatusRequest(
            contractId = contractId,
            coopId = coopId,
        ).encode().encodeBase64()

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
            return content.decodeBase64()?.let { CoopStatusResponse.ADAPTER.decode(it) }?.coopStatus
        }
    }
    //endregion
}
