/*
 * This source file has been placed under Apache 2.0 licence.
 * original source code by metabit, 2024.
 * formatting: modern whitesmiths
 */
package org.metabit.library.format.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * simple stream-forward event parser for JSON.
 * tracks input lines and columns, provides them to the consumer and in errors.
 * <br/>
 * written from scratch on 2023-03-05 by jwilkes, metabit
 * <br/>
 * Goal: self-contained. correct. as small as both goals allow.
 * <p>
 * There are excellent, full-fledged JSON parsing frameworks. Consider using
 * those instead if above goals are not what you need.
 * <p/>
 * <p>
 * more "clever" approaches like front-and-end etc. were dropped for simplicity.
 * Regex-based approach not possible with Java; its RegExp lacks subgroups.
 * </p>
 * sources used:
 *
 * @author jwilkes
 * @version $Id: $Id
 * @see <a href="https://www.rfc-editor.org/rfc/rfc8259">RFC 8259</a>
 * @see <a href="https://www.json.org/json-en.html">JSON specification</a>
 */
public class JsonStreamParser
{
    /** exception class derived from IllegalArgumentException, with position data. */
    public static class
    JsonParsingException extends IllegalArgumentException
    {
        private final int line;
        private final int column;

        // specific message
        public JsonParsingException(int line, int column, String msg)
            {
            super((msg == null ? "invalid input at " : msg)+line+":"+column);
            this.line = line;
            this.column = column;
            }

        // unexpected character, common case.
        public JsonParsingException(int line, int column, char c)
            {
            // sanitized: do not name the character if it is not within safe printable range.
            super("unexpected character "+(Character.isAlphabetic(c) ? "'"+c+"'" : "")+" at "+line+":"+column);
            this.line = line;
            this.column = column;
            }

        // wrap exception
        public JsonParsingException(int line, int column, Exception e)
            {
            super("Exception at "+line+":"+column, e);
            this.line = line;
            this.column = column;
            }

        public int getLine() { return line; }

        public int getColumn() { return column; }
    }

    /**
     * interface for consuming the tokens found in a JSON stream.
     * The numbers have been split by type and range; if you don't support BigNum,
     * then you can throw an out-of-range exception yourself etc.
     * use or ignore the position as suits your use case best.
     * <br/>
     * for checked exceptions, use or extend JsonParsingException;
     * for RuntimExceptions, IllegalArgumentException might be suitable.
     */
    public static interface JsonStreamConsumer
    {
        void consumeNull(int line, int column, int level)                                           throws JsonParsingException;
        void consumeFalse(int line, int column, int level)                                          throws JsonParsingException;
        void consumeTrue(int line, int column, int level)                                           throws JsonParsingException;
        void consumeString(int line, int column, int level, final String string)                    throws JsonParsingException;
        void consumeNumberInteger(int line, int column, int level, int i)                           throws JsonParsingException;
        void consumeNumberLong(int line, int column, int level, long l)                             throws JsonParsingException;
        void consumeNumberDouble(int line, int column, int level, double v)                         throws JsonParsingException;
        void consumeNumberBigInteger(int line, int column, int level, final BigInteger bigInteger)  throws JsonParsingException;
        void consumeNumberBigDecimal(int line, int column, int level, final BigDecimal bigDecimal)  throws JsonParsingException;
        void consumeObjectEntryStart(int line, int column, int level, final String key)             throws JsonParsingException;
        void consumeObjectEntryEnd(int line, int column, int level)                                 throws JsonParsingException;
        void consumeObjectStart(int line, int column, int level)                                    throws JsonParsingException;
        void consumeObjectEnd(int line, int column, int level)                                      throws JsonParsingException;
        void consumeArrayStart(int line, int column, int level)                                     throws JsonParsingException;
        void consumeArrayEnd(int line, int column, int level)                                       throws JsonParsingException;
    }

    /**
     * constructor. inits defaults. instantiated JsonStreamParser is reusable.
     * sets the ignoreCR flag depending on local OS; please set manually for content you receive from other sources.
     */
    public JsonStreamParser()
        {
        sb = new StringBuilder();
        sb2 = new StringBuilder();
        // default settings
        ignoreCR = !("\r".equals(System.lineSeparator())); // MacOS uses no '\n' - we adapt this to the local system by default.
        tabStep = 4; // count a tab as...
        unicodeWhitespace = true;
        oldStyle = false;
        }

