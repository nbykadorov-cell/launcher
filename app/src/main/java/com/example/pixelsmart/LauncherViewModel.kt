package com.example.pixelsmart

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var dockPackages by mutableStateOf<List<String>>(emptyList())
        private set

    val dockApps: List<AppInfo>
        get() = dockPackages.mapNotNull { p -> apps.find { it.packageName == p } }

    fun refresh() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) { loadApps() }
            apps = loaded
            dockPackages = if (prefs.contains("dock")) {
                prefs.getString("dock", "").orEmpty().split(",").filter { it.isNotBlank() }
            } else {
                defaultDock()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadApps(): List<AppInfo> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                val sys = ri.activityInfo.applicationInfo.category
                val override = prefs.getString("cat_$pkg", null)
                    ?.let { runCatching { Category.valueOf(it) }.getOrNull() }
                AppInfo(
                    packageName = pkg,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm).toBitmap(120, 120).asImageBitmap(),
                    category = override ?: Categorizer.detect(pkg, sys)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    @Suppress("DEPRECATION")
    private fun defaultDock(): List<String> {
        val pm = getApplication<Application>().packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent("android.media.action.IMAGE_CAPTURE")
        )
        return intents
            .mapNotNull { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }
            .filter { it != "android" }
            .distinct()
    }

    fun setCategory(pkg: String, category: Category) {
        prefs.edit().putString("cat_$pkg", category.name).apply()
        apps = apps.map { if (it.packageName == pkg) it.copy(category = category) else it }
    }

    fun isInDock(pkg: String) = pkg in dockPackages

    fun toggleDock(pkg: String) {
        val list = if (pkg in dockPackages) dockPackages - pkg
        else if (dockPackages.size < 5) dockPackages + pkg
        else dockPackages
        dockPackages = list
        prefs.edit().putString("dock", list.joinToString(",")).apply()
    }
}
