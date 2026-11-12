package cn.CDPersonal.blog.mapper;

import cn.CDPersonal.blog.domain.BlogTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 博客标签 Mapper（blog_tag）
 *
 * 标签没有状态列，也没有逻辑删除列：删除即物理删除，且必须先清理 blog_article_tag 关联（见本接口末尾方法）。
 * article_count 由关联表现算，不是 blog_tag 的字段；列表查询不带 LIMIT，分页由调用方 startPage 控制。
 */
@Mapper
public interface BlogTagMapper {

    /**
     * 管理端标签列表。
     *
     * tagName 只做模糊匹配，空串不参与过滤；article_count 统计引用该标签的全部文章，不区分发布状态。
     */
    @Select("""
            <script>
            SELECT t.tag_id, t.tag_name, t.sort,
                   t.create_by, t.create_time, t.update_by, t.update_time, t.remark,
                   (SELECT COUNT(*) FROM blog_article_tag at2 WHERE at2.tag_id = t.tag_id) AS article_count
            FROM blog_tag t
            <where>
              <if test="tagName != null and tagName != ''">
                AND t.tag_name LIKE CONCAT('%', #{tagName}, '%')
              </if>
            </where>
            ORDER BY t.sort ASC, t.tag_id ASC
            </script>
            """)
    List<BlogTag> selectTagList(BlogTag query);

    /** 全部标签（文章编辑页下拉用） */
    // 不分页也不筛选：标签量级小，多选需要一次取全量；与分类下拉不同，这里没有启停状态可过滤。
    @Select("""
            SELECT tag_id, tag_name, sort
            FROM blog_tag
            ORDER BY sort ASC, tag_id ASC
            """)
    List<BlogTag> selectAllList();

    @Select("""
            SELECT t.tag_id, t.tag_name, t.sort,
                   t.create_by, t.create_time, t.update_by, t.update_time, t.remark,
                   (SELECT COUNT(*) FROM blog_article_tag at2 WHERE at2.tag_id = t.tag_id) AS article_count
            FROM blog_tag t
            WHERE t.tag_id = #{tagId}
            """)
    BlogTag selectTagById(@Param("tagId") Long tagId);

    /**
     * 标签名是否重复：返回重复条数，0 条即通过。
     * 新增时 tagId 为 null，SQL 不拼排除条件；编辑时传入自身 id 以排除自己。
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM blog_tag
            WHERE tag_name = #{tagName}
            <if test="tagId != null">AND tag_id != #{tagId}</if>
            </script>
            """)
    int checkNameUnique(BlogTag tag);

    // create_time 用数据库 NOW() 生成；自增主键回填到 tagId，供后续插入关联表使用。
    @Insert("""
            INSERT INTO blog_tag
              (tag_name, sort, create_by, create_time, remark)
            VALUES
              (#{tagName}, #{sort}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "tagId")
    int insertTag(BlogTag tag);

    // 全字段覆盖式更新：未传的 sort/remark 会被写成 NULL，所以编辑时必须回传完整对象。
    @Update("""
            UPDATE blog_tag
            SET tag_name = #{tagName},
                sort = #{sort},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE tag_id = #{tagId}
            """)
    int updateTag(BlogTag tag);

    // 只删 blog_tag 本体；关联清理交给下一个方法，两步必须由 Service 放在同一事务里执行。
    @Delete("""
            <script>
            DELETE FROM blog_tag WHERE tag_id IN
            <foreach collection="array" item="tagId" open="(" separator="," close=")">
              #{tagId}
            </foreach>
            </script>
            """)
    int deleteTagByIds(@Param("array") Long[] tagIds);

    /** 删除标签时同步清理文章-标签关联 */
    // 两表之间没有外键级联，顺序必须「先关联、后本体」；反了会留下指向已删标签的中间表记录。
    @Delete("""
            <script>
            DELETE FROM blog_article_tag WHERE tag_id IN
            <foreach collection="array" item="tagId" open="(" separator="," close=")">
              #{tagId}
            </foreach>
            </script>
            """)
    int deleteArticleTagByTagIds(@Param("array") Long[] tagIds);
}
