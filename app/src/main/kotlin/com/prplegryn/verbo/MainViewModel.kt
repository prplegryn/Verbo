package com.prplegryn.verbo

import android.app.Application
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prplegryn.verbo.ai.AiConfiguration
import com.prplegryn.verbo.pdf.PdfPreviewRenderer
import com.prplegryn.verbo.storage.AiConfigStore
import com.prplegryn.verbo.storage.DocumentExporter
import com.prplegryn.verbo.translation.TranslationCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val configStore = AiConfigStore(application)
    private val coordinator = TranslationCoordinator(application)
    private val renderer = PdfPreviewRenderer()
    private val exporter = DocumentExporter(application)
    private var selectedUri: Uri? = null

    val progress = coordinator.progress

    private val _config = MutableStateFlow(configStore.load())
    val config: StateFlow<AiConfiguration> = _config

    private val _selectedFileName = MutableStateFlow("")
    val selectedFileName: StateFlow<String> = _selectedFileName

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap

    fun selectPdf(uri: Uri) {
        selectedUri = uri
        _selectedFileName.value = displayName(uri) ?: uri.lastPathSegment ?: "selected.pdf"
        _previewBitmap.value = null
    }

    fun updateConfig(config: AiConfiguration) {
        _config.value = config
        configStore.save(config)
    }

    fun startPreview() {
        val uri = selectedUri ?: return
        val currentConfig = _config.value
        configStore.save(currentConfig)
        _previewBitmap.value = null
        viewModelScope.launch {
            coordinator.startPreview(uri, _selectedFileName.value.ifBlank { "selected.pdf" }, currentConfig)
            progress.value.previewFile?.let { renderPreview(it) }
        }
    }

    fun approveFullTranslation() {
        viewModelScope.launch {
            coordinator.approveFullTranslation()
            progress.value.fullFile?.let { renderPreview(it) }
        }
    }

    fun exportPreview(target: Uri) {
        export(progress.value.previewFile, target)
    }

    fun exportFull(target: Uri) {
        export(progress.value.fullFile, target)
    }

    private fun export(file: File?, target: Uri) {
        if (file == null) return
        viewModelScope.launch {
            exporter.export(file, target)
        }
    }

    private suspend fun renderPreview(file: File) {
        _previewBitmap.value = renderer.renderFirstPage(file)
    }

    private fun displayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        val cursor: Cursor? = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return it.getString(index)
            }
        }
        return null
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(application) as T
                }
            }
    }
}
