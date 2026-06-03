import http from 'k6/http';
import { check, sleep } from 'k6';

// 这个脚本用于测试登录链路和登录态接口。
//
// 登录接口流程：
// 1. POST /user/code?phone=手机号 发送验证码
// 2. 后端把验证码写入 Redis，并打印到后端日志
// 3. POST /user/login 提交手机号 + 验证码，返回 JWT token
// 4. 带 authorization 请求头访问 /user/me
//
// 注意：你的项目不会把验证码返回给接口响应，所以 k6 无法自动拿到验证码。
// 需要你从后端日志或 Redis 里拿到验证码，然后用 LOGIN_CODE 环境变量传进来。

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const PHONE = __ENV.PHONE || '13800138000';
const LOGIN_CODE = __ENV.LOGIN_CODE || '';
// 默认读取测试代码生成的 token 文件，一行一个 token。
// 如果要换路径，再用 TOKEN_FILE=/path/to/tokens.txt 覆盖。
//
// 注意：k6 的 open() 解析相对路径时容易受脚本目录影响，所以这里会自动尝试多个路径。
const TOKEN_FILE = __ENV.TOKEN_FILE || '';
const TOKEN_FROM_ENV = __ENV.TOKEN || '';
const SEND_CODE = (__ENV.SEND_CODE || 'false').toLowerCase() === 'true';
const VUS = Number(__ENV.VUS || 1);
const DURATION = __ENV.DURATION || '5s';
const SLEEP = Number(__ENV.SLEEP || 1);

const tokenFileResult = readTokenFromFiles();
const tokenFromFile = tokenFileResult.token;

export const options = {
  vus: VUS,
  duration: DURATION,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

// setup 只在压测开始前执行一次。
// 这里负责准备 token：优先用 TOKEN，其次用 TOKEN_FILE，最后才用 PHONE + LOGIN_CODE 登录。
export function setup() {
  const existingToken = normalizeToken(TOKEN_FROM_ENV || tokenFromFile);
  if (existingToken) {
    if (!TOKEN_FROM_ENV && tokenFileResult.file) {
      console.log(`使用 token 文件：${tokenFileResult.file}`);
    }
    return { token: existingToken };
  }

  if (!LOGIN_CODE) {
    if (!SEND_CODE) {
      throw new Error(
        '没有读取到 token。请确认 token 文件存在，或显式指定：' +
          'TOKEN_FILE=/Users/morgan/Desktop/dianping/src/test/resources/tokens.txt k6 run k6/auth-login.js'
      );
    }

    // 没传验证码时，先调用发送验证码接口，方便你去后端日志或 Redis 里拿验证码。
    const codeRes = http.post(`${BASE_URL}/user/code?phone=${PHONE}`);
    check(codeRes, {
      'send code status is 200': (r) => r.status === 200,
      'send code success': (r) => {
        try {
          return r.json('success') === true;
        } catch (e) {
          return false;
        }
      },
    });

    throw new Error(
      '验证码已发送。请从后端日志或 Redis 获取验证码后重新运行：' +
        `LOGIN_CODE=验证码 PHONE=${PHONE} k6 run k6/auth-login.js`
    );
  }

  const loginRes = http.post(
    `${BASE_URL}/user/login`,
    JSON.stringify({
      phone: PHONE,
      code: LOGIN_CODE,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );

  const loginOk = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login success': (r) => {
      try {
        return r.json('success') === true;
      } catch (e) {
        return false;
      }
    },
    'login has token': (r) => {
      try {
        return !!r.json('data');
      } catch (e) {
        return false;
      }
    },
  });

  if (!loginOk) {
    throw new Error(`登录失败：${loginRes.body}`);
  }

  return {
    token: normalizeToken(loginRes.json('data')),
  };
}

// default function 会被每个虚拟用户反复执行。
// 这里用 token 请求 /user/me，验证登录态是否可用。
export default function (data) {
  const res = http.get(`${BASE_URL}/user/me`, {
    headers: {
      authorization: data.token,
    },
    tags: {
      api: 'user-me',
      auth: 'jwt',
    },
  });

  check(res, {
    'me status is 200': (r) => r.status === 200,
    'me business success': (r) => {
      try {
        return r.json('success') === true;
      } catch (e) {
        return false;
      }
    },
    'me has user id': (r) => {
      try {
        return r.json('data.id') !== undefined && r.json('data.id') !== null;
      } catch (e) {
        return false;
      }
    },
  });

  if (SLEEP > 0) {
    sleep(SLEEP);
  }
}

function normalizeToken(token) {
  if (!token) {
    return '';
  }
  return token.replace(/^Bearer\s+/i, '').trim();
}

function firstToken(content) {
  if (!content) {
    return '';
  }
  const lines = content.split(/\r?\n/);
  for (const line of lines) {
    const token = normalizeToken(line);
    if (token) {
      return token;
    }
  }
  return '';
}

function readTokenFromFiles() {
  const files = TOKEN_FILE
    ? [TOKEN_FILE]
    : [
        // k6 通常会按脚本所在目录解析相对路径，所以从 k6/auth-login.js 到项目根目录要先 ../。
        '../src/test/resources/tokens.txt',
        '../target/test-classes/tokens.txt',
        // 保留这两个路径，兼容从项目根目录解析的情况。
        'src/test/resources/tokens.txt',
        'target/test-classes/tokens.txt',
      ];

  for (const file of files) {
    try {
      const token = firstToken(open(file));
      if (token) {
        return { token, file };
      }
    } catch (e) {
      // 当前候选路径不存在就试下一个。
    }
  }
  return { token: '', file: '' };
}
