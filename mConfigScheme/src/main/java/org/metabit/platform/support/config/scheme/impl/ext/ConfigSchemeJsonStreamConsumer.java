package org.metabit.platform.support.config.scheme.impl.ext;

import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;

import org.metabit.library.format.json.JsonStreamParser;

import java.math.BigDecimal;
import java.math.BigInteger;

/*
  perform reading from JSON into one or more config Schemes.

  If top-level is object, then we have a single ConfigScheme instance;
  if top-level is array, then it's multiple ConfigScheme instances.
  (- and if top-level is something else, then it is wrong.)

--> see JsonConfigSchemeParser, and so on

 */
/**
 * <p>ConfigSchemeJsonStreamConsumer class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class ConfigSchemeJsonStreamConsumer implements JsonStreamParser.JsonStreamConsumer
{

    /** {@inheritDoc} */
    @Override
    public void consumeNull(int line, int column, int level)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeFalse(int line, int column, int level)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeTrue(int line, int column, int level)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeString(int line, int column, int level, String string)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeNumberInteger(int line, int column, int level, int i)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeNumberLong(int line, int column, int level, long l)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeNumberDouble(int line, int column, int level, double v)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeObjectEntryStart(int line, int column, int level, String key)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeObjectEntryEnd(int line, int column, int level)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeArrayStart(int line, int column, int level)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeArrayEnd(int line, int column, int level)
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeObjectEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {

        }

    /** {@inheritDoc} */
    @Override
    public void consumeObjectStart(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {

        }
}
