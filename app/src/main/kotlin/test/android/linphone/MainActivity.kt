package test.android.linphone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.linphone.core.Factory
import org.linphone.core.LogCollectionState
import org.linphone.core.RegistrationState
import sp.kx.functional.subject.Subject
import sp.kx.functional.subscription.Subscription

class MainActivity : AppCompatActivity() {
    private enum class Action {
        REGISTRATION, MAKE_CALL, EXIT
    }
    private var editTexts: Map<CallProperty, EditText>? = null
    private var buttons: Map<Action, Button>? = null
    private var statusTextView: TextView? = null

    private fun onRegistration(
        domain: String,
        portText: String,
        userFromName: String,
        userFromPassword: String
    ) {
        if (domain.isEmpty()) {
            showToast("Domain is empty!")
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
        hideAll()
        requireNotNull(statusTextView).text = "started..."
        CallState.start(
            context = this,
            userFromName = userFromName,
            userFromPassword = userFromPassword,
            domain = domain
        )
    }

    private var subscription: Subscription? = null

    private fun View.show() {
        visibility = View.VISIBLE
    }
    private fun Iterable<View>.show() {
        forEach {
            it.show()
        }
    }
    private fun View.hide() {
        visibility = View.GONE
    }
    private fun Iterable<View>.hide() {
        forEach {
            it.hide()
        }
    }
    private fun hideAll() {
        requireNotNull(editTexts).values.hide()
        requireNotNull(buttons).values.hide()
        requireNotNull(statusTextView).hide()
    }
    private fun render() {
        hideAll()
        val core = CallState.getCore()
        if (core == null) {
            setOf(
                CallProperty.DOMAIN,
                CallProperty.PORT,
                CallProperty.USER_FROM_NAME,
                CallProperty.USER_FROM_PASSWORD
            ).map { requireNotNull(editTexts)[it]!! }.show()
            requireNotNull(buttons)[Action.REGISTRATION]!!.show()
        } else {
            val account = core.accountList.firstOrNull() ?: TODO()
            when (val state = account.state ?: TODO()) {
                RegistrationState.None -> TODO()
                RegistrationState.Progress -> {
                    requireNotNull(statusTextView).show()
                    requireNotNull(statusTextView).text = state.name
                }
                RegistrationState.Ok -> {
                    requireNotNull(editTexts)[CallProperty.USER_TO_NAME]!!.show()
                    setOf(
                        Action.MAKE_CALL,
                        Action.EXIT,
                    ).map { requireNotNull(buttons)[it]!! }.show()
                }
                RegistrationState.Cleared -> TODO()
                RegistrationState.Failed -> TODO()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Factory.instance().setDebugMode(true, "Linphone")
//        Factory.instance().enableLogCollection(LogCollectionState.EnabledWithoutPreviousLogHandler)
//        Factory.instance().enableLogcatLogs(true)
//        val defaultDomain = "192.168.88.246"
        val defaultDomain = "62.152.90.226"
        val defaultPort = 5060
//        val defaultUserFromName = "100"
        val defaultUserFromName = "3006"
        val defaultUserFromPassword = "100"
//        val defaultUserToName = "102"
        val defaultUserToName = "3008"
        val editTexts = mutableMapOf<CallProperty, EditText>()
        val buttons = mutableMapOf<Action, Button>()
        setContentView(LinearLayout(this).also { root ->
            root.orientation = LinearLayout.VERTICAL
            CallProperty.values().forEach { property ->
                val editText = EditText(this).also {
                    it.hint = property.name.lowercase()
                    val default = when (property) {
                        CallProperty.DOMAIN -> defaultDomain
                        CallProperty.PORT -> defaultPort.toString()
                        CallProperty.USER_FROM_NAME -> defaultUserFromName
                        CallProperty.USER_FROM_PASSWORD -> defaultUserFromPassword
                        CallProperty.USER_TO_NAME -> defaultUserToName
                    }
                    it.setText(default)
                }
                editTexts[property] = editText
                root.addView(editText)
            }
            Action.values().forEach { action ->
                val view = Button(this).also {
                    it.text = action.name.lowercase()
                    when (action) {
                        Action.REGISTRATION -> {
                            it.setOnClickListener {
                                onRegistration(
                                    domain = editTexts[CallProperty.DOMAIN]!!.text.toString(),
                                    portText = editTexts[CallProperty.PORT]!!.text.toString(),
                                    userFromName = editTexts[CallProperty.USER_FROM_NAME]!!.text.toString(),
                                    userFromPassword = editTexts[CallProperty.USER_FROM_PASSWORD]!!.text.toString()
                                )
                            }
                        }
                        Action.MAKE_CALL -> {
                            it.setOnClickListener {
                                val userToName =
                                    editTexts[CallProperty.USER_TO_NAME]!!.text.toString()
                                finish()
                                requireNotNull(subscription).unsubscribe()
                                subscription = null
                                startActivity(
                                    Intent(this, CallActivity::class.java).also { intent ->
                                        intent.putExtra(CallProperty.USER_TO_NAME.name, userToName)
                                    }
                                )
                            }
                        }
                        Action.EXIT -> {
                            it.setOnClickListener {
                                CallState.stop()
                                render()
                            }
                        }
                    }
                }
                buttons[action] = view
                root.addView(view)
            }
            val statusTextView = TextView(this)
            this.statusTextView = statusTextView
            root.addView(statusTextView)
        })
        this.editTexts = editTexts.toMap()
        this.buttons = buttons.toMap()
        subscription = CallState.broadcast.subscribe(Subject.action {
            when (it) {
                is CallState.Broadcast.OnCallState -> TODO()
                is CallState.Broadcast.OnRegistration -> {
                    val state = it.account.state ?: TODO()
                    println("on -> registration $state")
                    when (state) {
                        RegistrationState.None -> TODO()
                        RegistrationState.Progress -> {
                            render()
                        }
                        RegistrationState.Ok -> {
                            render()
                        }
                        RegistrationState.Cleared -> TODO()
                        RegistrationState.Failed -> {
                            showToast("Failed")
                            render()
                        }
                    }
                }
            }
        })
        hideAll()
        val permissions = setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (isGranted(permissions)) {
            render()
        } else {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                val notGranted = it.toList().any { (_, isGranted) -> !isGranted }
                if (notGranted) {
                    showToast("Permissions error!")
                    finish()
                } else {
                    render()
                }
            }.launch(permissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.unsubscribe()
        subscription = null
    }
}
