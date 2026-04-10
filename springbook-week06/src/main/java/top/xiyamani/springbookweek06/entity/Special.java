package top.xiyamani.springbookweek06.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_special") // 强制绑定表名
public class Special {

    // 主键ID
    private String id;

    // 手动指定数据库字段名，避免映射失败
    @TableField("view_count")
    private String viewCount;

    @TableField("followers_count")
    private String followersCount;

    @TableField("is_following")
    private String isFollowing;

    private String title;

    private String introduction;

    @TableField("section_list")
    private String sectionList;

    private String banner;

    private String updated;
}