package cn.training.erp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/sql-lab")
public class SqlLabController {
    private final JdbcTemplate jdbc;
    public SqlLabController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping public Map<String,Object> run() throws Exception {
        String database;
        try(var connection=jdbc.getDataSource().getConnection()){database=connection.getMetaData().getDatabaseProductName();}
        String cast=database.equals("MySQL")?"CHAR":"VARCHAR";
        String before="SELECT COUNT(*) AS row_count, SUM(amount) AS total_amount FROM sql_lab_orders WHERE CAST(partner_id AS "+cast+")='42'";
        String after="SELECT COUNT(*) AS row_count, SUM(amount) AS total_amount FROM sql_lab_orders WHERE partner_id=42";
        var a=jdbc.queryForMap(before);var b=jdbc.queryForMap(after);
        double beforeMs=time(before),afterMs=time(after);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("database",database);result.put("tableRows",jdbc.queryForObject("SELECT COUNT(*) FROM sql_lab_orders",Long.class));
        result.put("beforeSql",before);result.put("afterSql",after);
        result.put("beforePlan",jdbc.queryForList("EXPLAIN "+before));result.put("afterPlan",jdbc.queryForList("EXPLAIN "+after));
        result.put("beforeMs",beforeMs);result.put("afterMs",afterMs);
        result.put("result",b);result.put("consistent",a.equals(b));
        if(database.equals("H2")){
            Long id=jdbc.queryForObject("SELECT MIN(id) FROM products",Long.class);
            result.put("functionSql","SELECT CALC_STOCK_VALUE("+id+") AS stock_value");
            result.put("functionValue",id==null?BigDecimal.ZERO:jdbc.queryForObject("SELECT CALC_STOCK_VALUE(?)",BigDecimal.class,id));
        }
        return result;
    }
    private double time(String sql){
        jdbc.queryForMap(sql);
        long start=System.nanoTime();
        for(int i=0;i<5;i++)jdbc.queryForMap(sql);
        return Math.round((System.nanoTime()-start)/5.0/1000)/1000.0;
    }
}
