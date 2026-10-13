package com.mrvicente.screentranslate

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object TranslatePipeline {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val identifier = LanguageIdentification.getClient()

    fun process(
        bitmap: Bitmap,
        sourcePref: String,
        target: String,
        onResult: (List<OverlayItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { visionText ->
                val lines = extractLines(visionText)
                if (lines.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }
                resolveSource(sourcePref, lines) { source ->
                    if (source == target) {
                        onResult(lines)
                        return@resolveSource
                    }
                    translateAll(lines, source, target, onResult, onError)
                }
            }
            .addOnFailureListener { onError(it) }
    }

    private fun extractLines(visionText: Text): List<OverlayItem> {
        val result = mutableListOf<OverlayItem>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box: Rect = line.boundingBox ?: continue
                val text = line.text.trim()
                if (text.isNotEmpty()) result.add(OverlayItem(box, text))
            }
        }
        return result
    }

    private fun resolveSource(pref: String, lines: List<OverlayItem>, cb: (String) -> Unit) {
        if (pref != Constants.AUTO) {
            cb(pref)
            return
        }
        val joined = lines.joinToString(" ") { it.text }.take(400)
        identifier.identifyLanguage(joined)
            .addOnSuccessListener { code ->
                cb(if (code == "und") "en" else code)
            }
            .addOnFailureListener { cb("en") }
    }

    private fun translateAll(
        lines: List<OverlayItem>,
        source: String,
        target: String,
        onResult: (List<OverlayItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translateSequential(translator, lines, 0, mutableListOf(), onResult, onError)
            }
            .addOnFailureListener { onError(it) }
    }

    private fun translateSequential(
        translator: Translator,
        lines: List<OverlayItem>,
        index: Int,
        acc: MutableList<OverlayItem>,
        onResult: (List<OverlayItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (index >= lines.size) {
            onResult(acc)
            return
        }
        val current = lines[index]
        translator.translate(current.text)
            .addOnSuccessListener { translated ->
                acc.add(OverlayItem(current.rect, translated))
                translateSequential(translator, lines, index + 1, acc, onResult, onError)
            }
            .addOnFailureListener { onError(it) }
    }
}
