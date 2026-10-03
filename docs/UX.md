# UX design

TapScript has two distinct modes: **authoring** and **running**.

## Authoring

The main editor is intentionally profile-first rather than code-first.

A profile contains:

- target app/package;
- recognition regions;
- tap/swipe targets;
- runtime interval and change sensitivity;
- either no-code rules or a JavaScript script.

### Region setup

The current MVP supports a frozen-screenshot visual picker:

1. import one screenshot;
2. drag a rectangle over interesting text, tap a control location, or drag from a swipe start to its end;
3. TapScript converts the region/point/swipe into normalized coordinates;
4. finish the region extractor / action name in the small form;
5. save.

Direct coordinate editing remains available for precision. Immediate OCR preview and tap-a-recognized-line extractor creation are natural next refinements, but the coordinate-picking workflow already avoids typing rectangles by hand.

### Rule builder

Rules cover common cases without a keyboard-heavy script:

```text
WHEN  candidate.score  >  equipped.score
THEN  tap equip
```

The MVP supports scalar comparisons. Future iterations can add AND/OR groups and formula nodes without changing the command executor.

### JavaScript escape hatch

JavaScript is for scoring formulas, arrays, loops, reusable functions, and game-specific logic that would be awkward in a visual builder.

The editor should always show an API cheat sheet beside the script:

```text
vars        recognized values
 tap(id)    queue a tap
 swipe(id)  queue a swipe
 waitMs(n)  queue a wait
 log(text)  add a runtime log line
```

## Running

The runtime screen focuses on confidence and control:

- current profile;
- screen capture status;
- accessibility status;
- last frame age;
- last OCR duration;
- last decision duration;
- most recent extracted variables;
- last command;
- prominent Stop action.

A later floating overlay should be deliberately tiny: status dot, Pause/Resume, Stop, and “freeze frame for editor”. It should not become a second full UI.

## Error UX

Errors should be actionable:

- **Accessibility off** → button opens the exact Android settings screen.
- **Capture permission missing** → button launches MediaProjection consent.
- **No frame yet** → “Waiting for first frame”, not a generic failure.
- **Regex did not match** → no extracted value is invented; raw region text remains available as `vars.<regionId>.text` for debugging. An inline OCR preview is planned.
- **Script error** → runtime status shows the error message, emits no gesture commands for that decision, and remains stoppable.
- **Action target missing** → show target id and profile name.

## Defaults

The app defaults should optimize for safety while editing:

- automation does not auto-start after app launch;
- gestures are disabled until accessibility is explicitly enabled;
- scripts can only emit TapScript commands;
- raw OCR is not written to disk unless the user exports debug data;
- destructive actions should be easy to pause immediately.
