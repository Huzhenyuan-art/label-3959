package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper
 * 继承 BaseMapper 获得完整的单表 CRUD 能力
 */
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE deleted = 1 " +
            "AND (#{username} IS NULL OR username LIKE CONCAT('%', #{username}, '%')) " +
            "AND (#{status} IS NULL OR status = #{status}) " +
            "AND (#{minAge} IS NULL OR age >= #{minAge}) " +
            "AND (#{maxAge} IS NULL OR age <= #{maxAge}) " +
            "AND (#{role} IS NULL OR role = #{role}) " +
            "ORDER BY created_time DESC")
    IPage<User> selectDeletedUsers(Page<User> page,
                                   @Param("username") String username,
                                   @Param("status") Integer status,
                                   @Param("minAge") Integer minAge,
                                   @Param("maxAge") Integer maxAge,
                                   @Param("role") String role);

    @Update("UPDATE user SET deleted = 0, version = version + 1, updated_time = NOW() " +
            "WHERE id = #{id} AND version = #{version} AND deleted = 1")
    int restoreUserById(@Param("id") Long id, @Param("version") Integer version);

    @Select("SELECT * FROM user WHERE deleted = 1 " +
            "AND (#{username} IS NULL OR username LIKE CONCAT('%', #{username}, '%')) " +
            "AND (#{status} IS NULL OR status = #{status}) " +
            "AND (#{minAge} IS NULL OR age >= #{minAge}) " +
            "AND (#{maxAge} IS NULL OR age <= #{maxAge}) " +
            "AND (#{role} IS NULL OR role = #{role}) " +
            "ORDER BY created_time DESC")
    List<User> listDeletedUsers(@Param("username") String username,
                                @Param("status") Integer status,
                                @Param("minAge") Integer minAge,
                                @Param("maxAge") Integer maxAge,
                                @Param("role") String role);
}
