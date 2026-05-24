// 文件说明：VoucherOrderControllerTest 测试类，用来验证 Voucher Order Controller Test 相关逻辑是否符合预期。

package com.hmdp;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;

@SpringBootTest
@AutoConfigureMockMvc
class VoucherOrderControllerTest {
    // token 输出文件，供 JMeter 或其他压测工具读取。
    private static final File TOKEN_OUTPUT_FILE = new File(
            System.getProperty("user.dir")
                    + File.separator
                    + "src"
                    + File.separator
                    + "test"
                    + File.separator
                    + "resources"
                    + File.separator
                    + "tokens.txt"
    );


    // MockMvc 可以在不启动真实浏览器的情况下，直接调用 Spring MVC 接口。
    @Resource
    private MockMvc mockMvc;

    // 用来从数据库读取用户手机号，批量生成登录 token。
    @Resource
    private IUserService userService;

    // Jackson 的 JSON 工具，用来把登录参数转成 JSON，或者把接口返回 JSON 转成 Result。
    @Resource
    private ObjectMapper mapper;

    // 验证码接口会把验证码写入 Redis，批量登录时需要从 Redis 取出验证码。
    @Resource
    private StringRedisTemplate stringRedisTemplate;



    @Test
    @SneakyThrows
    @DisplayName("登录1000个用户，并输出到文件中")
    void login() {
        // 1. 从用户表取 1000 个手机号；这里只查询 phone 字段，减少不必要的数据读取。
        List<String> phoneList = userService.query()
                // MyBatis-Plus 3.4.3 在 JDK 17 下解析 User::getPhone 可能触发模块访问限制，这里改用字段名查询。
                .select("phone")
                .last("limit 1000")
                .list().stream().map(User::getPhone).collect(Collectors.toList());
        // 2. 为每个手机号启动一个任务，并发调用验证码和登录接口。
        ExecutorService executorService = ThreadUtil.newExecutor(phoneList.size());
        // 多线程同时写 token，用线程安全的 List。
        List<String> tokenList = new CopyOnWriteArrayList<>();
        // CountDownLatch 用来等待所有手机号都完成登录流程后，再继续写文件。
        CountDownLatch countDownLatch = new CountDownLatch(phoneList.size());
        phoneList.forEach(phone -> {
            executorService.execute(() -> {
                try {
                    // 3. 调用 /user/code 获取验证码；这里通过 MockMvc 直接调用 Controller。
                    String codeJson = mockMvc.perform(MockMvcRequestBuilders
                                    .post("/user/code")
                                    .queryParam("phone", phone))
                            .andExpect(MockMvcResultMatchers.status().isOk())
                            .andReturn().getResponse().getContentAsString();
                    Result result = mapper.readerFor(Result.class).readValue(codeJson);
                    Assert.isTrue(result.getSuccess(), String.format("获取“%s”手机号的验证码失败", phone));
                    String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
                    Assert.notBlank(code, String.format("Redis 中没有找到“%s”手机号的验证码", phone));
                    // 4. 组装登录请求体：手机号 + 刚刚获取到的验证码。
                    LoginFormDTO formDTO = LoginFormDTO.builder().code(code).phone(phone).build();
                    String json = mapper.writeValueAsString(formDTO);
                    // 5. 调用 /user/login 获取 token；压测秒杀接口时，每个虚拟用户需要一个 token。
                    String tokenJson = mockMvc.perform(MockMvcRequestBuilders
                                    .post("/user/login").content(json).contentType(MediaType.APPLICATION_JSON))
                            .andExpect(MockMvcResultMatchers.status().isOk())
                            .andReturn().getResponse().getContentAsString();
                    result = mapper.readerFor(Result.class).readValue(tokenJson);
                    Assert.isTrue(result.getSuccess(), String.format("获取“%s”手机号的token失败,json为“%s”", phone, json));
                    String token = result.getData().toString();
                    tokenList.add(token);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 当前手机号处理完，计数减一；放在 finally 里避免单个任务异常导致主线程一直等待。
                    countDownLatch.countDown();
                }
            });
        });
        // 6. 等待所有并发登录任务结束。
        countDownLatch.await();
        executorService.shutdown();
        // 7. 确保每个手机号都生成了 token。
        Assert.isTrue(tokenList.size() == phoneList.size());
        // 8. 把 token 写入文件，供 JMeter 或其他压测工具读取。
        writeToTxt(tokenList);
        System.out.println("写入完成：" + TOKEN_OUTPUT_FILE.getAbsolutePath());
    }

    private static void writeToTxt(List<String> list) throws Exception {
        // 1. 创建输出目录和文件；当前写到 src/test/resources/tokens.txt。
        File parent = TOKEN_OUTPUT_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!TOKEN_OUTPUT_FILE.exists()) {
            TOKEN_OUTPUT_FILE.createNewFile();
        }
        // 2. 按行写入 token，一行一个。
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(TOKEN_OUTPUT_FILE), StandardCharsets.UTF_8));
        for (String content : list) {
            bw.write(content);
            bw.newLine();
        }
        bw.close();
        System.out.println("写入完成！");
    }
}