    /**
     * how many spaces a tab is considered to be wide.
     * default: 4
     *
     * @param columnStep spaces per tab, for position counting.
     */
    public void setTabWidth(int columnStep)
        {
        if (columnStep <= 0) throw new IllegalArgumentException();
        this.tabStep = columnStep;
        }

    /**
     * when encountering CR, should we just ignore it for line counts, (Windows), or is it a valid end-of-line (MacOS)?
     * default: true.
     *
     * @param ignoreCR true to simply ignore CRs, false to count them as separate line ends.
     */
    public void setIgnoreCR(boolean ignoreCR) { this.ignoreCR = ignoreCR; }

    /**
     * RFC 8259 allows any value to be the top-level element in a JSON structure.
     * <br/> The json.org documentation allows only objects to be at top-level.
     *
     * @param oldStyle true if you want to enforce the top-level item to be an object.
     */
    public void setOldJsonStyle(boolean oldStyle) { this.oldStyle = oldStyle; }

    /**
     * select whether to use the Unicode whitespace definition, or the Java whitespace definition.
     * Java omits some obscure whitespaces from its definition in java.lang.Character.isWhitespace().
     *
     * @param useUnicodeWhitespace use unicode whitespace (true, default); or use java whitespace (false)
     */
    public void setUnicodeWhitespace(boolean useUnicodeWhitespace) { this.unicodeWhitespace = useUnicodeWhitespace; }

    /**
     * parsing convenience function, wrapping String inputs.
     *
     * @param inputString string as input
     * @param consumer    consumer for the JSON tokens found.
     * @throws org.metabit.library.format.json.JsonStreamParser.JsonParsingException on format errors
     */
    public void parse(String inputString, JsonStreamConsumer consumer)
            throws JsonParsingException
        {
        this.parse(new StringReader(inputString), consumer);
        }


    /**
     * main parsing function.
     *
     * @param input    character input stream
     * @param consumer consumer for the JSON tokens found.
     * @throws org.metabit.library.format.json.JsonStreamParser.JsonParsingException on format or IO errors
     */
    public void parse(Reader input, JsonStreamConsumer consumer)
            throws JsonParsingException
        {
        if ((input == null) || (consumer == null)) throw new IllegalArgumentException("null parameter"); // guard clause
        this.input = input;
        this.output = consumer;
        this.line = 0;
        this.column = 0;
        this.level = 0;
        this.putback = false;
        if (oldStyle)
            {
            mustBeNext('{', "in strict mode, top-level must be an object");
            parseObjectStarted();
            }
        else
            { parseValue(); } // as RFC 8259 specifies.  <=== much preferred.
        if (level != 0)
            throw new JsonParsingException(line, column, "input ended before all collections were closed");
        return;
        }

    // ----- implementation follows --------------------------------------------

    private void parseValue()
        {
        // get next non-whitespace and start the respective type sequence by it
        char c = getNextCharSkippingWhitespace(false); // might end
        parseValueStarted(c);
        }

    private void parseValueStarted(char c)
        {
        switch (c)
            {
            // string
            case '"':
                parseStringStarted();
                break;
            // number
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
                parseNumberStarted(c);
                break;
            // object
            case '{':
                parseObjectStarted();
                break;
            // array
            case '[':
                parseArrayStarted();
                break;
            // true
            case 't':
                parseTrueStarted();
                break;
            // false
            case 'f':
                parseFalseStarted();
                break;
            // null
            case 'n':
                parseNullStarted();
                break;
            default:
                throw new JsonParsingException(line, column, c);
            }
        return;
        }

    private void parseNullStarted()
        {
        mustBeNext('u', null);
        mustBeNext('l', null);
        mustBeNext('l', null);
        output.consumeNull(line, column, level);
        return;
        }

    private void parseFalseStarted()
        {
        mustBeNext('a', null);
        mustBeNext('l', null);
        mustBeNext('s', null);
        mustBeNext('e', null);
        output.consumeFalse(line, column, level);
        return;
        }

    private void parseTrueStarted()
        {
        mustBeNext('r', null);
        mustBeNext('u', null);
        mustBeNext('e', null);
        output.consumeTrue(line, column, level);
        return;
        }

