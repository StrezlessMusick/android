package mega.privacy.android.app.presentation.favourites.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import nz.mega.sdk.MegaNode

/**
 * The favourite file entity
 */
data class FavouriteFile(
    override val handle: Long,
    @DrawableRes override val icon: Int,
    override val name: String,
    @ColorRes override val labelColour: Int,
    override val showLabel: Boolean,
    override val node: MegaNode,
    override val hasVersion: Boolean,
    override val info: String,
    override val isFavourite: Boolean,
    override val isExported: Boolean,
    override val isTakenDown: Boolean,
    override val isAvailableOffline: Boolean,
    override val isSelected: Boolean = false,
    override val thumbnailPath: String?,
    override val size: Long,
    override val label: Int,
    override val modificationTime: Long
) : Favourite {
    override val isFolder = false
}