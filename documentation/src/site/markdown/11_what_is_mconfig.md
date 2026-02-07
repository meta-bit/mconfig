# 1.1 What is mConfig?
mConfig: unified, portable configuration access

## What is it?

mConfig is a (Java) library for configuration access.
It allows to access configuration data without prior knowledge.

* compliant with OS/platform rules
* from many sources (files, network, hardware, registries, environment, …)
* in many formats (.properties, JSON, YAML, …)

It consists of several modules, so it can be kept small and adapted to the specific use case.

## minimal example

```java
    Configuration cfg = ConfigUtil.quickConfig("ACME","ourApp","itsConfig");
    String message = cfg.getString("hello");
    System.out.println(message);
```
See [examples](examples/index.md) for the recommended way to do this,
and sample code for some use cases.

## getting started

Proceed to the [Getting Started](13_getting_started.md) section for step-by-step instructions on integrating mConfig into your project, including dependencies, first code examples, and resource placement.

## Features

* unifying access, abstracting OS specifics
* cross-platform development
* support for CI/CD and automated testing
* not a framework, but a modular library

* File formats supported: JSON, JSON5, YAML, TOML, INI, Java Properties, raw binary and raw text files
* File formats planned: ASN.1 and PEM, DHALL, HOCON.

## Open Source

mConfig is an open source project distributed under the CC-BY-ND 4.0 license.
Commercial use is free of charge. The source code is available on github.

## Status

mConfig as of 0.7 is in pre-release; usable, but work in progress

## Citing mConfig

If you use mConfig in research or technical documentation, you can cite it as:

> metabit JWilkes. mConfig: Unified Configuration Access (2002-present). Available at: https://metabit.org

```bibtex
@misc{mConfig,
    author = { J.Wilkes, metabit },
    title = {mConfig: Unified Configuration Access},
    year = {2026},
    url = {https://metabit.org/}
}
```
