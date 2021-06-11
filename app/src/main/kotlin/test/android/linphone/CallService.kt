package test.android.linphone

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Surface
import android.widget.RemoteViews
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.CallParams
import org.linphone.core.CallStats
import org.linphone.core.ConfiguringState
import org.linphone.core.Factory
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.InfoMessage
import org.linphone.core.RegistrationState
import org.linphone.core.StreamType
import org.linphone.core.TransportType

class CallService : Service() {
    companion object {
        const val ACTION_STATE_REQUEST = "ACTION_STATE_REQUEST"
        const val ACTION_STATE_BROADCAST = "ACTION_STATE_BROADCAST"
        const val KEY_STATE = "KEY_STATE"
        const val ACTION_REGISTRATION = "ACTION_REGISTRATION"
        const val KEY_HOST = "KEY_HOST"
        const val KEY_REALM = "KEY_REALM"
        const val KEY_PORT = "KEY_PORT"
        const val KEY_USER_FROM_NAME = "KEY_USER_FROM_NAME"
        const val KEY_USER_FROM_PASSWORD = "KEY_USER_FROM_PASSWORD"
        const val KEY_CODE = "KEY_CODE"
        const val ACTION_EXIT = "ACTION_EXIT"
        const val ACTION_CALL_CANCEL = "ACTION_CALL_CANCEL"
        const val ACTION_CALL_CONFIRM = "ACTION_CALL_CONFIRM"
        const val ACTION_SCREEN_STATE_BROADCAST = "ACTION_SCREEN_STATE_BROADCAST"
        const val KEY_SCREEN_STATE = "KEY_SCREEN_STATE"
        const val VALUE_CREATED = "VALUE_CREATED"
        const val VALUE_RESUMED = "VALUE_RESUMED"
        const val VALUE_PAUSED = "VALUE_PAUSED"
        const val VALUE_DESTROYED = "VALUE_DESTROYED"
        const val ACTION_CALL_STATE_BROADCAST = "ACTION_CALL_STATE_BROADCAST"
        const val KEY_CALL_STATE = "KEY_CALL_STATE"
        const val VALUE_OUTGOING = "VALUE_OUTGOING"
        const val VALUE_INCOMING = "VALUE_INCOMING"
        const val VALUE_CONFIRMED = "VALUE_CONFIRMED"
        const val VALUE_DISCONNECTED = "VALUE_DISCONNECTED"
        const val VALUE_NONE = "VALUE_NONE"
        const val KEY_CALL_TIME_START = "KEY_CALL_TIME_START"
        const val ACTION_CALL_STATE_REQUEST = "ACTION_CALL_STATE_REQUEST"
        const val CHANNEL_INCOMING_CALL = "CHANNEL_INCOMING_CALL"
        const val CHANNEL_CALL = "CHANNEL_CALL"
        const val ACTION_NONE = "ACTION_NONE"
        const val ACTION_CHECK_PERMISSION_REQUEST = "ACTION_CHECK_PERMISSION_REQUEST"
        const val KEY_PERMISSIONS = "KEY_PERMISSIONS"
        const val ACTION_MEDIA_STATE_BROADCAST = "ACTION_MEDIA_STATE_BROADCAST"
        const val VALUE_VIDEO_SURFACE = "VALUE_VIDEO_SURFACE"
        const val KEY_MEDIA_TYPE = "KEY_MEDIA_TYPE"
        const val ACTION_SET_VIDEO_SURFACE = "ACTION_SET_VIDEO_SURFACE"

        private var isChannelsReady = false
        private fun startForeground(
            id: Int,
            service: Service,
            channel: String,
            contentView: RemoteViews,
            contentIntent: PendingIntent
        ) {
            val builder = NotificationCompat.Builder(service, channel)
                .setContentIntent(contentIntent)
                .setContent(contentView)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Test Service")
                .setContentText("CUSTOM MESSAGE")
            service.startForeground(id, builder.build())
        }

        private fun switchSpeakerphone(context: Context, isSpeakerphoneOn: Boolean) {
            val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)!!
            Log.d("[CallService|${context.hashCode()}]", "new $isSpeakerphoneOn / old ${audioManager.isSpeakerphoneOn}")
            if (audioManager.isSpeakerphoneOn == isSpeakerphoneOn) return
            if (isSpeakerphoneOn) {
                audioManager.mode = AudioManager.MODE_IN_CALL
            } else {
                audioManager.mode = AudioManager.MODE_NORMAL
            }
            audioManager.isSpeakerphoneOn = isSpeakerphoneOn
        }
    }
    private val TAG = "[CallService|${hashCode()}]"

    private var callStateSide: String = VALUE_NONE
    private var state: State = State.NONE
    private fun setState(newState: State) {
        if (state == newState) return
        state = newState
        sendBroadcast(Intent(ACTION_STATE_BROADCAST).also {
            it.putExtra(KEY_STATE, newState.name)
        })
    }
    private var core: Core? = null
    private var account: Account? = null
    private var coreListenerStub: CoreListenerStub? = null
    private var call: Call? = null

    private var callTimeStart: Long? = null

    private fun startForegroundCall() {
        val contentView = RemoteViews(packageName, R.layout.notification_call)
        val pendingIntentCancel = PendingIntent.getBroadcast(this, 0, Intent(ACTION_CALL_CANCEL), PendingIntent.FLAG_UPDATE_CURRENT)
        contentView.setOnClickPendingIntent(R.id.cancel, pendingIntentCancel)
        val contentIntent = PendingIntent.getBroadcast(this, 2, Intent(ACTION_NONE), 0)
        startForeground(
            id = 1235,
            service = this,
            channel = CHANNEL_CALL,
            contentIntent = contentIntent,
            contentView = contentView
        )
    }
    private fun startForegroundIncoming() {
        val contentView = RemoteViews(packageName, R.layout.notification_incoming_call)
        val pendingIntentCancel = PendingIntent.getBroadcast(this, 0, Intent(ACTION_CALL_CANCEL), PendingIntent.FLAG_UPDATE_CURRENT)
        contentView.setOnClickPendingIntent(R.id.cancel, pendingIntentCancel)
        val pendingIntentConfirm = PendingIntent.getBroadcast(this, 1, Intent(ACTION_CALL_CONFIRM), PendingIntent.FLAG_UPDATE_CURRENT)
        contentView.setOnClickPendingIntent(R.id.confirm, pendingIntentConfirm)
        val contentIntent = PendingIntent.getBroadcast(this, 2, Intent(ACTION_NONE), 0)
        startForeground(
            id = 1234,
            service = this,
            channel = CHANNEL_INCOMING_CALL,
            contentIntent = contentIntent,
            contentView = contentView
        )
    }
    private fun onStateRequest() {
        sendBroadcast(Intent(ACTION_STATE_BROADCAST).also {
            it.putExtra(KEY_STATE, state.name)
        })
    }
    private fun onIncomingCall(call: Call) {
        check(this.call == null)
        this.call = call
        callStateSide = VALUE_INCOMING
        setState(State.CALLING)
        if (!MainActivity.isResumed()) {
            startForegroundIncoming()
        }
        println("call " + call.remoteAddress.asStringUriOnly())
    }
