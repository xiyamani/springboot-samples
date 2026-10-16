package cn.training.erp;

import java.math.BigDecimal;
import java.sql.*;

public final class SqlFunctions {
    private SqlFunctions(){}
    public static BigDecimal stockValue(Connection connection,Long productId) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement("SELECT stock * average_cost FROM products WHERE id=?")){
            statement.setLong(1,productId);
            try(ResultSet rows=statement.executeQuery()){return rows.next()?rows.getBigDecimal(1):BigDecimal.ZERO;}
        }
    }
}
