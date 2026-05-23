// 文件说明：BlogImageController 控制器，负责处理博客图片上传和删除。

package com.hmdp.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：upload
@RequestMapping("upload")
// 控制器类：负责接收请求、调用业务层并返回结果
public class BlogImageController {

    // 博客图片上传目录，通过 application.yaml 配置，避免把本机路径写死在代码里
    @Value("${hmdp.upload.image-dir}")
    private String imageUploadDir;

    // 上传博客图片
    @PostMapping("blog")
    public Result uploadBlogImage(@RequestParam("file") MultipartFile file) {
        try {
            // 获取原始文件名称
            String originalFilename = file.getOriginalFilename();
            // 生成新文件名
            String relativePath = createBlogImagePath(originalFilename);
            // 保存文件
            file.transferTo(new File(imageUploadDir, relativePath));
            // 返回结果
            log.debug("文件上传成功，{}", relativePath);
            return Result.ok("/" + relativePath);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    // 删除博客图片
    @GetMapping("/blog/delete")
    public Result deleteBlogImage(@RequestParam("name") String imagePath) {
        String relativePath = StrUtil.removePrefix(imagePath, "/");
        File imageFile = new File(imageUploadDir, relativePath);
        if (imageFile.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(imageFile);
        return Result.ok();
    }

    // 生成分目录的新文件名
    private String createBlogImagePath(String originalFilename) {
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String imageName = UUID.randomUUID().toString();
        int hash = imageName.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        File dir = new File(imageUploadDir, StrUtil.format("blogs/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        return StrUtil.format("blogs/{}/{}/{}.{}", d1, d2, imageName, suffix);
    }

}
