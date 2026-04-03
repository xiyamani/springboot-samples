package top.xiyamani.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.xiyamani.common.Result;
import top.xiyamani.entity.User;
import top.xiyamani.service.UserService;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户CRUD接口")
public class UserController {
    private final UserService userService;

    // 其他代码
    /**
     * 搜索功能
     *
     * @param username ⽤户名
     * @param minAge 最⼩年龄
     * @return 数据列表
     */
    @GetMapping("/search")
    public List<User> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer minAge) {
        return userService.search(username, minAge);
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public Result<String> addUser(@RequestBody User user) {
        int row = userService.add(user);
        return row == 1 ? Result.success("添加成功") : Result.error("添加失败");
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户")
    public Result<User> get(@PathVariable Long id) {
        return Result.success(userService.getBuId(id));
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有用户")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }

    @PutMapping
    @Operation(summary = "更新用户")
    public Result<String> update(@RequestBody User user) {
        int row = userService.update(user);
        return row == 1 ? Result.success("更新成功") : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<String> delete(@PathVariable Long id) {
        int row = userService.delete(id);
        return row == 1 ? Result.success("删除成功") : Result.error("删除失败");
    }
}