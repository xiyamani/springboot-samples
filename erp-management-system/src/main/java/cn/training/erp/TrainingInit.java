package cn.training.erp;

import org.springframework.boot.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class TrainingInit implements ApplicationRunner {
    private final TrainingMapper db;
    private final JdbcTemplate jdbc;
    public TrainingInit(TrainingMapper db,JdbcTemplate jdbc){this.db=db;this.jdbc=jdbc;}
    @Override @Transactional public void run(ApplicationArguments args) throws Exception {
        if(db.manufacturingOrders().isEmpty()){
            db.manufacturingInsert("MO-2026-001","FIN-001","精密传动组件 A型",new BigDecimal("100"));
            db.manufacturingInsert("MO-2026-002","FIN-002","工业控制箱 B型",new BigDecimal("50"));
        }
        if(jdbc.queryForObject("SELECT COUNT(*) FROM sql_lab_orders",Long.class)==0){
            List<Integer> ids=new ArrayList<>();for(int i=1;i<=20000;i++)ids.add(i);
            LocalDate date=LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
            jdbc.batchUpdate("INSERT INTO sql_lab_orders(id,partner_id,business_date,amount,status) VALUES(?,?,?,?,?)",ids,500,(ps,id)->{
                ps.setLong(1,id);ps.setLong(2,id%400+1);ps.setDate(3,java.sql.Date.valueOf(date.minusDays(id%180)));
                ps.setBigDecimal(4,BigDecimal.valueOf((id%2000)+100,2));ps.setString(5,id%4==0?"DRAFT":"COMPLETED");
            });
            jdbc.execute("CREATE INDEX idx_lab_partner_date ON sql_lab_orders(partner_id,business_date)");
        }
        try(var connection=jdbc.getDataSource().getConnection()){
            if(connection.getMetaData().getDatabaseProductName().equals("H2"))
                jdbc.execute("CREATE ALIAS IF NOT EXISTS CALC_STOCK_VALUE FOR 'cn.training.erp.SqlFunctions.stockValue'");
        }
    }
}
