#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <memory>
#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"

#define TAG "DiscordJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::unique_ptr<discordpp::Client> g_client;

// Denotes that the client is ready.
static std::atomic g_isReady{false};

// Set true when the user cancels or rejects the OAuth authorization prompt.
// Kotlin polls this to stop the callback loop and let the user retry manually.
static std::atomic g_authorizationFailed{false};

// Set true when the Authorize callback fires successfully.
// Kotlin reads the code + verifier and POSTs them to the game server for exchange.
static std::atomic g_hasAuthorizationCode{false};

static std::string g_pendingCode;
static std::string g_pendingVerifier;
static std::string g_pendingRedirectUri;

// Trims a UTF-8 string to fit within Discord's 128-byte field limit.
// Strips one Unicode codepoint at a time from the end (by walking back past continuation bytes)
// and appends U+2026 HORIZONTAL ELLIPSIS if truncation was needed.
static std::string clampLength(std::string str) {
    if (str.size() <= 128) return str;

    // U+2026 HORIZONTAL ELLIPSIS in UTF-8: E2 80 A6 (3 bytes)
    constexpr size_t ellipsis_bytes = 3;

    size_t pos = 128 - ellipsis_bytes;
    // Walk back past UTF-8 continuation bytes (0x80–0xBF) to a codepoint boundary
    while (pos > 0 && (static_cast<unsigned char>(str[pos]) & 0xC0) == 0x80) {
        --pos;
    }

    str.resize(pos);
    str += "\xe2\x80\xa6";
    return str;
}

