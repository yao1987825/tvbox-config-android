package com.tvbox.configgen.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvbox.configgen.data.Base64Util
import com.tvbox.configgen.data.DEFAULT_FLAGS
import com.tvbox.configgen.data.DEFAULT_WALLPAPER
import com.tvbox.configgen.data.Live
import com.tvbox.configgen.data.Parse
import com.tvbox.configgen.data.ParseExt
import com.tvbox.configgen.data.Site
import com.tvbox.configgen.ui.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: ConfigViewModel = viewModel()) {
    val config by vm.config.collectAsStateWithLifecycle()
    val saved by vm.savedConfigs.collectAsStateWithLifecycle()
    val jsonText by vm.currentJson.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showBase64Dialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TVBox 配置生成器", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showPresetsDialog = true }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "已保存", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { showJsonDialog = true }) {
                        Icon(Icons.Default.Code, contentDescription = "JSON", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { showBase64Dialog = true }) {
                        Icon(Icons.Default.QrCode2, contentDescription = "Base64", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("站点") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("解析") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("直播") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("全局") })
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (tab) {
                    0 -> SitesTab(
                        sites = config.sites,
                        onAdd = vm::addSite,
                        onRemove = vm::removeSite,
                        onUpdate = vm::updateSite
                    )
                    1 -> ParsesTab(
                        parses = config.parses,
                        onAdd = vm::addParse,
                        onRemove = vm::removeParse,
                        onUpdate = vm::updateParse
                    )
                    2 -> LivesTab(
                        lives = config.lives,
                        onAdd = vm::addLive,
                        onRemove = vm::removeLive,
                        onUpdate = vm::updateLive
                    )
                    3 -> GlobalTab(
                        spider = config.spider,
                        wallpaper = config.wallpaper,
                        onSpiderChange = vm::setSpider,
                        onWallpaperChange = vm::setWallpaper,
                        onReset = vm::resetAll,
                        onSavePreset = { showSavePresetDialog = true },
                        onImport = { showImportDialog = true }
                    )
                }
            }
        }
    }

    // ============ 弹窗 ============
    if (showBase64Dialog) {
        Base64Dialog(text = vm.generateBase64(), onDismiss = { showBase64Dialog = false })
    }
    if (showJsonDialog) {
        JsonDialog(json = jsonText, vm = vm, onDismiss = { showJsonDialog = false })
    }
    if (showSavePresetDialog) {
        SavePresetDialog(
            onConfirm = { name ->
                vm.savePreset(name)
                showSavePresetDialog = false
            },
            onDismiss = { showSavePresetDialog = false }
        )
    }
    if (showPresetsDialog) {
        PresetsDialog(
            presets = saved.map { it.name },
            onLoad = { name ->
                vm.loadPreset(name)
                showPresetsDialog = false
            },
            onDelete = vm::deletePreset,
            onClear = vm::clearPresets,
            onDismiss = { showPresetsDialog = false }
        )
    }
    if (showImportDialog) {
        ImportDialog(
            onImport = { text ->
                val r = vm.importFromBase64(text)
                showImportDialog = false
                r
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

// ============== 全局设置 ==============
@Composable
private fun GlobalTab(
    spider: String,
    wallpaper: String,
    onSpiderChange: (String) -> Unit,
    onWallpaperChange: (String) -> Unit,
    onReset: () -> Unit,
    onSavePreset: () -> Unit,
    onImport: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("爬虫 Jar 包地址", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = spider,
                onValueChange = onSpiderChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("./custom_spider.jar 或 http://...") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            Text("背景图片地址", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = wallpaper,
                onValueChange = onWallpaperChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(DEFAULT_WALLPAPER) },
                singleLine = true
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("操作", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSavePreset) { Text("保存为预设") }
                OutlinedButton(onClick = onImport) { Text("导入Base64") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onReset, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("重置所有配置")
            }
        }
    }
}

// ============== Sites ==============
@Composable
private fun SitesTab(
    sites: List<Site>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, Site) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("视频源 (Sites)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("新增源")
        }
    }
    Spacer(Modifier.height(12.dp))

    sites.forEachIndexed { idx, s ->
        Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row {
                    Text("#${idx + 1}  ${s.name.ifBlank { "(未命名)" }}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onRemove(idx) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedTextField(
                    value = s.name,
                    onValueChange = { onUpdate(idx, s.copy(name = it)) },
                    label = { Text("源名称") },
                    placeholder = { Text("🎬我的视频源") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = s.api,
                    onValueChange = { onUpdate(idx, s.copy(api = it)) },
                    label = { Text("API 接口地址") },
                    placeholder = { Text("https://example.com/api.php/provide/vod") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = s.searchable == 1,
                        onClick = { onUpdate(idx, s.copy(searchable = if (s.searchable == 1) 0 else 1)) },
                        label = { Text("可搜索") }
                    )
                    FilterChip(
                        selected = s.quickSearch == 1,
                        onClick = { onUpdate(idx, s.copy(quickSearch = if (s.quickSearch == 1) 0 else 1)) },
                        label = { Text("快速搜索") }
                    )
                    FilterChip(
                        selected = s.filterable == 1,
                        onClick = { onUpdate(idx, s.copy(filterable = if (s.filterable == 1) 0 else 1)) },
                        label = { Text("可筛选") }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("key: ${s.key}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ============== Parses ==============
@Composable
private fun ParsesTab(
    parses: List<Parse>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, Parse) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("解析接口 (Parses)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("新增解析")
        }
    }
    Spacer(Modifier.height(12.dp))

    parses.forEachIndexed { idx, p ->
        Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row {
                    Text("#${idx + 1}  ${p.name.ifBlank { "(未命名)" }}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onRemove(idx) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedTextField(
                    value = p.name,
                    onValueChange = { onUpdate(idx, p.copy(name = it)) },
                    label = { Text("解析名称") },
                    placeholder = { Text("聚合解析") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = p.url,
                    onValueChange = { onUpdate(idx, p.copy(url = it)) },
                    label = { Text("解析 URL") },
                    placeholder = { Text("https://jx.example.com/?url=") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                val flagsText = remember(p.ext.flag) { p.ext.flag.joinToString(", ") }
                OutlinedTextField(
                    value = flagsText,
                    onValueChange = { newText ->
                        val list = newText.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
                        onUpdate(idx, p.copy(ext = ParseExt(flag = list)))
                    },
                    label = { Text("平台标识 (Flags)") },
                    placeholder = { Text("qq, youku, qiyi") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("默认: ${DEFAULT_FLAGS.joinToString(", ")}", fontSize = 11.sp)
                    },
                    singleLine = true
                )
            }
        }
    }
}

// ============== Lives ==============
@Composable
private fun LivesTab(
    lives: List<Live>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, Live) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("直播源 (Lives)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("新增线路")
        }
    }
    Spacer(Modifier.height(12.dp))

    lives.forEachIndexed { idx, l ->
        Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row {
                    Text("#${idx + 1}  ${l.name.ifBlank { "(未命名)" }}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onRemove(idx) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedTextField(
                    value = l.name,
                    onValueChange = { onUpdate(idx, l.copy(name = it)) },
                    label = { Text("线路名称") },
                    placeholder = { Text("央卫直播") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("直播类型", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = l.type == 0,
                        onClick = { onUpdate(idx, l.copy(type = 0)) },
                        label = { Text("M3U") }
                    )
                    FilterChip(
                        selected = l.type == 1,
                        onClick = { onUpdate(idx, l.copy(type = 1)) },
                        label = { Text("TXT") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = l.url,
                    onValueChange = { onUpdate(idx, l.copy(url = it)) },
                    label = { Text("直播地址") },
                    placeholder = { Text("https://example.com/live.txt") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = l.epg,
                    onValueChange = { onUpdate(idx, l.copy(epg = it)) },
                    label = { Text("EPG 地址 (可选)") },
                    placeholder = { Text("https://example.com/epg.xml") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

// ============== Base64 弹窗 ==============
@Composable
private fun Base64Dialog(text: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Base64 编码 (TVBox URL)") },
        text = {
            Column {
                Text("格式: 8位数字**base64", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                copyToClipboard(ctx, "tvbox_config", text)
                onDismiss()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("复制")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

// ============== JSON 弹窗 ==============
@Composable
private fun JsonDialog(json: String, vm: ConfigViewModel, onDismiss: () -> Unit) {
    var editing by remember { mutableStateOf(json) }
    var msg by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current

    LaunchedEffect(json) { editing = json }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("JSON 配置") },
        text = {
            Column {
                OutlinedTextField(
                    value = editing,
                    onValueChange = { editing = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 360.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )
                msg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { copyToClipboard(ctx, "tvbox_json", editing) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("复制")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val r = vm.applyJsonText(editing)
                    if (r.isSuccess) {
                        msg = "已应用"
                        onDismiss()
                    } else {
                        msg = r.exceptionOrNull()?.message ?: "解析失败"
                    }
                }) {
                    Text("应用")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

// ============== 保存预设弹窗 ==============
@Composable
private fun SavePresetDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存为预设") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("配置名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ============== 预设列表弹窗 ==============
@Composable
private fun PresetsDialog(
    presets: List<String>,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("已保存的配置 (${presets.size})") },
        text = {
            if (presets.isEmpty()) {
                Text("暂无保存的配置", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(Modifier.fillMaxWidth()) {
                    presets.forEach { name ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onLoad(name) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(name)
                            }
                            IconButton(onClick = { onDelete(name) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (presets.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("清空全部") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

// ============== 导入 Base64 弹窗 ==============
@Composable
private fun ImportDialog(onImport: (String) -> Result<*>, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入 Base64 配置") },
        text = {
            Column {
                Text("粘贴 数字**base64 或纯 base64 字符串", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; err = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                    placeholder = { Text("12345678**eyJzcGlk...") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )
                err?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val r = onImport(input)
                if (r.isFailure) err = r.exceptionOrNull()?.message
            }) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ============== 工具 ==============
private fun copyToClipboard(ctx: Context, label: String, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}