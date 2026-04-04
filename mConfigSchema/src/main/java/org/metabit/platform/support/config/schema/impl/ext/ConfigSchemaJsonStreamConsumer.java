package org.metabit.platform.support.config.schema.impl.ext;

/*
  perform reading from JSON into one or more config Schemes.

  If top-level is object, then we have a single ConfigSchema instance;
  if top-level is array, then it's multiple ConfigSchema instances.
  (- and if top-level is something else, then it is wrong.)

--> see JsonConfigSchemaParser, and so on

 */
/**
 * <p>ConfigSchemaJsonStreamConsumer class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class ConfigSchemaJsonStreamConsumer extends org.metabit.library.format.json.DummyJsonStreamConsumer
{
}
