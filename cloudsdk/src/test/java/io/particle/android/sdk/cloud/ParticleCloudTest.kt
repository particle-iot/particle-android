package io.particle.android.sdk.cloud

import android.content.Intent
import com.google.gson.Gson
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.squareup.okhttp.mockwebserver.RecordedRequest
import io.particle.android.sdk.cloud.ApiFactory.OauthBasicAuthCredentialsProvider
import io.particle.android.sdk.cloud.ApiFactory.TokenGetterDelegate
import io.particle.android.sdk.cloud.Responses.ClaimCodeResponse
import io.particle.android.sdk.persistance.AppDataStorage
import io.particle.android.sdk.persistance.SensitiveDataStorage
import io.particle.android.sdk.utils.Broadcaster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit.RestAdapter.LogLevel
import java.util.*
import java.util.concurrent.Executors


private const val FAKE_API_PORT = 8080
private val FAKE_API_URL = HttpUrl.parse("http://localhost:$FAKE_API_PORT")


class ParticleCloudTest {

    lateinit var cloud: ParticleCloud
    lateinit var mockServer: MockWebServer
    private lateinit var mainApi: ApiDefs.CloudApi

    @Before
    fun setUp() {
        initCloud()
    }

    @After
    fun teardown() {
        // close the socket, stop the listening thread
        mockServer.shutdown()
    }

    private fun initCloud() {
        // start the mock webserver
        mockServer = MockWebServer()
        mockServer.start(FAKE_API_PORT)

        // build up a few dependency fakes
        SDKGlobals.appDataStorage = FakeAppDataStorage()
        SDKGlobals.sensitiveDataStorage = FakeSensitiveDataStorage()

        val tokenDelegate = TokenGetterDelegate { "d34db33fd34db33fd34db33fd34db33fd34db33f" }
        val credsProvider = object : OauthBasicAuthCredentialsProvider {
            override fun getClientSecret(): String = "I_AM_A_CLIENT_SECRET"
            override fun getClientId(): String = "I_AM_A_CLIENT_ID"
        }

        // use actual ApiFactory to build other dependencies
        val factory = ApiFactory(FAKE_API_URL, LogLevel.FULL, tokenDelegate, credsProvider)

        mainApi = factory.buildNewCloudApi()

        // build the cloud itself
        cloud = ParticleCloud(
            FAKE_API_URL,
            mainApi,
            factory.buildNewIdentityApi(),
            SDKGlobals.appDataStorage!!,
            FakeBroadcaster(),
            Gson(),
            Executors.newSingleThreadExecutor()
        )
    }

    @Test
    fun test_generateClaimCode_HappyPath() {
        val (claimCode, request, claimCodeResponse) = setUpClaimCodeTest()

        assertEquals(claimCode, claimCodeResponse.claimCode)
        assertEquals("blank=okhttp_appeasement", request.body.readUtf8())
    }

    @Test
    fun test_generateClaimCodeWithProductId_HappyPath() {
        val productId = 42
        val (claimCode, request, claimCodeResponse) = setUpClaimCodeTest(productId)

        assertEquals(claimCode, claimCodeResponse.claimCode)
        assertEquals("blank=okhttp_appeasement", request.body.readUtf8())
        // verify that we are using the product ID URL
        assertEquals("/v1/products/$productId/device_claims", request.path)
    }

    private fun setUpClaimCodeTest(productId: Int? = null): Triple<String, RecordedRequest, ClaimCodeResponse> {
        val claimCode = "fed14c9c3b04058562b193c992f37b18604ca0e8"
        val deviceId1 = "fa80c4a898849e1bb35ada62"
        val deviceId2 = "afada5fe1b4fa16e9f499416"

        val mockedResponse = enqueueNew200ResponseWithBody(
            """ {
            claim_code: "$claimCode",
            device_ids: [
                "$deviceId1",
                "$deviceId2"
            ]
        }"""
        )

        // set the response
        mockServer.enqueue(mockedResponse)

        // make the API call
        val claimCodeResponse = if (productId == null) {
            cloud.generateClaimCode()
        } else {
            cloud.generateClaimCode(productId)
        }
        // grab the request that was given to the mock server
        val request = mockServer.takeRequest()

        return Triple(claimCode, request, claimCodeResponse)
    }

    @Test
    fun test_getDevices_PopulatedDeviceListHappyPath() {
        val device0 = ParticleDevice(
            mainApi,
            cloud,
            DEVICE_STATE_0
        )
        val device1 = ParticleDevice(
            mainApi,
            cloud,
            DEVICE_STATE_1
        )

        enqueueNew200ResponseWithBody(DEVICE_LIST_JSON)
        val devices = cloud.getDevices()
        assertEquals(devices[0], device0)
        assertEquals(devices[1], device1)
    }

    @Test
    fun test_getDevice_SingleDeviceHappyPath() {
        val device1 = ParticleDevice(mainApi, cloud, DEVICE_STATE_1_FULL)
        enqueueNew200ResponseWithBody(DEVICE_1_FULL_JSON)
        val deviceFromCloud = cloud.getDevice(device1.id)
        assertEquals(device1, deviceFromCloud)
    }

    private fun enqueueNew200ResponseWithBody(body: String): MockResponse {
        val mockedResponse = MockResponse()
        mockedResponse.setResponseCode(200)
        mockedResponse.setBody(body.trimIndent())
        mockServer.enqueue(mockedResponse)
        return mockedResponse
    }

}


class FakeAppDataStorage : AppDataStorage {

    override val userHasClaimedDevices: Boolean
        get() = TODO("not implemented")

    var saveUserHasClaimedDevicesWasCalled = false

    override fun saveUserHasClaimedDevices(value: Boolean) {
        saveUserHasClaimedDevicesWasCalled = true
    }

    override fun resetUserHasClaimedDevices() {
        TODO("not implemented")
    }

}



class FakeBroadcaster : Broadcaster {

    override fun sendBroadcast(intent: Intent) {
        println("Broadcasting intent (content not shown Because Android Reasons)")
    }

}


class FakeSensitiveDataStorage : SensitiveDataStorage {

    var _user: String? = null
    override val user: String?
        get() = _user

    var _password: String? = null
    override val password: String?
        get() = _password

    var _token: String? = null
    override val token: String?
        get() = _token

    var _refreshToken: String? = null
    override val refreshToken: String?
        get() = _refreshToken

    var _tokenExpirationDate: Date? = null
    override val tokenExpirationDate: Date?
        get() = _tokenExpirationDate

    var _hasEverHadStoredUsername: Boolean = false
    override val hasEverHadStoredUsername: Boolean
        get() = _hasEverHadStoredUsername

    override fun saveUser(user: String?) {
        _user = user
    }

    override fun resetUser() {
        _user = null
    }

    override fun savePassword(password: String?) {
        _password = password
    }

    override fun resetPassword() {
        _password = null
    }

    override fun saveToken(token: String?) {
        _token = token
    }

    override fun resetToken() {
        _token = null
    }

    override fun resetRefreshToken() {
        _refreshToken = null
    }

    override fun saveRefreshToken(token: String?) {
        _refreshToken = token
    }

    override fun saveTokenExpirationDate(expirationDate: Date) {
        _tokenExpirationDate = expirationDate
    }

    override fun resetTokenExpirationDate() {
        _tokenExpirationDate = null
    }

    override fun saveHasEverHadStoredUsername(value: Boolean) {
        _hasEverHadStoredUsername = value
    }

}