package test.android.linphone

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.px(dp: Float): Float {
    return kotlin.math.ceil(resources.displayMetrics.density * dp)
}

@RequiresApi(Build.VERSION_CODES.M)
infix fun Context.isGranted(permission: String): Boolean {
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

@RequiresApi(Build.VERSION_CODES.M)
fun Context.isGranted(permissions: Set<String>): Boolean {
    if (permissions.isEmpty()) TODO()
    return permissions.all(::isGranted)
}
