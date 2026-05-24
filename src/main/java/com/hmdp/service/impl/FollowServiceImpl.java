// 文件说明：FollowServiceImpl 业务实现类，真正编排 Follow 模块的业务流程。

package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 业务类：负责处理当前模块的核心业务逻辑
@Service
// 业务实现类：真正编排当前模块的业务流程
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 注入 userService（UserServiceImpl）
    @Resource
    private UserServiceImpl userService;
    // 关注或取关用户
    @Override
    public Result follow(Long followUserId, Boolean shouldFollow) {
        // 1. 当前用户对目标用户进行关注或取关。
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        if(shouldFollow) {
            // 2. 关注：先写数据库，再把关注用户 id 写入 Redis Set。
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean isSuccess = save(follow);
            if(isSuccess){
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        }else {
            // 3. 取关：删除数据库记录，并同步移除 Redis Set 中的关注关系。
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if(isSuccess){
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok();
    }

    // 判断是否已关注用户
    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        // 查询数据库判断当前用户是否关注了目标用户。
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
            return Result.ok(count>0);

    }

    // 查询共同关注
    @Override
    public Result followCommons(Long id) {
        // 1. 当前用户和目标用户的关注列表都维护在 Redis Set 中。
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        String key2 = "follows:" + id;
        // 2. Set 求交集可以快速得到共同关注的用户 id。
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if(intersect==null||intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        // 3. 根据共同关注 id 查询用户详情，并转换成前端需要的 UserDTO。
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        List<UserDTO> userDTOS = userService
                .listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);

    }
}
