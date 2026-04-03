package top.xiyamani.springbootweek05.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.xiyamani.entity.User;
import top.xiyamani.springbootweek05.mapper.UserMPMapper;
/**
 * @author xiyamani
 * @date 2026/4/2
 * @description UserMPService
 **/
@Service
@RequiredArgsConstructor
public class UserMPService {
    private final UserMPMapper userMPMapper;
    /**
     * 分⻚查询
     *
     * @param username ⽤户名
     * @param pageNum ⻚码
     * @param pageSize 每⻚条数
     * @return 分⻚数据
     */
    public Page<User> page(String username, Integer pageNum, Integer pageSize) {
        // 1. 构造分⻚参数：pageNum（⻚码，从1开始）、pageSize（每⻚条数）
        Page<User> page = Page.of(pageNum, pageSize);
        // 2. 构造条件构造器：LambdaQueryWrapper（类型安全，避免字段名写错）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 条件：⽤户名不为空时，模糊查询（第⼀个参数为条件，满⾜才拼接）
        wrapper.like(username != null && !username.isEmpty(), User::getUsername, username);
        // 3. 调⽤BaseMapper的selectPage⽅法，实现条件分⻚
        return userMPMapper.selectPage(page, wrapper);
    }
/**
 * 直接调⽤ BaseMapper 的新增⽅法（⽆SQL）
 *
 * @param user ⽤户
 * @return 影响条数
 */
public int add(User user) {
    return userMPMapper.insert(user);
}
    /**
     * 直接调⽤ BaseMapper 的删除⽅法（⽆SQL）
     *
     * @param id 主键
     * @return 影响条数
     */
    public int delete(Long id) {
        return userMPMapper.deleteById(id);
    }
}
