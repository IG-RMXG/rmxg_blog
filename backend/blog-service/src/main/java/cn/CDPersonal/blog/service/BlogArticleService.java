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

    /** 文章状态：0=草稿（仅作者/管理员可见） 1=已发布（前台可见） 2=待审核 3=已驳回 */
    public static final String STATUS_DRAFT = "0";
    public static final String STATUS_PUBLISHED = "1";
    public static final String STATUS_PENDING = "2";
    public static final String STATUS_REJECTED = "3";

    private final BlogArticleMapper articleMapper;

    public BlogArticleService(BlogArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    /**
     * 文章列表（分页由 Controller 的 startPage() 控制）。
     *
     * 非管理员一律用当前登录用户覆盖 query.userId，防止前端传别人的 userId 越权；
     * 管理员若前端指定 userId，则按该用户过滤（后台按作者筛选）。
     */
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
            // 仅已发布文章计数；同一用户反复刷新会重复 +1（没有去重/防刷机制）
            articleMapper.incrementViewCount(articleId);
        }
        return article;
    }

    /**
     * 新增文章。articleId / userId 由服务端强制覆盖，忽略前端传值。
     *
     * 状态经 normalizeStatusForSave 收敛（普通用户最高只能到待审核）；
     * 置顶/推荐只有管理员能设，普通用户一律落 0；
     * 计数列缺省补 0；只有最终状态为已发布时才写 publishTime，草稿/待审核保持 null。
     * 标签关联在拿到自增主键后写入，与文章插入处于同一事务。
     *
     * @return 新文章主键（由 @Options 的 useGeneratedKeys 回填）
     */
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
        // 前端表单可能不提交置顶/推荐字段：为空时沿用库里的旧值，避免只改正文却把这两列清空
        if (input.getIsTop() == null || input.getIsTop().isBlank()) {
            input.setIsTop(existing.getIsTop() == null ? "0" : existing.getIsTop());
        }
        if (input.getIsRecommend() == null || input.getIsRecommend().isBlank()) {
            input.setIsRecommend(existing.getIsRecommend() == null ? "0" : existing.getIsRecommend());
        }
        // 普通用户不能通过表单篡改置顶/推荐：这里无条件回退为库中的原值
        if (!isAdmin) {
            input.setIsTop(existing.getIsTop() == null ? "0" : existing.getIsTop());
            input.setIsRecommend(existing.getIsRecommend() == null ? "0" : existing.getIsRecommend());
        }
        // 首次发布才写当前时间；已发布过的保留原始发布时间；转回草稿/待审核则清空
        input.setPublishTime(STATUS_PUBLISHED.equals(input.getStatus())
                ? (existing.getPublishTime() == null ? new Date() : existing.getPublishTime())
                : null);

        // 该 UPDATE 不包含 view_count / like_count / comment_count：
        // 前端提交的计数值不会被写回，计数只由专门的增减语句维护。
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
        // 普通用户逐条校验归属：任一不存在或不属于自己就整体失败，不做部分删除
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
        // 先清中间表再删主表：两表之间没有外键级联，顺序反了会留下指向已删文章的关联行
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
        // 普通用户只能把自己的文章提交为待审核(2)；发布/驳回/转草稿都是管理员动作
        if (!isAdmin) {
            if (!article.getUserId().equals(userId)) {
                return "无权操作他人文章";
            }
            if (!STATUS_PENDING.equals(status)) {
                return "普通用户只能提交审核";
            }
        }
        // 发布时保留首次发布时间；转回其它状态置 null，避免草稿/待审核还带着发布时间
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
            // 管理员只做空值兜底，不校验取值合法性：前端传什么状态就落什么状态
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

    /**
     * 文章标签全量覆盖：先删旧关联，再按 tagIds 批量插入。
     *
     * 必须与文章写入同事务，否则中途失败会留下"文章已更新、标签丢失"的半成品；
     * tagIds 为 null 或空数组表示清空该文章的全部标签。
     */
    private void syncTags(Long articleId, Long[] tagIds) {
        articleMapper.deleteArticleTags(articleId);
        if (tagIds != null && tagIds.length > 0) {
            articleMapper.insertArticleTags(articleId, tagIds);
        }
    }
}
