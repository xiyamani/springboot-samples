package top.xiyamani.springbootweek05.mapper;

import org.apache.ibatis.annotations.*;
import top.xiyamani.entity.User;
import java.util.List;

/**
 * 用户表数据库操作Mapper
 */
@Mapper
public interface UserMapper {

    /**
     * 动态条件查询：⽤户名模糊查询 + 年龄⼤于等于
     *
     * @param username ⽤户名
     * @param minAge 最⼩年龄
     * @return 数据列表
     */
    List<User> selectByCondition(String username, Integer minAge);

    /**
     * 插入数据
     */
    @Insert("INSERT INTO t_user(username,password,age,email) VALUES (#{username}, #{password}, #{age}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User record);

    /**
     * 根据id查询
     */
    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User selectByPrimaryKey(Long id);

    /**
     * 查询所有
     */
    @Select("SELECT * FROM t_user")
    List<User> selectList();

    /**
     * 根据id更新（补全password字段，避免更新丢失）
     */
    @Update("UPDATE t_user SET username=#{username}, password=#{password}, age=#{age}, email=#{email} WHERE id=#{id}")
    int updateByPrimaryKey(User record);

    /**
     * 根据id删除
     */
    @Delete("DELETE FROM t_user WHERE id=#{id}")
    int deleteByPrimaryKey(Long id);
}