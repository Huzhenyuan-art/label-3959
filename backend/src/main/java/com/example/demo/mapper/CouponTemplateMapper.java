package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    @Update("UPDATE coupon_template SET received_count = received_count + 1 WHERE id = #{id} AND received_count < total_count")
    int incrementReceivedCount(@Param("id") Long id);

    @Update("UPDATE coupon_template SET used_count = used_count + 1 WHERE id = #{id}")
    int incrementUsedCount(@Param("id") Long id);

    @Update("UPDATE coupon_template SET used_count = used_count - 1 WHERE id = #{id} AND used_count > 0")
    int decrementUsedCount(@Param("id") Long id);
}
