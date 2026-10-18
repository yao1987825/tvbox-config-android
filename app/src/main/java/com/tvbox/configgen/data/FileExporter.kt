package com.tvbox.configgen.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * 文件导出工具
 *
 * 1. saveJsonToDownloads: 把 JSON 保存到公共 Downloads 目录
 *    - Android 10+: MediaStore.Downloads (无需 WRITE_EXTERNAL_STORAGE)
 *    - Android 9 及以下: 直接写入 Environment.DIRECTORY_DOWNLOADS
 *
 * 2. shareJson: 通过系统分享面板分享 JSON
 *    - 使用 FileProvider, 把文件存到 cache 目录, 输出 content:// URI
 *    - 用户可选择发到微信、QQ、邮件、网盘等
 */
object FileExporter {

    /** 把 JSON 内容保存到 Downloads/tvbox-config.json 并返回文件 URI */
    fun saveJsonToDownloads(ctx: Context, json: String, fileName: String = "tvbox-config.json"): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(ctx, json, fileName)
        } else {
            saveViaLegacyStorage(ctx, json, fileName)
        }
    }

    private fun saveViaMediaStore(ctx: Context, json: String, fileName: String): Uri? {
        val resolver = ctx.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return null
        try {
            resolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }
    }

    private fun saveViaLegacyStorage(ctx: Context, json: String, fileName: String): Uri? {
        @Suppress("DEPRECATION")
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()
        val file = File(downloads, fileName)
        return try {
            file.writeText(json, Charsets.UTF_8)
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 通过 FileProvider 分享 JSON 文件
     * 文件会先存到 cacheDir/shares/ 下, 然后构造 content:// URI 启动分享
     */
    fun shareJson(ctx: Context, json: String, fileName: String = "tvbox-config.json"): Intent? {
        return try {
            val sharesDir = File(ctx.cacheDir, "shares").apply { mkdirs() }
            val file = File(sharesDir, fileName)
            file.writeText(json, Charsets.UTF_8)
            val authority = "${ctx.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(ctx, authority, file)

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "TVBox 配置文件")
                putExtra(Intent.EXTRA_TEXT, "TVBox 配置 JSON 附件")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(send, "分享 TVBox 配置").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            null
        }
    }
}