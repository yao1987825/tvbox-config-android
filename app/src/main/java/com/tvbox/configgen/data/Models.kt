package com.tvbox.configgen.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 数据模型 - 对齐 JS 原始配置结构
 * 来源: https://17nas.com/assets/js/tvbox-config-simple.js
 *
 * 字段顺序需与 JS 的 JSON.stringify 输出顺序一致，
 * TVBox 客户端对字段顺序不敏感，但保持一致便于 diff 和肉眼比对。
 */

// ============== 默认值常量 ==============
val DEFAULT_FLAGS = listOf(
    "youku", "qq", "iqiyi", "qiyi", "letv",
    "sohu", "tudou", "pptv", "mgtv", "wasu"
)
const val DEFAULT_WALLPAPER = "https://api.kdcc.cn"

// ============== 数据类 ==============

/** 视频源 site */
@Serializable
data class Site(
    val key: String,
    val name: String = "",
    val type: Int = 1,
    val api: String = "",
    val searchable: Int = 1,
    val quickSearch: Int = 1,
    val filterable: Int = 1
)

/** 解析接口 parse */
@Serializable
data class ParseExt(
    val flag: List<String>
)

@Serializable
data class Parse(
    val name: String,
    val type: Int = 0,
    val url: String = "",
    val ext: ParseExt
)

/** 直播源 live */
@Serializable
data class Live(
    val name: String = "",
    val type: Int = 0,
    val url: String = "",
    val epg: String = ""
)

/** 顶层 TVBox 配置 */
@Serializable
data class TvBoxConfig(
    val spider: String = "",
    val wallpaper: String = DEFAULT_WALLPAPER,
    val sites: List<Site> = emptyList(),
    val parses: List<Parse> = emptyList(),
    val lives: List<Live> = emptyList()
) {
    companion object {
        /** 紧凑 JSON，匹配 JS 的 JSON.stringify(config) 输出 */
        val compactJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }

        fun empty(): TvBoxConfig = TvBoxConfig(
            sites = listOf(newSite()),
            parses = listOf(newParse()),
            lives = listOf(newLive())
        )

        fun newSite(): Site = Site(key = "site_${System.currentTimeMillis()}")

        fun newParse(): Parse = Parse(
            name = "新解析接口",
            ext = ParseExt(flag = DEFAULT_FLAGS)
        )

        fun newLive(): Live = Live()
    }
}

/** 一组保存的配置 */
@Serializable
data class SavedConfig(
    val name: String,
    val config: TvBoxConfig,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)