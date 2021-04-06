package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.graphics.Bitmap
import android.util.Pair
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.AvatarUtil.getCircleAvatar
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequestListenerInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateMeetingRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) {
//    suspend fun getDefaultAvatar(): Bitmap = withContext(Dispatchers.IO) {
//        AvatarUtil.getDefaultAvatar(
//            getColorAvatar(megaApi.myUser), megaChatApi.myFullname, Constants.AVATAR_SIZE, true
//        )
//    }
//
//    /**
//     * Get the actual avatar from the server and save it to the cache folder
//     */
//    suspend fun createAvatar(listener: BaseListener) = withContext(Dispatchers.IO) {
//        megaApi.getUserAvatar(
//            megaApi.myUser,
//            CacheFolderManager.buildAvatarFile(context, megaApi.myEmail + ".jpg").absolutePath,
//            listener
//        )
//    }

    fun createMeeting(
        group: Boolean,
        peers: MegaChatPeerList?,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.createChat(true, peers, listener)
    }
}