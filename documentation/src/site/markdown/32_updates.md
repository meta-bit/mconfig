# 3.2 Updates

mConfig automatically detects and uses updates in the configuration sources.

Thus, if you access Configuration contents, you'll receive the current values.

Conversely, if you store the values you got from the mConfig Configuration in a
local variable, that variable won't receive updates, of course.

Now for the details.

## 3.2.1 Update notifications

While the easiest way to use mConfig is just to `get()` from the Configuration
whenever you need a value - there are library objects created using the values
values, where you can't pass an mConfig handle. You would need to reinstantiate
these, or do similar things.

For this purpose, you can subscribe to changes, providing a callback which will
get executed when a change occurs.

### 3.2.1.1 Location-level updates
```java
Configuration.subscribeToUpdates(Consumer<ConfigLocation> listener)
Configuration.unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
```

The callback will be executed when any of the Configuration layers in the specified scopes gets
changed content.

**Note on initial discovery:** For subscribers, the transition from a non-existent entry
to its first assigned value is treated as a change event. 
This ensures that dynamic entry creation is captured correctly.

By default, the scopes exclude RUNTIME, because that's what you set yourself at runtime.

The callback is provided as parameter with the `ConfigLocation` where the change occurred.
(You can get the scope from it, e.g.)

### 3.2.1.2 Entry-level updates
```java
Configuration.subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
```
Subscribe to changes of individual entries by their full key. 

The callback is called with the `ConfigLocation` where the entry change was detected.

Unsubscribe with `Configuration.unsubscribeFromUpdates(Consumer<ConfigLocation> listener)`
when you don't need it anymore; this feature requires a bit of CPU time when active.

## 3.2.2 Update settings

To reduce overhead, mConfig limits periodic checks for new content to every *x* milliseconds.
You can configure this with the feature [`ConfigFeature.UPDATE_CHECK_FREQUENCY_MS`](25_config_features.md) (defaults to 2000 ms).

Setting `UPDATE_CHECK_FREQUENCY_MS` to `0` disables the background check thread. Checks then run
synchronously on every value access instead, which costs more per read; use it only when you need
changes to be visible immediately.

## 3.2.3 Known issues (0.8.17)

Change detection has known defects, to be fixed in 0.8.18 (MLIB-129). They are not new; all earlier
releases have them.

- **Several instances watching one source.** A source reports a change once, to the first
  `Configuration` that checks it. Where more than one `Configuration` instance watches the same
  source, for example the same configuration opened with different schemas in one factory,
  the others may not reload it and their subscribers may not be notified.
  Avoid this by keeping one instance per configuration where you rely on updates.
- **Kubernetes (Fabric8 storage): watch lost after an error.** If the Kubernetes API closes a watch
  with an error and the client gives up reconnecting, changes to that ConfigMap or Secret are not
  detected until a configuration that uses it is opened again. No event is recorded.
- **Files created after subscribing.** Subscribers are attached to the layers present when they
  subscribe; a configuration file created later does not notify them.
