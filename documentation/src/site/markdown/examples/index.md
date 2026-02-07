# mConfig Examples

## getting started

First, include the following dependency in your project:

Maven:
```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigStandard</artifactId>
    <version>${mconfig.version}</version>
</dependency>
```

Gradle:
```gradle
implementation 'org.metabit.platform.support.config:mConfigStandard:${mconfig.version}'
```

Next, in your code, instantiate a Configuration object/handle.

The shortcut is something like
`Configuration cfg = ConfigUtil.quickConfig("ACME", "ourApp", "itsConfig")`
but the proper, recommended way is to go like this:
```java
    try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "ourApp").build())
        {
        Configuration cfg = factory.getConfiguration("itsConfig"); 
        // ... use cfg ...
        }
```
This way, you can set features for the factory, and get different configurations for different purposes.

Supplying your company name (in this example, "ACME") and application name ("ourApp") is expected minimum.

These two are used across OS to differentiate between different applications, and avoid conflicts.

- You must supply the application name, it is mandatory. 
- If you choose to omit the company name, that may work, but it's not guaranteed - or recommended.
(Company name may also be organization, vendor, or anything similar. We call it "company" to be consistent with OS conventions.)

From the **Configuation**, you then get the values you need.
It's as simple as this:
```java
    String  myString = cfg.getString("theStringKey");
    Integer myInt = cfg.getString("theIntKey");
    byte[]  myBinary = cfg.getBytes("theBinaryKey");
    List<String> myList = cfg.getStringList("theListKey");
```
So, where do I put my configuration files?
Before we get into the topic of Scopes, a quick answer:
that depends on your OS; and
`ConfigUtil.printConfigPaths(final String companyName, final String applicationName)` 
will print you to stdout all the paths where you can find them in your OS.

In a regular maven project, you can put the defaults in `src/main/resources/.config/` and `src/test/resources/.config/` for development and testing respectively.
For above example `ACME.ourApp.itsConfig` would be in `src/main/resources/.config/ACME/ourApp/itsConfig.properties`.

Also, it doesn't have to be files, but we get to that later.

## Scopes and Overrides

Some config entries are for the installed application, some are for the current user.
Some are for the current machine. And some are your defaults. 

You can change that (as a lot of things), but out of the box, mConfig uses a
layered configuration hierarchy: 
**the more specific scopes override the less specific ones**.

So, the local application configuration overrides the defaults;
the user configuration overrides the local application configuration, and so on.

This is not "entire file replacement", it is granular for each entry.

And this is also why you should place your defaults in the resources folder,
instead of hardcoding them in your code.
But if you want to, you can do that too - there's the "cheese" module for that.

The mConfigStandard module is a sensible minimum to get you started.
The mConfigFull module has everything (stable), and in between, your choice.

mConfig is *not a framework*, it is a modular library. You pick and choose what you need.
See the [mConfig Modules](mconfig-modules.md) page for more details.

## Specific Examples

* [Simple Configuration Loading](simple_configuration_loading.md)
* [Testing Configurations](testing_configurations.md)
* [Immutable Configuration Views](configuration_views.md)
* [Configuration Scheme Examples](schemes.md)
* [Advanced Configuration Handling](advanced_configuration_handling.md)
* [Integration with CI/CD](integration_with_cicd.md)
* [Crypto Keys and Certificates](handling_crypto_keys_and_certificates.md)
* [ZooKeeper Configuration Source](zookeeper.md)

