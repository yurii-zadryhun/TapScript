# Performance

The target is responsive automation without treating OCR like a 60 FPS video filter.

## Strategy

1. `ImageReader.acquireLatestImage()` drops stale frames.
2. `ConflatedFrameBus` stores only the latest accepted frame.
3. `AutomationRunner` applies a minimum recognition interval (default 120 ms).
4. `SampledFrameChangeDetector` samples only the enabled recognition regions, so animation elsewhere in a game does not trigger pointless OCR.
5. only configured regions are cropped and sent to OCR.
6. decisions operate on extracted values, not image pixels.

## Why ROI-first matters

A game screenshot can be millions of pixels while a stat line is only a few percent of the display. Cropping before OCR reduces both inference work and false positives.

## Tuning

Profile settings:

- `minFrameIntervalMs`: lower for fast-changing UI, higher for menus.
- `changeThreshold`: higher if animations cause unnecessary OCR.
- `postActionCooldownMs`: short safety delay after emitted commands while the target UI transitions.
- number/size of recognition regions: usually the biggest cost after OCR model choice.

Recommended starting values:

```text
minFrameIntervalMs    = 120
changeThreshold        = 0.02
postActionCooldownMs   = 200–300
```

For a static loot dialog, 6–10 recognition passes/second is generally far more than necessary; the important part is low latency after a meaningful visual change.

## Allocation note

The MVP converts accepted MediaProjection images into `Bitmap` objects. Processed frames and ROI crops are explicitly recycled, and the capacity-one frame bus recycles buffered frames that are superseded before consumption. This is simple and reliable but not the theoretical minimum-allocation design. If profiling shows GC pressure, the next optimization is a small reusable bitmap pool / frame-lease abstraction, not premature native code.

## Benchmark plan

Use Android Studio profiler and add a deterministic screenshot benchmark:

- 100 repeated OCR runs for one ROI;
- median / p95 recognition latency;
- parser latency;
- total decision latency;
- allocations per accepted frame.

Do not optimize from a single end-to-end anecdote; measure each stage.
