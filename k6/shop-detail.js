import http from 'k6/http';
import { check, sleep } from 'k6';

// 这个脚本用于压测商铺详情接口：
// GET /shop/{id}
//
// 当前项目中该接口会走：
// ShopController.queryShopById()
// -> ShopServiceImpl.queryById()
// -> CacheClient.queryWithLogicalExpire()
//
// 所以这个脚本主要测试“逻辑过期缓存方案”在高并发读取时的表现。
// 注意：/shop/** 在 MvcConfig 中放行了，不需要 token。

// 被测服务地址。可通过环境变量覆盖：
// BASE_URL=http://localhost:8081 k6 run k6/shop-detail.js
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

// 被压测的商铺 id。默认压测 /shop/1。
// SHOP_ID=2 k6 run k6/shop-detail.js
const SHOP_ID = __ENV.SHOP_ID || '1';

// VUS = Virtual Users，虚拟并发用户数。
// VUS=100 k6 run k6/shop-detail.js
const VUS = Number(__ENV.VUS || 50);

// 压测持续时间。
// DURATION=60s k6 run k6/shop-detail.js
const DURATION = __ENV.DURATION || '5s';

// 每个虚拟用户每次请求后休眠多久。默认 0，表示尽可能快地连续请求。
// SLEEP=1 表示每个 VU 每次请求后停 1 秒。
const SLEEP = Number(__ENV.SLEEP || 0);

// k6 压测配置。
export const options = {
  // 多少个虚拟用户同时循环执行 default function。
  vus: VUS,
  // 压测多久。
  duration: DURATION,
  thresholds: {
    // 请求失败率必须小于 1%。
    http_req_failed: ['rate<0.01'],
    // 95% 的请求耗时必须小于 500ms。
    http_req_duration: ['p(95)<500'],
  },
};

// default function 是 k6 每个虚拟用户会反复执行的函数。
export default function () {
  // 发送 GET /shop/{SHOP_ID} 请求。
  const res = http.get(`${BASE_URL}/shop/${SHOP_ID}`, {
    // tags 只用于 k6 结果归类，方便以后按接口或缓存策略过滤指标。
    tags: {
      api: 'shop-detail',
      cache_strategy: 'logical-expire',
    },
  });

  // check 是断言：请求返回后检查结果是否符合预期。
  // 这些断言会体现在 k6 输出里的 checks_succeeded / checks_failed。
  check(res, {
    // HTTP 层成功。
    'status is 200': (r) => r.status === 200,

    // 业务层成功：Result.success == true。
    'business success': (r) => {
      try {
        return r.json('success') === true;
      } catch (e) {
        return false;
      }
    },

    // 返回体里存在商铺数据，说明不是空响应。
    'has shop data': (r) => {
      try {
        return r.json('data.id') !== undefined && r.json('data.id') !== null;
      } catch (e) {
        return false;
      }
    },
  });

  // 默认不休眠，方便测最大吞吐量。
  // 如果想模拟真实用户访问节奏，可以设置 SLEEP。
  if (SLEEP > 0) {
    sleep(SLEEP);
  }
}
