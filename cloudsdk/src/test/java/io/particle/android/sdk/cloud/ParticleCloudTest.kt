package io.particle.android.sdk.cloud

import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import io.particle.android.sdk.cloud.ApiDefs.CloudApi
import io.particle.android.sdk.cloud.ApiDefs.IdentityApi
import io.particle.android.sdk.cloud.ApiFactory.OauthBasicAuthCredentialsProvider
import io.particle.android.sdk.cloud.ApiFactory.TokenGetterDelegate
import io.particle.android.sdk.cloud.Responses.CallFunctionResponse
import io.particle.android.sdk.cloud.Responses.CardOnFileResponse
import io.particle.android.sdk.cloud.Responses.ClaimCodeResponse
import io.particle.android.sdk.cloud.Responses.DeviceMeshMembership
import io.particle.android.sdk.cloud.Responses.FirmwareUpdateInfoResponse
import io.particle.android.sdk.cloud.Responses.LogInResponse
import io.particle.android.sdk.cloud.Responses.MeshNetworkRegistrationResponse
import io.particle.android.sdk.cloud.Responses.Models.CompleteDevice
import io.particle.android.sdk.cloud.Responses.PingResponse
import io.particle.android.sdk.cloud.Responses.ReadDoubleVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadIntVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadObjectVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadStringVariableResponse
import io.particle.android.sdk.cloud.Responses.SimpleResponse
import io.particle.android.sdk.cloud.models.*
import io.particle.android.sdk.persistance.AppDataStorage
import io.particle.android.sdk.persistance.SensitiveDataStorage
import io.particle.android.sdk.utils.Broadcaster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit.RestAdapter.LogLevel
import retrofit.client.Response
import retrofit.mime.TypedOutput
import java.util.*
import java.util.concurrent.Executors


private const val FAKE_API_PORT = 8080
private val FAKE_API_URL = HttpUrl.parse("http://localhost:$FAKE_API_PORT")


class ParticleCloudTest {

    lateinit var cloud: ParticleCloud
    lateinit var mockServer: MockWebServer

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

        // build the cloud itself
        cloud = ParticleCloud(
            FAKE_API_URL,
            factory.buildNewCloudApi(),
            factory.buildNewIdentityApi(),
            SDKGlobals.appDataStorage!!,
            FakeBroadcaster(),
            Gson(),
            Executors.newSingleThreadExecutor()
        )
    }

    @Test
    fun test_generateClaimCode() {
        val claimCode = "fed14c9c3b04058562b193c992f37b18604ca0e8"
        val deviceId1 = "fa80c4a898849e1bb35ada62"
        val deviceId2 = "afada5fe1b4fa16e9f499416"

        val mockedResponse = MockResponse()
        mockedResponse.setResponseCode(200)
        // For many responses with smaller bodies, we could do this JSON inline, right in the test.
        // For larger response bodies, we can bust it out into separate test  files
        mockedResponse.setBody(""" {
            claim_code: "$claimCode",
            device_ids: [
                "$deviceId1",
                "$deviceId2"
            ]
        }""".trimIndent()
        )

        // set the response
        mockServer.enqueue(mockedResponse)

        // make the API call
        val claimCodeResponse = cloud.generateClaimCode()
        // grab the request that was given to the mock server
        val request = mockServer.takeRequest()

        // compare our results!
        assertEquals(claimCode, claimCodeResponse.claimCode)
        // We don't actually care about this, but it demonstrates the ability to examine requests
        assertEquals(request.body.readUtf8(), "blank=okhttp_appeasement")
    }

}


class FakeAppDataStorage : AppDataStorage {

    override val userHasClaimedDevices: Boolean
        get() = TODO("not implemented")

    override fun saveUserHasClaimedDevices(value: Boolean) {
        TODO("not implemented")
    }

    override fun resetUserHasClaimedDevices() {
        TODO("not implemented")
    }

}



class FakeBroadcaster : Broadcaster {

    override fun sendBroadcast(intent: Intent) {
        TODO("not implemented")
    }

}


class FakeSensitiveDataStorage() : SensitiveDataStorage {

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