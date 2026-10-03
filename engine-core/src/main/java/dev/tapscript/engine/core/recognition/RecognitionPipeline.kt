package dev.tapscript.engine.core.recognition

import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.RecognitionRegion
import dev.tapscript.engine.api.model.RecognizerKind
import dev.tapscript.engine.api.model.RegionObservation
import dev.tapscript.engine.api.model.ScreenFrame
import dev.tapscript.engine.api.ports.TextRecognizer

class RecognitionPipeline(
    private val textRecognizer: TextRecognizer,
    private val cropper: BitmapRegionCropper,
    private val extractor: RegexValueExtractor,
) {
    suspend fun observe(
        profile: AutomationProfile,
        frame: ScreenFrame,
    ): List<RegionObservation> {
        val observations = mutableListOf<RegionObservation>()
        for (region in profile.regions) {
            if (region.enabled) {
                observations += observeRegion(region, frame)
            }
        }
        return observations
    }

    private suspend fun observeRegion(
        region: RecognitionRegion,
        frame: ScreenFrame,
    ): RegionObservation {
        require(region.recognizer == RecognizerKind.TEXT) {
            "Unsupported recognizer: ${region.recognizer}"
        }

        val startedAt = System.nanoTime()
        return try {
            val cropped = cropper.crop(frame.bitmap, region.bounds)
            val rawText = try {
                textRecognizer.recognize(cropped).text
            } finally {
                if (!cropped.isRecycled) cropped.recycle()
            }

            val variables = buildMap<String, Any?> {
                put("${region.id}.text", rawText)
                region.textConfig.extractors.forEach { spec ->
                    putAll(extractor.extract(rawText, spec))
                }
            }

            RegionObservation(
                regionId = region.id,
                rawText = rawText,
                variables = variables,
                recognitionMs = elapsedMs(startedAt),
            )
        } catch (throwable: Throwable) {
            RegionObservation(
                regionId = region.id,
                rawText = "",
                variables = emptyMap(),
                recognitionMs = elapsedMs(startedAt),
                errorMessage = throwable.message ?: throwable::class.java.simpleName,
            )
        }
    }

    private fun elapsedMs(startedAtNanos: Long): Long =
        (System.nanoTime() - startedAtNanos) / 1_000_000
}
