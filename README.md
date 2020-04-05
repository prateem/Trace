        [![Latest release](https://img.shields.io/bintray/v/prateem/maven/trace?label=latest&style=flat-square)](#)

# Trace
Android library that can trace views to create silhouettes. Written in Kotlin.

Trace will iterate through the views in a given `ViewGroup` hierarchy and create
silhouettes based on whether or not the View implements the interface `Traceable`:

* If the View does implement `Traceable`, Trace will create a silhouette
from the result of `Path` object returned by the `Traceable.trace()` call.
* If the View does not implement the interface, Trace will use rounded
rectangles to create a silhouette based on the boundaries of the view.

An example app is available that will build the following to a device:


<table>
<thead><tr><th>XML</th><th>Visual</th></tr></thead>
<tbody>
<tr>
<td>
<pre lang="xml">
<ViewGroup id="traceTarget">
  <TextView lines="1" />
  <TextView lines="1" />
  <TextView lines="2" />
  <Square size="40dp" color="primary" />
  <TraceableView actualSize="0" traceableOutput="doubleBubble" />
</ViewGroup>
<ViewGroup id="traceContainer" />
<Square size="200dp" color="accent" />
</pre>
</td>
<td><img src="https://raw.githubusercontent.com/prateem/Trace/master/trace.gif"></td>
</tr>
</tbody>
</table>


## Get it
[![Latest release](https://img.shields.io/bintray/v/prateem/maven/trace?label=latest&style=flat-square)](#)

Available on jCenter.

```
implementation 'com.meetarp:trace:$traceVersion'
```
