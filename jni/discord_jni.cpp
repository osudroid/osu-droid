#include <jni.h>
#include <android/log.h>
#include <memory>
#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"

#define TAG "DiscordJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::unique_ptr<discordpp::Client> g_client;

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

extern "C" {

// Allocates the discordpp::Client and sets its application ID. This is all that's needed for
// Rich Presence via RPC to a locally running Discord client.
JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_create(JNIEnv*, jclass, const jlong clientId) {
    if (g_client) {
        return;
    }

    g_client = std::make_unique<discordpp::Client>();
    g_client->SetApplicationId(static_cast<uint64_t>(clientId));

    LOGI("Client created for application %lld", static_cast<long long>(clientId));
}

// Pumps the SDK event loop. Must be called repeatedly for UpdateRichPresence's callback to fire.
JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_runCallbacks(JNIEnv*, jclass) {
    discordpp::RunCallbacks();
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_updateRichPresence(
        JNIEnv* env, jclass,
        const jstring jDetails, const jstring jState,
        const jint partySize, const jint partyMax, const jlong startTimestamp,
        const jstring jLargeText, const jstring jButtonLabel, const jstring jButtonUrl) {
    if (!g_client) {
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

    if (jButtonLabel && jButtonUrl) {
        const char* labelStr = env->GetStringUTFChars(jButtonLabel, nullptr);
        std::string label(labelStr);
        env->ReleaseStringUTFChars(jButtonLabel, labelStr);

        const char* urlStr = env->GetStringUTFChars(jButtonUrl, nullptr);
        std::string url(urlStr);
        env->ReleaseStringUTFChars(jButtonUrl, urlStr);

        if (!label.empty() && !url.empty()) {
            discordpp::ActivityButton button{};
            button.SetLabel(std::move(label));
            button.SetUrl(std::move(url));
            activity.AddButton(std::move(button));
        }
    }

    // Failure here (e.g. Discord not installed/running) is expected and silent. This is a
    // best-effort RPC call, not a maintained connection.
    g_client->UpdateRichPresence(std::move(activity), [](const discordpp::ClientResult r) {
        if (!r.Successful()) {
            LOGW("UpdateRichPresence failed: %s", r.Error().c_str());
        }
    });
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_clearRichPresence(JNIEnv*, jclass) {
    if (!g_client) {
        return;
    }

    g_client->ClearRichPresence();
}

JNIEXPORT void JNICALL
Java_com_osudroid_discord_DiscordNative_destroy(JNIEnv*, jclass) {
    g_client.reset();
    LOGI("Client destroyed");
}

}  // extern "C"
