package com.example.rmxg_blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rmxg_blog.data.UserListPage;
import com.example.rmxg_blog.entity.User;
import com.example.rmxg_blog.service.UserService;
import com.example.rmxg_blog.util.CommonPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户 前端控制器
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@RestController
@RequestMapping("/rmxg_blog/user")
@Slf4j
public class UserController {



}
