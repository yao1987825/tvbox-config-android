package com.tvbox.configgen.data

import android.util.Base64
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.security.SecureRandom

/**
 * Base64 编解码工具
 *
 * 逆向自 https://17nas.com/assets/js/tvbox-config-simple.js 的 generateBase64():
 *
 *   function generateBase64() {
 *       const str = JSON.stringify(config);                       // 紧凑 JSON
 *       const base64 = btoa(
 *         encodeURIComponent(str).replace(/%([0-9A-F]{2})/g,
 *           (m, p1) => String.fromCharCode('0x' + p1)
 *         )
 *       );
 *       const digits = Array.from({length: 8}, () => Math.floor(Math.random() * 10)).join('');
 *       return `${digits}**${base64}`;
 *   }
 *
 * 核心思路:
 *   1. UTF-8 字符串 → JSON 紧凑字符串
 *   2. 把字符串编码成 UTF-8 字节序列
 *   3. 对字节做 Base64 编码
 *   4. 前缀拼接 8 位随机数字 + "**"
 *
 * 解码则反向操作:
 *   1. 剥离 "数字**" 前缀
 *   2. Base64 decode → 字节
 *   3. 字节按 UTF-8 解码 → 字符串
 *   4. JSON parse → 配置对象
 */
object Base64Util {

    private val secureRandom = SecureRandom()
    private val lenientJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        isLenient = true
    }

    /** 生成 8 位随机数字 */
    fun randomDigits(): String = buildString(8) {
        repeat(8) { append(secureRandom.nextInt(10)) }
    }

    /** 把 JSON 字符串编码为 TVBox 配置 URL */
    fun encodeTvBox(json: String): String {
        // UTF-8 字节 → Base64 (NO_WRAP 让结果无换行)
        val bytes = json.toByteArray(Charsets.UTF_8)
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "${randomDigits()}**$base64"
    }

    /** 从配置对象直接生成 TVBox 配置 URL */
    fun encodeTvBox(config: TvBoxConfig): String {
        val json = TvBoxConfig.compactJson.encodeToString(TvBoxConfig.serializer(), config)
        return encodeTvBox(json)
    }

    /**
     * 解码 TVBox 配置 URL
     * 输入可能包含: 8位数字**base64, 也可能是纯 base64, 也可能带 data: 前缀
     */
    fun decodeTvBox(input: String): TvBoxConfig {
        if (input.isBlank()) throw IllegalArgumentException("输入为空")

        var payload = input.trim()
        // 去掉可能的 data: 前缀
        payload = payload.replace(Regex("^data:.*?;base64,"), "")
        // 清理空白
        payload = payload.replace(Regex("\\s+"), "")
        // 剥离 "数字**" 前缀 (TVBox 8位数字**base64)
        if (payload.contains("**")) {
            payload = payload.substringAfter("**")
        }
        if (payload.isBlank()) throw IllegalArgumentException("未找到 Base64 内容")

        val bytes = try {
            Base64.decode(payload, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Base64 解码失败: ${e.message}")
        }
        val json = String(bytes, Charsets.UTF_8)
        return try {
            lenientJson.decodeFromString(TvBoxConfig.serializer(), json)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("JSON 解析失败: ${e.message}")
        }
    }
}