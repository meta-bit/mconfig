package org.metabit.platform.support.config.impl.format.toml;

import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlArray;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlArrayTable;
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
import java.util.List;
import java.util.Map;

final class TomlWriter
{
    private final StringBuilder out = new StringBuilder();
    private TomlTable currentTable;

    String write(TomlTable root)
        {
        writeTable(root, "");
        return out.toString();
        }

    private void writeTable(TomlTable table, String prefix)
        {
        TomlTable previousTable = currentTable;
        currentTable = table;
        for (Map.Entry<String, TomlValue> entry : table.getValues().entrySet())
            {
            TomlValue value = entry.getValue();
            if (value instanceof TomlTable && !((TomlTable) value).isInline())
                {
                TomlTable subTable = (TomlTable) value;
                String path = joinPath(prefix, entry.getKey());
                if (out.length() > 0)
                    {
                    out.append('\n');
                    }
                writeHeaderComments(subTable);
                out.append('[').append(path).append(']');
                if (subTable.getHeaderInlineComment() != null)
                    {
                    String inline = subTable.getHeaderInlineComment();
                    if (!inline.startsWith("#"))
                        {
                        out.append(" #");
                        }
                    else
                        {
                        out.append(" ");
                        }
                    out.append(inline);
                    }
                out.append('\n');
                writeTable(subTable, path);
                continue;
                }
            if (value instanceof TomlArrayTable)
                {
                TomlArrayTable arrayTable = (TomlArrayTable) value;
                String path = joinPath(prefix, entry.getKey());
                for (TomlTable item : arrayTable.getTables())
                    {
                    if (out.length() > 0)
                        {
                        out.append('\n');
                        }
                    writeHeaderComments(item);
                    out.append("[[").append(path).append("]]");
                    if (item.getHeaderInlineComment() != null)
                        {
                        String inline = item.getHeaderInlineComment();
                        if (!inline.startsWith("#"))
                            {
                            out.append(" #");
                            }
                        else
                            {
                            out.append(" ");
                            }
                        out.append(inline);
                        }
                    out.append('\n');
                    writeTable(item, path);
                    }
                continue;
                }
            writeKeyValue(entry.getKey(), value);
            }
        currentTable = previousTable;
        }

    private void writeKeyValue(String key, TomlValue value)
        {
        if (currentTable != null)
            {
            List<String> leadingComments = currentTable.getLeadingComments(key);
            if (leadingComments != null)
                {
                for (String comment : leadingComments)
                    {
                    if (comment != null && !comment.startsWith("#"))
                        {
                        out.append("# ").append(comment).append('\n');
                        }
                    else
                        {
                        out.append(comment == null ? "" : comment).append('\n');
                        }
                    }
                }
            }
        out.append(formatKey(key)).append(" = ").append(formatValue(value));
        if (currentTable != null)
            {
            String inlineComment = currentTable.getInlineComment(key);
            if (inlineComment != null)
                {
                if (!inlineComment.startsWith("#"))
                    {
                    out.append(" # ").append(inlineComment);
                    }
                else
                    {
                    out.append(" ").append(inlineComment);
                    }
                }
            }
        out.append('\n');
        }

    private String formatKey(String key)
        {
        if (key.matches("[A-Za-z0-9_-]+"))
            {
            return key;
            }
        return "\"" + escapeString(key) + "\"";
        }

