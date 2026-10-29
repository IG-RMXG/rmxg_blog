package cn.CDPersonal.blog.portal;

import cn.CDPersonal.blog.domain.BlogArticle;
import cn.CDPersonal.blog.domain.BlogCategory;
import cn.CDPersonal.blog.domain.BlogComment;
import cn.CDPersonal.blog.domain.BlogTag;
import cn.CDPersonal.blog.portal.domain.PortalArchiveItem;
import cn.CDPersonal.blog.portal.domain.PortalQuery;
import cn.CDPersonal.blog.portal.domain.PortalSiteInfo;
import cn.CDPersonal.blog.support.PageUtils;
import cn.CDPersonal.blog.support.TableDataInfo;
import cn.CDPersonal.common.core.ResponseVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户只读接口（/blog/public/**）。
 *
 * 面向游客开放，网关只需原有的 /blog/** 转发规则即可命中，不需要额外配置。
 * 路径刻意与需要登录的 /blog/article/** 分开，避免以后误改鉴权时把公开接口带偏。
 * 这里没有任何 POST/PUT/DELETE：评论、写作一律走需要登录的既有接口。
 */
@RestController
@RequestMapping("/blog/public")
public class BlogPortalController {

    private final BlogPortalService portalService;

    public BlogPortalController(BlogPortalService portalService) {
        this.portalService = portalService;
    }

    /** 站点信息：标题、副标题、博主、统计 */
    @GetMapping("/site")
    public ResponseVO<PortalSiteInfo> site() {
        return ResponseVO.success(portalService.siteInfo());
    }

    /**
     * 文章列表（分页）。
     * 查询参数：categoryId / tagId / keyword / month（yyyy-MM）/ orderBy（latest|hot|comments）/ pageNum / pageSize
     */
    @GetMapping("/articles")
    public TableDataInfo articles(PortalQuery query) {
        // startPage 之后不能再触发别的查询（PageHelper 会劫持），所以这里只调一次 service
        PageUtils.startPage();
        return PageUtils.getDataTable(portalService.articles(query));
    }

    /** 文章详情（浏览量 +1），只返回已发布文章 */
    @GetMapping("/articles/{articleId}")
    public ResponseVO<BlogArticle> article(@PathVariable Long articleId) {
        BlogArticle article = portalService.viewArticle(articleId);
        return article == null
                ? ResponseVO.error(404, "文章不存在或未发布")
                : ResponseVO.success(article);
    }

    /** 文章评论（正常状态，按楼层正序） */
    @GetMapping("/articles/{articleId}/comments")
    public ResponseVO<List<BlogComment>> comments(@PathVariable Long articleId) {
        return ResponseVO.success(portalService.comments(articleId));
    }

    /** 有已发布文章的分类 */
    @GetMapping("/categories")
    public ResponseVO<List<BlogCategory>> categories() {
        return ResponseVO.success(portalService.categories());
    }

    /** 有已发布文章的标签 */
    @GetMapping("/tags")
    public ResponseVO<List<BlogTag>> tags() {
        return ResponseVO.success(portalService.tags());
    }

    /** 归档：按月份统计已发布文章数 */
    @GetMapping("/archive")
    public ResponseVO<List<PortalArchiveItem>> archive() {
        return ResponseVO.success(portalService.archive());
    }

    /** 热门文章（按浏览量） */
    @GetMapping("/hot")
    public ResponseVO<List<BlogArticle>> hot(@RequestParam(defaultValue = "5") int limit) {
        return ResponseVO.success(portalService.hotArticles(limit));
    }

    /** 最新评论（侧栏） */
    @GetMapping("/comments/latest")
    public ResponseVO<List<BlogComment>> latestComments(@RequestParam(defaultValue = "5") int limit) {
        return ResponseVO.success(portalService.latestComments(limit));
    }
}
