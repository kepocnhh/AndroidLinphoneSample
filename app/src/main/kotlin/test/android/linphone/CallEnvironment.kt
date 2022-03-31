package test.android.linphone

import android.os.Parcel
import android.os.Parcelable

interface CallEnvironment {
    val realm: String
    val host: String
    val port: Int
    val userFromName: String
    val userFromPassword: String
    val userToName: String
}

private class CallEnvironmentImpl(
    override val realm: String,
    override val host: String,
    override val port: Int,
    override val userToName: String,
    override val userFromName: String,
    override val userFromPassword: String
) : CallEnvironment

fun callEnvironment(
    host: String,
    realm: String = host,
    port: Int,
    userToName: String,
    userFromName: String,
    userFromPassword: String
): CallEnvironment {
    return CallEnvironmentImpl(
        realm = realm,
        host = host,
        port = port,
        userToName = userToName,
        userFromName = userFromName,
        userFromPassword = userFromPassword
    )
}

private class CallEnvironmentParcelable(
    private val delegate: CallEnvironment
) : CallEnvironment by delegate, Parcelable {
    companion object CREATOR : Parcelable.Creator<CallEnvironmentParcelable> {
        override fun createFromParcel(source: Parcel): CallEnvironmentParcelable {
            return CallEnvironmentParcelable(
                callEnvironment(
                    realm = source.readString()!!,
                    host = source.readString()!!,
                    port = source.readInt(),
                    userToName = source.readString()!!,
                    userFromName = source.readString()!!,
                    userFromPassword = source.readString()!!
                )
            )
        }

        override fun newArray(size: Int) = arrayOfNulls<CallEnvironmentParcelable>(size)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(realm)
        dest.writeString(host)
        dest.writeInt(port)
        dest.writeString(userToName)
        dest.writeString(userFromName)
        dest.writeString(userFromPassword)
    }

    override fun toString(): String {
        return delegate.toString()
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is CallEnvironmentParcelable -> other.delegate == delegate
            is CallEnvironment -> other == delegate
            else -> false
        }
    }
}

fun CallEnvironment.toParcelable(): Parcelable {
    return CallEnvironmentParcelable(this)
}
