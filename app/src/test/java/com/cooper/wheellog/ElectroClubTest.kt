package com.cooper.wheellog

import com.cooper.wheellog.data.AppDatabase
import com.cooper.wheellog.data.TripEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class ElectroClubTest {
    private lateinit var wd: WheelData
    private lateinit var ec: ElectroClub
    private var successCalls = 0
    private var errorCalls = 0
    private lateinit var tripEntity: TripEntity
    private val data = byteArrayOf()
    private val fileName = "fileName"

    @Before
    fun setUp() {
        ec = ElectroClub()
        successCalls = 0
        errorCalls = 0
        ec.successListener = { _: String?, _: Any? -> successCalls++ }
        ec.errorListener = { _: String?, _: Any? -> errorCalls++ }
        wd = spyk(WheelData())
        tripEntity = TripEntity(fileName = fileName)
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
        WheelLog.db = mockkClass(AppDatabase::class, relaxed = true)
        coEvery { WheelLog.db.tripDao().getTripByFileName(any()) } returns tripEntity
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns wd
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `login success`() = runBlocking {
        // Assign.
        val email = "email"
        val password = "password"
        val server = MockWebServer()
        server.enqueue(MockResponse()
                .setBody("{\"status\":\"ok\",\"data\":{\"user\":{\"user_token\":\"super\",\"email\":\"email\",\"user_id\":\"123456\",\"nickname\":\"xxx\",\"thumbnail\":\"https:\\/\\/electro.club\\/images\\/noavatar.jpg\",\"account_created\":666,\"last_visit\":777}}}"))
        server.start()
        val url = server.url("/testendpoint")
        ec.url = url.toString()

        // Act.
        val success = suspendCoroutine<Boolean> { continuation ->
            ec.login(email, password) {
                continuation.resume(it)
            }
        }

        // Assert.
        assertThat(success).isEqualTo(true)
        assertThat(ec.lastError).isEqualTo(null)
        assertThat(successCalls).isEqualTo(1)
        assertThat(errorCalls).isEqualTo(0)
    }

    @Test
    fun `login - 500`() = runBlocking {
        // Assign.
        val email = "email"
        val password = "password"
        val server = MockWebServer()
        server.enqueue(MockResponse()
                .setResponseCode(500)
                .setBody("some text"))
        server.start()
        val url = server.url("/testendpoint")
        ec.url = url.toString()

        // Act.
        val success = suspendCoroutine<Boolean> { continuation ->
            ec.login(email, password) {
                continuation.resume(it)
            }
        }

        // Assert.
        assertThat(success).isEqualTo(false)
        assertThat(ec.lastError).isEqualTo("500 exception")
        assertThat(successCalls).isEqualTo(0)
        assertThat(errorCalls).isEqualTo(1)
    }

    @Test
    fun `uploadTrackAsync 200 - less than 5 seconds`() = runBlocking {
        // Assign.
        val server = MockWebServer()
        server.enqueue(MockResponse()
                .setBody("{\"status\":\"failure\",\"data\":{\"error\":\"Total track time less than 5 seconds\"}}"))
        server.start()
        val url = server.url("/testendpoint")
        ec.url = url.toString()

        // Act.
        val success = ec.uploadTrackAsync(data, fileName, true)
        server.shutdown()

        // Assert.
        assertThat(success).isEqualTo(false)
        assertThat(ec.lastError).isEqualTo("electro club id is wrong")
        assertThat(successCalls).isEqualTo(0)
        assertThat(errorCalls).isEqualTo(1)
    }

    @Test
    fun `uploadTrackAsync 400 - less than 5 seconds`() = runBlocking {
        // Assign.
        val server = MockWebServer()
        server.enqueue(MockResponse()
                .setResponseCode(400)
                .setBody("{\"status\":\"failure\",\"data\":{\"error\":\"Total track time less than 5 seconds\"}}"))
        server.start()
        val url = server.url("/testendpoint")
        ec.url = url.toString()

        // Act.
        val success = ec.uploadTrackAsync(data, fileName, true)
        server.shutdown()

        // Assert.
        assertThat(success).isEqualTo(false)
        assertThat(ec.lastError).isEqualTo("Total track time less than 5 seconds")
        assertThat(successCalls).isEqualTo(0)
        assertThat(errorCalls).isEqualTo(1)
    }

    @Test
    fun `uploadTrackAsync 500 - without body`() = runBlocking {
        // Assign.
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(500))
        server.start()
        val url = server.url("/testendpoint")
        ec.url = url.toString()

        // Act.
        val success = ec.uploadTrackAsync(data, fileName, true)
        server.shutdown()

        // Assert.
        assertThat(success).isEqualTo(false)
        assertThat(ec.lastError).contains("500 exception")
        assertThat(successCalls).isEqualTo(0)
        assertThat(errorCalls).isEqualTo(1)
    }

    @Test
    fun `uploadTrackAsync 200 - success`() = runBlocking {
        // Assign.
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("{\"status\":\"ok\",\"data\":{\"track\":{\"id\":\"666\",\"user_id\":\"777\",\"name\":\"2022_03_21_19_30_37.csv\",\"garage_id\":\"9999\",\"uploaded_time\":1616347584,\"start_time\":1616340639,\"duration\":6652,\"image\":\"https:\\/\\/electro.club\\/data\\/map\\/22451\\/track-72485b82b22769b98723bbcea83ca764.png\",\"stats_image\":\"https:\\/\\/electro.club\\/data\\/map\\/22451\\/stats-771668db2aa05c3541f7cd4c91b95942.png\"}}}"))
        server.start()
        val url = server.url("/testendpoint")
        ec.url = url.toString()

        // Act.
        val success = ec.uploadTrackAsync(data, fileName, true)
        server.shutdown()

        // Assert.
        val req = server.takeRequest()
        assertThat(req.method).isEqualTo("POST")
        assertThat(success).isEqualTo(true)
        assertThat(ec.lastError).isEqualTo(null)
        assertThat(successCalls).isEqualTo(1)
        assertThat(errorCalls).isEqualTo(0)
        assertThat(tripEntity.ecId).isEqualTo(666)
        assertThat(tripEntity.transportId).isEqualTo(9999)
        assertThat(tripEntity.startTime).isEqualTo(1616340639)
        assertThat(tripEntity.urlImage).isEqualTo("https://electro.club/data/map/22451/track-72485b82b22769b98723bbcea83ca764.png")
        assertThat(tripEntity.urlImage2).isEqualTo("https://electro.club/data/map/22451/stats-771668db2aa05c3541f7cd4c91b95942.png")
        assertThat(tripEntity.duration).isEqualTo(6652)
    }
}