//    private fun toString(item: CallParams): String {
//        return mapOf(
//            "" to item.getCustomSdpMediaAttribute()
//        )
//    }
    private fun onRegistration(
        host: String,
        realm: String,
        port: Int,
        userFromName: String,
        userFromPassword: String
    ) {
        check(core == null)
        val core = Factory.instance().createCore(null, null, this)
        core.enableVideoCapture(false)
//        core.enableVideoCapture(true)
        core.enableVideoDisplay(true)
        core.videoActivationPolicy.automaticallyAccept = true
//        core.videoActivationPolicy.automaticallyInitiate = true
        this.core = core
        val domain = host
        val accountParams = core.createAccountParams()
        val identity = Factory.instance().createAddress("sip:$userFromName@$domain")!!
        accountParams.identityAddress = identity
        val address = Factory.instance().createAddress("sip:$domain")!!
        address.transport = TransportType.Udp // todo
        accountParams.serverAddress = address
        accountParams.registerEnabled = true
        accountParams.expires = 10
        val authInfo = Factory.instance().createAuthInfo(
            userFromName, null, userFromPassword, null, null, domain, null
        )
        core.addAuthInfo(authInfo)
        check(account == null)
        val account = core.createAccount(accountParams)
        this.account = account
        core.addAccount(account)
//        core.defaultAccount = account // todo
        check(coreListenerStub == null)
        val listener = object: CoreListenerStub() {
            override fun onAccountRegistrationStateChanged(
                core: Core,
                account: Account,
                state: RegistrationState,
                message: String
            ) {
                println("Registration: $state, $message")
                when (state) {
                    RegistrationState.Ok -> {
                        setState(State.READY)
                    }
                    RegistrationState.Failed -> {
                        onAccountFinish()
                        setState(State.NONE)
                    }
                }
            }

            override fun onCallStateChanged(
                core: Core,
                call: Call,
                state: Call.State?,
                message: String
            ) {
                println("call state: $state")
                when (state) {
                    Call.State.IncomingReceived -> {
                        onIncomingCall(call = call)
                    }
                    Call.State.Connected -> {
                        val time = System.currentTimeMillis()
                        callTimeStart = time
                        sendBroadcast(Intent(ACTION_CALL_STATE_REQUEST))
                    }
                    Call.State.StreamsRunning -> {
                        val definition = core.preferredVideoDefinition
//                        val definition = call.params.receivedVideoDefinition!!
//                        val definition = call.params.sentVideoDefinition!!
                        sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                            it.putExtra(KEY_MEDIA_TYPE, VALUE_VIDEO_SURFACE)
                            val width = definition.width
                            val height = definition.height
                            it.putExtra(VALUE_INCOMING, intArrayOf(width, height))
                        })
                    }
                    Call.State.End -> {
                        onCallFinish(isCallOnly = true)
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_CALL_STATE, VALUE_NONE)
                        })
                    }
                }
            }

