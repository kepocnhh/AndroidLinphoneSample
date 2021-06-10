package test.android.linphone

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}
fun Context.px(dp: Float): Float {
    return kotlin.math.ceil(resources.displayMetrics.density * dp)
}
