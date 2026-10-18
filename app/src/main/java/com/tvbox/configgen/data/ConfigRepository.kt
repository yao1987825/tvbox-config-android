package com.tvbox.configgen.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 配置仓库 - 使用 Preferences DataStore 存储多套已保存配置
 *
 * 存储结构:
 *   - key "current_config": 当前正在编辑的配置 (JSON)
 *   - key "saved_configs":  已保存的所有配置名 (JSON List<SavedConfig>)
 */
private val Context.dataStore by preferencesDataStore(name = "tvbox_configs")

class ConfigRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val currentKey = stringPreferencesKey("current_config")
    private val savedKey = stringPreferencesKey("saved_configs")

    /** 当前编辑的配置 */
    val currentFlow: Flow<TvBoxConfig> = context.dataStore.data.map { prefs ->
        prefs[currentKey]?.let { deserialize(it) } ?: TvBoxConfig.empty()
    }

    /** 已保存的所有配置 */
    val savedFlow: Flow<List<SavedConfig>> = context.dataStore.data.map { prefs ->
        prefs[savedKey]?.let { decodeList(it) } ?: emptyList()
    }

    suspend fun saveCurrent(config: TvBoxConfig) {
        context.dataStore.edit { it[currentKey] = serialize(config) }
    }

    suspend fun saveAsPreset(name: String, config: TvBoxConfig) {
        val list = savedFlow.first().toMutableList()
        val now = System.currentTimeMillis()
        val idx = list.indexOfFirst { it.name == name }
        val entry = SavedConfig(
            name = name,
            config = config,
            createdAt = if (idx >= 0) list[idx].createdAt else now,
            updatedAt = now
        )
        if (idx >= 0) list[idx] = entry else list.add(entry)
        context.dataStore.edit { it[savedKey] = encodeList(list) }
    }

    suspend fun deletePreset(name: String) {
        val list = savedFlow.first().filterNot { it.name == name }
        context.dataStore.edit { it[savedKey] = encodeList(list) }
    }

    suspend fun clearAllPresets() {
        context.dataStore.edit { it[savedKey] = encodeList(emptyList()) }
    }

    suspend fun loadPreset(name: String): TvBoxConfig? {
        return savedFlow.first().firstOrNull { it.name == name }?.config
    }

    // ============== 序列化辅助 ==============

    private fun serialize(c: TvBoxConfig) =
        json.encodeToString(TvBoxConfig.serializer(), c)

    private fun deserialize(s: String): TvBoxConfig =
        json.decodeFromString(TvBoxConfig.serializer(), s)

    private fun encodeList(list: List<SavedConfig>) =
        json.encodeToString(ListSerializer(SavedConfig.serializer()), list)

    private fun decodeList(s: String): List<SavedConfig> =
        json.decodeFromString(ListSerializer(SavedConfig.serializer()), s)
}