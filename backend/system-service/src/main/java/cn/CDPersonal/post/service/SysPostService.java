package cn.CDPersonal.post.service;

import cn.CDPersonal.common.domain.entity.SysPost;
import cn.CDPersonal.post.mapper.SysPostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 岗位管理。
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

    @Transactional
    public int deletePostByIds(Long[] postIds) {
        return postMapper.deletePostByIds(postIds);
    }

    /** 岗位是否已分配给用户 */
    public boolean hasUserPostByIds(Long[] postIds) {
        return postMapper.countUserPostByIds(postIds) > 0;
    }

    private void fillDefaults(SysPost post) {
        if (post.getPostSort() == null) {
            post.setPostSort(0);
        }
        if (post.getStatus() == null || post.getStatus().isEmpty()) {
            post.setStatus("0");
        }
    }
}
