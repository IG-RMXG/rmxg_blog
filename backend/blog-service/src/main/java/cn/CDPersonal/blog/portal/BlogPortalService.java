package cn.CDPersonal.blog.portal;

import cn.CDPersonal.blog.domain.BlogArticle;
import cn.CDPersonal.blog.domain.BlogCategory;
import cn.CDPersonal.blog.domain.BlogComment;
import cn.CDPersonal.blog.domain.BlogTag;
import cn.CDPersonal.blog.portal.domain.PortalArchiveItem;
import cn.CDPersonal.blog.portal.domain.PortalAuthor;
import cn.CDPersonal.blog.portal.domain.PortalQuery;
import cn.CDPersonal.blog.portal.domain.PortalSiteInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户（/portal）的只读业务逻辑。
 *
 * 只读 + 只暴露已发布内容，因此不做登录态判断：
 * 游客没 token 也能看，登录用户看的内容与游客一致。
 * 「写」操作仍然走 /blog/article、/blog/comment 等需要登录的接口。
 *
 * 站点标题/副标题来自配置（blog.portal.title / subtitle），冒号后是未配置时的默认值，改文案不需要改代码。
 */
@Service
public class BlogPortalService {

    /** 侧栏列表的条数上限，避免前端传个巨大值把库拖慢 */
    private static final int MAX_LIMIT = 20;

    private final BlogPortalMapper portalMapper;

    @Value("${blog.portal.title:我的博客}")
    private String siteTitle;

    @Value("${blog.portal.subtitle:记录、思考与分享}")
    private String siteSubtitle;

    public BlogPortalService(BlogPortalMapper portalMapper) {
        this.portalMapper = portalMapper;
    }

    // 前端可能一个筛选条件都不传，这里兜底成空对象，动态 SQL 才会走「无条件」分支。
    public List<BlogArticle> articles(PortalQuery query) {
        return portalMapper.selectPublishedList(query == null ? new PortalQuery() : query);
    }

    /**
     * 门户文章详情：只返回已发布文章，并累加浏览量。
     *
     * @return null 表示文章不存在或未发布（含已下线的草稿/待审核）
     */
    public BlogArticle viewArticle(Long articleId) {
        if (articleId == null) {
            return null;
        }
        BlogArticle article = portalMapper.selectPublishedById(articleId);
        if (article == null) {
            return null;
        }
        // 先查再自增：查到的浏览数是自增前的值，所以先写库再手动 +1 回填，保证返回给前端的数字与库里一致。
        portalMapper.incrementPublishedViewCount(articleId);
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        // 详情 SQL 不含标签列，标签在这里单独补齐；未发布的文章在上面就已返回 null，走不到这一步。
        article.setTags(portalMapper.selectTagsByArticleId(articleId));
        return article;
    }

    public List<BlogComment> comments(Long articleId) {
        if (articleId == null) {
            return List.of();
        }
        return portalMapper.selectPublishedComments(articleId);
    }

    // 分类/标签直接复用门户列表查询：它们的长度就是「有已发布文章」的数量，与另外两条 COUNT 口径一致。
    public PortalSiteInfo siteInfo() {
        List<BlogCategory> categories = categories();
        List<BlogTag> tags = tags();
        PortalSiteInfo info = new PortalSiteInfo();
        info.setTitle(siteTitle);
        info.setSubtitle(siteSubtitle);
        PortalAuthor author = portalMapper.selectAuthor();
        info.setAuthor(author);
        info.setArticleCount(portalMapper.countPublishedArticles());
        info.setCategoryCount(categories.size());
        info.setTagCount(tags.size());
        info.setCommentCount(portalMapper.countPublishedComments());
        info.setViewCount(portalMapper.sumPublishedViews());
        return info;
    }

    public List<BlogCategory> categories() {
        return portalMapper.selectPublishedCategories();
    }

    public List<BlogTag> tags() {
        return portalMapper.selectPublishedTags();
    }

    public List<PortalArchiveItem> archive() {
        return portalMapper.selectArchive();
    }

    public List<BlogArticle> hotArticles(int limit) {
        return portalMapper.selectHotArticles(normalizeLimit(limit));
    }

    public List<BlogComment> latestComments(int limit) {
        return portalMapper.selectLatestComments(normalizeLimit(limit));
    }

    // 侧栏条数统一收敛：非正数（含前端传 0 / -1）按默认 5 处理，正数截断到 MAX_LIMIT，防止一次拉全表。
    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
