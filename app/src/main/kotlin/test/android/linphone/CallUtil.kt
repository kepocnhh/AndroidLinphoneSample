package test.android.linphone

import android.content.Context
import org.linphone.core.AccountParams
import org.linphone.core.AuthInfo
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.TransportType

object CallUtil {
    const val CAMERA_FRONT_KEY = "FrontFacingCamera"
    const val CAMERA_BACK_KEY = "BackFacingCamera"
}

private fun createAuthInfo(
    userFromName: String,
    userFromPassword: String,
    domain: String
): AuthInfo {
    return Factory.instance().createAuthInfo(
        userFromName, null, userFromPassword, null, null, domain, null
    )
}

private fun Core.createAccountParams(
    userFromName: String,
    domain: String,
    transportType: TransportType,
    isRegisterEnabled: Boolean,
    expires: Int,
    contactParameters: Map<String, String>
): AccountParams {
    val accountParams = createAccountParams()
    val identity = Factory.instance().createAddress("sip:$userFromName@$domain")!!
    accountParams.identityAddress = identity
    val address = Factory.instance().createAddress("sip:$domain")!!
    address.transport = transportType
    accountParams.serverAddress = address
    accountParams.isRegisterEnabled = isRegisterEnabled
    accountParams.expires = expires
    accountParams.contactParameters =
        contactParameters.toList().joinToString(separator = ";") { (k, v) -> "$k=$v" }
    return accountParams
}

fun Factory.createCore(
    context: Context,
    isVideoCaptureEnabled: Boolean,
    isVideoDisplayEnabled: Boolean,
    automaticallyAccept: Boolean,
    userFromName: String,
    userFromPassword: String,
    domain: String,
    transportType: TransportType,
    isRegisterEnabled: Boolean,
    expires: Int,
    contactParameters: Map<String, String>,
    listener: CoreListenerStub
): Core {
    val core = createCore(null, null, context)
    core.isVideoCaptureEnabled = isVideoCaptureEnabled
    core.isVideoDisplayEnabled = isVideoDisplayEnabled
    core.videoActivationPolicy.automaticallyAccept = automaticallyAccept
    val authInfo = createAuthInfo(
        userFromName = userFromName,
        userFromPassword = userFromPassword,
        domain = domain
    )
    core.addAuthInfo(authInfo)
    val accountParams = core.createAccountParams(
        userFromName = userFromName,
        domain = domain,
        transportType = transportType,
        isRegisterEnabled = isRegisterEnabled,
        expires = expires,
        contactParameters = contactParameters
    )
    val account = core.createAccount(accountParams)
    core.addAccount(account)
    core.addListener(listener)
    return core
}
