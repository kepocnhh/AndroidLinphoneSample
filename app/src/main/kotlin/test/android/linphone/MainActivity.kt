package test.android.linphone

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar

class MainActivity : Activity() {
    companion object {
        private var isResumed: Boolean = false
        fun isResumed(): Boolean {
            return isResumed
        }
    }
    private val TAG = "[MainActivity|${hashCode()}]"

    private var hostEditText: EditText? = null
    private var realmEditText: EditText? = null
    private var portEditText: EditText? = null
    private var userFromNameEditText: EditText? = null
    private var userFromPasswordEditText: EditText? = null
    private var userToNameEditText: EditText? = null
    private var registrationButton: Button? = null
    private var makeCallButton: Button? = null
    private var exitButton: Button? = null
    private var progressBar: ProgressBar? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            Log.d(TAG, "on receive ${intent.action}")
            when (intent.action) {
                CallService.ACTION_STATE_BROADCAST -> {
                    val stateName = intent.getStringExtra(CallService.KEY_STATE)
                    if (stateName == null) TODO()
                    val state = State.values().firstOrNull { it.name == stateName }
                    if (state == null) TODO()
                    Log.d(TAG, "${intent.action} $stateName")
                    render(state)
                    // todo
                }
            }
            // todo
        }
    }

    private var state: State = State.LOADING

    private fun render(state: State) {
        if (this.state == state) return
        val any: Any? = when (state) {
            State.LOADING -> {
                setOf(
                    hostEditText,
                    realmEditText,
                    portEditText,
                    userFromNameEditText,
                    userFromPasswordEditText,
                    userToNameEditText,
                    registrationButton,
                    makeCallButton,
                    exitButton
                ).forEach {
                    requireNotNull(it).visibility = View.GONE
                }
                requireNotNull(progressBar).visibility = View.VISIBLE
            }
            State.NONE -> {
                setOf(
                    userToNameEditText,
                    makeCallButton,
                    exitButton,
                    progressBar
                ).forEach {
                    requireNotNull(it).visibility = View.GONE
                }
                setOf(
                    hostEditText,
                    realmEditText,
                    portEditText,
                    userFromNameEditText,
                    userFromPasswordEditText,
                    registrationButton
                ).forEach {
                    requireNotNull(it).visibility = View.VISIBLE
                }
            }
            State.READY -> {
                setOf(
                    hostEditText,
                    realmEditText,
                    portEditText,
                    userFromNameEditText,
                    userFromPasswordEditText,
                    registrationButton,
                    progressBar
                ).forEach {
                    requireNotNull(it).visibility = View.GONE
                }
                setOf(
                    userToNameEditText,
                    makeCallButton,
                    exitButton
                ).forEach {
                    requireNotNull(it).visibility = View.VISIBLE
                }
            }
            State.CALLING -> {
                finish()
                startActivity(Intent(this, CallActivity::class.java))
                return
            }
        }
        this.state = state
    }

    private fun onRegistration(
        host: String,
        realm: String,
        portText: String,
        userFromName: String,
        userFromPassword: String
    ) {
        if (host.isEmpty()) {
            showToast("Host is empty!")
            return
        }
        if (realm.isEmpty()) {
            showToast("Realm is empty!")
            return
        }
        val port = portText.toIntOrNull()
        if (port == null) {
            showToast("Port is empty!")
            return
        }
        if (port < 0) {
            showToast("Port error!")
            return
        }
        if (userFromName.isEmpty()) {
            showToast("User from name is empty!")
            return
        }
        sendBroadcast(Intent(CallService.ACTION_REGISTRATION).also {
            it.putExtra(CallService.KEY_HOST, host)
            it.putExtra(CallService.KEY_REALM, realm)
            it.putExtra(CallService.KEY_PORT, port)
            it.putExtra(CallService.KEY_USER_FROM_NAME, userFromName)
            it.putExtra(CallService.KEY_USER_FROM_PASSWORD, userFromPassword)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "on create")
        val defaultHost = ""
        val defaultRealm = defaultHost
        val defaultPort = 5060
        val defaultUserFromName = ""
        val defaultUserFromPassword = ""
        val defaultUserToName = ""
        setContentView(LinearLayout(this).also { root ->
            root.orientation = LinearLayout.VERTICAL
            val hostEditText = EditText(this).also {
                it.hint = "host"
                it.setText(defaultHost)
            }
            this.hostEditText = hostEditText
            root.addView(hostEditText)
            val realmEditText = EditText(this).also {
                it.hint = "realm"
                it.setText(defaultRealm)
            }
            this.realmEditText = realmEditText
            root.addView(realmEditText)
            val portEditText = EditText(this).also {
                it.hint = "port"
                it.setText(defaultPort.toString())
            }
            this.portEditText = portEditText
            root.addView(portEditText)
            val userFromNameEditText = EditText(this).also {
                it.hint = "user from name"
                it.setText(defaultUserFromName)
            }
            this.userFromNameEditText = userFromNameEditText
            root.addView(userFromNameEditText)
            val userFromPasswordEditText = EditText(this).also {
                it.hint = "user from password"
                it.setText(defaultUserFromPassword)
            }
            this.userFromPasswordEditText = userFromPasswordEditText
            root.addView(userFromPasswordEditText)
            val userToNameEditText = EditText(this).also {
                it.hint = "user to name"
                it.setText(defaultUserToName)
            }
            this.userToNameEditText = userToNameEditText
            root.addView(userToNameEditText)
            val registrationButton = Button(this).also {
                it.text = "registration"
                it.setOnClickListener {
                    onRegistration(
                        host = hostEditText.text.toString(),
                        realm = realmEditText.text.toString(),
                        portText = portEditText.text.toString(),
                        userFromName = userFromNameEditText.text.toString(),
                        userFromPassword = userFromPasswordEditText.text.toString()
                    )
                }
            }
            this.registrationButton = registrationButton
            root.addView(registrationButton)
            val makeCallButton = Button(this).also {
                it.text = "make call"
                it.setOnClickListener {
                    TODO()
                }
            }
            this.makeCallButton = makeCallButton
            root.addView(makeCallButton)
            val exitButton = Button(this).also {
                it.text = "exit"
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_EXIT))
                }
            }
            this.exitButton = exitButton
            root.addView(exitButton)
            val progressBar = ProgressBar(this).also {
                it.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            this.progressBar = progressBar
            root.addView(progressBar)
        })
//        startService(Intent(this, CallService::class.java))
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                CallService.ACTION_STATE_BROADCAST
            ).forEach(it::addAction)
        })
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        Log.d(TAG, "on resume")
        state = State.LOADING
        render(State.LOADING)
        sendBroadcast(Intent(CallService.ACTION_STATE_REQUEST))
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        // todo
    }
}
