package com.yunos.alicontacts.database.util;


public class SqliteUtil {

    /**
     * Append a "column IN ('value1','value2'...)" sub-clause to a sql where clause.
     * @param clause The sql where clause to append with the IN sub-clause.
     * @param column The column name before the IN keyword.
     * @param values The values in the candidates for IN.
     */
    public static void appendInClause(StringBuilder clause, String column, String[] values) {
        clause.append(' ').append(column).append(" IN (");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                clause.append(',');
            }
            clause.append('\'')
                  .append(values[i].replace("'", "''"))
                  .append('\'');
        }
        clause.append(')');
    }

    /**
     * Append a "column like 'value'" sub-clause to a sql where clause.
     * @param clause The sql where clause to append with the like sub-clause
     * @param column The column name before the like keyword.
     * @param value The search text.
     * @param prefix If the value is searched as a prefix.
     * @param suffix If the value is searched as a suffix.
     */
    public static void appendLikeClause(StringBuilder clause, String column, String value, boolean prefix, boolean suffix) {
        clause.append(' ').append(column).append(" LIKE '");
        if (!prefix) {
            clause.append('%');
        }
        boolean escaped = appendEscapedValueForLike(clause, value);
        if (!suffix) {
            clause.append('%');
        }
        clause.append('\'');
        if (escaped) {
            clause.append(" ESCAPE '\\'");
        }
    }

    /**
     * Append escaped value string to clause.
     * This is only used in LIKE sub-clause.
     * NEVER use it for equals (=) sub-clause.
     * In SQLite expression:
     *     a) the single quote (') is escaped by another single quote ('').
     *     b) for the like sub-clause, '_' and '%' can be escaped by a specified escape character.
     *        Here we use '\' as escape character. The escape character can be escaped by itself.
     * Refer http://www.sqlite.org/lang_expr.html for detail.
     * @param clause The where clause to append the value.
     * @param value The value to be append.
     * @return If any character is escaped.
     */
    public static boolean appendEscapedValueForLike(StringBuilder clause, String value) {
        boolean escaped = false;
        int length = value.length();
        for (int i = 0; i < length; i++) {
            char ch = value.charAt(i);
            switch (ch) {
            case '\'':
                clause.append("''");
                break;
            case '\\':
            case '%':
            case '_':
                escaped = true;
                clause.append('\\');
                // do NOT break, fall through to default.
            default:
                clause.append(ch);
            }
        }
        return escaped;
    }

}
