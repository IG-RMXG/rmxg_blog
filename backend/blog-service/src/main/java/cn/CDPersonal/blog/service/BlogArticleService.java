package cn.CDPersonal.blog.service;

import cn.CDPersonal.blog.domain.BlogArticle;
import cn.CDPersonal.blog.domain.BlogTag;
import cn.CDPersonal.blog.mapper.BlogArticleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 博客文章管理（含管理员 / 普通用户分权）。
 *
 * 状态约定：0=草稿 1=已发布 2=待审核 3=已驳回。
 *
 * 分权规则：
 *   - 普通用户：只能看/改/删自己的文章；保存时状态只能是草稿(0)或待审核(2)，
 *     试图直接发布(1)会被强制转为待审核(2)；不能置顶/推荐，不能改他人文章。
 *   - 管理员：可看/改/删全部文章，可任意设定状态，可置顶/推荐。
 */
@Service
public class BlogArticleService {

    public static final String STATUS_DRAFT = "0";
    public static final String STATUS_PUBLISHED = "1";
    public static final String STATUS_PENDING = "2";
    public static final String STATUS_REJECTED = "3";

    private final BlogArticleMapper articleMapper;

    public BlogArticleService(BlogArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public List<BlogArticle> selectArticleList(BlogArticle query, Long userId, boolean isAdmin) {
        if (!isAdmin) {
            // 普通用户只能看到自己的文章
            query.setUserId(userId);
        }
        return articleMapper.selectArticleList(query);
    }

    /**
     * 文章详情（含标签）。普通用户只能看自己的文章或已发布文章。
     *
     * @return null 表示不存在或无权查看
     */
    public BlogArticle selectArticleById(Long articleId, Long userId, boolean isAdmin) {
        BlogArticle article = articleMapper.selectArticleById(articleId);
        if (article == null) {
            return null;
        }
        if (!isAdmin && !article.getUserId().equals(userId)
                && !STATUS_PUBLISHED.equals(article.getStatus())) {
            return null;
        }
        article.setTags(articleMapper.selectTagsByArticleId(articleId));
        return article;
    }

    /** 浏览详情：已发布文章浏览量 +1（读取页面用） */
    public BlogArticle viewArticle(Long articleId, Long userId, boolean isAdmin) {
        BlogArticle article = selectArticleById(articleId, userId, isAdmin);
        if (article != null && STATUS_PUBLISHED.equals(article.getStatus())) {
            articleMapper.incrementViewCount(articleId);
        }
        return article;
    }

    @Transactional
    public Long insertArticle(BlogArticle article, Long[] tagIds, Long userId, boolean isAdmin) {
        article.setArticleId(null);
        article.setUserId(userId);
        article.setStatus(normalizeStatusForSave(article.getStatus(), null, isAdmin));
        if (article.getIsTop() == null || article.getIsTop().isBlank()) {
            article.setIsTop("0");
        }
        if (article.getIsRecommend() == null || article.getIsRecommend().isBlank()) {
            article.setIsRecommend("0");
        }
        if (!isAdmin) {
            article.setIsTop("0");
            article.setIsRecommend("0");
        }
        article.setViewCount(article.getViewCount() == null ? 0 : article.getViewCount());
        article.setLikeCount(article.getLikeCount() == null ? 0 : article.getLikeCount());
        article.setCommentCount(article.getCommentCount() == null ? 0 : article.getCommentCount());
        article.setPublishTime(STATUS_PUBLISHED.equals(article.getStatus()) ? new Date() : null);

        articleMapper.insertArticle(article);
        syncTags(article.getArticleId(), tagIds);
        return article.getArticleId();
    }

    /**
     * 更新文章。
     *
     * @return null 表示成功；否则返回错误提示
     */
    @Transactional
    public String updateArticle(BlogArticle input, Long[] tagIds, Long userId, boolean isAdmin) {
        BlogArticle existing = articleMapper.selectArticleById(input.getArticleId());
        if (existing == null) {
            return "文章不存在";
        }
        if (!isAdmin && !existing.getUserId().equals(userId)) {
            return "无权修改他人文章";
        }

        input.setStatus(normalizeStatusForSave(input.getStatus(), existing.getStatus(), isAdmin));
        if (input.getIsTop() == null || input.getIsTop().isBlank()) {
            input.setIsTop(existing.getIsTop() == null ? "0" : existing.getIsTop());
        }
        if (input.getIsRecommend() == null || input.getIsRecommend().isBlank()) {
            input.setIsRecommend(existing.getIsRecommend() == null ? "0" : existing.getIsRecommend());
        }
        if (!isAdmin) {
            input.setIsTop(existing.getIsTop() == null ? "0" : existing.getIsTop());
            input.setIsRecommend(existing.getIsRecommend() == null ? "0" : existing.getIsRecommend());
        }
        input.setPublishTime(STATUS_PUBLISHED.equals(input.getStatus())
                ? (existing.getPublishTime() == null ? new Date() : existing.getPublishTime())
                : null);

        articleMapper.updateArticle(input);
        syncTags(input.getArticleId(), tagIds);
        return null;
    }

    /**
     * 删除文章。普通用户只能删自己的。
     *
     * @return null 表示成功；否则返回错误提示
     */
    @Transactional
    public String deleteArticleByIds(Long[] articleIds, Long userId, boolean isAdmin) {
        if (articleIds == null || articleIds.length == 0) {
            return "请选择要删除的文章";
        }
        if (!isAdmin) {
            for (Long id : articleIds) {
                BlogArticle article = articleMapper.selectArticleById(id);
                if (article == null) {
                    return "文章不存在";
                }
                if (!article.getUserId().equals(userId)) {
                    return "无权删除他人文章";
                }
            }
        }
        articleMapper.deleteArticleTagsByArticleIds(articleIds);
        return articleMapper.deleteArticleByIds(articleIds) > 0 ? null : "删除失败";
    }

    /**
     * 变更状态（发布 / 审核 / 驳回 / 转草稿）。
     *
     * 管理员可任意切换；普通用户只能把自己的文章提交为待审核(2)。
     *
     * @return null 表示成功；否则返回错误提示
     */
    @Transactional
    public String changeStatus(Long articleId, String status, Long userId, boolean isAdmin) {
        BlogArticle article = articleMapper.selectArticleById(articleId);
        if (article == null) {
            return "文章不存在";
        }
        if (!isAdmin) {
            if (!article.getUserId().equals(userId)) {
                return "无权操作他人文章";
            }
            if (!STATUS_PENDING.equals(status)) {
                return "普通用户只能提交审核";
            }
        }
        Date publishTime = STATUS_PUBLISHED.equals(status)
                ? (article.getPublishTime() == null ? new Date() : article.getPublishTime())
                : null;
        articleMapper.updateStatus(articleId, status, publishTime, null);
        return null;
    }

    // ==================== 内部 ====================

    /**
     * 普通用户保存时只能落到草稿(0)或待审核(2)，想发布(1)自动降级为待审核。
     * 管理员可任意状态；不传状态时回退到原状态或草稿。
     */
    private String normalizeStatusForSave(String requested, String existing, boolean isAdmin) {
        if (isAdmin) {
            return requested == null || requested.isBlank() ? (existing == null ? STATUS_DRAFT : existing) : requested;
        }
        if (requested == null || requested.isBlank()) {
            return existing == null ? STATUS_DRAFT : existing;
        }
        if (STATUS_DRAFT.equals(requested) || STATUS_PENDING.equals(requested)) {
            return requested;
        }
        // 普通用户不能直接发布/驳回，统一转待审核
        return STATUS_PENDING;
    }

    private void syncTags(Long articleId, Long[] tagIds) {
        articleMapper.deleteArticleTags(articleId);
        if (tagIds != null && tagIds.length > 0) {
            articleMapper.insertArticleTags(articleId, tagIds);
        }
    }
}
