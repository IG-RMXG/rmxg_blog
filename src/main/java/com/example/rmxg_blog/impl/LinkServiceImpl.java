package com.example.rmxg_blog.impl;

import com.example.rmxg_blog.entity.Link;
import com.example.rmxg_blog.mapper.LinkMapper;
import com.example.rmxg_blog.service.LinkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 友情连接 服务实现类
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Service
public class LinkServiceImpl extends ServiceImpl<LinkMapper, Link> implements LinkService {

}
