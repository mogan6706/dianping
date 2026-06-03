import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// 这个脚本用于压测秒杀下单接口：
// POST /voucher-order/seckill/{voucherId}
//
// 设计目标：
// 1. 从 token 文件里读取 10000 个不同用户的 token；
// 2. 每次迭代使用一个不同 token；
// 3. 让这些用户同时抢同一个秒杀券；
// 4. 通过 k6 指标观察成功下单、库存不足、重复下单等结果。
//
// 注意：
// - /voucher-order/** 需要登录，所以 token 必须是真实可用的 Redis 登录态 token。
// - 如果秒杀券库存小于 USERS，只有一部分请求会成功，其余请求返回“库存不足”是正常的。
// - 如果 token 文件里用户不足 USERS 个，脚本会直接停止，避免多个请求复用同一个用户。

// 被测服务地址。
// BASE_URL=http://localhost:8081 k6 run k6/seckill-voucher.js
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

// 被抢的秒杀券 id。
// VOUCHER_ID=11 k6 run k6/seckill-voucher.js
const VOUCHER_ID = __ENV.VOUCHER_ID || '11';

// 参与秒杀的用户总数，也是总请求数。
// USERS=10000 k6 run k6/seckill-voucher.js
const USERS = Number(__ENV.USERS || 1000);

// 并发虚拟用户数。
// 默认等于 USERS，也就是尽量模拟 10000 个用户同时发起一次请求。
const VUS = Number(__ENV.VUS || USERS);

// 整个压测最多运行多久。
const MAX_DURATION = __ENV.MAX_DURATION || '2m';

// 每次请求后是否休眠。秒杀压测默认不休眠。
const SLEEP = Number(__ENV.SLEEP || 0);

// token 文件路径。一行一个 token。
// 可以显式指定：
// TOKEN_FILE=/Users/morgan/Desktop/dianping/src/test/resources/tokens.txt k6 run k6/seckill-voucher.js
const TOKEN_FILE = __ENV.TOKEN_FILE || '';

// 自定义指标：比单纯看 HTTP 200 更有意义。
// 秒杀接口通常 HTTP 都是 200，真正成功或失败要看 Result.success 和 errorMsg。
const accepted = new Counter('seckill_accepted');
const businessFailed = new Counter('seckill_business_failed');
const stockNotEnough = new Counter('seckill_stock_not_enough');
const duplicated = new Counter('seckill_duplicated');
const stockNotInitialized = new Counter('seckill_stock_not_initialized');
const unexpectedResult = new Counter('seckill_unexpected_result');

const tokenFileResult = new SharedArray('seckill tokens', () => readTokensFromFiles());
const TOKENS = tokenFileResult[0].tokens;
const TOKEN_SOURCE = tokenFileResult[0].file;

export const options = {
  scenarios: {
    seckill: {
      // 每个 VU 只执行 1 次，更符合“每个用户只抢一次”的秒杀场景。
      // 比 shared-iterations 更适合这里：shared-iterations 太快时，k6 可能还没启动满 VU 就跑完了。
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: MAX_DURATION,
    },
  },
  thresholds: {
    // HTTP 层失败率必须小于 1%。
    http_req_failed: ['rate<0.01'],
    // 秒杀接口只是 Redis 预检 + 发 MQ，正常应该很快。
    http_req_duration: ['p(95)<1000'],
  },
};

export function setup() {
  if (VUS !== USERS) {
    throw new Error(
      `当前脚本要求 VUS 和 USERS 相等，保证每个用户只请求一次。` +
        `当前 USERS=${USERS}，VUS=${VUS}。`
    );
  }

  if (TOKENS.length < USERS) {
    throw new Error(
      `token 数量不足：需要 ${USERS} 个，实际只有 ${TOKENS.length} 个。` +
        '请先生成足够多的登录 token，或调小 USERS。'
    );
  }

  console.log(`使用 token 文件：${TOKEN_SOURCE}`);
  console.log(`秒杀券 id：${VOUCHER_ID}，用户数：${USERS}，并发 VU：${VUS}`);
}

export default function () {
  // vu.idInTest 从 1 开始，每个 VU 对应一个用户 token。
  // 因为本脚本设置了每个 VU 只执行 1 次，所以可以保证一个用户只抢一次。
  const index = exec.vu.idInTest - 1;
  const token = TOKENS[index];

  const res = http.post(`${BASE_URL}/voucher-order/seckill/${VOUCHER_ID}`, null, {
    headers: {
      authorization: token,
    },
    tags: {
      api: 'voucher-order-seckill',
      voucher_id: String(VOUCHER_ID),
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response is json result': (r) => {
      try {
        return r.json('success') !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  recordBusinessResult(res);

  if (SLEEP > 0) {
    sleep(SLEEP);
  }
}

function recordBusinessResult(res) {
  let success;
  let errorMsg;

  try {
    success = res.json('success');
    errorMsg = res.json('errorMsg') || '';
  } catch (e) {
    unexpectedResult.add(1);
    return;
  }

  if (success === true) {
    accepted.add(1);
    return;
  }

  businessFailed.add(1);

  if (errorMsg.includes('库存不足')) {
    stockNotEnough.add(1);
    return;
  }
  if (errorMsg.includes('重复下单')) {
    duplicated.add(1);
    return;
  }
  if (errorMsg.includes('库存未初始化')) {
    stockNotInitialized.add(1);
    return;
  }

  unexpectedResult.add(1);
}

function normalizeToken(token) {
  if (!token) {
    return '';
  }
  return token.replace(/^Bearer\s+/i, '').trim();
}

function parseTokens(content) {
  if (!content) {
    return [];
  }

  const tokens = [];
  const seen = {};

  for (const line of content.split(/\r?\n/)) {
    const token = normalizeToken(line);
    if (!token || seen[token]) {
      continue;
    }
    seen[token] = true;
    tokens.push(token);
  }

  return tokens;
}

function readTokensFromFiles() {
  const files = TOKEN_FILE
    ? [TOKEN_FILE]
    : [
        // k6 通常会按脚本所在目录解析相对路径，所以从 k6/seckill-voucher.js 到项目根目录要先 ../。
        '../src/test/resources/tokens.txt',
        '../target/test-classes/tokens.txt',
        // 保留这两个路径，兼容从项目根目录解析的情况。
        'src/test/resources/tokens.txt',
        'target/test-classes/tokens.txt',
      ];

  for (const file of files) {
    try {
      const tokens = parseTokens(open(file));
      if (tokens.length > 0) {
        return [{ tokens, file }];
      }
    } catch (e) {
      // 当前候选路径不存在就试下一个。
    }
  }

  return [{ tokens: [], file: '' }];
}