// Calls UpdateToken with the given access token then Connect.
// Token type is always Bearer for OAuth2.
static void handleTokenExchange(std::string accessToken) {
    g_client->UpdateToken(
        discordpp::AuthorizationTokenType::Bearer, std::move(accessToken),
        [](const discordpp::ClientResult ur) {
            if (!ur.Successful()) {
                LOGE("UpdateToken failed: %s", ur.Error().c_str());
                return;
            }

            LOGI("Token updated, connecting");
            g_client->Connect();
        });
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_create(JNIEnv*, jclass) {
    if (g_client) {
        return;
    }

    g_client = std::make_unique<discordpp::Client>();

    g_client->SetStatusChangedCallback(
        [](discordpp::Client::Status status, const discordpp::Client::Error error, const int32_t errorDetail) {
            const bool ready = status == discordpp::Client::Status::Ready;
            g_isReady.store(ready);

            if (status == discordpp::Client::Status::Disconnected && errorDetail != 0) {
                LOGE("Disconnected: %s (code %d)", discordpp::Client::ErrorToString(error).c_str(), errorDetail);
            } else {
                LOGI("Status changed to %d, ready=%s", static_cast<int>(status), ready ? "true" : "false");
            }
        });

    LOGI("Client created");
}

JNIEXPORT jboolean JNICALL
Java_com_osudroid_discord_DiscordNative_isReady() {
    return g_isReady.load();
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_authorize(JNIEnv*, jclass, const jlong clientId) {
    if (!g_client) {
        LOGE("authorize: client not created");
        return;
    }

    auto verifier = g_client->CreateAuthorizationCodeVerifier();
    discordpp::AuthorizationArgs args{};
    args.SetClientId(static_cast<uint64_t>(clientId));
    args.SetScopes(discordpp::Client::GetDefaultPresenceScopes());
    args.SetCodeChallenge(verifier.Challenge());

    g_client->Authorize(
        std::move(args),
        [v = std::move(verifier)](
            const discordpp::ClientResult result, const std::string &code, const std::string &redirectUri) mutable {
            if (!result.Successful()) {
                LOGE("Authorize failed: %s", result.Error().c_str());
                g_authorizationFailed.store(true);
                return;
            }

            // Surface the code, verifier, and redirect URI to Kotlin for server-side exchange.
            g_pendingCode = code;
            g_pendingVerifier = v.Verifier();
            g_pendingRedirectUri = redirectUri;
            g_hasAuthorizationCode.store(true);
            LOGI("Authorization code obtained, awaiting server-side exchange.");
        });
}

JNIEXPORT jboolean JNICALL
Java_com_osudroid_discord_DiscordNative_hasAuthorizationCode() {
    return g_hasAuthorizationCode.load();
}

JNIEXPORT jstring JNICALL
Java_com_osudroid_discord_DiscordNative_getAuthorizationCode(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_pendingCode.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_osudroid_discord_DiscordNative_getVerifier(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_pendingVerifier.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_osudroid_discord_DiscordNative_getRedirectUri(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_pendingRedirectUri.c_str());
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_clearAuthorizationCode() {
    g_hasAuthorizationCode.store(false);
    g_pendingCode.clear();
    g_pendingVerifier.clear();
    g_pendingRedirectUri.clear();
}

JNIEXPORT jboolean JNICALL
Java_com_osudroid_discord_DiscordNative_hasAuthorizationFailed() {
    return g_authorizationFailed.load();
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_clearAuthorizationFailed() {
    g_authorizationFailed.store(false);
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_abortAuthorize(JNIEnv*, jclass) {
    if (g_client) {
        g_client->AbortAuthorize();
    }
}

// Called from Kotlin after the server-side token exchange succeeds.
// Feeds the access token into UpdateToken + Connect to complete authentication.
JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_provideTokens(JNIEnv* env, jclass, const jstring jAccessToken) {
    if (!g_client) {
        LOGE("provideTokens: client not created");
        return;
    }

    const char* s = env->GetStringUTFChars(jAccessToken, nullptr);
    std::string accessToken(s);
    env->ReleaseStringUTFChars(jAccessToken, s);

    handleTokenExchange(std::move(accessToken));
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_runCallbacks(JNIEnv*, jclass) {
    discordpp::RunCallbacks();
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_updateRichPresence(
        JNIEnv* env, jclass,
        const jstring jDetails, const jstring jState,
        const jint partySize, const jint partyMax, const jlong startTimestamp,
        const jstring jLargeText) {
    if (!g_client || !g_isReady.load()) {
        return;
    }

    discordpp::Activity activity{};
    activity.SetType(discordpp::ActivityTypes::Playing);

    if (jDetails) {
        const char* s = env->GetStringUTFChars(jDetails, nullptr);
        std::string details(s);
        env->ReleaseStringUTFChars(jDetails, s);

        if (details.size() >= 2) {
            activity.SetDetails(clampLength(std::move(details)));
        }
    }

    if (jState) {
        const char* s = env->GetStringUTFChars(jState, nullptr);
        std::string state(s);
        env->ReleaseStringUTFChars(jState, s);
        if (state.size() >= 2) {
            activity.SetState(clampLength(std::move(state)));
        }
    }

    if (partySize > 0) {
        discordpp::ActivityParty party{};
        party.SetId("osudroid");
        party.SetCurrentSize(partySize);
        party.SetMaxSize(partyMax);
        activity.SetParty(std::move(party));
    }

    if (startTimestamp > 0) {
        discordpp::ActivityTimestamps ts{};
        ts.SetStart(static_cast<uint64_t>(startTimestamp));
        activity.SetTimestamps(std::move(ts));
    }

    {
        discordpp::ActivityAssets assets{};
        assets.SetLargeImage("large_image");

        if (jLargeText) {
            const char* s = env->GetStringUTFChars(jLargeText, nullptr);
            std::string largeText(s);
            env->ReleaseStringUTFChars(jLargeText, s);

            if (largeText.size() >= 2) {
                assets.SetLargeText(clampLength(std::move(largeText)));
            }
        }

        activity.SetAssets(std::move(assets));
    }

    g_client->UpdateRichPresence(std::move(activity), [](const discordpp::ClientResult r) {
        if (!r.Successful()) {
            LOGE("UpdateRichPresence failed: %s", r.Error().c_str());
        }
    });
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_clearRichPresence(JNIEnv*, jclass) {
    if (!g_client || !g_isReady.load()) {
        return;
    }

    g_client->ClearRichPresence();
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_destroy(JNIEnv*, jclass) {
    g_client.reset();
    g_isReady.store(false);
    g_authorizationFailed.store(false);
    g_hasAuthorizationCode.store(false);
    g_pendingCode.clear();
    g_pendingVerifier.clear();
    g_pendingRedirectUri.clear();
    LOGI("Client destroyed");
}

}  // extern "C"
