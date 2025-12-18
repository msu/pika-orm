package bigsky.pika.mapping;

import java.util.ArrayList;
import java.util.List;

public class ColumnsSpec {

    private record Column(String column, String table, String alias) {
    }

    List<Column> columns = new ArrayList<>();
    boolean acceptAll = true;

    public ColumnsSpec(List<String> cols) {
        if (cols != null) {
            acceptAll = false;
            for (String col : cols) {
                String[] colAlias = col.split(" (as|AS) ");
                String start = colAlias[0];
                String alias = null;
                if (colAlias.length == 2) {
                    alias = colAlias[1].strip();
                }
                String[] tableSplit = start.split("\\.");
                String table = null;
                String column;
                if (tableSplit.length == 2) {
                    table = tableSplit[0].strip();
                    column = tableSplit[1].strip();
                } else {
                    column = start.strip();
                }
                columns.add(new Column(column, table, alias));
            }
        }
    }

    public boolean accept(String tableName, String columnName) {
        if (acceptAll) {
            return true;
        }
        for (Column column : columns) {
            if (column.table == null) {
                if (columnName.equals(column.alias)) {
                    return true;
                } else if (columnName.equals(column.column)) {
                    return true;
                }
            } else {
                if (columnName.equals(column.alias)) {
                    return true;
                } else if (tableName.equals(column.table) && (columnName.equals(column.column) || "*".equals(column.column))) {
                    return true;
                }
            }
        }
        return false;
    }
}