/*
            override fun onCallStatsUpdated(core: Core, call: Call, callStats: CallStats) {
                println("call stats: " + callStats.type)
                when (callStats.type) {
                    StreamType.Video -> {
//                        println("call video: " + call.params.receivedVideoDefinition)
                        println("call video: " + call.params.getCustomSdpMediaAttribute(StreamType.Video, "max-recv-width"))
                        println("call video: " + call.params.getCustomSdpMediaAttribute(StreamType.Video, "max-recv-height"))
                    }
                }
            }
*/
/*
            override fun onInfoReceived(core: Core, call: Call, message: InfoMessage) {
                println("call info: " + message.content)
            }
*/
        }
        coreListenerStub = listener
        core.addListener(listener)
//        account.addListener { _, state, message ->
//            println("[Account] Registration state changed: $state, $message")
//        }
        core.start()
    }
    private fun onAccountFinish() {
        println("on -> account finish")
        val core = requireNotNull(core)
        val account = requireNotNull(account)
        val listener = requireNotNull(coreListenerStub)
        core.removeListener(listener)
        coreListenerStub = null
        core.removeAccount(account)
        this.account = null
        core.clearAccounts()
        core.clearAllAuthInfo()
        core.stop()
        this.core = null
    }
    private fun onCallFinish(isCallOnly: Boolean) {
        println("on -> call finish")
        setState(State.READY)
        stopForeground(true)
        val call = requireNotNull(call)
        check(call.state == Call.State.End)
        this.call = null
        callStateSide = VALUE_NONE
        switchSpeakerphone(this, isSpeakerphoneOn = false)
        if (!isCallOnly) {
            TODO()
            onAccountFinish()
        }
    }
    private fun onCallConfirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = setOf(
                Manifest.permission.RECORD_AUDIO
            ).map {
                it to checkSelfPermission(it)
            }.filter { (_, isGranted) ->
                isGranted != PackageManager.PERMISSION_GRANTED
            }
            if (permissions.isNotEmpty()) {
                sendBroadcast(Intent(ACTION_CHECK_PERMISSION_REQUEST).also {
                    it.putExtra(KEY_PERMISSIONS, permissions.map { (k, _) -> k }.toTypedArray())
                })
                return
            }
        }
        stopForeground(true)
        val core = requireNotNull(core)
        val call = requireNotNull(call)
//        val params = core.createCallParams(call)!!
        val params = call.params
        params.enableVideo(true)
//        call.update(params)
//        call.accept()
        call.acceptWithParams(params)
