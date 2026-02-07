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

To reduce overhead, mConfig limits the checks for new content to every x seconds.
You can set this with the parameter XXXXXXXXXXXXTOBENAMED.

NOT IMPLEMENTED YET:
If you set this parameter to 0, mConfig is instructed to perform checks on all
its potential sources on each variable access. If you really need this amount
of overhead... please give notice, explain the use case, and we'll get it done.
