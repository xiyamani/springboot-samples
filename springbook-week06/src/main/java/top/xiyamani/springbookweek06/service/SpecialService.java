package top.xiyamani.springbookweek06.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.xiyamani.springbookweek06.entity.Special;

public interface SpecialService extends IService<Special> {
    IPage<Special> pageList(Integer pageNum, Integer pageSize, String keyword);
}