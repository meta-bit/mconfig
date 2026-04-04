package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;


/**
 * <p>ConfigEntryMetadata class.</p>
 *
 * @author jwilkes
 * @version $Id: $Id
 */
/*
we want:
- scope
- location we've read this from.
-- which may, in files, contain the precise line and even column(s);
watch out when writing, though - that'll invalidate these informations.
- Scheme entry for this, if available.
 */
public class ConfigEntryMetadata
{
private ConfigEntrySpecification specification;
private ConfigSource             source;
private String                   comment;

/**
 * <p>Constructor for ConfigEntryMetadata.</p>
 *
 * @param source a {@link org.metabit.platform.support.config.ConfigSource} object
 */
public ConfigEntryMetadata(ConfigSource source)
    {
    this.source = source;
    }
/**
 * <p>getLocation.</p>
 *
 * @return a {@link org.metabit.platform.support.config.ConfigLocation} object
 */
public ConfigLocation getLocation()
    {
    return source;
    }

/**
 * <p>Setter for the field <code>source</code>.</p>
 *
 * @param source a {@link org.metabit.platform.support.config.ConfigSource} object
 */
public void setSource(ConfigSource source)
    {
    this.source = source;
    }

/**
 * <p>Getter for the field <code>source</code>.</p>
 *
 * @return a {@link org.metabit.platform.support.config.ConfigSource} object
 */
public ConfigSource getSource() { return source; }

/**
 * <p>getScope.</p>
 *
 * @return a {@link org.metabit.platform.support.config.ConfigScope} object
 */
public ConfigScope getScope()
    {
    return this.source.getScope();
    }

/**
 * <p>Getter for the field <code>specification</code>.</p>
 *
 * @return a {@link org.metabit.platform.support.config.interfaces.ConfigEntrySpecification} object
 */
public ConfigEntrySpecification getSpecification()
    {
    return specification;
    }

/**
 * <p>Setter for the field <code>specification</code>.</p>
 *
 * @param specification a {@link org.metabit.platform.support.config.interfaces.ConfigEntrySpecification} object
 */
    public void setSpecification(ConfigEntrySpecification specification)
        {
        this.specification = specification;
        }

    /**
     * <p>Getter for the field <code>comment</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getComment()
        {
        return comment;
        }

    /**
     * <p>Setter for the field <code>comment</code>.</p>
     *
     * @param comment a {@link java.lang.String} object
     */
    public void setComment(String comment)
        {
        this.comment = comment;
        }
// type
// flags (e.g. "mandatory")
// default value
// description
}
