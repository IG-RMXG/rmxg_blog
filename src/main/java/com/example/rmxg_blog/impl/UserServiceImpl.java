package com.example.rmxg_blog.impl;

import com.example.rmxg_blog.entity.User;
import com.example.rmxg_blog.mapper.UserMapper;
import com.example.rmxg_blog.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
