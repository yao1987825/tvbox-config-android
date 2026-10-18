package com.tvbox.configgen.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvbox.configgen.data.Base64Util
import com.tvbox.configgen.data.ConfigRepository
import com.tvbox.configgen.data.Live
import com.tvbox.configgen.data.Parse
import com.tvbox.configgen.data.ParseExt
import com.tvbox.configgen.data.SavedConfig
import com.tvbox.configgen.data.Site
import com.tvbox.configgen.data.TvBoxConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 顶层 ViewModel: 持有当前配置 + 已保存列表
 */
class ConfigViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ConfigRepository(app)

    private val _config = MutableStateFlow(TvBoxConfig.empty())
    val config: StateFlow<TvBoxConfig> = _config.asStateFlow()

    val savedConfigs: StateFlow<List<SavedConfig>> =
        repo.savedFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentJson: StateFlow<String> = MutableStateFlow("").also { sf ->
        viewModelScope.launch {
            _config.collect { c ->
                sf.value = TvBoxConfig.compactJson
                    .encodeToString(TvBoxConfig.serializer(), c)
            }
        }
    }.asStateFlow()

    init {
        viewModelScope.launch {
            _config.value = repo.currentFlow.first()
        }
    }

    private fun persist() {
        viewModelScope.launch { repo.saveCurrent(_config.value) }
    }

    // ============== 全局设置 ==============
    fun setSpider(v: String) { _config.update { it.copy(spider = v) }; persist() }
    fun setWallpaper(v: String) { _config.update { it.copy(wallpaper = v) }; persist() }

    // ============== Sites ==============
    fun addSite() {
        _config.update { it.copy(sites = it.sites + TvBoxConfig.newSite()) }
        persist()
    }
    fun removeSite(i: Int) {
        _config.update { c ->
            c.copy(sites = c.sites.toMutableList().also { it.removeAt(i) })
        }
        persist()
    }
    fun updateSite(i: Int, site: Site) {
        _config.update { c ->
            c.copy(sites = c.sites.toMutableList().also { it[i] = site })
        }
        persist()
    }

    // ============== Parses ==============
    fun addParse() {
        _config.update { it.copy(parses = it.parses + TvBoxConfig.newParse()) }
        persist()
    }
    fun removeParse(i: Int) {
        _config.update { c ->
            c.copy(parses = c.parses.toMutableList().also { it.removeAt(i) })
        }
        persist()
    }
    fun updateParse(i: Int, parse: Parse) {
        _config.update { c ->
            c.copy(parses = c.parses.toMutableList().also { it[i] = parse })
        }
        persist()
    }

    // ============== Lives ==============
    fun addLive() {
        _config.update { it.copy(lives = it.lives + TvBoxConfig.newLive()) }
        persist()
    }
    fun removeLive(i: Int) {
        _config.update { c ->
            c.copy(lives = c.lives.toMutableList().also { it.removeAt(i) })
        }
        persist()
    }
    fun updateLive(i: Int, live: Live) {
        _config.update { c ->
            c.copy(lives = c.lives.toMutableList().also { it[i] = live })
        }
        persist()
    }

    // ============== 整体操作 ==============
    fun resetAll() {
        _config.value = TvBoxConfig.empty()
        persist()
    }

    fun loadFromJsonText(text: String): Result<TvBoxConfig> = runCatching {
        TvBoxConfig.compactJson.decodeFromString(TvBoxConfig.serializer(), text)
    }.onSuccess { c ->
        _config.value = c
        persist()
    }

    fun importFromBase64(text: String): Result<TvBoxConfig> = runCatching {
        Base64Util.decodeTvBox(text)
    }.onSuccess { c ->
        _config.value = c
        persist()
    }

    fun generateBase64(): String = Base64Util.encodeTvBox(_config.value)

    /** 把当前 JSON 文本回填到模型(JSON 编辑器) */
    fun applyJsonText(text: String): Result<Unit> = loadFromJsonText(text).map { }

    // ============== 预设 ==============
    fun savePreset(name: String) {
        viewModelScope.launch { repo.saveAsPreset(name, _config.value) }
    }
    fun loadPreset(name: String) {
        viewModelScope.launch {
            repo.loadPreset(name)?.let {
                _config.value = it
                persist()
            }
        }
    }
    fun deletePreset(name: String) {
        viewModelScope.launch { repo.deletePreset(name) }
    }
    fun clearPresets() {
        viewModelScope.launch { repo.clearAllPresets() }
    }
}

// 工具: 让 MutableStateFlow.update 写法更顺手
private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    while (true) {
        val prev = value
        val next = transform(prev)
        if (compareAndSet(prev, next)) return
    }
}