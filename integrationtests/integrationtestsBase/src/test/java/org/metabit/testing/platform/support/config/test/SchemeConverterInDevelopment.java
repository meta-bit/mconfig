package org.metabit.testing.platform.support.config.test;

import org.metabit.library.format.json.JsonStreamParser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

// merge with/copy from JsonConfigSchemeParser
public class SchemeConverterInDevelopment implements JsonStreamParser.JsonStreamConsumer
{


    enum State
    {STARTING, ARRAY_OF_SCHEMES, SCHEME,EXPECT_SCHEME_ENTRY_ARRAY, ARRAY_OF_ENTRIES, ENTRY, END, ERROR_END }

    enum ValidKeys {}; // string-enumclass for later, to be type of currentKey for increased type safety.

    private ArrayList<SchemePlaceholder> collector     = new ArrayList<SchemePlaceholder>();
    private SchemePlaceholder            currentScheme = null;
    private SchemeEntryPlaceholder       currentEntry  = null;
    private String                       currentKey    = null;
    private Object                       currentValue  = null;
    private State                        state         = State.STARTING;

    @Override
    public void consumeArrayStart(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        switch (state)
            {
            case STARTING:
                collector.clear();
                state = State.ARRAY_OF_SCHEMES;
                System.out.println("[ starting array of SCHEMES");
                break;
            case EXPECT_SCHEME_ENTRY_ARRAY:
            case SCHEME: // scheme is missing its name at this point.
                System.out.println("[ starting array of ENTRIES");
                assert(currentScheme != null); // because that's where we put those entries.
                state = State.ARRAY_OF_ENTRIES;
                break;

            default:
                System.err.println("consume arrayStart at line "+line+" column "+column+" level "+level + " in state " + state);
                state = State.ERROR_END;
                throw new JsonStreamParser.JsonParsingException(line, column, "unexpected [");
            }
        }

