package test.android.linphone

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.*

class CallActivity : Activity() {
    companion object {
        private var isResumed: Boolean = false
        fun isResumed(): Boolean {
            return isResumed
        }
    }
    private val TAG = "[CallActivity|${hashCode()}]"

    private var foregroundView: View? = null
//    private var cancelView: View? = null
    private var confirmView: View? = null
    private var incomingSurfaceView: SurfaceView? = null
//    private var outgoingSurfaceView: SurfaceView? = null

    private var receiver: BroadcastReceiver? = null

    private fun render(state: String) {
        when (state) {
            CallService.VALUE_NONE, CallService.VALUE_DISCONNECTED -> {
                val foregroundView = requireNotNull(foregroundView)
                foregroundView.visibility = View.GONE
            }
            CallService.VALUE_INCOMING -> {
                requireNotNull(confirmView).visibility = View.VISIBLE
                val foregroundView = requireNotNull(foregroundView)
                foregroundView.visibility = View.VISIBLE
            }
            CallService.VALUE_CONFIRMED -> {
                requireNotNull(confirmView).visibility = View.GONE
                val foregroundView = requireNotNull(foregroundView)
                foregroundView.visibility = View.VISIBLE
            }
            CallService.VALUE_OUTGOING -> {
                requireNotNull(confirmView).visibility = View.GONE
                requireNotNull(foregroundView).visibility = View.VISIBLE
            }
            else -> TODO()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    CallService.ACTION_MEDIA_STATE_BROADCAST -> {
                        when (intent.getStringExtra(CallService.KEY_MEDIA_TYPE)) {
                            CallService.VALUE_VIDEO_SURFACE -> {
                                val iSize = intent.getIntArrayExtra(CallService.VALUE_INCOMING)
                                checkNotNull(iSize)
                                check(iSize.size == 2)
                                val foregroundView = requireNotNull(foregroundView)
                                val incomingSurfaceView = requireNotNull(incomingSurfaceView)
//                            val outgoingSurfaceView = requireNotNull(outgoingSurfaceView)
                                foregroundView.post {
                                    val width = iSize[0]
                                    val height = iSize[1]
                                    Log.d(TAG, "incoming video size $width/$height")
                                    val maxW = foregroundView.width
                                    val maxH = foregroundView.height
                                    val layoutParams = incomingSurfaceView.layoutParams
                                    when {
                                        maxW > maxH -> {
                                            layoutParams.height = maxH
                                            when {
                                                width > height -> {
                                                    val d = width.toDouble() / height.toDouble()
                                                    layoutParams.width = (layoutParams.height * d).toInt()
                                                }
                                                width == height -> layoutParams.width = maxH
                                                else ->TODO()
                                            }
                                        }
                                        maxH > maxW -> {
                                            layoutParams.width = maxW
                                            when {
                                                width > height -> {
                                                    val d = height.toDouble() / width.toDouble()
                                                    layoutParams.height = (layoutParams.width * d).toInt()
                                                }
                                                width == height -> layoutParams.height = maxW
                                                else -> TODO()
                                            }
                                        }
                                        else -> TODO()
                                    }
                                    Log.d(TAG, "incoming video view size ${layoutParams.width}/${layoutParams.height}")
                                    incomingSurfaceView.layoutParams = layoutParams
                                    incomingSurfaceView.visibility = View.VISIBLE
//                                outgoingSurfaceView.layoutParams = FrameLayout.LayoutParams(
//                                    maxW / 3, maxH / 3, Gravity.BOTTOM or Gravity.LEFT
//                                )
//                                outgoingSurfaceView.visibility = View.VISIBLE
                                    sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_SURFACE).also {
                                        it.putExtra(CallService.VALUE_INCOMING, incomingSurfaceView.holder.surface)
//                                    it.putExtra(CallService.VALUE_OUTGOING, outgoingSurfaceView.holder.surface)
                                    })
                                }
                            }
                        }
                    }
                    CallService.ACTION_CHECK_PERMISSION_REQUEST -> {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            val permissions = intent.getStringArrayExtra(CallService.KEY_PERMISSIONS)
                            checkNotNull(permissions)
                            requestPermissions(permissions, 4321)
                        }
                    }
                    CallService.ACTION_CALL_STATE_BROADCAST -> {
                        val state = intent.getStringExtra(CallService.KEY_CALL_STATE)!!
                        render(state)
                        println("on receive " + intent.action + " state " + state)
                        when (state) {
                            CallService.VALUE_NONE, CallService.VALUE_DISCONNECTED -> {
                                unregisterReceiver(this)
                                receiver = null
                                finish()
                                if (isResumed()) {
                                    startActivity(Intent(this@CallActivity, MainActivity::class.java))
                                }
                            }
                            CallService.VALUE_INCOMING -> {
                                val foregroundView = requireNotNull(foregroundView)
                                foregroundView.visibility = View.VISIBLE
                            }
                            CallService.VALUE_CONFIRMED -> {
                                val foregroundView = requireNotNull(foregroundView)
                                foregroundView.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
        val foregroundView = LinearLayout(this).also { root ->
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.BOTTOM
            root.addView(LinearLayout(this).also {
                it.addView(TextView(this).also { textView ->
                    textView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        px(dp = 54f).toInt(),
                        1f
                    )
                    textView.background = ColorDrawable(Color.RED)
                    textView.text = "cancel"
                    textView.setTextColor(Color.WHITE)
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(dp = 14f))
                    textView.gravity = Gravity.CENTER
                    textView.setOnClickListener {
                        sendBroadcast(Intent(CallService.ACTION_CALL_CANCEL))
                        finish()
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                })
                it.addView(TextView(this).also { textView ->
                    textView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        px(dp = 54f).toInt(),
                        1f
                    )
                    textView.background = ColorDrawable(Color.GREEN)
                    textView.text = "answer"
                    textView.setTextColor(Color.WHITE)
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(dp = 14f))
                    textView.gravity = Gravity.CENTER
                    textView.setOnClickListener {
                        sendBroadcast(Intent(CallService.ACTION_CALL_CONFIRM))
                    }
                    confirmView = textView
                })
            })
            root.visibility = View.GONE
        }
        this.foregroundView = foregroundView
        val incomingSurfaceView = SurfaceView(this).also {
            it.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            )
            it.visibility = View.INVISIBLE
        }
        this.incomingSurfaceView = incomingSurfaceView
