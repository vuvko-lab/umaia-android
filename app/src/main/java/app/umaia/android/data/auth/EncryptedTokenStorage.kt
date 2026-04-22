package app.umaia.android.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedTokenStorage @Inject constructor(@ApplicationContext ctx: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        ctx,
        "umaia_auth_tokens",
        MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(v) { prefs.edit().putString("access_token", v).apply() }

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(v) { prefs.edit().putString("refresh_token", v).apply() }

    var userId: String?
        get() = prefs.getString("user_id", null)
        set(v) { prefs.edit().putString("user_id", v).apply() }

    var email: String?
        get() = prefs.getString("email", null)
        set(v) { prefs.edit().putString("email", v).apply() }

    fun clear() = prefs.edit().clear().apply()
}
