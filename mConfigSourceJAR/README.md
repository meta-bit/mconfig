This is a mConfig module for accessing JAR file contents for configuration purposes,
from the "inside" of a JAR especially.

## from inside the JAR
That is, if a program is packaged in a JAR file or similar types (WAR, EAR), 
it can read (not write) configurations which are packaged in the JAR itself.

The intended uses cases are:
1. default configurations
2. pre-set configurations to select from

The default path starts in:
"main/resources/configs/"

if test mode is activated, this is replaced by
"test/resources/configs/"

The config name is added as file name.
company name and application name are not used in this "from inside the JAR" scenario
@PLANNED: An additional directory layer can be added with config options, to allow selection of pre-selected configs.

((@todo check  RAR (Resource Adapter ARchive), SAR (Service ARchive) oder HAR (Hibernate ARchive)))
((@TODO check "configs" or "configurations" ?))

## accessing external JARs
@PLANNED planned feature; may allow writing.
