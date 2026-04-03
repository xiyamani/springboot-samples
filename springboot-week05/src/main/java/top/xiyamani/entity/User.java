package top.xiyamani.entity;


import java.time.LocalDateTime;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("t_user")
public class User {
    @Schema(description="主键")
    private Long id;
    @Schema(description="用户名")
    private String username;
    @Schema(description="密码")
    private String password;
    @Schema(description="年龄")
    private Integer age;
    @Schema(description="邮箱")
    private String email;
    @Schema(description="创建时间")
    private LocalDateTime createTime;
}
