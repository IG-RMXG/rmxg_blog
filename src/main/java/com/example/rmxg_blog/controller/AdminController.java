package com.example.rmxg_blog.controller;

import cn.hutool.system.HostInfo;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rmxg_blog.data.UserListPage;
import com.example.rmxg_blog.entity.User;
import com.example.rmxg_blog.service.*;
import com.example.rmxg_blog.util.CommonPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 * 管理员 前端控制器
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Controller
@RequestMapping("/rmxg_blog/admin")
@Slf4j
public class AdminController {

    @Autowired
    private ArticleTypeService articleTypeService;
    @Autowired
    private ArticleTagListService articleTagListService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String adminIndex(Model model){

        //获取系统的信息
        OsInfo osInfo = SystemUtil.getOsInfo();
        HostInfo hostInfo = SystemUtil.getHostInfo();
        model.addAttribute("osName",osInfo.getName());
        model.addAttribute("hostAddress",hostInfo.getAddress());

        //文章数量
        long articleTypeCount = articleTypeService.count();
        long articleTagListCount = articleTagListService.count();
        long articleCount = articleService.count();
        model.addAttribute("articleTypeCount", articleTypeCount);
        model.addAttribute("articleTagListCount", articleTagListCount);
        model.addAttribute("articleCount", articleCount);

        //用户数量
        long userCount = userService.count();
        model.addAttribute("userCount", userCount);

        return "/admin/adminIndex";
    }

    /**
     * 用户列表
     * @param userListPage
     * @param model
     * @return
     */
    @RequestMapping(value = "/userList",method = RequestMethod.GET)
    public String userList(UserListPage userListPage, Model model){

        Integer pageNumber = userListPage.getPageNumber();
        Integer pageSize = userListPage.getPageSize();
        IPage<User> iPage = new Page<>(pageNumber, pageSize);

        LambdaQueryWrapper<User> userLambdaQueryWrapper = Wrappers.<User>lambdaQuery().orderByDesc(User::getUserRegisterTime);
        IPage<User> userIPage = userService.page(iPage, userLambdaQueryWrapper);
        model.addAttribute("userPage", CommonPage.restPage(userIPage.getRecords()));

        return "/admin/userList";
    }

}
