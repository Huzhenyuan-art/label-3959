package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.dto.CategoryStatsDTO;
import com.example.demo.entity.Product;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品 Mapper
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 按分类统计商品数量、库存总量、平均价格
     * 演示：自定义 SQL 注解查询
     */
    @Select("SELECT category, COUNT(*) AS count, SUM(stock) AS totalStock, AVG(price) AS avgPrice " +
            "FROM product GROUP BY category ORDER BY count DESC")
    List<CategoryStatsDTO> selectCategoryStats();
}
