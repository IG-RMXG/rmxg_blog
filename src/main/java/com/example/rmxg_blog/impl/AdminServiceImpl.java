package com.example.rmxg_blog.impl;

import com.example.rmxg_blog.entity.Admin;
import com.example.rmxg_blog.mapper.AdminMapper;
import com.example.rmxg_blog.service.AdminService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 管理员 服务实现类
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

}
