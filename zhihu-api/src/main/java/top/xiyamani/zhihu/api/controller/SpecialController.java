package top.xiyamani.zhihu.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.xiyamani.zhihu.api.common.Result;
import top.xiyamani.zhihu.api.entity.Special;
import top.xiyamani.zhihu.api.service.SpecialService;

@RestController
@RequestMapping("/api/v1/special")
@Tag(name = "专栏接口", description = "专栏接口")
@RequiredArgsConstructor
public class SpecialController {
    private final SpecialService specialService;
    /**
     * 根据标题查询专栏
     *
     * @param title    标题
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 专栏列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询专栏", description = "分页查询专栏接口")
    public Result<Page<Special>> selectByTitle(@RequestParam(required = false) String title, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success("查询成功", specialService.selectByTitle(title, pageNum, pageSize));
    }
    /**
     * 获取专栏详情
     *
     * @param id 专栏ID
     * @return 专栏详情
     */
    @GetMapping("/detail")
    @Operation(summary = "获取专栏详情", description = "获取专栏详情接口")
    public Result<Special> get(@RequestParam String id) {
        return Result.success("查询成功", specialService.getById(id));
    }
}
