package top.xiyamani.springbookweek06.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.xiyamani.springbookweek06.entity.Special;
import top.xiyamani.springbookweek06.mapper.SpecialMapper;
import top.xiyamani.springbookweek06.service.SpecialService;

@Service
public class SpecialServiceImpl extends ServiceImpl<SpecialMapper, Special> implements SpecialService {

    @Override
    public IPage<Special> pageList(Integer pageNum, Integer pageSize, String keyword) {
        Page<Special> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Special> wrapper = new LambdaQueryWrapper<>();

        // 模糊查询：标题 or 简介
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Special::getTitle, keyword)
                    .or()
                    .like(Special::getIntroduction, keyword);
        }
        // 按更新时间倒序
        wrapper.orderByDesc(Special::getUpdated);
        return this.page(page, wrapper);
    }
}