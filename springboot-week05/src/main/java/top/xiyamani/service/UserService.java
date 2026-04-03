package top.xiyamani.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.xiyamani.entity.User;
import top.xiyamani.springbootweek05.mapper.UserMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

    // 其他代码
    public List<User> search(String username, Integer minAge) {
        return userMapper.selectByCondition(username, minAge);
    }

    public int add(User user){
        // 先设置创建时间，再插入数据库
        user.setCreateTime(LocalDateTime.now());
        int result = userMapper.insert(user);
        System.out.println(user);
        return result;
    }

    // 方法名修正：getBuId → getById
    public User getBuId(Long id){
        return userMapper.selectByPrimaryKey(id);
    }

    public List<User> list(){
        return userMapper.selectList();
    }

    public int update(User user){
        return userMapper.updateByPrimaryKey(user);
    }

    public int delete(Long id){
        return userMapper.deleteByPrimaryKey(id);
    }
}