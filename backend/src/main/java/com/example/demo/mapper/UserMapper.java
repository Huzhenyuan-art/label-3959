package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.User;

/**
 * 用户 Mapper
 * 继承 BaseMapper 获得完整的单表 CRUD 能力
 */
public interface UserMapper extends BaseMapper<User> {
}
