package cn.training.erp;

import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.*;

@Mapper
public interface TrainingMapper {
    @Select("SELECT * FROM order_workflows WHERE order_id=#{id}")
    Map<String,Object> workflow(long id);
    @Insert("INSERT INTO order_workflows(order_id,state) VALUES(#{id},'DRAFT')")
    void createWorkflow(long id);
    @Update("UPDATE order_workflows SET state='SUBMITTED',submitted_by=#{user},submitted_at=CURRENT_TIMESTAMP,reviewed_by='',review_note='',reviewed_at=NULL WHERE order_id=#{id}")
    void submit(@Param("id")long id,@Param("user")String user);
    @Update("UPDATE order_workflows SET state=#{state},reviewed_by=#{user},review_note=#{note},reviewed_at=CURRENT_TIMESTAMP WHERE order_id=#{id}")
    void review(@Param("id")long id,@Param("state")String state,@Param("user")String user,@Param("note")String note);
    @Select("SELECT w.*,o.order_no,o.type,o.total_amount,o.status,p.name AS partner_name FROM order_workflows w JOIN business_orders o ON o.id=w.order_id JOIN partners p ON p.id=o.partner_id ORDER BY w.submitted_at DESC")
    List<Map<String,Object>> workflows();
    @Insert("INSERT INTO audit_logs(user_name,action,object_ref,detail) VALUES(#{user},#{action},#{object},#{detail})")
    void audit(@Param("user")String user,@Param("action")String action,@Param("object")String object,@Param("detail")String detail);
    @Select("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 300")
    List<Map<String,Object>> audits();
    @Insert("INSERT INTO integration_jobs(order_id,target,operation,payload) VALUES(#{orderId},#{target},#{operation},#{payload})")
    void enqueue(@Param("orderId")long orderId,@Param("target")String target,@Param("operation")String operation,@Param("payload")String payload);
    @Select("SELECT j.*,o.order_no FROM integration_jobs j JOIN business_orders o ON o.id=j.order_id ORDER BY j.id DESC")
    List<Map<String,Object>> jobs();
    @Select("SELECT * FROM integration_jobs WHERE status='PENDING' ORDER BY id LIMIT 10")
    List<Map<String,Object>> pendingJobs();
    @Select("SELECT * FROM integration_jobs WHERE id=#{id} FOR UPDATE")
    Map<String,Object> lockJob(long id);
    @Update("UPDATE integration_jobs SET status=#{status},attempts=attempts+1,response_text=#{response},last_error=#{error},updated_at=CURRENT_TIMESTAMP WHERE id=#{id}")
    void updateJob(@Param("id")long id,@Param("status")String status,@Param("response")String response,@Param("error")String error);
    @Insert("INSERT INTO integration_attempts(job_id,success,response_text) VALUES(#{id},#{success},#{response})")
    void attempt(@Param("id")long id,@Param("success")boolean success,@Param("response")String response);
    @Select("SELECT * FROM integration_attempts WHERE job_id=#{id} ORDER BY id DESC")
    List<Map<String,Object>> attempts(long id);
    @Insert("INSERT INTO integration_exchanges(target,operation,request_text,response_text,success) VALUES(#{target},#{operation},#{request},#{response},#{success})")
    void exchange(@Param("target")String target,@Param("operation")String operation,@Param("request")String request,@Param("response")String response,@Param("success")boolean success);
    @Select("SELECT * FROM integration_exchanges ORDER BY id DESC LIMIT 100")
    List<Map<String,Object>> exchanges();
    @Select("SELECT * FROM platform_receipts WHERE request_key=#{key}")
    Map<String,Object> receipt(String key);
    @Insert("INSERT INTO platform_receipts(request_key,target,receipt_no,payload) VALUES(#{key},#{target},#{number},#{payload})")
    void receiptInsert(@Param("key")String key,@Param("target")String target,@Param("number")String number,@Param("payload")String payload);
    @Select("SELECT * FROM platform_receipts WHERE target=#{target} ORDER BY id DESC LIMIT 100")
    List<Map<String,Object>> receipts(String target);
    @Select("SELECT * FROM manufacturing_orders ORDER BY order_no")
    List<Map<String,Object>> manufacturingOrders();
    @Select("SELECT * FROM manufacturing_orders WHERE order_no=#{number} FOR UPDATE")
    Map<String,Object> lockManufacturingOrder(String number);
    @Insert("INSERT INTO manufacturing_orders(order_no,product_sku,product_name,planned_qty,completed_qty,status) VALUES(#{number},#{sku},#{name},#{planned},0,'RELEASED')")
    void manufacturingInsert(@Param("number")String number,@Param("sku")String sku,@Param("name")String name,@Param("planned")BigDecimal planned);
    @Update("UPDATE manufacturing_orders SET completed_qty=#{quantity},status=#{status} WHERE order_no=#{number}")
    void manufacturingUpdate(@Param("number")String number,@Param("quantity")BigDecimal quantity,@Param("status")String status);
    @Select("SELECT * FROM manufacturing_reports WHERE request_key=#{key}")
    Map<String,Object> manufacturingReport(String key);
    @Insert("INSERT INTO manufacturing_reports(request_key,order_no,quantity) VALUES(#{key},#{number},#{quantity})")
    void manufacturingReportInsert(@Param("key")String key,@Param("number")String number,@Param("quantity")BigDecimal quantity);
}
