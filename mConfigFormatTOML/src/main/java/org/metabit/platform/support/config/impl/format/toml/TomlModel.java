package org.metabit.platform.support.config.impl.format.toml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TomlModel
{
    private TomlModel()
        {
        }

    enum TomlType
    {
        STRING,
        INTEGER,
        FLOAT,
        BOOLEAN,
        DATE,
        TIME,
        DATETIME,
        ARRAY,
        TABLE,
        INLINE_TABLE,
        ARRAY_TABLE
    }

    interface TomlValue
    {
        TomlType getType();
    }

    static final class TomlScalar implements TomlValue
    {
        private final TomlType type;
        private final Object value;

        TomlScalar(TomlType type, Object value)
            {
            this.type = type;
            this.value = value;
            }

        @Override
        public TomlType getType()
            {
            return type;
            }

        Object getValue()
            {
            return value;
            }
    }

    static final class TomlArray implements TomlValue
    {
        private final List<TomlArrayItem> items;
        private final List<String> trailingComments;

        TomlArray(List<TomlArrayItem> items, List<String> trailingComments)
            {
            this.items = items;
            this.trailingComments = trailingComments;
            }

        @Override
        public TomlType getType()
            {
            return TomlType.ARRAY;
            }

        List<TomlValue> getValues()
            {
            List<TomlValue> values = new ArrayList<>();
            for (TomlArrayItem item : items)
                {
                values.add(item.getValue());
                }
            return values;
            }

        List<TomlArrayItem> getItems()
            {
            return items;
            }

        List<String> getTrailingComments()
            {
            return trailingComments;
            }

        boolean hasComments()
            {
            if (trailingComments != null && !trailingComments.isEmpty())
                {
                return true;
                }
            for (TomlArrayItem item : items)
                {
                if (item.hasComments())
                    {
                    return true;
                    }
                }
            return false;
            }
    }

    static final class TomlArrayItem
    {
        private final TomlValue value;
        private final List<String> leadingComments;
        private final String inlineComment;

        TomlArrayItem(TomlValue value, List<String> leadingComments, String inlineComment)
            {
            this.value = value;
            this.leadingComments = leadingComments;
            this.inlineComment = inlineComment;
            }

        TomlValue getValue()
            {
            return value;
            }

        List<String> getLeadingComments()
            {
            return leadingComments;
            }

        String getInlineComment()
            {
            return inlineComment;
            }

        boolean hasComments()
            {
            return (leadingComments != null && !leadingComments.isEmpty()) || inlineComment != null;
            }
    }

    static final class TomlArrayTable implements TomlValue
    {
        private final List<TomlTable> tables = new ArrayList<>();

        @Override
        public TomlType getType()
            {
            return TomlType.ARRAY_TABLE;
            }

        List<TomlTable> getTables()
            {
            return tables;
            }
    }

    static final class TomlTable implements TomlValue
    {
        private final Map<String, TomlValue> values = new LinkedHashMap<>();
        private final Map<String, List<String>> leadingComments = new LinkedHashMap<>();
        private final Map<String, String> inlineComments = new LinkedHashMap<>();
        private List<String> headerComments = new ArrayList<>();
        private String headerInlineComment;
        private boolean explicit;
        private final boolean inline;

        TomlTable(boolean inline)
            {
            this.inline = inline;
            }

        @Override
        public TomlType getType()
            {
            return inline ? TomlType.INLINE_TABLE : TomlType.TABLE;
            }

        boolean isInline()
            {
            return inline;
            }

        boolean isExplicit()
            {
            return explicit;
            }

        void setExplicit(boolean explicit)
            {
            this.explicit = explicit;
            }

        Map<String, TomlValue> getValues()
            {
            return values;
            }

        List<String> getLeadingComments(String key)
            {
            return leadingComments.get(key);
            }

        void setLeadingComments(String key, List<String> comments)
            {
            if (comments != null && !comments.isEmpty())
                {
                leadingComments.put(key, comments);
                }
            }

        String getInlineComment(String key)
            {
            return inlineComments.get(key);
            }

        void setInlineComment(String key, String comment)
            {
            if (comment != null)
                {
                inlineComments.put(key, comment);
                }
            }

        List<String> getHeaderComments()
            {
            return headerComments;
            }

        void setHeaderComments(List<String> comments)
            {
            if (comments != null && !comments.isEmpty())
                {
                this.headerComments = comments;
                }
            }

        String getHeaderInlineComment()
            {
            return headerInlineComment;
            }

        void setHeaderInlineComment(String comment)
            {
            this.headerInlineComment = comment;
            }
    }
}
