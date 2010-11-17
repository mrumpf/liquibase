package liquibase.sqlgenerator.core;

import liquibase.database.Database;
import liquibase.database.core.*;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.DropPrimaryKeyStatement;

public class DropPrimaryKeyGenerator extends AbstractSqlGenerator<DropPrimaryKeyStatement> {

    @Override
    public boolean supports(DropPrimaryKeyStatement statement, Database database) {
        return (!(database instanceof SQLiteDatabase));
    }

    public ValidationErrors validate(DropPrimaryKeyStatement dropPrimaryKeyStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.checkRequiredField("tableName", dropPrimaryKeyStatement.getTableName());

        if (database instanceof PostgresDatabase || database instanceof FirebirdDatabase || database instanceof InformixDatabase) {
            validationErrors.checkRequiredField("constraintName", dropPrimaryKeyStatement.getConstraintName());
        }
		
        return validationErrors;
    }

    public Sql[] generateSql(DropPrimaryKeyStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        String sql;

        if (database instanceof MSSQLDatabase) {
			if (statement.getConstraintName() == null) {
				StringBuilder query = new StringBuilder();
				query.append("DECLARE @pkname nvarchar(255)");
				query.append("\n");
				query.append("DECLARE @sql nvarchar(2048)");
				query.append("\n");
				query.append("select @pkname=i.name from sysindexes i");
				query.append(" join sysobjects o ON i.id = o.id");
				query.append(" join sysobjects pk ON i.name = pk.name AND pk.parent_obj = i.id AND pk.xtype = 'PK'");
				query.append(" join sysindexkeys ik on i.id = ik.id AND i.indid = ik.indid");
				query.append(" join syscolumns c ON ik.id = c.id AND ik.colid = c.colid");
				query.append(" where o.name = '").append(statement.getTableName()).append("'");
				query.append("\n");
				query.append("set @sql='alter table ").append(database.escapeTableName(statement.getSchemaName(), statement.getTableName())).append(" drop constraint ' + @pkname");
				query.append("\n");
				query.append("exec(@sql)");
				query.append("\n");
				sql = query.toString();
			} else {
				sql = "ALTER TABLE " + database.escapeTableName(statement.getSchemaName(), statement.getTableName()) + " DROP CONSTRAINT " + database.escapeConstraintName(statement.getConstraintName());
			}
        } else if (database instanceof PostgresDatabase) {
            sql = "ALTER TABLE " + database.escapeTableName(statement.getSchemaName(), statement.getTableName()) + " DROP CONSTRAINT " + database.escapeConstraintName(statement.getConstraintName());
        } else if (database instanceof FirebirdDatabase) {
            sql = "ALTER TABLE " + database.escapeTableName(statement.getSchemaName(), statement.getTableName()) + " DROP CONSTRAINT "+database.escapeConstraintName(statement.getConstraintName());
        } else if (database instanceof OracleDatabase) {
            sql = "ALTER TABLE " + database.escapeTableName(statement.getSchemaName(), statement.getTableName()) + " DROP PRIMARY KEY DROP INDEX";
        } else if (database instanceof InformixDatabase) {
            sql = "ALTER TABLE " + database.escapeTableName(statement.getSchemaName(), statement.getTableName()) + " DROP CONSTRAINT " + database.escapeConstraintName(statement.getConstraintName());
        } else {
            sql = "ALTER TABLE " + database.escapeTableName(statement.getSchemaName(), statement.getTableName()) + " DROP PRIMARY KEY";
        }

        return new Sql[] {
                new UnparsedSql(sql)
        };
    }
}
