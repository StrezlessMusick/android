package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.FileCompressionGateway
import mega.privacy.android.app.data.gateway.LogbackLogConfigurationGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.preferences.LoggingPreferencesGateway
import mega.privacy.android.app.di.ApplicationScope
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.logging.ChatLogger
import mega.privacy.android.app.logging.SdkLogger
import mega.privacy.android.app.logging.loggers.ChatFlowLogTree
import mega.privacy.android.app.logging.loggers.FileLogMessage
import mega.privacy.android.app.logging.loggers.FileLogger
import mega.privacy.android.app.logging.loggers.LineNumberDebugTree
import mega.privacy.android.app.logging.loggers.SdkLogFlowTree
import mega.privacy.android.app.logging.loggers.TimberChatLogger
import mega.privacy.android.app.logging.loggers.TimberMegaLogger
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Timber logging repository
 *
 * @property timberMegaLogger
 * @property timberChatLogger
 * @property sdkLogFlowTree
 * @property chatLogFlowTree
 * @property loggingConfig
 * @property sdkLogger
 * @property chatLogger
 * @property context
 * @property fileCompressionGateway
 * @property megaApiGateway
 * @property ioDispatcher
 * @property loggingPreferencesGateway
 * @property appScope
 */
class TimberLoggingRepository @Inject constructor(
    private val timberMegaLogger: TimberMegaLogger,
    private val timberChatLogger: TimberChatLogger,
    private val sdkLogFlowTree: SdkLogFlowTree,
    private val chatLogFlowTree: ChatFlowLogTree,
    private val loggingConfig: LogbackLogConfigurationGateway,
    @SdkLogger private val sdkLogger: FileLogger,
    @ChatLogger private val chatLogger: FileLogger,
    @ApplicationContext private val context: Context,
    private val fileCompressionGateway: FileCompressionGateway,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val loggingPreferencesGateway: LoggingPreferencesGateway,
    @ApplicationScope private val appScope: CoroutineScope,
) : LoggingRepository {

    init {
        if (!Timber.forest().contains(sdkLogFlowTree)) {
            Timber.plant(sdkLogFlowTree)
        }
        if (!Timber.forest().contains(chatLogFlowTree)) {
            Timber.plant(chatLogFlowTree)
        }
        MegaChatApiAndroid.setLoggerObject(timberChatLogger)
        MegaApiAndroid.addLoggerObject(timberMegaLogger)
    }

    override fun enableLogAllToConsole() {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
        Timber.plant(LineNumberDebugTree())
    }

    override fun resetSdkLogging() {
        MegaApiAndroid.removeLoggerObject(timberMegaLogger)
        MegaApiAndroid.addLoggerObject(timberMegaLogger)
    }

    override fun getSdkLoggingFlow(): Flow<FileLogMessage> = sdkLogFlowTree.logFlow.onSubscription {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        loggingConfig.resetLoggingConfiguration()
    }.onCompletion {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL)
    }

    override fun getChatLoggingFlow(): Flow<FileLogMessage> =
        chatLogFlowTree.logFlow.onSubscription {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
            loggingConfig.resetLoggingConfiguration()
        }.onCompletion {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR)
        }

    override fun logToSdkFile(logMessage: FileLogMessage) = sdkLogger.logToFile(logMessage)

    override fun logToChatFile(logMessage: FileLogMessage) = chatLogger.logToFile(logMessage)

    override suspend fun compressLogs(): File = withContext(ioDispatcher) {
        val loggingDirectoryPath = loggingConfig.getLoggingDirectoryPath()
        require(loggingDirectoryPath != null) { "Logging configuration file missing or logging directory not configured" }
        createEmptyFile().apply {
            fileCompressionGateway.zipFolder(
                File(loggingDirectoryPath),
                this
            )
        }
    }

    private fun createEmptyFile() =
        File("${context.cacheDir.path}/${getLogFileName()}").apply {
            if (exists()) delete()
            createNewFile()
        }

    private fun getLogFileName() =
        "${getFormattedDate()}_Android_${megaApiGateway.accountEmail}.zip"

    private fun getFormattedDate() = DateTimeFormatter.ofPattern("dd_MM_yyyy__HH_mm_ss")
        .withZone(ZoneId.from(ZoneOffset.UTC))
        .format(Instant.now())

    override fun isSdkLoggingEnabled(): SharedFlow<Boolean> =
        loggingPreferencesGateway.isLoggingPreferenceEnabled()
            .shareIn(appScope, SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun setSdkLoggingEnabled(enabled: Boolean) {
        loggingPreferencesGateway.setLoggingEnabledPreference(enabled)
    }

    override fun isChatLoggingEnabled(): Flow<Boolean> =
        loggingPreferencesGateway.isChatLoggingPreferenceEnabled()

    override suspend fun setChatLoggingEnabled(enabled: Boolean) {
        loggingPreferencesGateway.setChatLoggingEnabledPreference(enabled)
    }
}