package test.mega.privacy.android.app.presentation.recentactions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetRecentActions
import mega.privacy.android.app.presentation.recentactions.RecentActionsViewModel
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.GetVisibleContacts
import mega.privacy.android.domain.usecase.MonitorHideRecentActivity
import mega.privacy.android.domain.usecase.SetHideRecentActivity
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRecentActionBucket
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

@ExperimentalCoroutinesApi
class RecentActionsViewModelTest {
    private lateinit var underTest: RecentActionsViewModel

    private val getRecentActions = mock<GetRecentActions> {
        onBlocking { invoke() }.thenReturn(emptyList())
    }
    private val getVisibleContacts = mock<GetVisibleContacts> {
        onBlocking { invoke() }.thenReturn(emptyList())
    }
    private val setHideRecentActivity = mock<SetHideRecentActivity>()
    private val monitorHideRecentActivity = mock<MonitorHideRecentActivity>()
    private val monitorNodeUpdates = FakeMonitorUpdates()

    private val megaNode: MegaNode = mock {
        on { handle }.thenReturn(123)
    }

    private val megaNodeList: MegaNodeList = mock {
        on { size() }.thenReturn(1)
        on { get(0) }.thenReturn(megaNode)
    }

    private val megaRecentActionBucket = mock<MegaRecentActionBucket> {
        on { this.nodes }.thenReturn(megaNodeList)
        on { this.parentHandle }.thenReturn(321)
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(0L)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
    }

    private val megaRecentActionBucket2 = mock<MegaRecentActionBucket> {
        on { this.nodes }.thenReturn(megaNodeList)
        on { this.parentHandle }.thenReturn(111)
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(0L)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
    }

    private val megaRecentActionBucket3 = mock<MegaRecentActionBucket> {
        on { this.nodes }.thenReturn(megaNodeList)
        on { this.parentHandle }.thenReturn(111)
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(1L)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
    }


    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = RecentActionsViewModel(
            getRecentActions,
            getVisibleContacts,
            setHideRecentActivity,
            monitorHideRecentActivity,
            monitorNodeUpdates,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.recentActionItems).isEqualTo(emptyList<RecentActionItemType>())
            assertThat(initial.hideRecentActivity).isEqualTo(false)
        }
    }

    @Test
    fun `test that recent action items is updated at initialization`() =
        runTest {
            whenever(getRecentActions()).thenReturn(listOf(megaRecentActionBucket))

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    // initialization
                    assertThat(awaitItem().size).isEqualTo(0)
                    // 1 item + 1 header
                    val item = awaitItem()
                    assertThat(item.filterIsInstance<RecentActionItemType.Item>().size).isEqualTo(1)
                    assertThat(item.filterIsInstance<RecentActionItemType.Header>().size).isEqualTo(
                        1)
                }
        }

    @Test
    fun `test that 2 recent action items are grouped under same header if timestamp is same`() =
        runTest {
            whenever(getRecentActions()).thenReturn(
                listOf(
                    megaRecentActionBucket,
                    megaRecentActionBucket2,
                )
            )

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    // initialization
                    assertThat(awaitItem().size).isEqualTo(0)
                    // 1 header + 2 items
                    val item = awaitItem()
                    assertThat(item.filterIsInstance<RecentActionItemType.Item>().size)
                        .isEqualTo(2)
                    assertThat(item.filterIsInstance<RecentActionItemType.Header>().size)
                        .isEqualTo(1)
                }
        }

    @Test
    fun `test that 2 recent action items are under two different headers if timestamp is different`() =
        runTest {
            whenever(getRecentActions()).thenReturn(
                listOf(
                    megaRecentActionBucket,
                    megaRecentActionBucket3,
                )
            )

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem().size).isEqualTo(0)
                    // 2 header + 2 items
                    val item = awaitItem()
                    assertThat(item.filterIsInstance<RecentActionItemType.Item>().size)
                        .isEqualTo(2)
                    assertThat(item.filterIsInstance<RecentActionItemType.Header>().size)
                        .isEqualTo(2)
                }
        }

    @Test
    fun `test that the recent action item is populated with the fullName if retrieved from email`() =
        runTest {
            val expected = "FirstName LastName"
            val contact = mock<ContactData> {
                on { fullName }.thenReturn(expected)
            }
            val contactItem = mock<ContactItem> {
                on { email }.thenReturn("aaa@aaa.com")
                on { contactData }.thenReturn(contact)
            }
            whenever(getRecentActions()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getVisibleContacts()).thenReturn(listOf(contactItem))

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).userName)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that the recent action item is populated with empty string if not retrieved from email`() =
        runTest {
            val expected = ""
            val contactItem = mock<ContactItem> {
                on { email }.thenReturn("aaa@aaa.com")
                on { contactData }.thenReturn(mock())
            }
            whenever(getRecentActions()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getVisibleContacts()).thenReturn(listOf(contactItem))

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).userName)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that recent action items is updated when receiving a node update`() =
        runTest {
            whenever(getRecentActions()).thenReturn(emptyList())

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem().size).isEqualTo(0)
                    advanceUntilIdle()
                    monitorNodeUpdates.emit(listOf(mock()))
                    whenever(getRecentActions()).thenReturn(listOf(megaRecentActionBucket))
                    assertThat(awaitItem().size).isEqualTo(2)
                }
            verify(getRecentActions, times(2)).invoke()
        }

    @Test
    fun `test that hide recent activity is set with value of monitor hide recent activity`() =
        runTest {
            whenever(monitorHideRecentActivity()).thenReturn(
                flow {
                    emit(true)
                    emit(false)
                }
            )
            underTest.state.map { it.hideRecentActivity }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(false)
                    assertThat(awaitItem()).isEqualTo(true)
                    assertThat(awaitItem()).isEqualTo(false)
                }
        }

    @Test
    fun `test that disable hide recent activity calls setHideRecentActivity use case with value true `() =
        runTest {
            underTest.disableHideRecentActivitySetting()
            advanceUntilIdle()
            verify(setHideRecentActivity).invoke(false)
        }

    @Test
    fun `test that calling select will set selected and snapShotActionList properties`() =
        runTest {
            val expectedSelected = megaRecentActionBucket
            val expectedSnapshotActionList = listOf(megaRecentActionBucket)
            whenever(getRecentActions()).thenReturn(expectedSnapshotActionList)
            assertThat(underTest.selected).isEqualTo(null)
            assertThat(underTest.snapshotActionList).isEqualTo(null)
            advanceUntilIdle()
            underTest.select(expectedSelected)
            assertThat(underTest.selected).isEqualTo(expectedSelected)
            assertThat(underTest.snapshotActionList).isEqualTo(expectedSnapshotActionList)
        }


}