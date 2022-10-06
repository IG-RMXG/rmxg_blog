package com.example.rmxg_blog.mapper;

import com.example.rmxg_blog.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户 Mapper 接口
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
