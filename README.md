[![Latest release](https://img.shields.io/bintray/v/prateem/maven/trace?label=latest&style=flat-square)](#)

# Trace
Android library that can trace views to create silhouettes. Written in Kotlin.

Trace will iterate through the views in a given `View` hierarchy and create
silhouettes based on whether or not the View implements the interface `Traceable`:

* If a View does implement `Traceable`, Trace will create a silhouette
from the result of the `Path` object returned by the `Traceable.trace` call.

* If a View does not implement the `Traceable` interface, Trace will try to use the user-provided
`TraceDelegate`, if one was supplied. If a delegate isn't provided or if `TraceDelegate.handle`
does not return true, then Trace will hand-off to the `DefaultTraceDelegate`. The default
delegate will handle some basic views elegantly but otherwise utilizes rounded rectangles
to create a silhouette based on the boundaries of the view.

If the lambda `shouldExcludeView` is defined and returns true for a given View, it will be
ignored and not drawn. Said lambda can be specified when calling `TraceContainer.startShimmer`
or when identifying the target for tracing via `Trace.of`.

Additionally, `Trace` instances can have their shimmer animations synchronized by the use of a
`ShimmerSynchronizer` - which can be designated either through `TraceContainer.startShimmer` or
`Trace.syncWith`.

#### Important Notes:
* `Trace` uses a `Path` object with fill type `WINDING` (non-zero). Thus, please
    [consider the ramifications](https://blog.stylingandroid.com/vectordrawable-fill-windings/)
    when creating Paths.
* The default implementation of `TraceDelegate` will ignore views whose visibilities are
    set to either `View.INVISIBLE` or `View.GONE`.

## Get it
[![Latest release](https://img.shields.io/bintray/v/prateem/maven/trace?label=latest&style=flat-square)](#)

Available on jCenter.

```
dependencies {
    implementation "com.meetarp:trace:$traceVersion"
}

repositories {
    jcenter()
}
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

            <TraceableView
                nonTraceOutput="none"
                traceableOutput="doubleBubble" />
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

        // Internally, TraceContainer calls Trace with a similar setup to the following...
        // (defaults shown here are for clarification only)
        val trace = Trace(this) // `this` here is context, since Trace inherits from View
            .of(target, delegate = null, shouldExcludeView = null)
            .setColorResource(android.R.color.darker_gray) // Same as default
            .setShimmerColorResource(android.R.color.white) // Same as default
            .also { it.startShimmer(1200) } // Same as default

        // container.addView(trace)
    }

}
```

## Usage

As in the example above, `Trace` is most easily utilized by wrapping a `ViewGroup` with
a `TraceContainer`. Alternatively, it can be created and used manually by instantiating `Trace` and
calling `Trace.of()`. Shimmer must be controlled programmatically.

<table>
  <thead>
    <tr>
      <th colspan="2">
        <h2>TraceContainer</h2>
      </th>
    </tr>
    <tr>
      <td>Type</td>
      <td>Definition</td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td valign="top"><code>variable</code></td>
      <td><b><code>@ColorInt traceSilhouetteColor: Int</code></b>
        <p>The color of the traced silhouette.</p>
        <p>
          Default: <code>android.R.color.darker_gray</code>
          <kbd><img src="https://via.placeholder.com/15/aaaaaa/000000?text=+" alt="#aaaaaa" title="#aaaaaa" /></kbd>
        </p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>variable</code></td>
      <td><b><code>@ColorInt traceShimmerColor: Int</code></b>
        <p>The color of the shimmer that runs across the traced silhouette.</p>
        <p>
          Default: <code>android.R.color.white</code>
          <kbd><img src="https://via.placeholder.com/15/ffffff/000000?text=+" alt="#ffffff" title="#ffffff" /></kbd>
        </p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>variable</code></td>
      <td><b><code>crossFadeEnabled: Boolean</code></b>
        <p>Toggle for enabling or disabling cross-fade animation when starting or stopping shimmer.</p>
        <p>Default: <code>true</code></p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>variable</code></td>
      <td><b><code>crossFadeDuration: Long</code></b>
        <p>If <code>crossFadeEnabled</code> is true, this value controls the duration of the cross-fade
          animation.</p>
        <p>Default: <code>750 ms</code></p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>function</code></td>
      <td>
        <b>
<pre>
startShimmer(
  shimmerSpeed: Long = 1200L,
  delegate: TraceDelegate? = null,
  shouldExcludeView: ((View) -> Boolean)? = null,
  crossFade: Boolean = true,
  synchronizer: ShimmerSynchronizer? = null
): Unit
</pre>
        </b>
        <p>Start the shimmer animation with the given synchronizer (if provided), and with period duration for
          the shimmer in milliseconds.</p>
        <p>Trace will be performed with the given delegate, if provided. Views for which lambda invocations
          return true will be ignored during trace.</p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>function</code></td>
      <td><b>
          <code>
            stopShimmer(crossFade: Boolean = true): Unit
            </code>
        </b>
        <p>Stops any active shimmer animation.</p>
      </td>
    </tr>
  </tbody>
</table>

<table>
  <thead>
    <tr>
      <th colspan="2">
        <h2>Trace</h2>
      </th>
    </tr>
    <tr>
      <td>Type</td>
      <td>Definition</td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td valign="top"><code>function</code></td>
      <td><b>
<pre>
of(
  root: View,
  delegate: TraceDelegate? = null,
  shouldExcludeView: ((View) -> Boolean)? = null
): Trace  
</pre>
        </b>
        <p>Identify the given <code>View</code> as the trace target, utilizing the provided <code>TraceDelegate</code> for silhouettes, if applicable.</p>
        <p>Views for which lambda invocations return true will be ignored during trace.</p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>function</code></td>
      <td><b>
          <code>
            syncWith(sync: ShimmerSynchronizer?): Trace
            </code>
        </b>
        <p>Set the <code>ShimmerSynchronizer</code> to be used to sync <code>Trace</code> instances with.</p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>function</code></td>
      <td>
        <b>
          <code>colored(@ColorInt color: Int): Trace</code><br>
          <code>setColorResource(@ColorRes color: Int): Trace</code>
        </b>
        <p>Set the color for the traced silhouette segments.</p>
        <p>
          Default: <code>android.R.color.darker_gray</code>
          <kbd><img src="https://via.placeholder.com/15/aaaaaa/000000?text=+" alt="#aaaaaa" title="#aaaaaa" /></kbd>
        </p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>function</code></td>
      <td>
        <b>
          <code>shimmerColored(@ColorInt color: Int): Trace</code><br>
          <code>setShimmerColorResource(@ColorRes color: Int): Trace</code>
        </b>
        <p>Set the color for the shimmer that animates when <code>startShimmer</code> is called.</p>
        <p>
          Default: <code>android.R.color.white</code>
          <kbd><img src="https://via.placeholder.com/15/ffffff/000000?text=+" alt="#ffffff" title="#ffffff" /></kbd>
        </p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>function</code></td>
      <td><b>
          <code>
            startShimmer(shimmerSpeed: Long): Unit
            </code>
        </b>
        <p>Start the shimmer animation over the traced silhouette.</p>
      </td>
    </tr>
    <tr>
      <td valign="top"><code>function</code></td>
      <td><b>
          <code>
            stopShimmer(): Unit
            </code>
        </b>
        <p>Stop the shimmer animation over the traced silhouette.</p>
      </td>
    </tr>
  </tbody>
</table>
