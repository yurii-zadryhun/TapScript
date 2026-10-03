package dev.tapscript.engine.api.ports

import android.graphics.Bitmap
import dev.tapscript.engine.api.model.RecognizedText

interface TextRecognizer {
    suspend fun recognize(bitmap: Bitmap): RecognizedText
}
