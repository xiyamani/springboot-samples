const {createApp,nextTick}=Vue;
const navigation=[
  {id:'overview',label:'业务概览',icon:'layout-dashboard'},
  {id:'purchase',label:'采购管理',icon:'shopping-cart'},
  {id:'sales',label:'销售管理',icon:'receipt-text'},
  {id:'approvals',label:'审批中心',icon:'clipboard-check'},
  {id:'inventory',label:'库存管理',icon:'boxes'},
  {id:'reports',label:'经营报表',icon:'chart-no-axes-combined'},
  {id:'master',label:'基础资料',icon:'folder-open'},
  {id:'integrations',label:'系统协同',icon:'network'},
  {id:'audit',label:'操作记录',icon:'history'}
];
const app=createApp({
  data:()=>({
    navigation,page:'overview',mobileNav:false,
    data:{products:[],partners:[],orders:[],movements:[],salesLines:[],workflows:[],jobs:[],audits:[],today:''},
    user:null,csrfToken:'',authChecked:false,loginForm:{username:'admin',password:'admin123'},loginError:'',
    accounts:[{username:'admin',password:'admin123',label:'管理员',role:'ADMIN'},{username:'buyer',password:'buyer123',label:'业务员',role:'OPERATOR'},{username:'reviewer',password:'review123',label:'审批主管',role:'REVIEWER'},{username:'keeper',password:'keeper123',label:'仓库员',role:'WAREHOUSE'}],
    approvalFilter:'SUBMITTED',connectionTab:'jobs',connectionState:{connected:false,jobs:[],exchanges:[]},kingdeeDemo:null,manufacturing:[],connectionLoading:false,
    messageData:null,workForm:{},failureTarget:'WMS',sqlResult:null,sqlLoading:false,sqlError:'',
    loading:true,loaded:false,fatal:'',saving:false,lastRefresh:'',
    search:'',statusFilter:'ALL',stockFilter:'all',movementFilter:'ALL',masterTab:'products',partnerFilter:'ALL',inventoryTab:'stock',
    sortKey:'',sortDirection:1,tablePage:1,pageSize:10,
    statuses:[{id:'ALL',label:'全部单据'},{id:'DRAFT',label:'待处理'},{id:'COMPLETED',label:'已完成'},{id:'CANCELLED',label:'已作废'}],
    reportRange:'month',reportStart:'',reportEnd:'',chartColors:['#188a76','#4b87d6','#dd9b49','#ac7fba','#d76c6c'],
    modal:null,formError:'',detail:null,confirmation:null,confirmError:'',toast:null,toastTimer:null,focusBeforeModal:null,
    orderForm:{type:'PURCHASE',partnerId:'',businessDate:'',note:'',lines:[]},productForm:{},partnerForm:{},integration:null,
    integrations:[
      {name:'金蝶 ERP',icon:'building-2',color:'blue',purpose:'企业基础资料、采购销售单据与财务数据',requirements:['具体产品名称、版本与部署方式','测试环境地址与官方接口文档','获授权的应用账号、认证方式与权限范围','组织、物料、仓库与往来单位编码映射','同步方向、触发方式与异常重试规则']},
      {name:'WMS 仓储系统',icon:'warehouse',color:'teal',purpose:'仓库库存、入库回执与出库执行数据',requirements:['WMS 产品版本与测试环境','入库单、出库单、库存查询接口文档','接口认证账号与调用权限','物料、仓库、库位及批次编码规则','回调、幂等、防重复过账与差异处理约定']},
      {name:'MOM 制造运营系统',icon:'factory',color:'amber',purpose:'制造工单、生产领料与完工入库数据',requirements:['MOM 产品版本与测试环境','工单、领料、退料、完工入库接口文档','接口认证方式与授权信息','工厂、产线、工单与物料编码规则','生产状态流转、消息回执与异常处理约定']}
    ]
  }),
  computed:{
    roleCode(){return this.user?.roles?.[0]||'ADMIN'},
    roleLabel(){return{ADMIN:'系统管理员',OPERATOR:'采购销售专员',REVIEWER:'审批主管',WAREHOUSE:'仓储执行员'}[this.roleCode]||'业务用户'},
    roleSummary(){return{ADMIN:'统一管理采购、销售、审批、库存和系统协同。',OPERATOR:'负责基础资料、业务建单和审批提交。',REVIEWER:'负责采购销售单据审核和审批意见留痕。',WAREHOUSE:'负责审批通过单据的入库、出库和库存执行。'}[this.roleCode]||'按授权范围处理业务。'},
    visibleNavigation(){
      const all=navigation.map(n=>n.id);
      const allowed={OPERATOR:['overview','purchase','sales','inventory','reports','master'],REVIEWER:['overview','purchase','sales','approvals','inventory','reports','audit'],WAREHOUSE:['overview','purchase','sales','inventory','reports','audit']}[this.roleCode]||all;
      return navigation.filter(n=>allowed.includes(n.id));
    },
    currentTitle(){return navigation.find(n=>n.id===this.page)?.label||'业务概览'},
    pageDescription(){return {overview:'主仓 · 本月经营数据',purchase:'主仓 · 人民币结算',sales:'主仓 · 人民币结算',approvals:'采购与销售 · 业务主管审核',inventory:'主仓 · 实时结存与过账流水',reports:'单仓账套 · 仅统计已完成销售',master:'制造业务账套',integrations:'金蝶业务通道 · 仓储回执 · 生产协同',audit:'业务操作与审批记录'}[this.page]},
    currentAccount(){return this.accounts.find(a=>a.username===this.user?.username)||{label:this.user?.username||''}},
    pendingApprovals(){return this.data.workflows.filter(w=>w.state==='SUBMITTED'&&w.status==='DRAFT').length},
    approvalRows(){return this.data.workflows.filter(w=>(this.approvalFilter==='ALL'||w.state===this.approvalFilter)&&[w.order_no,w.partner_name,w.submitted_by].some(v=>String(v||'').includes(this.search.trim())))},
    auditRows(){return this.data.audits.filter(a=>[a.user_name,a.action,a.object_ref,a.detail].some(v=>String(v||'').includes(this.search.trim())))},
    dateLabel(){return this.data.today?this.data.today.replace('-',' 年 ').replace('-',' 月 ')+' 日':''},
    monthPrefix(){return this.data.today.slice(0,7)},
    completedMonthSales(){return this.data.orders.filter(o=>o.type==='SALE'&&o.status==='COMPLETED'&&o.business_date.startsWith(this.monthPrefix))},
    completedMonthPurchases(){return this.data.orders.filter(o=>o.type==='PURCHASE'&&o.status==='COMPLETED'&&o.business_date.startsWith(this.monthPrefix))},
    monthSales(){return this.completedMonthSales.reduce((s,o)=>s+Number(o.total_amount),0)},
    monthPurchases(){return this.completedMonthPurchases.reduce((s,o)=>s+Number(o.total_amount),0)},
    stockValue(){return this.data.products.reduce((s,p)=>s+Number(p.stock)*Number(p.average_cost),0)},
    pendingPurchases(){return this.data.orders.filter(o=>o.type==='PURCHASE'&&o.status==='DRAFT').length},
    pendingSales(){return this.data.orders.filter(o=>o.type==='SALE'&&o.status==='DRAFT').length},
    lowStock(){return this.data.products.filter(p=>Number(p.stock)<Number(p.min_stock)).sort((a,b)=>a.stock/Math.max(1,a.min_stock)-b.stock/Math.max(1,b.min_stock))},
    trend(){if(!this.data.today)return[];return Array.from({length:14},(_,i)=>{const date=this.shiftDate(this.data.today,i-13);const orders=this.data.orders.filter(o=>o.business_date===date&&o.status==='COMPLETED');return{date,sales:orders.filter(o=>o.type==='SALE').reduce((s,o)=>s+Number(o.total_amount),0),purchase:orders.filter(o=>o.type==='PURCHASE').reduce((s,o)=>s+Number(o.total_amount),0)}})},
    chartMax(){const max=Math.max(1,...this.trend.flatMap(d=>[d.sales,d.purchase]));return Math.ceil(max/1000)*1000},
    orderType(){return this.page==='purchase'?'PURCHASE':'SALE'},
    typeOrders(){return this.data.orders.filter(o=>o.type===this.orderType)},
    categories(){return [...new Set(this.data.products.map(p=>p.category))]},
    orderPartners(){return this.data.partners.filter(p=>p.type===(this.orderForm.type==='PURCHASE'?'SUPPLIER':'CUSTOMER'))},
    filteredRows(){
      const q=this.search.toLowerCase().trim();let rows=[];
      if(this.page==='purchase'||this.page==='sales')rows=this.typeOrders.filter(o=>(this.statusFilter==='ALL'||o.status===this.statusFilter)&&[o.order_no,o.partner_name,o.note].some(v=>String(v||'').toLowerCase().includes(q)));
      else if(this.page==='inventory'&&this.inventoryTab==='movements')rows=this.data.movements.filter(m=>(this.movementFilter==='ALL'||m.type===this.movementFilter)&&[m.product_name,m.sku,m.order_no].some(v=>String(v||'').toLowerCase().includes(q)));
      else if(this.page==='master'&&this.masterTab==='partners')rows=this.data.partners.filter(p=>(this.partnerFilter==='ALL'||p.type===this.partnerFilter)&&[p.name,p.contact,p.phone].some(v=>String(v||'').toLowerCase().includes(q)));
      else rows=this.data.products.filter(p=>[p.name,p.sku,p.category].some(v=>v.toLowerCase().includes(q))&&(this.page!=='inventory'||this.stockFilter==='all'||(this.stockFilter==='low'?Number(p.stock)<Number(p.min_stock):Number(p.stock)===0)));
      if(this.sortKey)rows=[...rows].sort((a,b)=>{const x=a[this.sortKey],y=b[this.sortKey];return(typeof x==='number'?x-y:String(x??'').localeCompare(String(y??''),'zh-CN'))*this.sortDirection});
      return rows;
    },
    pageCount(){return Math.max(1,Math.ceil(this.filteredRows.length/this.pageSize))},
    pagedRows(){return this.filteredRows.slice((this.tablePage-1)*this.pageSize,this.tablePage*this.pageSize)},
    reportLines(){return this.data.salesLines.filter(l=>this.reportStart&&this.reportEnd&&l.business_date>=this.reportStart&&l.business_date<=this.reportEnd)},
    reportSales(){return this.reportLines.reduce((s,l)=>s+Number(l.line_amount),0)},
    reportCost(){return this.reportLines.reduce((s,l)=>s+Number(l.cost_amount),0)},
    categoryReport(){const m={};for(const l of this.reportLines)m[l.category]=(m[l.category]||0)+Number(l.line_amount);return Object.entries(m).map(([name,amount])=>({name,amount})).sort((a,b)=>b.amount-a.amount)},
    customerReport(){const m={};for(const l of this.reportLines)m[l.partner_id]=(m[l.partner_id]||0)+Number(l.line_amount);return Object.entries(m).map(([id,amount])=>({name:this.data.partners.find(p=>p.id===Number(id))?.name||'未知客户',amount})).sort((a,b)=>b.amount-a.amount)},
    productReport(){const m={};for(const l of this.reportLines){if(!m[l.product_id])m[l.product_id]={sku:l.sku,name:l.product_name,category:l.category,unit:l.unit,quantity:0,amount:0,cost:0};m[l.product_id].quantity+=Number(l.quantity);m[l.product_id].amount+=Number(l.line_amount);m[l.product_id].cost+=Number(l.cost_amount)}return Object.values(m).sort((a,b)=>b.amount-a.amount)},
    modalTitle(){return this.modal==='order'?(this.orderForm.type==='PURCHASE'?'新建采购单':'新建销售单'):this.modal==='product'?(this.productForm.id?'编辑商品':'新增商品'):this.modal==='partner'?'新增往来单位':this.modal==='detail'?(this.detail?.type==='PURCHASE'?'采购单详情':'销售单详情'):this.modal==='messages'?'接口报文记录':this.modal==='work'?'生产报工':this.integration?.name+' 接入条件'}
  },
  watch:{
    search(){this.tablePage=1},
    stockFilter(){this.tablePage=1},
    movementFilter(){this.tablePage=1},
    partnerFilter(){this.tablePage=1},
    page(value){if(!this.user)return;if(value==='integrations')this.loadConnections()},
    pageCount(value){if(this.tablePage>value)this.tablePage=value},
    modal(value){document.body.classList.toggle('modal-open',!!value);nextTick(()=>{if(value)this.$refs.dialog?.focus();else this.focusBeforeModal?.focus()})},
    confirmation(value){if(value)nextTick(()=>this.$refs.confirmDialog?.focus())}
  },
  methods:{
    can(action){if(!this.user)return false;return this.user.roles.includes('ADMIN')||this.user.roles.includes({edit:'OPERATOR',review:'REVIEWER',post:'WAREHOUSE'}[action])},
    flowLabel(state){return{DRAFT:'草稿',SUBMITTED:'待审批',APPROVED:'待执行',REJECTED:'已驳回'}[state]||'草稿'},
    orderStateLabel(order){return order.status==='DRAFT'?this.flowLabel(order.workflow_state||order.workflow?.state):this.statusLabel(order.status)},
    jobLabel(status){return{PENDING:'待发送',SUCCESS:'已同步',FAILED:'同步失败'}[status]||status},
    workLabel(status){return{RELEASED:'已下达',IN_PROGRESS:'生产中',COMPLETED:'已完成'}[status]||status},
    chooseAccount(account){this.loginForm={username:account.username,password:account.password};this.loginError=''},
    async bootstrap(){
      try{const session=await this.api('/auth/session');this.csrfToken=session.csrfToken;this.user=session.user;if(this.user){this.navigate();await this.refresh()}else this.loading=false}
      catch(e){this.loginError=e.message;this.loading=false}finally{this.authChecked=true}
    },
    async login(){
      if(this.saving)return;this.saving=true;this.loginError='';
      try{await this.api('/auth/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(this.loginForm)});await this.bootstrap();if(this.page==='integrations')await this.loadConnections()}
      catch(e){this.loginError=e.message;await this.bootstrap()}finally{this.saving=false}
    },
    async logout(){
      try{await this.api('/auth/logout',{method:'POST'});this.modal=null;this.confirmation=null;this.user=null;this.loaded=false;await this.bootstrap()}
      catch(e){this.notify(e.message,'error')}
    },
    money(value){return Number(value||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})},
    qty(value){return Number(value||0).toLocaleString('zh-CN',{maximumFractionDigits:3})},
    compact(value){return value>=10000?(value/10000).toFixed(1)+'万':this.qty(value)},
    statusLabel(status){return{DRAFT:'待处理',COMPLETED:'已完成',CANCELLED:'已作废'}[status]||status},
    movementLabel(type){return{PURCHASE:'采购入库',SALE:'销售出库',OPENING:'期初库存'}[type]||type},
    categoryIcon(category){return{'原材料':'layers','标准件':'settings-2','电气件':'zap','产成品':'package'}[category]||'box'},
    categoryColor(category){return{'原材料':'blue','标准件':'amber','电气件':'violet','产成品':'teal'}[category]||'blue'},
    shiftDate(date,days){const d=new Date(date+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+days);return d.toISOString().slice(0,10)},
    formatTimestamp(value){if(!value)return'';return new Date(value).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai',hour12:false})},
    async api(url,options={}){
      const response=await fetch('/api'+url,{...options,headers:{'Content-Type':'application/json','X-XSRF-TOKEN':this.csrfToken,...options.headers}});
      if(!response.ok){let body;try{body=await response.json()}catch{}if(response.status===401&&this.user){this.user=null;this.loaded=false;this.modal=null;this.bootstrap()}throw new Error(body?.message||'请求失败，请稍后重试')}
      return response.json();
    },
    async refresh(quiet=false){
      this.loading=true;
      try{this.data=await this.api('/workspace');this.fatal='';this.loaded=true;this.lastRefresh=new Date().toLocaleTimeString('zh-CN',{hour12:false});if(!this.reportStart)this.setReportRange('month')}
      catch(e){if(!this.loaded)this.fatal=e.message;else if(!quiet)this.notify(e.message,'error');if(quiet)throw e}
      finally{this.loading=false}
    },
    navigate(){
      const hash=location.hash.slice(1);const next=this.visibleNavigation.some(n=>n.id===hash)?hash:(this.visibleNavigation[0]?.id||'overview');
      this.page=next;
      if(hash!==next)history.replaceState(null,'','#'+next);
      this.search='';this.tablePage=1;this.sortKey='';this.statusFilter='ALL';window.scrollTo(0,0);
    },
    sortBy(key){this.sortDirection=this.sortKey===key?-this.sortDirection:1;this.sortKey=key},
    setReportRange(range){this.reportRange=range;if(range==='custom')return;this.reportEnd=this.data.today;this.reportStart=range==='month'?this.data.today.slice(0,7)+'-01':this.shiftDate(this.data.today,-Number(range)+1)},
    notify(text,kind='success'){clearTimeout(this.toastTimer);this.toast={text,kind};this.toastTimer=setTimeout(()=>this.toast=null,6000)},
    beginModal(type){this.focusBeforeModal=document.activeElement;this.formError='';this.modal=type},
    closeModal(){if(!this.saving&&!this.confirmation)this.modal=null},
    productById(id){return this.data.products.find(p=>p.id===id)||{stock:0,unit:''}},
    addLine(){this.orderForm.lines.push({key:crypto.randomUUID(),productId:'',quantity:1,unitPrice:0})},
    lineProductChanged(line){const p=this.productById(line.productId);line.unitPrice=Number(this.orderForm.type==='PURCHASE'?p.purchase_price:p.sale_price)},
    openOrder(type,partner=null,product=null){
      this.orderForm={type,partnerId:partner||'',businessDate:this.data.today,note:'',lines:[]};this.addLine();
      if(product){const line=this.orderForm.lines[0];line.productId=product.id;line.quantity=Math.max(1,Number(product.min_stock)*2-Number(product.stock));this.lineProductChanged(line)}
      this.beginModal('order');
    },
    async saveOrder(){
      if(this.saving)return;this.formError='';
      if(!this.orderForm.partnerId){this.formError='请选择往来单位';return}
      if(new Set(this.orderForm.lines.map(l=>l.productId)).size!==this.orderForm.lines.length){this.formError='同一张单据不能重复选择商品';return}
      this.saving=true;
      try{
        const body={...this.orderForm,lines:this.orderForm.lines.map(({productId,quantity,unitPrice})=>({productId,quantity,unitPrice}))};
        const order=await this.api('/orders',{method:'POST',body:JSON.stringify(body)});
        this.modal=null;location.hash=this.orderForm.type==='PURCHASE'?'purchase':'sales';this.notify('单据已保存，请提交审批');
        await this.refresh(true);
      }catch(e){if(this.modal)this.formError=e.message;else this.notify('单据已保存，但数据刷新失败，请点击刷新','error')}finally{this.saving=false}
    },
    openProduct(p=null){this.productForm=p?{id:p.id,sku:p.sku,name:p.name,category:p.category,unit:p.unit,purchasePrice:p.purchase_price,salePrice:p.sale_price,minStock:p.min_stock}:{sku:'',name:'',category:'原材料',unit:'个',purchasePrice:0,salePrice:0,minStock:0};this.beginModal('product')},
    async saveProduct(){
      if(this.saving)return;this.saving=true;this.formError='';
      try{const {id,...body}=this.productForm;await this.api('/products'+(id?'/'+id:''),{method:id?'PUT':'POST',body:JSON.stringify(body)});this.modal=null;this.notify('商品资料已保存');await this.refresh(true)}
      catch(e){if(this.modal)this.formError=e.message;else this.notify('资料已保存，但数据刷新失败','error')}finally{this.saving=false}
    },
    openPartner(){this.partnerForm={name:'',type:'SUPPLIER',contact:'',phone:''};this.beginModal('partner')},
    async savePartner(){
      if(this.saving)return;this.saving=true;this.formError='';
      try{await this.api('/partners',{method:'POST',body:JSON.stringify(this.partnerForm)});this.modal=null;this.notify('往来单位已保存');await this.refresh(true)}
      catch(e){if(this.modal)this.formError=e.message;else this.notify('资料已保存，但数据刷新失败','error')}finally{this.saving=false}
    },
    async showDetail(id){try{this.detail=await this.api('/orders/'+id);this.beginModal('detail')}catch(e){this.notify(e.message,'error')}},
    confirmPost(order){
      this.confirmError='';const purchase=order.type==='PURCHASE';
      this.confirmation={id:order.id,kind:'post',title:purchase?'确认采购入库？':'确认销售出库？',message:order.order_no+' · ¥'+this.money(order.total_amount)+'。确认后将'+(purchase?'增加':'扣减')+'库存并记录流水，单据不可再次修改。',label:purchase?'确认入库':'确认出库'};
    },
    confirmCancel(order){this.confirmError='';this.confirmation={id:order.id,kind:'cancel',title:'确认作废这张单据？',message:order.order_no+'。作废后不影响库存，单据将保留以便追溯，无法恢复为待处理。',label:'确认作废'}},
    confirmWorkflow(order,kind){
      this.confirmError='';
      this.confirmation={id:order.id||order.order_id,kind,note:'',title:{submit:'提交业务审批？',approve:'确认审批通过？',reject:'驳回这张单据？'}[kind],message:order.order_no+' · ¥'+this.money(order.total_amount),label:{submit:'提交审批',approve:'审批通过',reject:'确认驳回'}[kind]};
    },
    async executeConfirmation(){
      if(this.saving)return;this.saving=true;this.confirmError='';
      try{
        const c=this.confirmation;
        await this.api('/orders/'+c.id+'/'+c.kind,{method:'POST',body:['approve','reject'].includes(c.kind)?JSON.stringify({note:c.note}):undefined});
        if(this.detail?.id===c.id)this.detail=await this.api('/orders/'+c.id);
        this.confirmation=null;
        this.notify({cancel:'单据已作废',submit:'已提交主管审批',approve:'审批通过，等待仓库执行',reject:'单据已驳回',post:'执行成功，库存已更新，协同任务已创建'}[c.kind]);
        await this.refresh(true);
      }
      catch(e){if(this.confirmation)this.confirmError=e.message;else this.notify('操作已完成，但数据刷新失败，请点击刷新','error')}
      finally{this.saving=false}
    },
    download(type){const a=document.createElement('a');a.href='/api/export/'+type;a.download='';a.click()},
    exportReport(){
      const escape=value=>{let s=String(value??'');if(/^[=+@\t\r-]/.test(s.trimStart()))s="'"+s;return'"'+s.replaceAll('"','""')+'"'};
      const rows=[['商品编码','商品名称','分类','单位','销售数量','销售收入','销售成本','毛利'],...this.productReport.map(p=>[p.sku,p.name,p.category,p.unit,p.quantity,p.amount.toFixed(2),p.cost.toFixed(2),(p.amount-p.cost).toFixed(2)])];
      const blob=new Blob(['\uFEFF'+rows.map(r=>r.map(escape).join(',')).join('\r\n')],{type:'text/csv;charset=utf-8;'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='商品销售报表_'+this.reportStart+'_'+this.reportEnd+'.csv';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
    },
    openIntegration(s){this.integration=s;this.beginModal('integration')},
    async loadConnections(){
      if(this.connectionLoading)return;this.connectionLoading=true;
      try{this.connectionState=await this.api('/integrations');this.kingdeeDemo=await this.api('/integrations/kingdee/demo');this.manufacturing=await this.api('/integrations/mom/work-orders')}
      catch(e){this.notify(e.message,'error')}finally{this.connectionLoading=false}
    },
    showKingdeeDemo(){
      if(!this.kingdeeDemo){this.notify('金蝶接口状态尚未加载，请稍后再试','error');return}
      this.messageData={kingdee:this.kingdeeDemo};this.beginModal('messages');
    },
    async syncMaterials(){
      if(this.saving)return;this.saving=true;
      try{await this.api('/integrations/materials/pull',{method:'POST'});this.notify('物料同步成功');await this.refresh(true);await this.loadConnections()}
      catch(e){this.notify(e.message,'error')}finally{this.saving=false}
    },
    async dispatchJobs(id=null){
      if(this.saving)return;this.saving=true;
      try{await this.api(id?'/integrations/jobs/'+id+'/retry':'/integrations/dispatch',{method:'POST'});await this.refresh(true);await this.loadConnections();this.notify('任务处理完成，请查看回执状态')}
      catch(e){this.notify(e.message,'error')}finally{this.saving=false}
    },
    async showMessages(job){
      try{this.messageData=await this.api('/integrations/jobs/'+job.id);this.beginModal('messages')}catch(e){this.notify(e.message,'error')}
    },
    showExchange(exchange){this.messageData={exchange};this.beginModal('messages')},
    pretty(value){if(!value)return'—';try{return JSON.stringify(typeof value==='string'?JSON.parse(value):value,null,2)}catch{return value}},
    planText(plan){return Array.isArray(plan)?plan.map(row=>Object.entries(row).map(([key,value])=>key==='PLAN'?value:key+': '+value).join('\n')).join('\n\n'):String(plan||'')},
    openWork(order){this.workForm={orderNo:order.order_no,productName:order.product_name,quantity:1,max:Number(order.planned_qty)-Number(order.completed_qty),requestId:crypto.randomUUID()};this.beginModal('work')},
    async saveWork(){
      if(this.saving)return;this.saving=true;this.formError='';
      try{await this.api('/integrations/mom/report',{method:'POST',body:JSON.stringify(this.workForm)});this.modal=null;this.notify('报工已确认');await this.loadConnections();await this.refresh(true)}
      catch(e){this.formError=e.message}finally{this.saving=false}
    },
    async injectFailure(){
      try{await this.api('/integrations/'+this.failureTarget+'/fail-next',{method:'POST'});this.notify('已设置：下一次 '+this.failureTarget+' 请求返回临时失败')}
      catch(e){this.notify(e.message,'error')}
    },
    async loadSql(){
      this.sqlLoading=true;this.sqlError='';
      try{this.sqlResult=await this.api('/sql-lab')}catch(e){this.sqlError=e.message}finally{this.sqlLoading=false}
    },
    printDetail(){window.print()},
    handleKey(e){
      if(e.key==='Escape'){if(this.confirmation&&!this.saving)this.confirmation=null;else this.closeModal();this.mobileNav=false}
      if(e.key==='Tab'&&(this.modal||this.confirmation)){
        const dialog=this.confirmation?this.$refs.confirmDialog:this.$refs.dialog;
        const nodes=[...dialog.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]')].filter(el=>el.offsetParent!==null);
        if(!nodes.length){e.preventDefault();return}
        const first=nodes[0],last=nodes[nodes.length-1];
        if(e.shiftKey&&(document.activeElement===first||document.activeElement===dialog)){e.preventDefault();last.focus()}
        else if(!e.shiftKey&&(document.activeElement===last||document.activeElement===dialog)){e.preventDefault();first.focus()}
      }
    }
  },
  mounted(){
    this.navigate();window.addEventListener('hashchange',this.navigate);window.addEventListener('keydown',this.handleKey);
    this.bootstrap().then(()=>{if(this.user&&this.page==='integrations')this.loadConnections()});
    setInterval(()=>{if(this.user&&this.page==='integrations'&&!this.modal&&!this.saving){this.refresh(true).catch(()=>{});this.loadConnections()}},7000);
  }
});
app.component('app-icon',{
  props:{name:{type:String,default:'circle'}},
  inheritAttrs:false,
  setup(props,{attrs}){
    return()=>{
      const key=props.name.replace(/(^|-)(\w)/g,(_,separator,letter)=>letter.toUpperCase());
      const nodes=lucide.icons[key]||lucide.icons.Circle;
      return Vue.h('svg',{...attrs,xmlns:'http://www.w3.org/2000/svg',width:18,height:18,viewBox:'0 0 24 24',fill:'none',stroke:'currentColor','stroke-width':1.7,'stroke-linecap':'round','stroke-linejoin':'round','aria-hidden':'true',class:['lucide',attrs.class]},
        nodes.map(([tag,attributes])=>Vue.h(tag,attributes)));
    };
  }
});
app.mount('#app');
