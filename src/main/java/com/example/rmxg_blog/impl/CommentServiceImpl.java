package com.example.rmxg_blog.impl;

import com.example.rmxg_blog.entity.Comment;
import com.example.rmxg_blog.mapper.CommentMapper;
import com.example.rmxg_blog.service.CommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章评论 服务实现类
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

}
