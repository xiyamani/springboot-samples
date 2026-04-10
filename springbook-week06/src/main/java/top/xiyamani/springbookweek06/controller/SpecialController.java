package top.xiyamani.springbookweek06.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import top.xiyamani.springbookweek06.entity.Special;
import top.xiyamani.springbookweek06.service.SpecialService;


import java.util.List;

/**
 * 知乎专题管理接口
 */
@Tag(name = "知乎专题管理", description = "知乎专题相关接口")
@RestController
@RequestMapping("/api/topic")
public class SpecialController {


    // 构造器注入（避免字段注入警告）
    private final SpecialService specialService;
    public SpecialController(SpecialService specialService) {
        this.specialService = specialService;
    }

    /**
     * 分页查询专题列表
     */
    @Operation(summary = "分页查询专题列表")
    @GetMapping("/page")
    public IPage<Special> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return specialService.pageList(pageNum, pageSize, keyword);
    }

    /**
     * 新增专题
     */
    @Operation(summary = "新增专题")
    @PostMapping
    public boolean add(@RequestBody Special special) {
        return specialService.save(special);
    }

    /**
     * 关注/取消关注专题
     */
    @Operation(summary = "关注/取消关注专题")
    @PostMapping("/{id}/follow")
    public boolean follow(@PathVariable String id) {
        Special special = specialService.getById(id);
        if (special != null) {
            special.setIsFollowing(String.valueOf(!Boolean.parseBoolean(special.getIsFollowing())));
            return specialService.updateById(special);
        }
        return false;
    }

    /**
     * 查询专题详情
     */
    @Operation(summary = "查询专题详情")
    @GetMapping("/{id}")
    public Special getById(@PathVariable("id") String id) {
        return specialService.getById(id);
    }

    /**
     * 删除专题
     */
    @Operation(summary = "删除专题")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") String id) {
        return specialService.removeById(id);
    }

    /**
     * 更新专题
     */
    @Operation(summary = "更新专题")
    @PutMapping
    public boolean update(@RequestBody Special special) {
        return specialService.updateById(special);
    }
}