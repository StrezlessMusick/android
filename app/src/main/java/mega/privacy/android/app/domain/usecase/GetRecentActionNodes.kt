package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.fragments.homepage.NodeItem
import nz.mega.sdk.MegaNodeList

/**
 * Transform a [MegaNodeList] into a list of [NodeItem]
 */
fun interface GetRecentActionNodes {

    /**
     * Transform a [MegaNodeList] into a list of [NodeItem]
     *
     * @param nodes the nodes to convert
     * @return a list of node item resulting from the conversion
     */
    suspend operator fun invoke(nodes: MegaNodeList): List<NodeItem>
}