package mega.privacy.android.app.data.extensions

import mega.privacy.android.app.utils.StringUtils.decodeBase64
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaStringMap
import timber.log.Timber

/**
 * Decode each alias within MegaStringMap into a Map<Long, String>
 */
fun MegaStringMap.getDecodedAliases(): Map<Long, String> {
    val aliases = mutableMapOf<Long, String>()

    for (i in 0 until keys.size()) {
        val base64Handle = keys[i]
        val handle = MegaApiJava.base64ToUserHandle(base64Handle)
        try {
            aliases[handle] = get(base64Handle).decodeBase64()
        } catch (error: IllegalArgumentException) {
            Timber.w(error)
        }
    }

    return aliases
}