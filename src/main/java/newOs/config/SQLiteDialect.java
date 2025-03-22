package newOs.config;

import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.render.SelectRenderContext;


public class SQLiteDialect implements Dialect {

    @Override
    public LimitClause limit() {
        return new LimitClause() {
            @Override
            public String getLimit(long limit) {
                return "LIMIT " + limit;
            }

            @Override
            public String getOffset(long offset) {
                return "OFFSET " + offset;
            }

            @Override
            public String getLimitOffset(long limit, long offset) {
                return String.format("LIMIT %d OFFSET %d", limit, offset);
            }

            @Override
            public Position getClausePosition() {
                return Position.AFTER_ORDER_BY;
            }
        };
    }

    @Override
    public LockClause lock() {
        return null; // SQLite 不支持锁语法
    }

    @Override
    public SelectRenderContext getSelectContext() {
        return null;
    }
}