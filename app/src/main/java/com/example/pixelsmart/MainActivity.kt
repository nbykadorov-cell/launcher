package com.example.pixelsmart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LauncherScreen(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}

private fun launchApp(ctx: Context, pkg: String) {
    ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(it)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val ctx = LocalContext.current
    var drawerOpen by remember { mutableStateOf(false) }
    var dragSum by remember { mutableFloatStateOf(0f) }
    var dialogPkg by remember { mutableStateOf<String?>(null) }
    val date = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    }

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragSum = 0f },
                    onDragEnd = { if (dragSum < -120f) drawerOpen = true },
                    onVerticalDrag = { _, delta -> dragSum += delta }
                )
            }
    ) {
        // Домашний экран
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(date, color = Color.White, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                vm.dockApps.forEach { app ->
                    AppIcon(
                        app = app,
                        showLabel = false,
                        onClick = { launchApp(ctx, app.packageName) },
                        onLongClick = { dialogPkg = app.packageName }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                onClick = { drawerOpen = true },
                shape = RoundedCornerShape(50),
                color = Color(0xCC202124),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Поиск приложений", color = Color(0xFFBBBBBB))
                }
            }
        }

        // Меню приложений
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            DrawerScreen(
                vm = vm,
                onLaunch = { pkg -> drawerOpen = false; launchApp(ctx, pkg) },
                onLongClick = { dialogPkg = it }
            )
        }
    }

    dialogPkg?.let { pkg ->
        val app = vm.apps.find { it.packageName == pkg }
        if (app == null) {
            dialogPkg = null
        } else {
            AlertDialog(
                onDismissRequest = { dialogPkg = null },
                confirmButton = { TextButton(onClick = { dialogPkg = null }) { Text("Закрыть") } },
                title = { Text(app.label) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("Категория", fontSize = 13.sp)
                        Category.values().forEach { c ->
                            TextButton(onClick = { vm.setCategory(pkg, c) }) {
                                Text((if (c == app.category) "✓ " else "") + c.title)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { vm.toggleDock(pkg) }) {
                            Text(if (vm.isInDock(pkg)) "Убрать из дока" else "Добавить в док")
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(
    vm: LauncherViewModel,
    onLaunch: (String) -> Unit,
    onLongClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Category?>(null) }
    val tabs: List<Category?> = listOf<Category?>(null) + Category.values().toList()

    val list = vm.apps.filter {
        (selected == null || it.category == selected) &&
            it.label.contains(query, ignoreCase = true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF2101114))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Поиск приложений") },
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs) { cat ->
                FilterChip(
                    selected = selected == cat,
                    onClick = { selected = cat },
                    label = { Text(cat?.title ?: "Все") }
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(list, key = { it.packageName }) { app ->
                AppIcon(
                    app = app,
                    showLabel = true,
                    onClick = { onLaunch(app.packageName) },
                    onLongClick = { onLongClick(app.packageName) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(56.dp))
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                app.label,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
