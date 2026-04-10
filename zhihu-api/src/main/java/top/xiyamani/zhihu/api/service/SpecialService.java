package top.xiyamani.zhihu.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.xiyamani.zhihu.api.entity.Special;

public interface SpecialService {
    /**
     * 根据标题获取专栏（分页）
     *
     * @param title    标题
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 专栏列表
     */
    Page<Special> selectByTitle(String title, int pageNum, int pageSize);
    /**
     * 获取专栏详情
     *
     * @param id 专栏ID
     * @return 专栏详情
     */
    Special getById(String id);
}