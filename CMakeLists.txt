cmake_minimum_required(VERSION 3.22.1)
project(discord_jni)

find_package(discord_partner_sdk REQUIRED CONFIG)

add_library(discord_jni SHARED jni/discord_jni.cpp)
target_compile_features(discord_jni PRIVATE cxx_std_17)

target_link_libraries(discord_jni
    discord_partner_sdk::discord_partner_sdk
    android
    log)

# Expose Prefab include dirs to the IDE indexer (clangd)
get_target_property(_discord_includes discord_partner_sdk::discord_partner_sdk INTERFACE_INCLUDE_DIRECTORIES)
target_include_directories(discord_jni PRIVATE ${_discord_includes})