//        val outgoingSurfaceView = SurfaceView(this).also {
//            it.layoutParams = FrameLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                Gravity.CENTER
//            )
//            it.visibility = View.INVISIBLE
//        }
//        this.outgoingSurfaceView = outgoingSurfaceView
        setContentView(FrameLayout(this).also { root ->
            root.background = ColorDrawable(Color.BLACK)
            root.addView(incomingSurfaceView)
//            root.addView(outgoingSurfaceView)
            root.addView(foregroundView)
        })
        sendBroadcast(Intent(CallService.ACTION_SCREEN_STATE_BROADCAST).also {
            it.putExtra(CallService.KEY_SCREEN_STATE, CallService.VALUE_CREATED)
        })
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                CallService.ACTION_MEDIA_STATE_BROADCAST,
                CallService.ACTION_CHECK_PERMISSION_REQUEST,
                CallService.ACTION_CALL_STATE_BROADCAST
            ).forEach(it::addAction)
        })
        sendBroadcast(Intent(CallService.ACTION_CALL_STATE_REQUEST))
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        sendBroadcast(Intent(CallService.ACTION_SCREEN_STATE_BROADCAST).also {
            it.putExtra(CallService.KEY_SCREEN_STATE, CallService.VALUE_RESUMED)
        })
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
        sendBroadcast(Intent(CallService.ACTION_SCREEN_STATE_BROADCAST).also {
            it.putExtra(CallService.KEY_SCREEN_STATE, CallService.VALUE_PAUSED)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        val receiver = receiver
        if (receiver != null) {
            unregisterReceiver(receiver)
            this.receiver = null
        }
        sendBroadcast(Intent(CallService.ACTION_SCREEN_STATE_BROADCAST).also {
            it.putExtra(CallService.KEY_SCREEN_STATE, CallService.VALUE_DESTROYED)
        })
        // todo
    }
}
