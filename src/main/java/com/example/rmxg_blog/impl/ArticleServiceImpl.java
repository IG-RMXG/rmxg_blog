package com.example.rmxg_blog.impl;

import com.example.rmxg_blog.entity.Article;
import com.example.rmxg_blog.mapper.ArticleMapper;
import com.example.rmxg_blog.service.ArticleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章 服务实现类
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

}
