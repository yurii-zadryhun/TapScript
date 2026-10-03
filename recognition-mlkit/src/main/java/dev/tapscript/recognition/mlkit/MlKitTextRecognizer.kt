package dev.tapscript.recognition.mlkit

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.tapscript.engine.api.model.RecognizedLine
import dev.tapscript.engine.api.model.RecognizedText
import dev.tapscript.engine.api.ports.TextRecognizer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class MlKitTextRecognizer : TextRecognizer, AutoCloseable {
    private val delegate = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(bitmap: Bitmap): RecognizedText = suspendCancellableCoroutine { continuation ->
        delegate.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (!continuation.isActive) return@addOnSuccessListener
                continuation.resume(
                    RecognizedText(
                        text = result.text,
                        lines = result.textBlocks.flatMap { block ->
                            block.lines.map { line -> RecognizedLine(text = line.text) }
                        },
                    ),
                )
            }
            .addOnFailureListener { throwable ->
                if (continuation.isActive) continuation.resumeWithException(throwable)
            }
    }

    override fun close() {
        delegate.close()
    }
}