    @Override
    public void consumeArrayEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        switch (state)
            {
            case ENTRY: // scheme entries array ended; we didn't notice the individual scheme end
                if (currentEntry != null) // pending entry?
                    {
                    currentScheme.addEntry(currentEntry);
                    currentEntry = null;
                    }
                System.out.println(" array of entries ended");
                state = State.SCHEME; // ???
                break;
            case ARRAY_OF_SCHEMES:
                System.out.println(" array of schemes ended");
                state = State.END;
                //@TOD forward/write collector out
                System.out.println(collector);
                break;

            case SCHEME:
            default:
                System.err.println("consume arrayEnd at line "+line+" column "+column+" level "+level + " in state " + state);
                throw new JsonStreamParser.JsonParsingException(line, column, "state mismatch: arrayEnd in invalid state");
            }
        }

    @Override
    public void consumeObjectStart(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        switch (state)
            {
            case STARTING: // that's format 2, then
            case ARRAY_OF_SCHEMES: // format 3, with the array outside.
                if (currentScheme != null)
                    throw new JsonStreamParser.JsonParsingException(line, column, "state mismatch: new scheme started with existing scheme in place");
                currentScheme = new SchemePlaceholder();
                state = State.SCHEME;
                System.out.println("{ starting SCHEME");
                break;
            case ARRAY_OF_ENTRIES: // entry begun in array of entries.
                assert(currentScheme!=null);
                assert(currentEntry==null);
                currentEntry = new SchemeEntryPlaceholder();
                state = State.ENTRY;
                System.out.println("{ starting scheme entries object");
                break;
            default:
                System.err.println("consume objectStart { at line "+line+" column "+column+" level "+level + " in state " + state);
                throw new JsonStreamParser.JsonParsingException(line,column,"unexpected object start");
            }
        }

    @Override
    public void consumeObjectEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        switch (state)
            {
            case ENTRY:
                currentScheme.addEntry(currentEntry);
                currentEntry = null;
                System.out.println("} ending ENTRY");
                break;
            case SCHEME:
                System.out.println("} ending SCHEME");
                // validate the scheme had a name
                if (currentScheme.getName() == null)
                    throw new JsonStreamParser.JsonParsingException(line,column, "nameless scheme");
                collector.add(currentScheme);
                currentScheme = null;
                state = State.ARRAY_OF_SCHEMES;
                break;
            default:
                System.err.println("consume objectEnd } at line "+line+" column "+column+" level "+level + " in state " + state);
                throw new JsonStreamParser.JsonParsingException(line,column,"unexpected object end");
            }
        }



    @Override
    public void consumeObjectEntryStart(int line, int column, int level, String key)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume objectEntryStart with key \"" + key + "\" in state " + state);
        assert(currentKey == null);
        assert(currentValue == null);
        switch (state)
            {
            case SCHEME:
                switch (key.toLowerCase())
                    {
                    case "name":
                        currentKey = key.toLowerCase();
                        break;
                    case "entries":
                        state = State.EXPECT_SCHEME_ENTRY_ARRAY;
                        return; // do not set currentKey
                    default:
                        throw new JsonStreamParser.JsonParsingException(line,column, "invalid key in scheme: " + sanitize(key));
                    }
                break;
            case ENTRY:
                switch (key.toLowerCase())
                    {
                    case "key":
                    case "default":
                    case "type":
                        // ...
                        currentKey = key.toLowerCase();
                        break;
                    default:
                        throw new JsonStreamParser.JsonParsingException(line,column, "invalid key in scheme entry: " + sanitize(key));
                    }
                break;
            default:
                throw new JsonStreamParser.JsonParsingException(line,column, "invalid state in scheme: " + sanitize(key));
            }
        return;
        }

    @Override
    public void consumeObjectEntryEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume objectEntryEnd in state " + state + " with currentKey " + currentKey + " and currentValue " + currentValue );
        switch (state)
            {
            case SCHEME:
                if (currentKey != null)
                    {
                    switch (currentKey)
                        {
                        case "name":
                            currentScheme.setName((String) currentValue);
                            System.err.println("scheme name set to " + currentValue);
                            break;
                        case "entries":
                            //
                        } //@TODO different handling
                    }
                else
                    {
                    System.err.println("currentKey is null");
                    }
                break;
            case ENTRY:
                switch (currentKey) // which is already lower case --
                    {
                    case "key":
                        currentEntry.setKey((String) currentValue); break;
                    case "default":
                        currentEntry.setDefault((String) currentValue); break;
                    case "type":
                        currentEntry.setType((String) currentValue); break;
                    default:
                        throw new JsonStreamParser.JsonParsingException(line,column, "invalid state " + state + " for objectEntryEnd");
                    }
                break;
            default:
                throw new JsonStreamParser.JsonParsingException(line,column, "invalid state " + state + " for objectEntryEnd");
            }
        currentKey = null; // @OPTIMISE
        currentValue = null; //@OPTIMISE
        return;
        }


    @Override
    public void consumeString(int line, int column, int level, String string)
            throws JsonStreamParser.JsonParsingException
        {
        assert(currentKey!=null);
        currentValue = string;
        /*
        switch (state)
            {
            case SCHEME:
                if (currentKey == null)
                    throw new JsonStreamParser.JsonParsingException(line, column, "state mismatch: new scheme started with existing scheme in place");
                switch (currentKey.toLowerCase())
                    {
                    case "name":
                        currentScheme.setName(string);
                        currentKey = null;
                        System.out.println("scheme name set to " + string);
                        break;
                    case "entries":
                    default:
                        throw new JsonStreamParser.JsonParsingException(line, column, "invalid entry named \"" + sanitize(currentKey) + "\" in scheme");
                    }
                break;
            case ENTRY:
                assert(currentEntry != null);
                assert(currentKey != null);
                switch (currentKey.toLowerCase())
                    {
                    case "key":
                        //@TODO sanitation
                        currentEntry.setKey(string);
                        currentKey = null;
                        break;
                    case "type":
                        //@TODO pre-parse type
                        currentEntry.setType(string);
                        currentKey = null;
                        break;
                    case "default":
                        //@TODO pre-parse default content
                        currentEntry.setDefault(string);
                        currentKey = null;
                        break;
                        
                    default:
                        throw new JsonStreamParser.JsonParsingException(line, column, "invalid entry key named \"" + sanitize(currentKey) + "\" in scheme");
                    }
                // state =
                break;
            default:
                System.err.println("consume string at ["+line+":"+column+", ^ "+level+"] with content string \""+string+"\"");
                state = State.ERROR_END;
                throw new JsonStreamParser.JsonParsingException(line, column, "unexpected string contents");
            }
         */
        return;
        }

    private static String sanitize(String currentKey)
        {
        // security and safety: replace/remove all non-ascii-characters
        return currentKey.replaceAll("[^ -~]", ""); // magic regexp... it means "[^\\x20-\\x7E]", really.
        }

    @Override
    public void consumeNull(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume null at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeFalse(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume false at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeTrue(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume true at line "+line+" column "+column+" level "+level);
        }



    @Override
    public void consumeNumberInteger(int line, int column, int level, int i)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberInteger at line "+line+" column "+column+" level "+level+" i "+i);
        }

    @Override
    public void consumeNumberLong(int line, int column, int level, long l)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberLong at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeNumberDouble(int line, int column, int level, double v)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberDouble at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberBigInteger at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberBigDecimal at line "+line+" column "+column+" level "+level);
        }


    private class SchemePlaceholder {
        private String name;
        private ArrayList<SchemeEntryPlaceholder> entries = new ArrayList<>();

        public void setName(String name)
            {
            this.name = name;
            }

        public String getName()
            {
            return name;
            }

        public void addEntry(SchemeEntryPlaceholder currentEntry)
            {
            System.out.println("adding entry " + currentEntry);
            this.entries.add(currentEntry);
            }
    }

    private class SchemeEntryPlaceholder {
        private String key;
        private String type;
        private String aDefault;

        public void setKey(String key)
            {
            //@TODO sanitation
            this.key = key;
            }

        public String getKey()
            {
            return key;
            }

        public void setType(String type)
            {
            this.type = type;
            }

        public String getType()
            {
            return type;
            }

        public void setDefault(String aDefault)
            {
            this.aDefault = aDefault;
            }

        public String getDefault()
            {
            return aDefault;
            }
    }
}
