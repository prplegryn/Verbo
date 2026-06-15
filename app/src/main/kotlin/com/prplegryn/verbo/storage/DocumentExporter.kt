package com.prplegryn.verbo.storage

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocumentExporter(
    private val context: Context,
) {
    suspend fun export(source: File, target: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(target).use { output ->
            requireNotNull(output) { "无法写入目标文件" }
            source.inputStream().use { input -> input.copyTo(output) }
        }
    }
}
