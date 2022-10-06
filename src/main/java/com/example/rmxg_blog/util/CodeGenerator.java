package com.example.rmxg_blog.util;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.util.Collections;

/**
 * @author 泥偶
 */
public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://47.101.202.81:3306/rmxg_blog", "root", "qQ@2191826738!")
                //全局配置
                .globalConfig(builder -> {
                    builder.author("rmxg") // 设置作者
                            .enableSwagger() // 开启 swagger 模式
                            .outputDir(System.getProperty("user.dir") + "/src/main/java") // 指定输出目录
                            .commentDate("yyyy-MM-dd hh:mm:ss")
                            .dateType(DateType.ONLY_DATE)
//                            .fileOverride() //覆盖写入
                            .disableOpenDir();
                })
                //包配置
                .packageConfig(builder -> {
                    builder.parent("com.example") // 设置父包名
                            .moduleName("rmxg_blog") // 设置父包模块名
                            .controller("controller")
                            .service("service")
                            .mapper("mapper")
                            .xml("mapper")
                            .entity("entity")
                            .serviceImpl("impl")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper")); // 设置mapperXml生成路径
                })
                //策略配置
                .strategyConfig(builder -> {
                    builder.addInclude("ad",
                                    "ad_type",
                                    "admin",
                                    "article",
                                    "article_tag",
                                    "article_tag_list",
                                    "article_type",
                                    "comment",
                                    "comment_reply",
                                    "link",
                                    "upload_file_list",
                                    "user",
                                    "user_collection_article") // 设置需要生成的表名
//                            .addTablePrefix("t_", "c_") // 设置过滤表前缀

                            //mapper策略配置
                            .mapperBuilder()
                            .superClass(BaseMapper.class) //设置父类
                            .formatMapperFileName("%sMapper") //格式化文件名称
                            .enableMapperAnnotation() //开启@Mpper注解
                            .formatXmlFileName("%sXml") //格式化Xml文件

                            //service策略配置
                            .serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")

                            //实体类配置策略
                            .entityBuilder()
                            .enableLombok()
                            .disableSerialVersionUID()
                            .logicDeleteColumnName("deleted")
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .addTableFills(
                                    new Column("create_time", FieldFill.INSERT),
                                    new Column("update_time", FieldFill.INSERT_UPDATE)
                            )
                            .enableTableFieldAnnotation()

                            //controller策略配置
                            .controllerBuilder()
                            .formatFileName("%sController")
                            .enableRestStyle();

                })
                //模板引擎
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
