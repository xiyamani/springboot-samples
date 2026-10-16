package cn.training.erp;

import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Mapper
public interface ErpMapper {
    @Select("SELECT * FROM products ORDER BY sku")
    List<Map<String,Object>> products();

    @Select("SELECT * FROM products WHERE id=#{id}")
    Map<String,Object> product(long id);

    @Select("SELECT * FROM products WHERE id=#{id} FOR UPDATE")
    Map<String,Object> lockProduct(long id);

    @Insert("""
        INSERT INTO products(sku,name,category,unit,purchase_price,sale_price,min_stock)
        VALUES(#{sku},#{name},#{category},#{unit},#{purchasePrice},#{salePrice},#{minStock})
        """)
    @Options(useGeneratedKeys=true, keyProperty="id")
    void insertProduct(Map<String,Object> row);

    @Update("""
        UPDATE products SET name=#{name},category=#{category},unit=#{unit},
        purchase_price=#{purchasePrice},sale_price=#{salePrice},min_stock=#{minStock}
        WHERE id=#{id}
        """)
    void updateProduct(Map<String,Object> row);

    @Update("UPDATE products SET stock=#{stock},average_cost=#{averageCost} WHERE id=#{id}")
    void updateStock(@Param("id") long id, @Param("stock") BigDecimal stock, @Param("averageCost") BigDecimal averageCost);

    @Select("SELECT * FROM partners ORDER BY type,name")
    List<Map<String,Object>> partners();

    @Select("SELECT * FROM partners WHERE id=#{id}")
    Map<String,Object> partner(long id);

    @Insert("INSERT INTO partners(name,type,contact,phone) VALUES(#{name},#{type},#{contact},#{phone})")
    @Options(useGeneratedKeys=true, keyProperty="id")
    void insertPartner(Map<String,Object> row);

    @Select("""
        SELECT o.*,p.name AS partner_name,COALESCE(w.state,'DRAFT') AS workflow_state,
        (SELECT COUNT(*) FROM order_lines l WHERE l.order_id=o.id) AS line_count
        FROM business_orders o JOIN partners p ON p.id=o.partner_id LEFT JOIN order_workflows w ON w.order_id=o.id
        ORDER BY o.created_at DESC,o.id DESC
        """)
    List<Map<String,Object>> orders();

    @Select("SELECT * FROM business_orders WHERE id=#{id} FOR UPDATE")
    Map<String,Object> lockOrder(long id);

    @Select("SELECT o.*,p.name AS partner_name FROM business_orders o JOIN partners p ON p.id=o.partner_id WHERE o.id=#{id}")
    Map<String,Object> order(long id);

    @Select("""
        SELECT l.*,p.sku,p.name AS product_name,p.unit
        FROM order_lines l JOIN products p ON p.id=l.product_id
        WHERE l.order_id=#{id} ORDER BY l.product_id,l.id
        """)
    List<Map<String,Object>> lines(long id);

    @Insert("""
        INSERT INTO business_orders(order_no,type,partner_id,status,business_date,total_amount,note)
        VALUES(#{orderNo},#{type},#{partnerId},'DRAFT',#{businessDate},#{totalAmount},#{note})
        """)
    @Options(useGeneratedKeys=true, keyProperty="id")
    void insertOrder(Map<String,Object> row);

    @Insert("""
        INSERT INTO order_lines(order_id,product_id,quantity,unit_price,line_amount)
        VALUES(#{orderId},#{productId},#{quantity},#{unitPrice},#{lineAmount})
        """)
    void insertLine(Map<String,Object> row);

    @Update("UPDATE order_lines SET cost_amount=#{cost} WHERE id=#{id}")
    void updateLineCost(@Param("id") long id, @Param("cost") BigDecimal cost);

    @Update("UPDATE business_orders SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE id=#{id}")
    void completeOrder(long id);

    @Update("UPDATE business_orders SET status='CANCELLED' WHERE id=#{id}")
    void cancelOrder(long id);

    @Insert("""
        INSERT INTO inventory_movements(product_id,order_id,type,quantity,balance,unit_cost,business_date)
        VALUES(#{productId},#{orderId},#{type},#{quantity},#{balance},#{unitCost},#{businessDate})
        """)
    void insertMovement(Map<String,Object> row);

    @Select("""
        SELECT m.*,p.sku,p.name AS product_name,p.unit,o.order_no
        FROM inventory_movements m JOIN products p ON p.id=m.product_id
        LEFT JOIN business_orders o ON o.id=m.order_id ORDER BY m.id DESC
        """)
    List<Map<String,Object>> movements();

    @Select("""
        SELECT l.product_id,p.sku,p.name AS product_name,p.category,p.unit,
        l.quantity,l.line_amount,l.cost_amount,o.business_date,o.partner_id
        FROM order_lines l JOIN business_orders o ON o.id=l.order_id
        JOIN products p ON p.id=l.product_id WHERE o.type='SALE' AND o.status='COMPLETED'
        """)
    List<Map<String,Object>> salesLines();

    @Select("SELECT COUNT(*) FROM products")
    long productCount();
}
