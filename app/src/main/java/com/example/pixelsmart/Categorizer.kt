package com.example.pixelsmart

import android.content.pm.ApplicationInfo

/** Определяет категорию приложения: своя база -> системная категория -> ключевые слова. */
object Categorizer {

    private val known: Map<String, Category> = buildMap {
        fun add(c: Category, vararg pkgs: String) = pkgs.forEach { put(it, c) }

        add(Category.SOCIAL,
            "org.telegram.messenger", "org.telegram.messenger.web", "com.whatsapp",
            "com.vkontakte.android", "com.instagram.android", "com.facebook.katana",
            "com.facebook.orca", "com.twitter.android", "com.zhiliaoapp.musically",
            "com.discord", "ru.ok.android", "com.viber.voip", "com.snapchat.android",
            "org.thoughtcrime.securesms", "com.reddit.frontpage", "com.google.android.apps.messaging",
            "com.google.android.dialer", "com.android.mms", "com.linkedin.android")

        add(Category.MEDIA,
            "com.google.android.youtube", "com.google.android.apps.youtube.music",
            "com.spotify.music", "com.netflix.mediaclient", "ru.yandex.music",
            "com.google.android.apps.photos", "org.videolan.vlc", "com.google.android.videos",
            "com.google.android.music", "ru.kinopoisk", "com.amazon.avod.thirdpartyclient")

        add(Category.WORK,
            "com.google.android.gm", "com.google.android.apps.docs",
            "com.google.android.apps.docs.editors.docs", "com.google.android.apps.docs.editors.sheets",
            "com.google.android.apps.docs.editors.slides", "com.microsoft.office.word",
            "com.microsoft.office.excel", "com.microsoft.office.powerpoint",
            "com.microsoft.office.outlook", "com.slack", "us.zoom.videomeetings",
            "com.google.android.calendar", "com.google.android.keep", "com.notion.id",
            "com.microsoft.teams", "com.google.android.apps.meetings")

        add(Category.TOOLS,
            "com.android.chrome", "org.mozilla.firefox", "com.android.settings",
            "com.google.android.calculator", "com.android.calculator2",
            "com.google.android.deskclock", "com.android.deskclock",
            "com.google.android.apps.nbu.files", "com.android.camera2",
            "com.google.android.GoogleCamera", "com.google.android.apps.maps",
            "com.android.vending", "ru.yandex.yandexmaps", "com.google.android.googlequicksearchbox")
    }

    private val keywords: List<Pair<Category, List<String>>> = listOf(
        Category.GAMES to listOf("game", "games", "gaming"),
        Category.SOCIAL to listOf("messenger", "chat", "social", "dialer", "contacts", "messaging"),
        Category.MEDIA to listOf("music", "video", "player", "photos", "gallery", "radio", "podcast"),
        Category.WORK to listOf("mail", "docs", "office", "calendar", "notes", "meet", "drive"),
        Category.TOOLS to listOf("camera", "calculator", "clock", "settings", "files", "vpn",
            "browser", "maps", "weather", "flashlight")
    )

    fun detect(pkg: String, systemCategory: Int): Category {
        known[pkg]?.let { return it }

        val fromSystem = when (systemCategory) {
            ApplicationInfo.CATEGORY_GAME -> Category.GAMES
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> Category.MEDIA
            ApplicationInfo.CATEGORY_SOCIAL,
            ApplicationInfo.CATEGORY_NEWS -> Category.SOCIAL
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> Category.WORK
            ApplicationInfo.CATEGORY_MAPS -> Category.TOOLS
            else -> null
        }
        if (fromSystem != null) return fromSystem

        val p = pkg.lowercase()
        for ((cat, words) in keywords) {
            if (words.any { p.contains(it) }) return cat
        }
        return Category.OTHER
    }
}
