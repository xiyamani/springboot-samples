package top.xiyamani.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.xiyamani.common.Result;
import top.xiyamani.entity.User;
import top.xiyamani.service.UserTxService;
import java.util.Map;
/**
 * @author xiyamani
 * @date 2026/4/2
 * @description UserTxController 事务测试接⼝
 **/
@RestController
@RequestMapping("/api/user/tx")
@RequiredArgsConstructor
public class UserTxController {
    private final UserTxService userTxService;
    /**
     * 新增两个⽤户，添加事务：⽅法内任意⼀步出错，全部回滚
     *
     * @param map ⽤户数据
     * @return 新增结果
     */
    @PostMapping("/addTwo")
    public Result<String> addTwo(@RequestBody Map<String, User> map) {
        User user1 = map.get("user1");
        User user2 = map.get("user2");
        userTxService.addTwoUsers(user1, user2);
        return Result.success("两个⽤户均新增成功");
    }
}
