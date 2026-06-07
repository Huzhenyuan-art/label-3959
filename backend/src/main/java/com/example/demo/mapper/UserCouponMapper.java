package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.dto.UserCouponDTO;
import com.example.demo.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id = #{userId} AND template_id = #{templateId}")
    int countByUserIdAndTemplateId(@Param("userId") Long userId, @Param("templateId") Long templateId);

    @Update("UPDATE user_coupon SET status = 2 WHERE status = 0 AND valid_end_time < #{now}")
    int expireCoupons(@Param("now") LocalDateTime now);

    List<UserCouponDTO> selectUserCouponList(@Param("userId") Long userId, @Param("status") Integer status);

    UserCouponDTO selectUserCouponDetail(@Param("id") Long id);
}
