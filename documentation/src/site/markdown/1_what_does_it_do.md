# 1. What does mConfig do?

mConfig abstracts configuration access.

mConfig:

* abstracts OS platform differences in configuration locations
* makes cross-platform configuration access easier
* abstracts config file formats
* automatically detects changes to configuration sources
* provides a fallback hierarchy for defaults
* supports hierarchical scopes, e.g. user settings override local installation settings
* provides a TEST mode for automated tests
* supports development vs. production separation
* supports optional parameter type checking and parsing
* supports runtime documentation
* supports build-time documentation generation

You set things up, then your code can just getInteger(), getString(),
getBoolean(), getBigDecimal() and so on
from the Configuration object, whenever it needs a config value.

* handles XDG Desktop standards overrides
* supports late-bound configurations (useful e.g. in cloud-native environments)
* supports multiple configuration sources