    private String formatValue(TomlValue value)
        {
        if (value instanceof TomlScalar)
            {
            return formatScalar((TomlScalar) value);
            }
        if (value instanceof TomlArray)
            {
            TomlArray array = (TomlArray) value;
            return formatArray(array);
            }
        if (value instanceof TomlTable && ((TomlTable) value).isInline())
            {
            TomlTable inline = (TomlTable) value;
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, TomlValue> entry : inline.getValues().entrySet())
                {
                if (!first)
                    {
                    sb.append(", ");
                    }
                first = false;
                sb.append(formatKey(entry.getKey())).append(" = ").append(formatValue(entry.getValue()));
                }
            sb.append('}');
            return sb.toString();
            }
        return "\"\"";
        }

    private String formatArray(TomlArray array)
        {
        if (!array.hasComments())
            {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            List<TomlValue> values = array.getValues();
            for (int i = 0; i < values.size(); i++)
                {
                if (i > 0)
                    {
                    sb.append(", ");
                    }
                sb.append(formatValue(values.get(i)));
                }
            sb.append(']');
            return sb.toString();
            }
        StringBuilder sb = new StringBuilder();
        sb.append('[').append('\n');
        List<TomlModel.TomlArrayItem> items = array.getItems();
        for (int i = 0; i < items.size(); i++)
            {
            TomlModel.TomlArrayItem item = items.get(i);
            List<String> leadingComments = item.getLeadingComments();
            if (leadingComments != null)
                {
                for (String comment : leadingComments)
                    {
                    sb.append("  #").append(comment == null ? "" : comment).append('\n');
                    }
                }
            sb.append("  ").append(formatValue(item.getValue()));
            if (i < items.size() - 1 || (array.getTrailingComments() != null && !array.getTrailingComments().isEmpty()))
                {
                sb.append(',');
                }
            if (item.getInlineComment() != null)
                {
                sb.append(" #").append(item.getInlineComment());
                }
            sb.append('\n');
            }
        List<String> trailingComments = array.getTrailingComments();
        if (trailingComments != null)
            {
            for (String comment : trailingComments)
                {
                sb.append("  #").append(comment == null ? "" : comment).append('\n');
                }
            }
        sb.append(']');
        return sb.toString();
        }

    private String formatScalar(TomlScalar scalar)
        {
        Object value = scalar.getValue();
        TomlType type = scalar.getType();
        switch (type)
            {
            case STRING:
                return "\"" + escapeString(String.valueOf(value)) + "\"";
            case BOOLEAN:
                return String.valueOf(value).toLowerCase();
            case INTEGER:
                if (value instanceof BigInteger)
                    {
                    return ((BigInteger) value).toString();
                    }
                return String.valueOf(value);
            case FLOAT:
                if (value instanceof BigDecimal)
                    {
                    return ((BigDecimal) value).toPlainString();
                    }
                if (value instanceof Double)
                    {
                    double d = (Double) value;
                    if (Double.isNaN(d))
                        {
                        return "nan";
                        }
                    if (Double.isInfinite(d))
                        {
                        return d < 0 ? "-inf" : "inf";
                        }
                    return Double.toString(d);
                    }
                return String.valueOf(value);
            case DATE:
                if (value instanceof LocalDate)
                    {
                    return value.toString();
                    }
                return String.valueOf(value);
            case TIME:
                if (value instanceof LocalTime)
                    {
                    return value.toString();
                    }
                return String.valueOf(value);
            case DATETIME:
                if (value instanceof OffsetDateTime || value instanceof LocalDateTime)
                    {
                    return value.toString();
                    }
                return String.valueOf(value);
            default:
                return "\"\"";
            }
        }

    private String escapeString(String value)
        {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++)
            {
            char ch = value.charAt(i);
            switch (ch)
                {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (ch < 0x20 || ch > 0x7E)
                        {
                        sb.append(String.format("\\u%04X", (int) ch));
                        }
                    else
                        {
                        sb.append(ch);
                        }
                    break;
                }
            }
        return sb.toString();
        }

    private String joinPath(String prefix, String key)
        {
        if (prefix == null || prefix.isEmpty())
            {
            return key;
            }
        return prefix + "." + key;
        }

    private void writeHeaderComments(TomlTable table)
        {
        List<String> comments = table.getHeaderComments();
        if (comments == null)
            {
            return;
            }
        for (String comment : comments)
            {
            if (comment != null && !comment.startsWith("#"))
                {
                out.append("# ").append(comment).append('\n');
                }
            else
                {
                out.append(comment == null ? "" : comment).append('\n');
                }
            }
        }
}