    private void parseArrayStarted()
        {
        level++;
        output.consumeArrayStart(line, column, level);
        boolean first = true;
        boolean loop = true;
        do
            {
            char c = getNextCharSkippingWhitespace(true);
            if (c == ']')
                {
                loop = false;
                continue;
                } // end loop immediately.

            if (!first) // separating ,
                {
                if (c != ',')
                    throw new JsonParsingException(line, column, "array elements must be separated by ,");
                c = getNextCharSkippingWhitespace(true);
                }
            else
                { first = false; }

            parseValueStarted(c);
            }
        while (loop);
        output.consumeArrayEnd(line, column, level);
        level--;
        return;
        }

    private void parseObjectStarted()
        {
        level++;
        output.consumeObjectStart(line, column, level);
        boolean first = true;
        boolean loop = true;
        do
            {
            // expect separators for all but the first entry
            char c = getNextCharSkippingWhitespace(false);
            switch (c)
                {
                case '}':
                    output.consumeObjectEnd(line, column, level);
                case '\0':
                    loop = false;
                    continue;

                case ',': // expected if we're not at first entry
                    if (!first)
                        { continue; } // expected, let's continue with next entry.
                    else
                        { throw new JsonParsingException(line, column, "object started with empty pair"); } // non-strict mode to ignore this
                    // break; unreachable break

                case '"': // string, as expected
                    // next must be a string
                    String key = readStringStarted();
                    output.consumeObjectEntryStart(line, column, level, key); // the next thing will be the corresponding value.
                    mustBeNext(':', "object key and value must be separated by :");
                    // type = KEY
                    parseValue();
                    output.consumeObjectEntryEnd(line, column, level);
                    first = false;
                    break;

                default: // anything else
                    throw new JsonParsingException(line, column, "object keys must be strings");
                }
            }
        while (loop);
        level--;
        return;
        }

    // @SuppressFBWarnings(value="SF_SWITCH_FALLTHROUGH", justification="the same case for two values")
    private void parseNumberStarted(char c)
        {
        boolean isNegative = false;
        boolean hasDecimals = false;
        boolean hasExponent = false;
        boolean loop = true;
        // the "reset" for StringBuilders. avoids reallocation.
        sb.setLength(0);
        sb2.setLength(0);
        // leading zero special handling
        if (c == '0')
            {
            output.consumeNumberInteger(line, column, level, 0); // or number-string?
            return; // then we're done here.
            // any digit or -eE+ after the 0 would be invalid.
            // the next char may be a valid , } ], or a whitespace; we just continue.
            }
        // keep and use first character
        sb.append(c);
        // loop
        do
            {
            switch (c)
                {
                case '-':
                    if (isNegative)
                        throw new JsonParsingException(line, column, "second - in a number");
                    if (sb.length() > 1)
                        throw new JsonParsingException(line, column, "- only at start of number");
                    isNegative = true;
                    break;
                // digits
                case '0':
                    // intentional fallthrough -- we could try the "leading zero" handling here.
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (hasExponent)
                        sb2.append(c);
                    else
                        sb.append(c);
                    break;
                case '.':
                    if (hasDecimals)
                        throw new JsonParsingException(line, column, "numbers must contain at most one .");
                    hasDecimals = true;
                    sb.append(c);
                    break;
                case 'E':
                case 'e':
                    if (hasExponent)
                        throw new JsonParsingException(line, column, "numbers must contain at most one exponent");
                    hasExponent = true;
                    // exponent signs only right after the E
                    c = readNext(true);
                    switch (c)
                        {
                        case '-':
                        case '+':
                            sb2.append(c);
                            break; // special handling
                        default:
                            continue; // go right back to loop start, evaluate the c
                            // we could have no digits after the exponent introducer. detect in preparsing.
                        }
                    break;

                case ']': // array ends @TODO check how outside can process this -- putback?
                    // not even a fallthrough; this is just two combined cases!

                case '}': // object ends
                    output.consumeObjectEnd(line, column, level);
                    putback = true; // workaround
                    // intentional fallthrough
                    // case -1: not using int here.
                case 0: //end of input. @TODO check whether we keep the NUL in-band signal
                    loop = false;
                    break;
                default:
                    if (isWhitespace(c, unicodeWhitespace))
                        { loop = false; }
                    // we might be ending an array or object already @TODO how to trigger that?
                    // re-use global "c" maybe?
                    else
                        { throw new JsonParsingException(line, column, "invalid character in number at "); }
                }
            c = readNext(false); //@TODO for the "unexpected", omit the flag, and use the level instead!
            }
        while (loop);

        try // conversion, depending on flags and sizes
            {
            if (hasExponent)
                {
                int exponent = Integer.parseInt(sb2.toString()); // may throw a parse error
                if (Math.abs(exponent) <= 307) // max of what fits into a Double
                    { output.consumeNumberDouble(line, column, level, Double.parseDouble(sb.toString()+'e'+sb2.toString())); }
                else
                    { output.consumeNumberBigDecimal(line, column, level, new BigDecimal(sb.toString())); }
                }
            else if (hasDecimals)
                {
                //@TODO check prove and improve the length limit.
                // 1-15 decimals: double.
                if (sb.length() <= 15)
                    { output.consumeNumberDouble(line, column, level, Double.parseDouble(sb.toString())); }
                else
                    { output.consumeNumberBigDecimal(line, column, level, new BigDecimal(sb.toString())); }
                }
            else // regular integer
                {
                // no exponent, no decimals
                // we determine by length, and stay on the safe side.
                // <=9 digits: integer. (+-2147483647 as actual max)
                // 10-18 digits: long. (+- 9223372036854775807 actual max)
                // >18 digits: bignum
                if (sb.length() <= 9) //
                    { output.consumeNumberInteger(line, column, level, Integer.parseInt(sb.toString())); }
                else if (sb.length() <= 18)
                    { output.consumeNumberLong(line, column, level, Long.parseLong(sb.toString())); }
                else
                    { output.consumeNumberBigInteger(line, column, level, new BigInteger(sb.toString())); }
                }
            }
        catch (NumberFormatException ex) // should be extremely rare, given that we've checked all digits
            {
            throw new JsonParsingException(line, column, ex);
            }
        return;
        }

