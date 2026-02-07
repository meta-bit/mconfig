# 3.1 Writing Configurations

Most of the time, we just read configs from software. After all, they are how
software gets told where to find things and how to behave.

But if you want to make things more comfortable, you'd also want to write 
configs. 

One main difference from reading is that it is less automatic; it can't be.
When writing a config, you must specify the Scope. Otherwise, we'd have the
writes all over the place, users overwriting each other's settings, and so on.

So, you specify which Scope you are about to write.


## 3.1.1 Writing Priorities
mConfig follows the "principle of least surprise" when writing to a scope:
1. **Priority 1**: If an entry for the key already exists in a writeable layer within the scope, that entry is updated.
2. **Priority 2**: If the key is new, it is added to the highest-priority writeable layer already available in the scope.
3. **Priority 3**: Only if no writeable layers exist is a new configuration file/storage created.

What's half-automatic is the format and location. We can use the hierarchy used
for reading in writing as well, and we have a default format preference list.

But for your specific project, conventions may be different.


## 3.1.2 Format caveats when writing

Some formats allow extraneous information to be added manually, which is
invisible from code.

e.g. Java Properties allow comments, but its .load() function omits all of them.
So if you do a load() followed by a store(), the comments will be gone in the
stored version.

You may want to think about this aspect when designing your application.
mConfig helps you in this regard with layers and Scopes. 

## 3.1.3 Type Preservation (Planned)

**Current**: String-first impl (`String.valueOf(value)`) for simplicity.

**Planned**: Pass typed objects to formats (e.g., unquoted numbers/booleans in JSON/YAML). See `devdocs/type_preservation_plan.md`.

## 3.1.4 Write Cache (Planned)

For batch writes, enable cache via future `ENABLE_CONFIG_CACHE` feature, then `flush()` to persist.

`flush()` will write cache and invalidate read cache.
