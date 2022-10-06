package com.example.rmxg_blog.impl;

import com.example.rmxg_blog.entity.Ad;
import com.example.rmxg_blog.mapper.AdMapper;
import com.example.rmxg_blog.service.AdService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 广告 服务实现类
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Service
public class AdServiceImpl extends ServiceImpl<AdMapper, Ad> implements AdService {

}
