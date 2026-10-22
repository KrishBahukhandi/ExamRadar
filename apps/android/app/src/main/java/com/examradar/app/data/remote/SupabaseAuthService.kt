package com.examradar.app.data.remote

import android.util.Log
import com.examradar.app.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class AuthUser(
    val id: String,
    val email: String,
    @SerializedName("user_metadata")
    val userMetadata: Map<String, Any>? = null
) {
    val fullName: String
        get() = (userMetadata?.get("full_name") as? String)
            ?: email.substringBefore("@")
}

data class AuthSession(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Long? = 3600,
    @SerializedName("token_type")
    val tokenType: String? = "bearer",
    val user: AuthUser
)

sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class SupabaseAuthService(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val backendBaseUrl: String = BuildConfig.API_BASE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun signUp(email: String, password: String, fullName: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = JsonObject().apply {
                    addProperty("email", email.trim())
                    addProperty("password", password)
                    val data = JsonObject().apply {
                        addProperty("full_name", fullName.trim())
                    }
                    add("data", data)
                }

                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/signup")
                    .header("apikey", anonKey)
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        val errorMsg = parseErrorMessage(bodyStr, "Sign up failed (HTTP ${response.code})")
                        return@withContext AuthResult.Error(errorMsg)
                    }

                    val session = gson.fromJson(bodyStr, AuthSession::class.java)
                    if (session?.accessToken != null && session.user != null) {
                        AuthResult.Success(session)
                    } else {
                        // User created but session missing (e.g. if email confirmation was pending)
                        // Attempt immediate login since auto-confirm trigger confirms user
                        signIn(email, password)
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuthService", "SignUp exception", e)
                AuthResult.Error(e.localizedMessage ?: "Network connection error")
            }
        }

    suspend fun signIn(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = JsonObject().apply {
                    addProperty("email", email.trim())
                    addProperty("password", password)
                }

                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/token?grant_type=password")
                    .header("apikey", anonKey)
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        val errorMsg = parseErrorMessage(bodyStr, "Invalid email or password")
                        return@withContext AuthResult.Error(errorMsg)
                    }

                    val session = gson.fromJson(bodyStr, AuthSession::class.java)
                    if (session?.accessToken != null && session.user != null) {
                        AuthResult.Success(session)
                    } else {
                        AuthResult.Error("Unable to retrieve session token")
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuthService", "SignIn exception", e)
                AuthResult.Error(e.localizedMessage ?: "Network connection error")
            }
        }

    suspend fun refreshSession(refreshToken: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = JsonObject().apply {
                    addProperty("refresh_token", refreshToken)
                }

                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                    .header("apikey", anonKey)
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext AuthResult.Error("Session expired, please sign in again")
                    }

                    val session = gson.fromJson(bodyStr, AuthSession::class.java)
                    if (session?.accessToken != null && session.user != null) {
                        AuthResult.Success(session)
                    } else {
                        AuthResult.Error("Unable to refresh session token")
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuthService", "refreshSession exception", e)
                AuthResult.Error(e.localizedMessage ?: "Network connection error")
            }
        }

    suspend fun syncPreferencesToBackend(
        token: String,
        followedExams: Set<String>,
        followedCategories: Set<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JsonObject().apply {
                val examArray = com.google.gson.JsonArray()
                followedExams.forEach { examArray.add(it) }
                add("followedExamIds", examArray)

                val catArray = com.google.gson.JsonArray()
                followedCategories.forEach { catArray.add(it) }
                add("followedCategoryIds", catArray)

                addProperty("reminder7Days", true)
                addProperty("reminder3Days", true)
                addProperty("reminder1Day", true)
                addProperty("pushEnabled", true)
            }

            val request = Request.Builder()
                .url("$backendBaseUrl/api/v1/me/preferences")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .put(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w("SupabaseAuthService", "Failed to sync preferences to backend: ${e.message}")
            false
        }
    }

    suspend fun logout(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/logout")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w("SupabaseAuthService", "Supabase logout exception: ${e.message}")
            false
        }
    }

    private fun parseErrorMessage(jsonStr: String, fallback: String): String {
        return try {
            val obj = gson.fromJson(jsonStr, JsonObject::class.java)
            obj.get("msg")?.asString
                ?: obj.get("error_description")?.asString
                ?: obj.get("message")?.asString
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
