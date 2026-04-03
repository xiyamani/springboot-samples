package top.xiyamani.springbootweek05.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.xiyamani.entity.User;

@Mapper
public interface UserMPMapper extends BaseMapper<User> {
}