    private void parseStringStarted()
        {
        output.consumeString(line, column, level, readStringStarted());
        }

    private String readStringStarted()
        {
        sb.setLength(0);
        Character c;
        boolean loop = true;
        do
            {
            c = readNext(true); // throw exception if stream ends during reading.
            switch (c)
                {
                case '\\':
                    {
                    c = readNext(true);
                    switch (c)
                        {
                        case '\"':
                            sb.append('"');
                            break; // quotation mark
                        case '\\':
                            sb.append('\\');
                            break; // reverse solidus itself
                        case '/':
                            sb.append('/');
                            break; // slash / "solidus"
                        case 'b':
                            sb.append('\b');
                            break; // backspace
                        case 'f':
                            sb.append('\f');
                            break; // FF
                        case 'n':
                            sb.append('\n');
                            break; // LF
                        case 'r':
                            sb.append('\r');
                            break; // CR
                        case 't':
                            sb.append('\t');
                            break; // tab
                        case 'u':   // unicode escape; 4 hexits to follow.
                            sb2.setLength(0);
                            sb2.append("0x");
                            sb2.append(readNext(true));
                            sb2.append(readNext(true));
                            sb2.append(readNext(true));
                            sb2.append(readNext(true));
                            // try to parse as hex
                            try
                                {
                                // Integer.parseInt() has some strange issue with leading 0s, so we use decode and a prefix instead.
                                int escapedUnicodeValue = Integer.decode(sb2.toString());
                                sb.append(Character.toChars(escapedUnicodeValue)); // and that's the value we store.
                                }
                            catch (NumberFormatException ex)
                                {
                                throw new JsonParsingException(line, column, "invalid character in escaped unicode value");
                                }
                            break;
                        default:
                            throw new JsonParsingException(line, column, "invalid escape sequence "+(Character.isAlphabetic(c) ? "'"+c+"'" : ""));
                        } // end escaping switch-case
                    }
                    break;
                case '"':
                    loop = false;
                    break; // end of string
                default:
                    sb.append(c);
                    break; // normal case
                } // end outer switch-case
            }
        while (loop);
        return sb.toString();
        }

    // --- util functions ---
    // next non-whitespace character must be the expected one, otherwise throw exception
    private void mustBeNext(final char expected, final String errorMessage)
        {
        char c = getNextCharSkippingWhitespace(true);
        if (c != expected)
            throw new JsonParsingException(line, column, errorMessage);
        return;
        }

    /*
     get next non-whitespace character.
     */
    private char getNextCharSkippingWhitespace(boolean wouldStreamEndBeUnexpected)
        {
        char c;
        do
            { c = readNext(wouldStreamEndBeUnexpected); }
        while (isWhitespace(c, unicodeWhitespace));
        return c;
        }

