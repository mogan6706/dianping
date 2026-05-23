// 文件说明：BlogServiceImpl 业务实现类，真正编排 Blog 模块的业务流程。

package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IBlogImageService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

// 业务类：负责处理当前模块的核心业务逻辑
@Service
// 业务实现类：真正编排当前模块的业务流程
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    // 注入 userService（IUserService）
    @Resource
    private IUserService userService;

    // 注入 followService（IFollowService）
    @Resource
    private IBlogImageService followService;

    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // 分页查询热门博客
    @Override
    public Result queryHotBlog(Integer current) {
        // 1. 热门博客按点赞数倒序分页。
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 2. 补充作者信息和当前用户点赞状态，前端展示需要这些冗余字段。
        List<Blog> records = page.getRecords();
        records.forEach(blog -> {
            this.isBlogLiked(blog);
            this.queryBlogUser(blog);
        });
        return Result.ok(records);
    }

    // 点赞或取消点赞博客
    @Override
    public Result updateLike(Long id){
        // 1. 获取当前登录用户，并用博客 id 作为点赞 ZSet 的 key。
        Long userId = UserHolder.getUser().getId();
        String key=BLOG_LIKED_KEY+id;
        // 2. ZSet score 存点赞时间；查到 score 说明当前用户已经点过赞。
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score==null) {
            // 3. 未点赞：先更新数据库点赞数，再把用户 id 写入 Redis ZSet。
            boolean isSuccess = update().setSql("liked=liked+1").eq("id", id).update();
            if(isSuccess){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else {
            // 4. 已点赞：数据库点赞数减一，并从 ZSet 移除用户 id。
            boolean isSuccess = update().setSql("liked=liked-1").eq("id", id).update();
            if(isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    // 查询前五个点赞用户
    @Override
    public Result queryBlogLikes(Long id) {
        // 1. 从 ZSet 取最早点赞的前 5 个用户，score 越小点赞越早。
        String key=BLOG_LIKED_KEY+id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5==null||top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        // 2. 按 Redis 返回顺序组装 id，并用 order by field 保持这个顺序。
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",",ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id",ids).last("order by field(id,"+idStr+")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        //4.返回
        return Result.ok(userDTOS);

    }

    // 保存博客并推送给粉丝
    @Override
    public Result saveBlog(Blog blog) {
        // 1. 博客归属当前登录用户。
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 2. 先保存博客，拿到博客 id 后才能推送到粉丝收件箱。
        boolean isSuccess = save(blog);
        if(!isSuccess){
           return Result.fail("新增笔记失败");
        }
        // 3. 查询作者的所有粉丝。
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 4. 推模式：把博客 id 写入每个粉丝的收件箱 ZSet，score 使用发布时间。
        for (Follow follow : follows) {
            Long userId = follow.getUserId();
            String key=FEED_KEY+userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    // 分页查询关注推送
    @Override
    public Result queryBlogOfFollow(Long maxTime, Integer offset) {
        // 1. 获取当前用户收件箱，ZSet 中 value 是 blogId，score 是推送时间。
        Long userId = UserHolder.getUser().getId();
        String key=FEED_KEY+userId;
        // 2. 按时间倒序滚动分页；maxTime 是上次返回的最小时间，offset 处理同一时间戳的重复数据。
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, maxTime, offset, 2);
        if(typedTuples==null||typedTuples.isEmpty()){
            return Result.ok();
        }
        // 3. 解析 blogId，并计算下一页需要的 minTime 和 offset。
        List<Long> ids=new ArrayList<>(typedTuples.size());
        long minTime=0;
        int os=1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //3.1.获取id
            ids.add(Long.valueOf( typedTuple.getValue()));
            //3.2.获取分数（时间戳）
            long time=typedTuple.getScore().longValue();
            if(time==minTime){
                os++;
            }else {
                minTime = time;
                os = 1;
            }

        }
        // 4. 根据 id 批量查询博客，并保持 Redis 中的排序。
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query()
                .in("id", ids).last("order by field(id," + idStr + ")").list();

        for (Blog blog : blogs) {
            // 5. 补充作者信息和当前用户点赞状态。
            queryBlogUser(blog);
            isBlogLiked(blog);
        }
        // 6. 返回滚动分页结果，前端下次请求会带上 minTime 和 offset。
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);

    }

    // 根据 id 查询博客
    @Override
    public Result  queryBlogById(Long id) {
        // 1. 查询博客主体。
        Blog blog = getById(id);
        if(blog==null){
            return Result.fail("博客不存在");
        }
        // 2. 补充作者信息和当前用户点赞状态。
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    // 判断当前用户是否点赞
    private void isBlogLiked(Blog blog) {
        //1.获取当前用户
        UserDTO user = UserHolder.getUser();
        if(user==null){
            //用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();

        // 2. 当前用户 id 存在于点赞 ZSet 中，就表示已经点赞。
        String key=BLOG_LIKED_KEY+blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }

    // 查询博客作者信息
    private void queryBlogUser(Blog blog) {
        // 博客表只存 userId，这里把用户昵称和头像补到 Blog 对象里给前端展示。
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

}
