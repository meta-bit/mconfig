package org.metabit.platform.support.config.impl.format.toml;

import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlArray;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlArrayTable;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlArrayItem;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlScalar;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlTable;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlType;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TomlParser
{
    private final TomlCharStream stream;

    TomlParser(String content)
        {
        this.stream = new TomlCharStream(content);
        }

    TomlTable parse()
            throws TomlParseException
        {
        TomlTable root = new TomlTable(false);
        TomlTable currentTable = root;
        boolean firstKeyFound = false;

        while (true)
            {
            stream.skipWhitespace();
            if (stream.eof())
                {
                break;
                }

            if (stream.peek() == '\n' || stream.peek() == '\r')
                {
                stream.skipLineEnd();
                continue;
                }

            List<String> leadingComments = readLeadingComments();
            stream.skipWhitespace();

            if (stream.eof())
                {
                if (!leadingComments.isEmpty())
                    {
                    root.setHeaderComments(copyComments(leadingComments));
                    }
                break;
                }

            if (stream.peek() == '\n' || stream.peek() == '\r')
                {
                // Orphaned block because of empty line after it
                root.setHeaderComments(mergeComments(root.getHeaderComments(), leadingComments));
                stream.skipLineEnd();
                continue;
                }

            if (stream.peek() == '[')
                {
                currentTable = parseTableHeader(root, leadingComments);
                firstKeyFound = true;
                continue;
                }

            List<String> keyPath = parseKeyPath();
            stream.skipWhitespace();
            stream.expect('=');
            stream.skipWhitespace();
            TomlValue value = parseValue();
            stream.skipWhitespace();
            String inlineComment = null;
            if (stream.peek() == '#')
                {
                inlineComment = stream.readCommentText();
                }
            stream.skipLineEnd();

            firstKeyFound = true;
            assignValue(currentTable, keyPath, value, leadingComments, inlineComment);
            }

        return root;
        }

    private List<String> mergeComments(List<String> existing, List<String> additional)
        {
        if (additional == null || additional.isEmpty())
            {
            return existing;
            }
        List<String> result = (existing == null) ? new ArrayList<>() : new ArrayList<>(existing);
        result.addAll(additional);
        return result;
        }

    private TomlTable parseTableHeader(TomlTable root, List<String> leadingComments)
            throws TomlParseException
        {
        stream.expect('[');
        boolean arrayTable = false;
        if (stream.peek() == '[')
            {
            arrayTable = true;
            stream.next();
            }

        stream.skipWhitespace();
        List<String> keyPath = parseKeyPath();
        stream.skipWhitespace();

        if (arrayTable)
            {
            stream.expect(']');
            stream.expect(']');
            }
        else
            {
            stream.expect(']');
            }

        stream.skipWhitespace();
        String inlineComment = null;
        if (stream.peek() == '#')
            {
            inlineComment = stream.readCommentText();
            }
        stream.skipLineEnd();

        TomlTable table = root;
        for (int i = 0; i < keyPath.size() - 1; i++)
            {
            String segment = keyPath.get(i);
            table = getOrCreateTable(table, segment, false);
            }

        String last = keyPath.get(keyPath.size() - 1);
        TomlValue existing = table.getValues().get(last);
        if (arrayTable)
            {
            TomlArrayTable arrayTableValue;
            if (existing == null)
                {
                arrayTableValue = new TomlArrayTable();
                table.getValues().put(last, arrayTableValue);
                }
            else if (existing instanceof TomlArrayTable)
                {
                arrayTableValue = (TomlArrayTable) existing;
                }
            else
                {
                throw stream.error("array of tables conflicts with existing value", stream.getLine(), stream.getColumn());
                }
            TomlTable newTable = new TomlTable(false);
            newTable.setExplicit(true);
            arrayTableValue.getTables().add(newTable);
            newTable.setHeaderComments(copyComments(leadingComments));
            newTable.setHeaderInlineComment(inlineComment);
            return newTable;
            }
        else
            {
            TomlTable nextTable;
            if (existing == null)
                {
                nextTable = new TomlTable(false);
                nextTable.setExplicit(true);
                table.getValues().put(last, nextTable);
                }
            else if (existing instanceof TomlTable && !((TomlTable) existing).isInline())
                {
                nextTable = (TomlTable) existing;
                nextTable.setExplicit(true);
                }
            else
                {
                throw stream.error("table conflicts with existing value", stream.getLine(), stream.getColumn());
                }
            nextTable.setHeaderComments(copyComments(leadingComments));
            nextTable.setHeaderInlineComment(inlineComment);
            return nextTable;
            }
        }

    private List<String> parseKeyPath()
            throws TomlParseException
        {
        List<String> segments = new ArrayList<>();
        while (true)
            {
            String key = parseKeySegment();
            if (key.isEmpty())
                {
                throw stream.error("empty key segment", stream.getLine(), stream.getColumn());
                }
            segments.add(key);
            stream.skipWhitespace();
            if (stream.peek() == '.')
                {
                stream.next();
                stream.skipWhitespace();
                continue;
                }
            break;
            }
        return segments;
        }

    private String parseKeySegment()
            throws TomlParseException
        {
        char ch = stream.peek();
        if (ch == '"' || ch == '\'')
            {
            return parseString();
            }
        return parseBareKey();
        }

    private String parseBareKey()
            throws TomlParseException
        {
        StringBuilder sb = new StringBuilder();
        while (!stream.eof())
            {
            char ch = stream.peek();
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-')
                {
                sb.append(ch);
                stream.next();
                continue;
                }
            break;
            }
        if (sb.length() == 0)
            {
            throw stream.error("invalid key", stream.getLine(), stream.getColumn());
            }
        return sb.toString();
        }

    private TomlValue parseValue()
            throws TomlParseException
        {
        char ch = stream.peek();
        if (ch == '"' || ch == '\'')
            {
            String value = parseString();
            return new TomlScalar(TomlType.STRING, value);
            }
        if (ch == '[')
            {
            return parseArray();
            }
        if (ch == '{')
            {
            return parseInlineTable();
            }
        String token = parseToken();
        if (token.isEmpty())
            {
            throw stream.error("missing value", stream.getLine(), stream.getColumn());
            }
        return parseTokenValue(token);
        }

    private TomlValue parseInlineTable()
            throws TomlParseException
        {
        stream.expect('{');
        TomlTable table = new TomlTable(true);
        stream.skipWhitespace();
        if (stream.peek() == '}')
            {
            stream.next();
            return table;
            }
        while (true)
            {
            List<String> keyPath = parseKeyPath();
            stream.skipWhitespace();
            stream.expect('=');
            stream.skipWhitespace();
            TomlValue value = parseValue();
            assignInlineValue(table, keyPath, value);
            stream.skipWhitespace();
            char ch = stream.peek();
            if (ch == ',')
                {
                stream.next();
                stream.skipWhitespace();
                continue;
                }
            if (ch == '}')
                {
                stream.next();
                break;
                }
            throw stream.error("expected ',' or '}' in inline table", stream.getLine(), stream.getColumn());
            }
        return table;
        }

    private TomlValue parseArray()
            throws TomlParseException
        {
        stream.expect('[');
        List<TomlArrayItem> items = new ArrayList<>();
        List<String> trailingComments = null;
        TomlType elementType = null;
        while (true)
            {
            List<String> leadingComments = readArrayItemLeadingComments();
            if (stream.peek() == ']')
                {
                stream.next();
                trailingComments = copyComments(leadingComments);
                break;
                }
            TomlValue value = parseValue();
            if (value.getType() == TomlType.TABLE || value.getType() == TomlType.ARRAY_TABLE)
                {
                throw stream.error("arrays cannot contain tables", stream.getLine(), stream.getColumn());
                }
            if (elementType == null)
                {
                elementType = value.getType();
                }
            else if (elementType != value.getType())
                {
                throw stream.error("array elements must be of the same type", stream.getLine(), stream.getColumn());
                }
            stream.skipWhitespace();
            String inlineComment = null;
            char ch = stream.peek();
            if (ch == '#')
                {
                inlineComment = stream.readCommentText();
                stream.skipLineEnd();
                }
            items.add(new TomlArrayItem(value, copyComments(leadingComments), inlineComment));
            stream.skipWhitespace();
            ch = stream.peek();
            if (ch == ',')
                {
                stream.next();
                continue;
                }
            if (ch == ']')
                {
                stream.next();
                break;
                }
            throw stream.error("expected ',' or ']' in array", stream.getLine(), stream.getColumn());
            }
        return new TomlArray(items, trailingComments);
        }

    private String parseString()
            throws TomlParseException
        {
        char quote = stream.peek();
        if (quote != '"' && quote != '\'')
            {
            throw stream.error("expected string", stream.getLine(), stream.getColumn());
            }
        if (quote == '"')
            {
            return parseBasicString();
            }
        return parseLiteralString();
        }

    private String parseBasicString()
            throws TomlParseException
        {
        stream.expect('"');
        boolean multiline = stream.peek() == '"' && stream.peek(1) == '"';
        if (multiline)
            {
            stream.next();
            stream.next();
            if (stream.peek() == '\n')
                {
                stream.next();
                }
            if (stream.peek() == '\r' && stream.peek(1) == '\n')
                {
                stream.next();
                stream.next();
                }
            StringBuilder sb = new StringBuilder();
            while (!stream.eof())
                {
                if (stream.peek() == '"' && stream.peek(1) == '"' && stream.peek(2) == '"')
                    {
                    stream.next();
                    stream.next();
                    stream.next();
                    return sb.toString();
                    }
                char ch = stream.next();
                if (ch == '\\')
                    {
                    if (stream.peek() == '\n' || (stream.peek() == '\r' && stream.peek(1) == '\n'))
                        {
                        skipLineContinuation();
                        continue;
                        }
                    sb.append(parseEscape());
                    continue;
                    }
                if (ch == '\r' && stream.peek() == '\n')
                    {
                    stream.next();
                    sb.append('\n');
                    continue;
                    }
                sb.append(ch);
                }
            throw stream.error("unterminated multiline basic string", stream.getLine(), stream.getColumn());
            }

        StringBuilder sb = new StringBuilder();
        while (!stream.eof())
            {
            char ch = stream.next();
            if (ch == '"')
                {
                return sb.toString();
                }
            if (ch == '\\')
                {
                sb.append(parseEscape());
                continue;
                }
            if (ch == '\n' || ch == '\r')
                {
                throw stream.error("newline in basic string", stream.getLine(), stream.getColumn());
                }
            sb.append(ch);
            }
        throw stream.error("unterminated basic string", stream.getLine(), stream.getColumn());
        }

    private void skipLineContinuation()
            throws TomlParseException
        {
        if (stream.peek() == '\r' && stream.peek(1) == '\n')
            {
            stream.next();
            stream.next();
            }
        else if (stream.peek() == '\n')
            {
            stream.next();
            }
        while (!stream.eof())
            {
            char ch = stream.peek();
            if (ch == ' ' || ch == '\t')
                {
                stream.next();
                continue;
                }
            break;
            }
        }

    private String parseEscape()
            throws TomlParseException
        {
        char ch = stream.next();
        switch (ch)
            {
            case 'b':
                return "\b";
            case 't':
                return "\t";
            case 'n':
                return "\n";
            case 'f':
                return "\f";
            case 'r':
                return "\r";
            case '"':
                return "\"";
            case '\\':
                return "\\";
            case '/':
                return "/";
            case 'u':
                return String.valueOf((char) parseHex(4));
            case 'U':
                {
                int codePoint = parseHex(8);
                if (codePoint > Character.MAX_CODE_POINT)
                    {
                    throw stream.error("invalid unicode escape", stream.getLine(), stream.getColumn());
                    }
                if (codePoint <= Character.MAX_VALUE)
                    {
                    return String.valueOf((char) codePoint);
                    }
                return new String(Character.toChars(codePoint));
                }
            default:
                throw stream.error("invalid escape sequence", stream.getLine(), stream.getColumn());
            }
        }

    private int parseHex(int length)
            throws TomlParseException
        {
        int value = 0;
        for (int i = 0; i < length; i++)
            {
            char ch = stream.next();
            int digit = Character.digit(ch, 16);
            if (digit < 0)
                {
                throw stream.error("invalid hex digit", stream.getLine(), stream.getColumn());
                }
            value = (value << 4) + digit;
            }
        return value;
        }

    private String parseLiteralString()
            throws TomlParseException
        {
        stream.expect('\'');
        boolean multiline = stream.peek() == '\'' && stream.peek(1) == '\'';
        if (multiline)
            {
            stream.next();
            stream.next();
            if (stream.peek() == '\n')
                {
                stream.next();
                }
            if (stream.peek() == '\r' && stream.peek(1) == '\n')
                {
                stream.next();
                stream.next();
                }
            StringBuilder sb = new StringBuilder();
            while (!stream.eof())
                {
                if (stream.peek() == '\'' && stream.peek(1) == '\'' && stream.peek(2) == '\'')
                    {
                    stream.next();
                    stream.next();
                    stream.next();
                    return sb.toString();
                    }
                char ch = stream.next();
                if (ch == '\r' && stream.peek() == '\n')
                    {
                    stream.next();
                    sb.append('\n');
                    continue;
                    }
                sb.append(ch);
                }
            throw stream.error("unterminated multiline literal string", stream.getLine(), stream.getColumn());
            }

        StringBuilder sb = new StringBuilder();
        while (!stream.eof())
            {
            char ch = stream.next();
            if (ch == '\'')
                {
                return sb.toString();
                }
            if (ch == '\n' || ch == '\r')
                {
                throw stream.error("newline in literal string", stream.getLine(), stream.getColumn());
                }
            sb.append(ch);
            }
        throw stream.error("unterminated literal string", stream.getLine(), stream.getColumn());
        }

    private String parseToken()
            throws TomlParseException
        {
        StringBuilder sb = new StringBuilder();
        while (!stream.eof())
            {
            char ch = stream.peek();
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == ',' || ch == ']' || ch == '}' || ch == '#')
                {
                break;
                }
            sb.append(ch);
            stream.next();
            }
        return sb.toString();
        }

    private TomlValue parseTokenValue(String token)
            throws TomlParseException
        {
        if ("true".equals(token))
            {
            return new TomlScalar(TomlType.BOOLEAN, Boolean.TRUE);
            }
        if ("false".equals(token))
            {
            return new TomlScalar(TomlType.BOOLEAN, Boolean.FALSE);
            }

        TomlValue dateTimeValue = tryParseDateTime(token);
        if (dateTimeValue != null)
            {
            return dateTimeValue;
            }

        TomlValue numberValue = tryParseNumber(token);
        if (numberValue != null)
            {
            return numberValue;
            }

        throw stream.error("invalid value: " + token, stream.getLine(), stream.getColumn());
        }

    private TomlValue tryParseDateTime(String token)
        {
        String value = token;
        if (value.indexOf('T') >= 0 || value.indexOf('t') >= 0 || value.indexOf(' ') >= 0)
            {
            String normalized = value.replace('t', 'T').replace(' ', 'T');
            try
                {
                OffsetDateTime odt = OffsetDateTime.parse(normalized);
                return new TomlScalar(TomlType.DATETIME, odt);
                }
            catch (DateTimeParseException ignored)
                {
                }
            try
                {
                LocalDateTime ldt = LocalDateTime.parse(normalized);
                return new TomlScalar(TomlType.DATETIME, ldt);
                }
            catch (DateTimeParseException ignored)
                {
                }
            }
        if (value.indexOf('-') >= 0)
            {
            try
                {
                LocalDate date = LocalDate.parse(value);
                return new TomlScalar(TomlType.DATE, date);
                }
            catch (DateTimeParseException ignored)
                {
                }
            }
        if (value.indexOf(':') >= 0)
            {
            try
                {
                LocalTime time = LocalTime.parse(value);
                return new TomlScalar(TomlType.TIME, time);
                }
            catch (DateTimeParseException ignored)
                {
                }
            }
        return null;
        }

    private TomlValue tryParseNumber(String token)
            throws TomlParseException
        {
        String cleaned = token.replace("_", "");
        if (cleaned.equalsIgnoreCase("inf") || cleaned.equalsIgnoreCase("+inf") || cleaned.equalsIgnoreCase("-inf")
                || cleaned.equalsIgnoreCase("nan") || cleaned.equalsIgnoreCase("+nan") || cleaned.equalsIgnoreCase("-nan"))
            {
            double value;
            if (cleaned.startsWith("-"))
                {
                value = Double.NEGATIVE_INFINITY;
                if (cleaned.toLowerCase().contains("nan"))
                    {
                    value = Double.NaN;
                    }
                }
            else if (cleaned.startsWith("+") || cleaned.toLowerCase().contains("inf"))
                {
                value = Double.POSITIVE_INFINITY;
                if (cleaned.toLowerCase().contains("nan"))
                    {
                    value = Double.NaN;
                    }
                }
            else
                {
                value = Double.NaN;
                }
            return new TomlScalar(TomlType.FLOAT, value);
            }

        if (!isValidNumberFormat(token))
            {
            throw stream.error("invalid number format", stream.getLine(), stream.getColumn());
            }

        boolean isFloat = cleaned.indexOf('.') >= 0 || cleaned.indexOf('e') >= 0 || cleaned.indexOf('E') >= 0;
        boolean negative = false;
        if (cleaned.startsWith("-") || cleaned.startsWith("+"))
            {
            negative = cleaned.startsWith("-");
            cleaned = cleaned.substring(1);
            }

        if (cleaned.startsWith("0x") || cleaned.startsWith("0o") || cleaned.startsWith("0b"))
            {
            int radix = cleaned.startsWith("0x") ? 16 : (cleaned.startsWith("0o") ? 8 : 2);
            String digits = cleaned.substring(2);
            if (digits.isEmpty())
                {
                throw stream.error("invalid number format", stream.getLine(), stream.getColumn());
                }
            BigInteger intValue = new BigInteger(digits, radix);
            if (negative)
                {
                intValue = intValue.negate();
                }
            return new TomlScalar(TomlType.INTEGER, intValue);
            }

        if (!isFloat)
            {
            if (cleaned.startsWith("0") && cleaned.length() > 1)
                {
                throw stream.error("leading zeros in integer are not allowed", stream.getLine(), stream.getColumn());
                }
            try
                {
                if (negative)
                    {
                    cleaned = "-" + cleaned;
                    }
                BigInteger intValue = new BigInteger(cleaned);
                return new TomlScalar(TomlType.INTEGER, intValue);
                }
            catch (NumberFormatException ignored)
                {
                }
            return null;
            }

        if (negative)
            {
            cleaned = "-" + cleaned;
            }
        try
            {
            BigDecimal decimal = new BigDecimal(cleaned);
            return new TomlScalar(TomlType.FLOAT, decimal);
            }
        catch (NumberFormatException ignored)
            {
            }
        return null;
        }

    private void assignValue(TomlTable currentTable, List<String> keyPath, TomlValue value,
            List<String> leadingComments, String inlineComment)
            throws TomlParseException
        {
        TomlTable table = currentTable;
        for (int i = 0; i < keyPath.size() - 1; i++)
            {
            String segment = keyPath.get(i);
            table = getOrCreateTable(table, segment, true);
            }
        String last = keyPath.get(keyPath.size() - 1);
        if (table.getValues().containsKey(last))
            {
            throw stream.error("duplicate key: " + last, stream.getLine(), stream.getColumn());
            }
        table.getValues().put(last, value);
        table.setLeadingComments(last, copyComments(leadingComments));
        table.setInlineComment(last, inlineComment);
        }

    private void assignInlineValue(TomlTable inlineTable, List<String> keyPath, TomlValue value)
            throws TomlParseException
        {
        TomlTable table = inlineTable;
        for (int i = 0; i < keyPath.size() - 1; i++)
            {
            String segment = keyPath.get(i);
            table = getOrCreateInlineTable(table, segment);
            }
        String last = keyPath.get(keyPath.size() - 1);
        if (table.getValues().containsKey(last))
            {
            throw stream.error("duplicate key in inline table", stream.getLine(), stream.getColumn());
            }
        table.getValues().put(last, value);
        }

    private TomlTable getOrCreateInlineTable(TomlTable parent, String key)
            throws TomlParseException
        {
        Map<String, TomlValue> values = parent.getValues();
        TomlValue existing = values.get(key);
        if (existing == null)
            {
            TomlTable table = new TomlTable(true);
            values.put(key, table);
            return table;
            }
        if (existing instanceof TomlTable)
            {
            TomlTable table = (TomlTable) existing;
            if (!table.isInline())
                {
                throw stream.error("inline table conflicts with existing table", stream.getLine(), stream.getColumn());
                }
            return table;
            }
        throw stream.error("inline table conflicts with existing value", stream.getLine(), stream.getColumn());
        }

    private TomlTable getOrCreateTable(TomlTable parent, String key, boolean forKeyAssignment)
            throws TomlParseException
        {
        Map<String, TomlValue> values = parent.getValues();
        TomlValue existing = values.get(key);
        if (existing == null)
            {
            TomlTable table = new TomlTable(false);
            values.put(key, table);
            return table;
            }
        if (existing instanceof TomlTable)
            {
            TomlTable table = (TomlTable) existing;
            if (table.isInline())
                {
                throw stream.error("inline table cannot be re-opened", stream.getLine(), stream.getColumn());
                }
            return table;
            }
        if (existing instanceof TomlArrayTable)
            {
            if (forKeyAssignment)
                {
                throw stream.error("cannot assign through array of tables", stream.getLine(), stream.getColumn());
                }
            throw stream.error("array of tables conflicts with table", stream.getLine(), stream.getColumn());
            }
        throw stream.error("key conflicts with existing value", stream.getLine(), stream.getColumn());
        }

    private boolean isValidNumberFormat(String token)
        {
        String value = token;
        if (value == null || value.isEmpty())
            {
            return false;
            }
        String lower = value.toLowerCase();
        if (lower.equals("inf") || lower.equals("+inf") || lower.equals("-inf")
                || lower.equals("nan") || lower.equals("+nan") || lower.equals("-nan"))
            {
            return true;
            }
        if (HEX_INT.matcher(value).matches() || OCT_INT.matcher(value).matches() || BIN_INT.matcher(value).matches())
            {
            return true;
            }
        if (DEC_INT.matcher(value).matches())
            {
            return true;
            }
        if (value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0)
            {
            return FLOAT.matcher(value).matches();
            }
        return false;
        }

    private static final java.util.regex.Pattern DEC_INT =
            java.util.regex.Pattern.compile("^[+-]?(0|[1-9](?:_?[0-9])*)$");
    private static final java.util.regex.Pattern HEX_INT =
            java.util.regex.Pattern.compile("^[+-]?0x[0-9A-Fa-f](?:_?[0-9A-Fa-f])*$");
    private static final java.util.regex.Pattern OCT_INT =
            java.util.regex.Pattern.compile("^[+-]?0o[0-7](?:_?[0-7])*$");
    private static final java.util.regex.Pattern BIN_INT =
            java.util.regex.Pattern.compile("^[+-]?0b[01](?:_?[01])*$");
    private static final java.util.regex.Pattern FLOAT =
            java.util.regex.Pattern.compile("^[+-]?(?:0|[1-9](?:_?[0-9])*)(?:\\.[0-9](?:_?[0-9])*)?(?:[eE][+-]?[0-9](?:_?[0-9])*)?$");

    private List<String> readLeadingComments()
            throws TomlParseException
        {
        List<String> comments = new ArrayList<>();
        while (!stream.eof())
            {
            stream.skipWhitespace();
            if (stream.peek() == '#')
                {
                comments.add(stream.readCommentText());
                stream.skipLineEnd();
                continue;
                }
            break;
            }
        return comments;
        }

    private List<String> copyComments(List<String> comments)
        {
        if (comments == null || comments.isEmpty())
            {
            return null;
            }
        return new ArrayList<>(comments);
        }

    private List<String> readArrayItemLeadingComments()
            throws TomlParseException
        {
        List<String> comments = new ArrayList<>();
        boolean progress = true;
        while (progress && !stream.eof())
            {
            progress = false;
            stream.skipWhitespace();
            if (stream.peek() == '#')
                {
                comments.add(stream.readCommentText());
                stream.skipLineEnd();
                progress = true;
                continue;
                }
            if (stream.peek() == '\n' || stream.peek() == '\r')
                {
                stream.skipLineEnd();
                progress = true;
                continue;
                }
            }
        return comments;
        }

    static final class TomlParseException extends Exception
    {
        private final int line;
        private final int column;

        TomlParseException(String message, int line, int column)
            {
            super(message + " at line " + line + ", column " + column);
            this.line = line;
            this.column = column;
            }

        int getLine()
            {
            return line;
            }

        int getColumn()
            {
            return column;
            }
    }

    static final class TomlCharStream
    {
        private final String content;
        private int index;
        private int line;
        private int column;

        TomlCharStream(String content)
            {
            this.content = content == null ? "" : content;
            this.index = 0;
            this.line = 1;
            this.column = 1;
            }

        boolean eof()
            {
            return index >= content.length();
            }

        char peek()
            {
            if (eof())
                {
                return '\0';
                }
            return content.charAt(index);
            }

        char peek(int offset)
            {
            int pos = index + offset;
            if (pos >= content.length())
                {
                return '\0';
                }
            return content.charAt(pos);
            }

        char next()
            {
            if (eof())
                {
                return '\0';
                }
            char ch = content.charAt(index++);
            if (ch == '\n')
                {
                line++;
                column = 1;
                }
            else
                {
                column++;
                }
            return ch;
            }

        void expect(char expected)
                throws TomlParseException
            {
            char ch = next();
            if (ch != expected)
                {
                throw error("expected '" + expected + "', got '" + ch + "'", line, column);
                }
            }

        void skipWhitespace()
            {
            while (!eof())
                {
                char ch = peek();
                if (ch == ' ' || ch == '\t')
                    {
                    next();
                    continue;
                    }
                break;
                }
            }

        void skipWhitespaceAndComments()
            {
            boolean progress = true;
            while (progress)
                {
                progress = false;
                while (!eof())
                    {
                    char ch = peek();
                    if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r')
                        {
                        next();
                        progress = true;
                        continue;
                        }
                    break;
                    }
                if (peek() == '#')
                    {
                    skipComment();
                    progress = true;
                    }
                }
            }

        void skipComment()
            {
            while (!eof())
                {
                char ch = next();
                if (ch == '\n')
                    {
                    break;
                    }
                }
            }

        String readCommentText()
            {
            StringBuilder sb = new StringBuilder();
            while (!eof())
                {
                char ch = peek();
                if (ch == '\n' || ch == '\r')
                    {
                    break;
                    }
                sb.append(next());
                }
            return sb.toString();
            }

        void skipLineEnd()
            {
            if (peek() == '\r')
                {
                next();
                if (peek() == '\n')
                    {
                    next();
                    }
                return;
                }
            if (peek() == '\n')
                {
                next();
                }
            }

        int getLine()
            {
            return line;
            }

        int getColumn()
            {
            return column;
            }

        TomlParseException error(String message, int line, int column)
            {
            return new TomlParseException(message, line, column);
            }
    }
}
