package com.example.pixelsmart

import androidx.compose.ui.graphics.ImageBitmap

enum class Category(val title: String) {
    SOCIAL("Соцсети"),
    GAMES("Игры"),
    WORK("Работа"),
    MEDIA("Медиа"),
    TOOLS("Инструменты"),
    OTHER("Прочее")
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    val category: Category
)
