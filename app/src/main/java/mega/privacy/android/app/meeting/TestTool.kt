package mega.privacy.android.app.meeting

import android.content.Context
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.ParticipantVideoAdapter
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil

object TestTool {

    fun getTestParticipants(context: Context) : List<Participant> {
        val megaApi = MegaApplication.getInstance().megaApi
        val avatar =
            CacheFolderManager.buildAvatarFile(context, megaApi.myEmail + FileUtil.JPG_EXTENSION)
        return listOf(
            Participant("Joanna Zhao", avatar, "#abcdef", false, true, false, false),
            Participant("Yeray Rosales", avatar, "#bcd111", true, false, true, false),
            Participant("Harmen Porter", avatar, "#ccddee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#123456", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#ff2312", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1223ee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1266ee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1223ff", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1223dd", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1223ee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#ff23ee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#12ffee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#b323ee", false, false, false, true),
        )
    }

    fun testData() = listOf(
        Participant("Joanna Zhao", null, "#abcdef", false, true, false, false),
        Participant("Yeray Rosales", null, "#bcd111", true, false, true, false),
        Participant("Harmen Porter", null, "#ccddee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#123456", false, false, false, true),
        Participant("Katayama Fumiki", null, "#ff2312", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1223ee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1266ee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1223ff", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1223dd", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1223ee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#ff23ee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#12ffee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#b323ee", false, false, false, true),
    )
}

