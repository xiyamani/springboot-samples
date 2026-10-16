package cn.training.erp;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.training.erp.ErpService.*;

@Service
public class WorkflowService {
    private final TrainingMapper db;
    private final ErpMapper erp;
    public WorkflowService(TrainingMapper db,ErpMapper erp){this.db=db;this.erp=erp;}
    public static String actor(){
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        return authentication==null?"系统":authentication.getName();
    }
    public void audit(String action,String object,String detail){db.audit(actor(),action,object,detail);}
    @Transactional public Map<String,Object> submit(long id){
        var order=existing(erp.lockOrder(id),"单据不存在");
        require(order.get("status").equals("DRAFT"),"仅待处理单据可以提交");
        var workflow=db.workflow(id);
        if(workflow==null){db.createWorkflow(id);workflow=db.workflow(id);}
        require(Set.of("DRAFT","REJECTED").contains(workflow.get("state")),"单据已提交或已审批");
        db.submit(id,actor());audit("提交审批",order.get("order_no").toString(),"提交至业务主管");
        return db.workflow(id);
    }
    @Transactional public Map<String,Object> review(long id,boolean approved,String note){
        var order=existing(erp.lockOrder(id),"单据不存在");
        require(order.get("status").equals("DRAFT"),"该单据已处理");
        var workflow=existing(db.workflow(id),"请先提交审批");
        require(workflow.get("state").equals("SUBMITTED"),"单据不在待审批状态");
        require(note!=null&&note.length()<=500,"审批意见最多 500 字");
        require(approved||!note.isBlank(),"驳回时请填写原因");
        db.review(id,approved?"APPROVED":"REJECTED",actor(),note);
        audit(approved?"审批通过":"审批驳回",order.get("order_no").toString(),note);
        return db.workflow(id);
    }
}
