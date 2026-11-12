package cn.CDPersonal.blog.mapper;

import cn.CDPersonal.blog.domain.BlogCategory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 博客分类 Mapper（blog_category）
 *
 * 分类只有「正常 / 停用」两态（status：0=正常、1=停用），表里没有逻辑删除列，删除即物理删除。
 * article_count 不是 blog_category 的字段，而是列表/详情 SQL 里用相关子查询现算的统计值。
 * 列表查询本身不带 LIMIT：分页由 Controller 先调 PageUtils.startPage() 交给 PageHelper 拦截。
 */
@Mapper
public interface BlogCategoryMapper {

    /**
     * 管理端分类列表。
     *
     * article_count 统计该分类下的全部文章、不区分状态，所以草稿与待审核也会被计入；
     * 排序 sort ASC, category_id ASC，sort 相同时用主键兜底，保证翻页时顺序稳定。
     */
    @Select("""
            <script>
            SELECT c.category_id, c.category_name, c.sort, c.status,
                   c.create_by, c.create_time, c.update_by, c.update_time, c.remark,
                   (SELECT COUNT(*) FROM blog_article a WHERE a.category_id = c.category_id) AS article_count
            FROM blog_category c
            <where>
              <if test="categoryName != null and categoryName != ''">
                AND c.category_name LIKE CONCAT('%', #{categoryName}, '%')
              </if>
              <if test="status != null and status != ''">
                AND c.status = #{status}
              </if>
            </where>
            ORDER BY c.sort ASC, c.category_id ASC
            </script>
            """)
    List<BlogCategory> selectCategoryList(BlogCategory query);

    /** 启用的分类（文章编辑页下拉用） */
    // 只有 status = '0' 才算启用；停用分类不会出现在下拉里，但已有文章的 category_id 仍然指向它。
    @Select("""
            SELECT category_id, category_name, sort, status
            FROM blog_category
            WHERE status = '0'
            ORDER BY sort ASC, category_id ASC
            """)
    List<BlogCategory> selectEnabledList();

    // 详情同样带 article_count：Service 的「分类下是否有文章」校验直接复用这条查询，省掉一次 COUNT。
    @Select("""
            SELECT c.category_id, c.category_name, c.sort, c.status,
                   c.create_by, c.create_time, c.update_by, c.update_time, c.remark,
                   (SELECT COUNT(*) FROM blog_article a WHERE a.category_id = c.category_id) AS article_count
            FROM blog_category c
            WHERE c.category_id = #{categoryId}
            """)
    BlogCategory selectCategoryById(@Param("categoryId") Long categoryId);

    /**
     * 分类名是否重复：返回重复条数，0 条即通过（Service 负责转成 boolean 语义）。
     * 新增时 categoryId 为 null，SQL 不拼排除条件；编辑时传入自身 id 以排除自己。
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM blog_category
            WHERE category_name = #{categoryName}
            <if test="categoryId != null">AND category_id != #{categoryId}</if>
            </script>
            """)
    int checkNameUnique(BlogCategory category);

    // create_time 用数据库 NOW() 生成以统一时钟；create_by 由调用方传入，自增主键回填到 categoryId。
    @Insert("""
            INSERT INTO blog_category
              (category_name, sort, status, create_by, create_time, remark)
            VALUES
              (#{categoryName}, #{sort}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "categoryId")
    int insertCategory(BlogCategory category);

    // 全字段覆盖式更新：未传的 sort/status/remark 会被写成 NULL，所以编辑时必须回传完整对象。
    @Update("""
            UPDATE blog_category
            SET category_name = #{categoryName},
                sort = #{sort},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE category_id = #{categoryId}
            """)
    int updateCategory(BlogCategory category);

    // 物理批量删除，不级联处理文章：删除前必须由 Controller 校验分类下没有文章，否则会留下孤儿 category_id。
    @Delete("""
            <script>
            DELETE FROM blog_category WHERE category_id IN
            <foreach collection="array" item="categoryId" open="(" separator="," close=")">
              #{categoryId}
            </foreach>
            </script>
            """)
    int deleteCategoryByIds(@Param("array") Long[] categoryIds);
}
