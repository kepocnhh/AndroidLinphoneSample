package test.android.linphone

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Factory
import org.linphone.mediastream.video.capture.CaptureTextureView
import sp.kx.functional.subject.Subject
import sp.kx.functional.subscription.Subscription
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class CallActivity : Activity() {
    private var statusTextView: TextView? = null
    private var foregroundView: View? = null
    private var incomingView: TextureView? = null
    private var outgoingView: CaptureTextureView? = null

    private var subscription: Subscription? = null
    private var timer: Timer? = null

    private fun updateTimerTask(timer: Timer, timeUnit: TimeUnit, duration: Long) {
        val delay = 0L
        val period = 100L
        val textView = requireNotNull(statusTextView)
        timer.scheduleAtFixedRate(timerTask {
            val dTime = System.nanoTime() - timeUnit.toNanos(duration)
            val hours = TimeUnit.NANOSECONDS.toHours(dTime)
            val dMinutes = TimeUnit.NANOSECONDS.toMinutes(dTime)
            val minutes = dMinutes - TimeUnit.HOURS.toMinutes(hours)
            val seconds =
                TimeUnit.NANOSECONDS.toSeconds(dTime) - TimeUnit.MINUTES.toSeconds(dMinutes)
            val text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            textView.post { textView.text = text }
        }, delay, period)
    }

    private fun onConnected() {
        val timer = Timer()
        this.timer = timer
        updateTimerTask(
            timer = timer,
            timeUnit = TimeUnit.NANOSECONDS,
            duration = System.nanoTime()
        )
    }

    private fun onStreamsRunning(call: Call) {
        println("stream count " + call.streamCount)
        println("video enabled " + call.params.videoEnabled())
        if (!call.params.videoEnabled()) return
        println("video direction " + call.params.videoDirection)
        requireNotNull(incomingView).visibility = View.VISIBLE
        requireNotNull(foregroundView).also { root ->
            root.post {
                println("root " + root.width + "/" + root.height)
                requireNotNull(outgoingView).also {
                    val definition = call.core.previewVideoDefinition ?: TODO()
                    val width = root.width / 3
                    val d = definition.width.toDouble() / definition.height.toDouble()
                    val height: Int = (width * d).toInt()
                    println("outgoing $width/$height")
                    it.layoutParams = FrameLayout.LayoutParams(width, height).also { lp ->
//                        lp.setMargins(0, requireNotNull(insets).top, 0, 0) // todo
                    }
                    it.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = requireNotNull(intent)
        val userToName = intent.getStringExtra(CallProperty.USER_TO_NAME.name)
        check(!userToName.isNullOrEmpty())
        val foregroundView = FrameLayout(this).also { root ->
            statusTextView = TextView(this).also {
                it.setTextColor(Color.WHITE)
                root.addView(it)
            }
            Button(this).also {
                it.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
                it.text = "cancel"
                it.setOnClickListener {
                    val core = CallState.getCore() ?: TODO()
                    val call = core.currentCall ?: TODO()
                    call.terminate()
                }
                root.addView(it)
            }
        }
        this.foregroundView = foregroundView
        val incomingView = TextureView(this).also {
            it.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
            it.visibility = View.INVISIBLE
        }
        this.incomingView = incomingView
        val outgoingView = CaptureTextureView(this).also {
            it.layoutParams = FrameLayout.LayoutParams(
                0,
                0
            )
            it.visibility = View.INVISIBLE
        }
        this.outgoingView = outgoingView
        setContentView(FrameLayout(this).also { root ->
            root.keepScreenOn = true
            root.background = ColorDrawable(Color.BLACK)
            root.addView(incomingView)
            root.addView(outgoingView)
            root.addView(foregroundView)
        })
        val core = CallState.getCore() ?: TODO()
        val isBackSupported = core.videoDevicesList.contains(CallUtil.CAMERA_BACK_KEY)
        val isFrontSupported = core.videoDevicesList.contains(CallUtil.CAMERA_FRONT_KEY)
        if (!isFrontSupported) TODO()
        core.videoDevice = CallUtil.CAMERA_FRONT_KEY
        core.defaultOutputAudioDevice =
            core.audioDevices.firstOrNull { it.type == AudioDevice.Type.Speaker } ?: TODO()
        core.nativeVideoWindowId = incomingView
        core.nativePreviewWindowId = outgoingView
        val definition = Factory.instance().supportedVideoDefinitions.firstOrNull {
            it.width == 640 && it.height == 480
        } ?: TODO()
        core.previewVideoDefinition = definition
        CallState.makeCall(userToName = userToName)
        subscription = CallState.broadcast.subscribe(Subject.action {
            when (it) {
                is CallState.Broadcast.OnCallState -> {
                    val state = it.call.state ?: TODO()
                    when (state) {
                        Call.State.Idle -> TODO()
                        Call.State.IncomingReceived -> TODO()
                        Call.State.PushIncomingReceived -> TODO()
                        Call.State.OutgoingInit -> TODO()
                        Call.State.OutgoingProgress -> TODO()
                        Call.State.OutgoingRinging -> {
                            requireNotNull(statusTextView).text = state.name
                        }
                        Call.State.OutgoingEarlyMedia -> TODO()
                        Call.State.Connected -> {
                            requireNotNull(statusTextView).text = state.name
                            onConnected()
                        }
                        Call.State.StreamsRunning -> {
                            onStreamsRunning(it.call)
                        }
                        Call.State.Pausing -> TODO()
                        Call.State.Paused -> TODO()
                        Call.State.Resuming -> TODO()
                        Call.State.Referred -> TODO()
                        Call.State.Error -> TODO()
                        Call.State.End -> {
                            requireNotNull(statusTextView).text = state.name
                        }
                        Call.State.PausedByRemote -> TODO()
                        Call.State.UpdatedByRemote -> TODO()
                        Call.State.IncomingEarlyMedia -> TODO()
                        Call.State.Updating -> TODO()
                        Call.State.Released -> {
                            finish()
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                        Call.State.EarlyUpdatedByRemote -> TODO()
                        Call.State.EarlyUpdating -> TODO()
                    }
                }
                is CallState.Broadcast.OnRegistration -> {
                    // ignored
                }
            }
        })
        requireNotNull(statusTextView).text = "calling..."
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.unsubscribe()
        subscription = null
    }
}