    /**
     * whitespace check, with unicode option.
     *
     * @param c                    character to check
     * @param useUnicodeWhitespace false to treat unicode whitespace as non-whitespace.
     *                             that leaves Tab, CR, LF, space
     * @return true if the input is considered whitespace, false if not.
     *         <p>
     *         Intentionally using switch-case rather than a LUT; compiler can optimize
     *         better this way (plus, translation to C/C++/Rust is more efficient).
     */
    public static boolean isWhitespace(char c, boolean useUnicodeWhitespace)
        {
        // boolean javaFlag = Character.isWhitespace(c);
        // there is also java Character.isWhitespace(integer) with a different result
        // and neither matches the entire unicode code set.
        switch (c)
            {
            // the usual suspects
            case 0x0B: // VT (vertical tab)
            case 0x0C: // FF (form feed)
            case 0x85: // NEL (next line)
            case 0xA0: // NBSP (non-breakable space)
                // according to Unicode, these too are whitespace:
            case 0x1680: // ogham space mark
            case 0x2000: // en quad
            case 0x2001: // em quad
            case 0x2002: // en space
            case 0x2003: // em space
            case 0x2004: // three-per-em space
            case 0x2005: // four-per-em space
            case 0x2006: // six-per-em space
            case 0x2007: // figure space
            case 0x2008: // punctuation space
            case 0x2009: // thin space
            case 0x200A: // hair space
            case 0x2028: // line separator
            case 0x2029: // paragraph separator
            case 0x202F: // narrow NBSP
            case 0x205F: // medium math space
            case 0x3000: // ideographic space
                if (!useUnicodeWhitespace)
                    { break; }
                // else intentional fallthrough
                // these standard whitespace are defined in JSON explicitly
            case '\t':  // 0x09
            case '\n':  // 0x0A
            case '\r':  // 0x0D
            case ' ':   // 0x20
                return true;
            default:
                // return false;
                // to avoid "unreachable" niggling, fallthrough here.
            }
        return false; // default case
        }

    // read, counting columns and lines.
    private char readNext(boolean wouldStreamEndBeUnexpected)
        {
        char c;
        try
            {
            if (putback) // workaround?
                {
                putback = false;
                // and reuse currentChar
                }
            else
                currentChar = input.read();
            if (currentChar < 0)
                {
                if (wouldStreamEndBeUnexpected)
                    { throw new JsonParsingException(line, column, "unexpected end of input"); }
                else
                    { return Character.MIN_VALUE; } // use NUL as in-band signal. avoiding mis-use of Checked Exceptions.
                // NUL byte content is allowed within strings; but there, the stream end would be unexpected.
                }
            c = (char) currentChar; // seems to be the official java way. would prefer explicit unicode UTF-16 cast.
            }
        catch (IOException e)
            {
            throw new JsonParsingException(line, column, e);
            } // wrap exception.
        // that out of the way... back to the actual code.
        switch (c)
            {
            case '\r': // CR
                if (ignoreCR)
                    break;
                // else intentional fallthrough to LF
            case '\n': // LF
                line++;
                column = 0;
                break;
            case '\t': // tab. for column handling.
                column += (tabStep-(column%tabStep)); //@TODO check
                break;
            default:
                column++;
            }
        return c;
        }


    private       Reader             input;
    private       JsonStreamConsumer output;
    // variables
    private       boolean            oldStyle;
    private       int                line;
    private       int                column;
    private       int                level;
    private       int                currentChar;
    private       boolean            putback; // workaround. parts of this need an overhaul; instead of putback, we'd always go with "currentChar"; and make much more use of nextCharMustBe(whatever)
    private final StringBuilder      sb; // reused local buffer.
    private final StringBuilder      sb2; // reused local buffer.
    // settings.
    private       boolean            ignoreCR;
    private       boolean            unicodeWhitespace;
    private       int                tabStep;
}
//___EOF___


/*
 * simplified JSON parsing.
 * @param input string to parse as JSON
 * @param consumer consumer to receive results
 * @throws IllegalArgumentException on format errors -- use internal error class derived? with position in string.
 * //returns when we get there
 */
//@TODO static functions here for ease of use?

//@TODO change from CharSequence to Reader. we cannot use maxOffset, and have to check for -1 on reads. that's all
// provide wrapper for string inputs: StringReader (SE8)
// provide wrapper for InputStream: InputStreamReader with unicode charset UTF-16 explicit
// provide Java streams API

//@TODO handle -1 stream ends in a clever way. e.g. would an end be unexpected?
