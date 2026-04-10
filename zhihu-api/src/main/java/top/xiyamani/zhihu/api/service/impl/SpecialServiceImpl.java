package top.xiyamani.zhihu.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.xiyamani.zhihu.api.entity.Special;
import top.xiyamani.zhihu.api.mapper.SpecialMapper;
import top.xiyamani.zhihu.api.service.SpecialService;


@Service
@RequiredArgsConstructor
public class SpecialServiceImpl implements SpecialService {
    private final SpecialMapper specialMapper;


    @Override
    public Page<Special> selectByTitle(String title, int pageNum, int pageSize) {
        // 1. 构造分页参数：pageNum（页码，从1开始）、pageSize（每页条数）
        Page<Special> page = Page.of(pageNum, pageSize);
        // 2. 构造条件构造器：LambdaQueryWrapper（类型安全，避免字段名写错）
        LambdaQueryWrapper<Special> wrapper = new LambdaQueryWrapper<>();
        // 条件：标题不为空时，模糊查询（第一个参数为条件，满足才拼接）
        wrapper.like(title != null && !title.isEmpty(), Special::getTitle, title);
        // 3. 调用BaseMapper的selectPage方法，实现条件分页
        return specialMapper.selectPage(page, wrapper);
    }

    @Override
    public Special getById(String id) {
        return specialMapper.selectById(id);
    }

}