//        call.acceptEarlyMedia()
        switchSpeakerphone(this, isSpeakerphoneOn = true)
        if (!CallActivity.isResumed()) {
            startForegroundCall()
        }
    }
    private fun onCallStateRequest() {
        val core = core
        if (core == null) {
            sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                it.putExtra(KEY_CALL_STATE, VALUE_NONE)
            })
            return
        }
        val account = requireNotNull(account)
        val call = call
        if (call == null) {
            sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                it.putExtra(KEY_CALL_STATE, VALUE_NONE)
            })
            return
        }
        println("on call state: " + call.state)
        when (call.state) {
            Call.State.Released -> {
                sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                    it.putExtra(KEY_CALL_STATE, VALUE_DISCONNECTED)
                })
            }
            Call.State.Connected, Call.State.StreamsRunning -> {
                sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                    it.putExtra(KEY_CALL_STATE, VALUE_CONFIRMED)
                    it.putExtra(KEY_CALL_TIME_START, callTimeStart)
                })
            }
            else -> {
                sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                    it.putExtra(KEY_CALL_STATE, callStateSide)
                })
            }
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            Log.d(TAG, "on receive ${intent.action}")
            when (intent.action) {
                ACTION_SET_VIDEO_SURFACE -> {
                    val incoming = intent.getParcelableExtra<Surface>(VALUE_INCOMING)
                    val core = requireNotNull(core)
                    core.nativeVideoWindowId = incoming
                }
                ACTION_CALL_STATE_REQUEST -> {
                    onCallStateRequest()
                }
                ACTION_SCREEN_STATE_BROADCAST -> {
                    val state = intent.getStringExtra(KEY_SCREEN_STATE)
                    Log.d(TAG, "${intent.action} $state")
                    when (state) {
                        VALUE_RESUMED -> {
                            stopForeground(true)
                        }
                        VALUE_PAUSED -> {
                            val call = call
                            if (call == null) {
                                // todo
                            } else {
                                when (call.state) {
                                    Call.State.Released -> {
                                        // todo
                                    }
                                    Call.State.Connected -> {
                                        startForegroundCall()
                                    }
                                    else -> {
                                        when (callStateSide) {
                                            VALUE_INCOMING -> {
                                                startForegroundIncoming()
                                            }
                                            else -> TODO()
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
                ACTION_CALL_CONFIRM -> {
                    onCallConfirm()
                }
                ACTION_CALL_CANCEL -> {
                    requireNotNull(call).terminate()
                }
                ACTION_EXIT -> {
                    onAccountFinish()
                    setState(State.NONE)
                }
                ACTION_STATE_REQUEST -> {
                    onStateRequest()
                }
                ACTION_REGISTRATION -> {
                    val host = intent.getStringExtra(KEY_HOST)
                    if (host.isNullOrEmpty()) error("Host is empty!")
                    val realm = intent.getStringExtra(KEY_REALM)
                    if (realm.isNullOrEmpty()) error("Realm is empty!")
                    val port = intent.getIntExtra(KEY_PORT, -1)
                    check(port > 0)
                    val userFromName = intent.getStringExtra(KEY_USER_FROM_NAME)
                    if (userFromName.isNullOrEmpty()) error("User from name is empty!")
                    val userFromPassword = intent.getStringExtra(KEY_USER_FROM_PASSWORD).orEmpty()
                    setState(State.LOADING)
                    onRegistration(
                        host = host,
                        realm = realm,
                        port = port,
                        userFromName = userFromName,
                        userFromPassword = userFromPassword
                    )
                }
            }
            // todo
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "on create")
        if (!isChannelsReady) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                setOf(
                    CHANNEL_INCOMING_CALL,
                    CHANNEL_CALL
                ).forEach {
                    val channel = NotificationChannel(it, it, NotificationManager.IMPORTANCE_HIGH)
                    channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    manager.createNotificationChannel(channel)
                }
            }
            isChannelsReady = true
        }
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                ACTION_SET_VIDEO_SURFACE,
                ACTION_CALL_STATE_REQUEST,
                ACTION_SCREEN_STATE_BROADCAST,
                ACTION_CALL_CANCEL,
                ACTION_CALL_CONFIRM,
                ACTION_EXIT,
                ACTION_REGISTRATION,
                ACTION_STATE_REQUEST
            ).forEach(it::addAction)
        })
        sendBroadcast(Intent(ACTION_STATE_BROADCAST).also {
            it.putExtra(KEY_STATE, state.name)
        })
        // todo
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "on start command")
        // todo
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        // todo
    }
}
