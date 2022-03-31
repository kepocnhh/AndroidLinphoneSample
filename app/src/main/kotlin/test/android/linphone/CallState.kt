package test.android.linphone

import android.content.Context
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import sp.kx.functional.subject.PublishSubject
import sp.kx.functional.subject.Subject
import sp.kx.functional.subject.SubjectConsumer

object CallState {
    sealed interface Broadcast {
        class OnRegistration(val account: Account) : Broadcast
        class OnCallState(val call: Call) : Broadcast
    }

    private val subject: Subject<Broadcast> = PublishSubject()
    val broadcast: SubjectConsumer<Broadcast> = subject
    private val coreListenerStub = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            when (account.state) {
                RegistrationState.Failed -> {
                    try {
                        requireNotNull(coreInternal).stop()
                    } catch (e: Throwable) {
                        println("stop error: $e")
                    }
                    coreInternal = null
                }
            }
            subject next Broadcast.OnRegistration(account)
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            subject next Broadcast.OnCallState(call)
        }
    }

    private var coreInternal: Core? = null

    fun start(
        context: Context,
        userFromName: String,
        userFromPassword: String,
        domain: String
    ) {
        check(coreInternal == null)
        val core = Factory.instance().createCore(
            context = context,
            enableVideoCapture = true,
            enableVideoDisplay = true,
            automaticallyAccept = true,
            userFromName = userFromName,
            userFromPassword = userFromPassword,
            domain = domain,
            transportType = TransportType.Udp,
            isRegisterEnabled = true,
            expires = 10,
            contactParameters = emptyMap(),
            listener = coreListenerStub
        )
        coreInternal = core
        core.start()
    }

    fun stop() {
        val core = requireNotNull(coreInternal)
        core.removeListener(coreListenerStub)
        core.clearAccounts()
        core.clearAllAuthInfo()
        core.stop()
        this.coreInternal = null
    }

    fun makeCall(userToName: String) {
        val core = requireNotNull(coreInternal)
        val account = core.accountList.single()
        check(account.state == RegistrationState.Ok)
        val params = core.createCallParams(null) ?: TODO()
        params.enableAudio(true)
        params.enableVideo(true)
        val authInfo = core.authInfoList.single()
        core.inviteWithParams("sip:$userToName@${authInfo.domain}", params)
    }

    fun getCore(): Core? {
        return coreInternal
    }
}
