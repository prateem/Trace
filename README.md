[![Latest release](https://img.shields.io/bintray/v/prateem/maven/trace?label=latest&style=flat-square)](#)

# Trace
Android library that can trace views to create silhouettes. Written in Kotlin.

Trace will iterate through the views in a given `View` hierarchy and create
silhouettes based on whether or not the View implements the interface `Traceable`:

If a View does implement `Traceable`, Trace will create a silhouette
from the result of `Path` object returned by the `Traceable.trace` call.

If a View does not implement the `Traceable` interface, Trace will try to use the user-provided
`TraceDelegate`, if one was supplied. If a delegate isn't provided or if `TraceDelegate.handle`
does not return true, then Trace will hand-off to the `DefaultTraceDelegate`. The default
delegate will handle some basic views elegantly but otherwise utilizes rounded rectangles
to create a silhouette based on the boundaries of the view.

#### Important Notes:
* `Trace` uses a `Path` object with fill type `WINDING` (non-zero). Thus, please
    [consider the ramifications](https://blog.stylingandroid.com/vectordrawable-fill-windings/)
    when creating Paths.
* Views whose IDS are found in the `exclusions` list are ignored.
    The exclusions list can be specified when calling `TraceContainer.startShimmer`
    or when identifying the target for `Trace` via `Trace.of`.
* The default implementation of `TraceDelegate` will ignore views whose visibilities are
    set to either `View.INVISIBLE` or `View.GONE`.

## Get it
[![Latest release](https://img.shields.io/bintray/v/prateem/maven/trace?label=latest&style=flat-square)](#)

Available on jCenter.

```
implementation 'com.meetarp:trace:$traceVersion'
```

## Example
An example app is available that will build the following to a device:

### Visual
<img src="https://raw.githubusercontent.com/prateem/Trace/master/trace.gif" width="360" height="740">

### XML Layout
Pseudo-code is used for clarity of mapping to the visual(s) above.
See [activity_main.xml](app/src/main/res/layout/activity_main.xml) for full/proper xml.

```xml
<Screen>
    <Toggle />

    <TraceContainer id="traceContainer">
        <LinearLayout>
            <TextView lines="1" gravity="center" />
            <TextView lines="2" />
            <Square size="200dp" color="accent" />
            <Button />
            <RadioButton enabled="false" />
            <TraceableView actualSize="0" traceableOutput="doubleBubble" />
        </LinearLayout>
    </TraceContainer>
</Screen>
```

### Activity
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toggle = findViewById<Checkbox>(R.id.toggle)
        val traceContainer = findViewById<TraceContainer>(R.id.traceContainer)
        
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                traceContainer.startShimmer()
            else
                traceContainer.stopShimmer()
        }

        // Internally, TraceContainer calls Trace with a similar setup to the following:
        val trace = Trace(this) // `this` here is context, since Trace inherits from View
            .of(target, delegate = null, exclusions = listOf())
            .setColorResource(android.R.color.darker_gray) // Same as default
            .setShimmerColorResource(android.R.color.white) // Same as default
            .also { it.startShimmer() }
        // container.addView(trace)
    }

}
```

## Usage

As in the example above, `Trace` is most easily utilized by wrapping a `ViewGroup` with
a `TraceContainer`. Alternatively, it can be created and used manually by instantiating `Trace` and
calling `Trace.of()`. Shimmer must be controlled programmatically.

#### `TraceContainer` exposes the following:

| Method or attribute | Description | Defaults |
|-|-|-|
| `startShimmer(Long, TraceDelegate, List<Int>)` | Start the shimmer animation with the given period duration for the shimmer in milliseconds.<br>The list of integers provided are the view IDs (if any) to ignore during trace. | Long = 1000 ms<br>TraceDelegate = null<br>List<Int> = empty |
| `stopShimmer()` | End any shimmer animation that may be active. | N/A |
| `app:trace_silhouetteColor` (XML) | Set the color used by Trace to draw the silhouettes. | `android.R.color.darker_gray` |
| `app:trace_shimmerColor` (XML) | Set the color used by Trace for the shimmer. | `android.R.color.white` |

#### `Trace` exposes the following:

| Method | Description | Defaults |
|-|-|-|
| `of(View, TraceDelegate, List&lt;Int&gt;)` | The View is the trace target. The list of integers provided are the view IDs (if any) to ignore during trace. | N/A<br>TraceDelegate = null<br>List&lt;Int&gt; = empty |
| `colored(ColorInt)` | Set the color of the traced silhouette(s). Argument must be a color integer. | Darker Gray |
| `setColorResource(ColorRes)` | Set the color of the traced silhouette(s). Argument must be a color resource integer. | `android.R.color.darker_gray` |
| `shimmerColored(ColorInt)` | Set the color of the shimmer. Argument must be a color integer. | White |
| `setShimmerColorResource(ColorRes)` | Set the color of the shimmer. Argument must be a color resource integer. | `android.R.color.white` |
| `startShimmer(Long)` | Start the shimmer animation. The argument provided is the period duration of the shimmer in milliseconds. | 1000 ms |
| `stopShimmer()` | End any shimmer animation that may be active. | N/A |
