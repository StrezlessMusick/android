package test.mega.privacy.android.app.presentation.photos.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.albums.model.mapper.UIAlbumMapper
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumsMap
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.RemoveFavourites
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class AlbumsViewModelTest {
    private lateinit var underTest: AlbumsViewModel

    private val getDefaultAlbumPhotos = mock<GetDefaultAlbumPhotos>()
    private val uiAlbumMapper = mock<UIAlbumMapper>()
    private val getFeatureFlag =
        mock<GetFeatureFlagValue> { onBlocking { invoke(any()) }.thenReturn(true) }
    private val getDefaultAlbumsMap = mock<GetDefaultAlbumsMap>()
    private val removeFavourites = mock<RemoveFavourites>()
    private val getNodeListByIds = mock<GetNodeListByIds>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        whenever(uiAlbumMapper(any(), any())).thenAnswer {
            val photos = it.arguments[0] as List<Photo>
            UIAlbum(
                id = it.arguments[1] as Album,
                title = { _ -> "" },
                count = photos.size,
                coverPhoto = photos.maxByOrNull { it.modificationTime },
                photos = photos
            )
        }

        underTest = AlbumsViewModel(
            getDefaultAlbumPhotos = getDefaultAlbumPhotos,
            getDefaultAlbumsMap = getDefaultAlbumsMap,
            uiAlbumMapper = uiAlbumMapper,
            getFeatureFlag = getFeatureFlag,
            removeFavourites = removeFavourites,
            getNodeListByIds = getNodeListByIds
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.albums).isEmpty()
        }
    }

    @Test
    fun `test that an error would return an empty list`() = runTest {
        whenever(getDefaultAlbumPhotos(listOf())).thenReturn(flow { throw Exception("Error") })

        underTest.state.test {
            assertEquals(emptyList(), awaitItem().albums)
        }
    }

    @Test
    fun `test that returned albums are added to the state if there are photos for them`() =
        runTest {
            val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
                Album.FavouriteAlbum to { true },
                Album.GifAlbum to { true },
                Album.RawAlbum to { true },
            )
            whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

            whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(listOf(createImage())))

            underTest.state.drop(1).test {
                assertThat(awaitItem().albums.map { it.id }).containsExactlyElementsIn(defaultAlbums.keys)
            }
        }

    @Test
    fun `test that albums are not added, if there are no photos in them`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { true },
            Album.RawAlbum to { false },
        )
        whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

        whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(listOf(createImage())))

        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.id })
                .containsExactlyElementsIn(defaultAlbums.keys.filterNot { it == Album.RawAlbum })
        }
    }

    @Test
    fun `test that favourite album is displayed even if it contains no photos`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { false },
            Album.GifAlbum to { false },
            Album.RawAlbum to { false },
        )
        whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

        whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(emptyList()))

        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.id })
                .containsExactlyElementsIn(defaultAlbums.keys.filter { it == Album.FavouriteAlbum })
        }
    }

    @Test
    fun `test that feature flag filters out the correct albums`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { true },
            Album.RawAlbum to { true },
        )


        whenever(getFeatureFlag(any())).thenReturn(false)

        whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

        whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(emptyList()))

        underTest.state.drop(1).test {
            val albums = awaitItem().albums
            assertEquals(albums.size, 1)
            assertThat(albums.first().id).isEqualTo(Album.FavouriteAlbum)
        }
    }

    @Test
    fun `test that album is using latest modification time photo`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { false },
            Album.RawAlbum to { false },
        )


        whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

        whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(listOf(
            createImage(id = 1L, modificationTime = LocalDateTime.MAX),
            createImage(id = 2L, modificationTime = LocalDateTime.MIN)
        )))

        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.coverPhoto?.id }.firstOrNull()).isEqualTo(1L)
        }
    }


    private fun createImage(
        id: Long = 2L,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
        fileTypeInfo: FileTypeInfo = StaticImageFileTypeInfo("", ""),
    ): Photo {
        return Photo.Image(
            id = id,
            parentId = parentId,
            name = "",
            isFavourite = isFavourite,
            creationTime = LocalDateTime.now(),
            modificationTime = modificationTime,
            thumbnailFilePath = "thumbnailFilePath",
            previewFilePath = "previewFilePath",
            fileTypeInfo = fileTypeInfo
        )
    }
}