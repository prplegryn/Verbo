package com.prplegryn.verbo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.prplegryn.verbo.MainViewModel
import com.prplegryn.verbo.ai.AiConfiguration
import com.prplegryn.verbo.translation.PageWorkState
import com.prplegryn.verbo.translation.TranslationPhase
import com.prplegryn.verbo.translation.TranslationProgress
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ConvertFile
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun VerboApp(
    viewModel: MainViewModel,
    onPickPdf: () -> Unit,
    onExportPreview: () -> Unit,
    onExportFull: () -> Unit,
) {
    val progress by viewModel.progress.collectAsState()
    val config by viewModel.config.collectAsState()
    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()

    AppTheme {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = "Verbo",
                    subtitle = phaseLabel(progress.phase),
                    actions = {
                        Icon(
                            imageVector = MiuixIcons.Settings,
                            contentDescription = "AI 配置",
                            modifier = Modifier.size(22.dp),
                        )
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SourceCard(
                        selectedFileName = selectedFileName,
                        onPickPdf = onPickPdf,
                    )
                }
                item {
                    AiConfigCard(
                        config = config,
                        onConfigChange = viewModel::updateConfig,
                    )
                }
                item {
                    ActionCard(
                        selectedFileName = selectedFileName,
                        config = config,
                        progress = progress,
                        onStartPreview = viewModel::startPreview,
                        onApproveFull = viewModel::approveFullTranslation,
                        onExportPreview = onExportPreview,
                        onExportFull = onExportFull,
                    )
                }
                if (progress.guide.isNotBlank()) {
                    item {
                        GuideCard(progress.guide)
                    }
                }
                if (previewBitmap != null) {
                    item {
                        PreviewCard {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "PDF 预览",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
                if (progress.pageProgress.isNotEmpty()) {
                    item {
                        Text(
                            text = "页面状态",
                            style = MiuixTheme.textStyles.title2,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }
                    items(progress.pageProgress.values.sortedByDescending { it.page }.take(18)) { page ->
                        PageStatusRow(page.page, page.state, page.attempts, page.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(
    selectedFileName: String,
    onPickPdf: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(MiuixIcons.File, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "PDF", style = MiuixTheme.textStyles.title2)
                Text(
                    text = selectedFileName.ifBlank { "未选择" },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Button(onClick = onPickPdf) {
                Icon(MiuixIcons.Import, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("选择")
            }
        }
    }
}

@Composable
private fun AiConfigCard(
    config: AiConfiguration,
    onConfigChange: (AiConfiguration) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(MiuixIcons.Settings, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(text = "AI", style = MiuixTheme.textStyles.title2)
        }
        Spacer(Modifier.height(12.dp))
        TextField(
            value = config.baseUrl,
            onValueChange = { onConfigChange(config.copy(baseUrl = it)) },
            label = "Base URL",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = config.endpointPath,
            onValueChange = { onConfigChange(config.copy(endpointPath = it)) },
            label = "Endpoint",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = config.model,
            onValueChange = { onConfigChange(config.copy(model = it)) },
            label = "Model",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = config.apiKey,
            onValueChange = { onConfigChange(config.copy(apiKey = it)) },
            label = "API Key",
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = config.temperature.toString(),
                onValueChange = { value ->
                    value.toDoubleOrNull()?.let { onConfigChange(config.copy(temperature = it)) }
                },
                label = "Temperature",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            TextField(
                value = config.maxOutputTokens.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { onConfigChange(config.copy(maxOutputTokens = it)) }
                },
                label = "Max Tokens",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActionCard(
    selectedFileName: String,
    config: AiConfiguration,
    progress: TranslationProgress,
    onStartPreview: () -> Unit,
    onApproveFull: () -> Unit,
    onExportPreview: () -> Unit,
    onExportFull: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(MiuixIcons.ConvertFile, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = progress.statusMessage, style = MiuixTheme.textStyles.title2)
                Text(
                    text = "${progress.translatedPages}/${progress.totalPages} 页 · ${progress.workerCount} 并发",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = progress.progressFraction,
            modifier = Modifier.fillMaxWidth(),
        )
        if (progress.errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(text = progress.errorMessage, color = MiuixTheme.colorScheme.error)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStartPreview,
                enabled = selectedFileName.isNotBlank() && config.isUsable() && progress.canStartPreview,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.weight(1f),
            ) {
                Icon(MiuixIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("预览")
            }
            Button(
                onClick = onApproveFull,
                enabled = progress.canApproveFull,
                modifier = Modifier.weight(1f),
            ) {
                Icon(MiuixIcons.Ok, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("全本")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                text = "保存预览",
                onClick = onExportPreview,
                enabled = progress.previewFile != null,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = "保存全本",
                onClick = onExportFull,
                enabled = progress.fullFile != null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GuideCard(guide: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Text(text = "翻译指南", style = MiuixTheme.textStyles.title2)
        Spacer(Modifier.height(8.dp))
        SelectionContainer {
            Text(text = guide, style = MiuixTheme.textStyles.body2)
        }
    }
}

@Composable
private fun PreviewCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(12.dp),
    ) {
        Text(text = "PDF 预览", style = MiuixTheme.textStyles.title2)
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun PageStatusRow(
    page: Int,
    state: PageWorkState,
    attempts: Int,
    message: String,
) {
    val color = when (state) {
        PageWorkState.Done -> MiuixTheme.colorScheme.primary
        PageWorkState.Failed -> MiuixTheme.colorScheme.error
        PageWorkState.Running -> MiuixTheme.colorScheme.secondary
        PageWorkState.Queued -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "P$page", style = MiuixTheme.textStyles.title2, color = color)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${stateLabel(state)} · $attempts 次 · $message",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun phaseLabel(phase: TranslationPhase): String = when (phase) {
    TranslationPhase.Idle -> "待处理"
    TranslationPhase.Extracting -> "读取 PDF"
    TranslationPhase.BuildingGuide -> "翻译指南"
    TranslationPhase.PreviewTranslating -> "15 页预览"
    TranslationPhase.AwaitingPreviewApproval -> "等待确认"
    TranslationPhase.FullTranslating -> "全本翻译"
    TranslationPhase.Completed -> "完成"
    TranslationPhase.Failed -> "失败"
}

private fun stateLabel(state: PageWorkState): String = when (state) {
    PageWorkState.Queued -> "排队"
    PageWorkState.Running -> "翻译中"
    PageWorkState.Done -> "完成"
    PageWorkState.Failed -> "失败"
}
