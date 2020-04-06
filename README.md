[![Latest release](https://img.shields.io/bintray/v/prateem/maven/trace?label=latest&style=flat-square)](#)

# Trace
Android library that can trace views to create silhouettes. Written in Kotlin.

Trace will iterate through the views in a given `ViewGroup` hierarchy and create
silhouettes based on whether or not the View implements the interface `Traceable`:

* If the View does implement `Traceable`, Trace will create a silhouette
from the result of `Path` object returned by the `Traceable.trace()` call.
* If the View does not implement the interface, Trace will use rounded
rectangles to create a silhouette based on the boundaries of the view.

**Important**: Views whose visibilities are set to either `View.INVISIBLE`
or `View.GONE` will be ignored, as well as any views whose IDs are included
in the `exclusions` list specified when identifying the target for `Trace`
via `Trace.of`.

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
    <ViewGroup id="traceTarget">
        <TextView lines="1" />
        <TextView lines="1" />
        <TextView lines="2" />
        <Square size="40dp" color="primary" />
        <TraceableView actualSize="0" traceableOutput="doubleBubble" />
    </ViewGroup>
    
    <ViewGroup id="traceContainer" />
    
    <Square size="200dp" color="accent" />
</Screen>
```

### Activity
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val target = findViewById<ViewGroup>(R.id.traceTarget)
        val container = findViewById<ViewGroup>(R.id.traceContainer)

        val trace = Trace(this) // `this` here is context, since Trace inherits from View
            .of(target, exclusions = listOf())
            .colored(android.R.color.darker_gray) // Same as default
            .shimmerColored(android.R.color.white) // Same as default
            .also { it.startShimmer() }

        container.addView(trace)
    }

}
```

## Usage

As in the example above, `Trace` is simply a `View` that is kicked off
when `Trace.of()` is called. Due to this limitation, it can only be
utilized programmatically.

`Trace` exposes the following methods:

|Method|Description|Defaults|
|-------------|-----------|-------|
|`of(ViewGroup, List<Int>)`|The ViewGroup is the trace target. The list of integers provided are the view IDs (if any) to ignore during trace.|N/A|
|`colored(ColorRes)`|Set the color of the traced silhouette(s). Argument must be a color resource integer.|`android.R.color.darker_gray`|
|`shimmerColored(ColorRes)`|Set the color of the shimmer. Argument must be a color resource integer.|`android.R.color.white`|
|`startShimmer(Long)`|Start the shimmer animation. The argument provided is the period duration of the shimmer in milliseconds.|1000 ms|
|`stopShimmer()`|End any shimmer animation that may be active.|N/A|
