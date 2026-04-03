package top.xiyamani.week04.service;

import org.springframework.stereotype.Service; // 必须导入

@Service // 关键注解：标记为Spring服务层组件
public class UserService {
    public void findUser() {
        // 业务逻辑
        System.out.println("查询用户信息");
    }
}
