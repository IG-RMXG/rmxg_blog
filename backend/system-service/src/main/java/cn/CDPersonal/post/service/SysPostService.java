package cn.CDPersonal.post.service;

import cn.CDPersonal.common.domain.entity.SysPost;
import cn.CDPersonal.post.mapper.SysPostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 岗位管理。
 *
 * sys_post 没有逻辑删除列，deletePostByIds 是物理删除；调用方需先用
 * hasUserPostByIds 确认岗位没有被用户占用。
 * 岗位编码 / 名称都要求全局唯一，唯一性判断在 service 里排除自身。
 */
@Service
public class SysPostService {

    private final SysPostMapper postMapper;

    public SysPostService(SysPostMapper postMapper) {
        this.postMapper = postMapper;
    }

    public List<SysPost> selectPostList(SysPost query) {
        return postMapper.selectPostList(query);
    }

    public SysPost selectPostById(Long postId) {
        return postMapper.selectPostById(postId);
    }

    /** 岗位编码是否唯一（新增时 postId 为空；修改时查到的那条必须是自己） */
    public boolean checkPostCodeUnique(SysPost post) {
        SysPost exist = postMapper.checkPostCodeUnique(post.getPostCode());
        return exist == null || exist.getPostId().equals(post.getPostId());
    }

    /** 岗位名称是否唯一 */
    public boolean checkPostNameUnique(SysPost post) {
        SysPost exist = postMapper.checkPostNameUnique(post.getPostName());
        return exist == null || exist.getPostId().equals(post.getPostId());
    }

    @Transactional
    public int insertPost(SysPost post) {
        fillDefaults(post);
        return postMapper.insertPost(post);
    }

    @Transactional
    public int updatePost(SysPost post) {
        fillDefaults(post);
        return postMapper.updatePost(post);
    }

    /** 物理删除；已被用户占用的岗位由 controller 用 hasUserPostByIds 先拦截 */
    @Transactional
    public int deletePostByIds(Long[] postIds) {
        return postMapper.deletePostByIds(postIds);
    }

    /** 岗位是否已分配给用户 */
    public boolean hasUserPostByIds(Long[] postIds) {
        return postMapper.countUserPostByIds(postIds) > 0;
    }

    /** 补默认值：排序 0、状态 "0"（正常），避免 NOT NULL / 有默认语义的列写入异常 */
    private void fillDefaults(SysPost post) {
        if (post.getPostSort() == null) {
            post.setPostSort(0);
        }
        if (post.getStatus() == null || post.getStatus().isEmpty()) {
            post.setStatus("0");
        }
    }
}
