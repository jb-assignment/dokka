/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// Based on https://github.com/JetBrains/kotlin/blob/c20f644ee4cd8d28b39b12ea5304b68c5639e531/repo/gradle-settings-conventions/develocity/src/main/kotlin/develocity.settings.gradle.kts
// Because Dokka uses Composite Builds, Build Cache must be configured consistently on:
// - the root settings.gradle.kts,
// - and the settings.gradle.kts of any projects added with `pluginManagement { includedBuild("...") }`
// The Content of this file should be kept in sync with the content at the end of:
//   `build-settings-logic/settings.gradle.kts`
// useful links:
// - develocity: https://docs.gradle.com/develocity/gradle-plugin/
// - build cache: https://docs.gradle.org/8.4/userguide/build_cache.html#sec:build_cache_composite

plugins {
    id("com.gradle.develocity")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
    id("org.gradle.toolchains.foojay-resolver-convention")
}

//region properties
val buildingOnTeamCity: Boolean = System.getenv("TEAMCITY_VERSION") != null
val buildingOnGitHub: Boolean = System.getenv("GITHUB_ACTION") != null
val buildingOnCi: Boolean = System.getenv("CI") != null || buildingOnTeamCity || buildingOnGitHub

fun dokkaProperty(name: String): Provider<String> =
    providers.gradleProperty("org.jetbrains.dokka.$name")

fun <T : Any> dokkaProperty(name: String, convert: (String) -> T): Provider<T> =
    dokkaProperty(name).map(convert)
//endregion

//region Gradle Build Scan
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
        publishing.onlyIf { true }
    }
}
//endregion

//region Gradle Build Cache
val buildCacheLocalEnabled: Provider<Boolean> =
    dokkaProperty("build.cache.local.enabled", String::toBoolean)
        .orElse(!buildingOnCi)
val buildCacheLocalDirectory: Provider<String> =
    dokkaProperty("build.cache.local.directory")
val buildCachePushEnabled: Provider<Boolean> =
    dokkaProperty("build.cache.push", String::toBoolean)
        .orElse(buildingOnCi)

buildCache {
    local {
        isEnabled = buildCacheLocalEnabled.get()
        if (buildCacheLocalDirectory.orNull != null) {
            directory = buildCacheLocalDirectory.get()
        }
    }
    buildCache {
        remote<HttpBuildCache> {
            url = uri("https://build-cache-node-hv2u6plrda-ew.a.run.app/cache")
            isPush = System.getenv().containsKey("TEAMCITY_VERSION")
        }
    }
}
//endregion
