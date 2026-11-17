# PlayEdu 二次开发：学习轨迹与学员管理

基于 PlayEdu 2.3 开源版进行二次开发，在保留原有培训功能的基础上，为 H5 学员端增加学习轨迹、分页加载和界面优化，并扩展管理后台的学员手机号、身份证号管理能力。

二次开发分支：`main`

## 二次开发功能

### 学习轨迹

- 当前登录学员查看自己的已完成课时。
- 展示课程名称、课时名称、课时类型和完成时间。
- 按完成时间倒序排列，并按 `YYYY-MM` 年月分组。
- 后端同时返回当前页记录和完成记录总数。
- 学员 ID 由后端登录上下文获取，不信任前端传入的 `userId`。
- 没有记录时展示空状态。

### 分页加载

- 默认每页 20 条，首次进入加载第一页。
- 点击“加载更多”后保留旧记录，并追加下一页。
- 加载期间防止重复点击。
- 全部加载后显示“没有更多了”。

### H5 界面优化

- 学习轨迹返回栏固定在顶部，滚动后仍可直接返回。
- “我的 → 更多”弹层中，切换部门、学习轨迹、更换头像、修改密码位于同一个白色卡片。
- 退出登录保留下方独立卡片。
- 保留原项目的文字、样式风格和既有操作逻辑。

### 管理员学员管理

- 创建、编辑学员时可填写手机号和身份证号，两个字段均允许为空。
- 手机号按中国大陆 11 位号码格式校验，数据库使用可空唯一索引，列表支持完整手机号精确筛选。
- 身份证号仅支持 18 位号码，后端校验格式、真实出生日期和校验码。
- 身份证号保存前去除首尾空格，并将尾号 `X` 统一为大写。
- 学员列表的手机号、身份证号列可显示或隐藏，搜索栏与表格使用一致的左右边界。
- 学员学习记录支持通过完整身份证号筛选。

### 敏感信息保护

- 身份证号使用 `AES/GCM/NoPadding` 加密后存入 `users.id_card`，不保存明文。
- 每次加密使用 12 字节随机 IV 和 128 位认证标签，密文采用带 `v1:` 前缀的 Base64 格式。
- 使用 `HmacSHA256` 生成 64 位十六进制盲索引，存入 `users.id_card_bidx`，支持完整身份证号精确查询。
- 盲索引使用独立域分隔串，不与身份证号密文混用；该字段不会序列化到前端。
- 姓名、邮箱、手机号、身份证号分别受管理员数据权限控制，无权限时输出脱敏内容。
- 管理操作日志会屏蔽密码、Token 和身份证号等敏感字段。

## 功能入口

```text
H5 学员端 → 我的 → 右上角更多 → 学习轨迹
```

页面路由为 `/learning-tracks`，需要学员登录。

## 学习轨迹接口

### 请求

```http
GET /api/v1/user/learning-tracks?page=1&size=20
```

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `page` | 否 | `1` | 页码，从 1 开始 |
| `size` | 否 | `20` | 每页记录数 |

接口不接收 `userId`。后端通过 `FCtx.getId()` 获取当前登录学员，避免查询其他学员的记录。

### 响应示例

```json
{
  "code": 0,
  "data": {
    "data": [
      {
        "record_id": 45,
        "course_id": 1,
        "course_title": "示例课程",
        "hour_id": 45,
        "hour_title": "示例课时",
        "hour_type": "video",
        "finished_at": "2026-11-20 09:00:00"
      }
    ],
    "total": 45
  }
}
```

`data.data` 是当前页数组，`data.total` 是完成记录总数。

## 数据查询规则

学习轨迹关联 `user_course_hour_records`、`course_hour` 和 `courses`，只查询：

```sql
r.user_id = 当前登录学员ID
AND r.is_finished = 1
AND r.finished_at IS NOT NULL
```

列表按 `r.finished_at DESC` 排序。分页偏移量为：

```text
pageStart = (page - 1) * size
```

总数查询使用相同的学员和完成状态条件，但不应用分页。

## 主要改动位置

后端：

```text
playedu-api/playedu-api/src/main/java/xyz/playedu/api/controller/frontend/UserController.java
playedu-api/playedu-course/src/main/java/xyz/playedu/course/domain/UserLearningTrack.java
playedu-api/playedu-course/src/main/java/xyz/playedu/course/mapper/UserCourseHourRecordMapper.java
playedu-api/playedu-course/src/main/java/xyz/playedu/course/service/UserCourseHourRecordService.java
playedu-api/playedu-course/src/main/java/xyz/playedu/course/service/impl/UserCourseHourRecordServiceImpl.java
playedu-api/playedu-course/src/main/resources/mapper/UserCourseHourRecordMapper.xml
```

H5 学员端：

```text
playedu-h5/src/api/user.ts
playedu-h5/src/pages/learning-tracks/index.tsx
playedu-h5/src/pages/learning-tracks/index.module.scss
playedu-h5/src/pages/member/index.tsx
playedu-h5/src/pages/member/index.module.scss
playedu-h5/src/routes/index.tsx
playedu-h5/src/playedu.d.ts
```

管理后台：

```text
playedu-admin/src/pages/member/index.tsx
playedu-admin/src/pages/member/compenents/create.tsx
playedu-admin/src/pages/member/compenents/update.tsx
playedu-admin/src/pages/member/index.module.less
playedu-admin/src/api/user.ts
```

学员管理后端：

```text
playedu-api/playedu-api/src/main/java/xyz/playedu/api/request/backend/UserRequest.java
playedu-api/playedu-common/src/main/java/xyz/playedu/common/domain/User.java
playedu-api/playedu-common/src/main/java/xyz/playedu/common/service/impl/UserServiceImpl.java
playedu-api/playedu-common/src/main/java/xyz/playedu/common/util/IdCardCrypto.java
playedu-api/playedu-common/src/main/java/xyz/playedu/common/util/IdCardValidator.java
playedu-api/playedu-common/src/main/java/xyz/playedu/common/constant/BPermissionConstant.java
playedu-api/playedu-common/src/main/resources/mapper/UserMapper.xml
playedu-api/playedu-course/src/main/resources/mapper/UserCourseRecordMapper.xml
playedu-api/playedu-system/src/main/java/xyz/playedu/system/aspectj/AdminLogAspect.java
```

数据库升级与旧数据迁移：

```text
docker/mysql/upgrade/20261105_add_mobile_to_users.sql
docker/mysql/upgrade/20261110_encrypt_id_card.sql
playedu-api/playedu-system/src/main/java/xyz/playedu/system/checks/IdCardMigrationRunner.java
```

## 技术栈

Java 17、Spring Boot 3.3.4、MyBatis-Plus、MySQL 8、React 18、TypeScript、Vite 4、Ant Design Mobile、Docker Compose。

## 本地构建

### 后端

Windows：

```bash
cd playedu-api
./mvnw.cmd -DskipTests package
```

Linux 或 macOS：

```bash
cd playedu-api
./mvnw -DskipTests package
```

可执行 JAR 位于 `playedu-api/playedu-api/target/playedu-api.jar`。

`-DskipTests` 只跳过测试执行，不代表测试已经通过。需要执行测试时使用 `./mvnw test`。

### H5 学员端

```bash
cd playedu-h5
corepack pnpm install
VITE_APP_URL=/api/ corepack pnpm build
```

构建产物位于 `playedu-h5/dist`。

Windows Git Bash 需要防止 MSYS 将 `/api/` 转换为 Windows 路径：

```bash
cd playedu-h5
export VITE_APP_URL='/api/'
export MSYS2_ENV_CONV_EXCL='VITE_APP_URL'
corepack.cmd pnpm build
```

如果构建后的 JavaScript 出现 `D:/Program Files/Git/api/`，必须修正环境变量并重新构建，不能直接部署。

## Docker Compose 部署

```bash
cp .env.example .env
docker compose up -d --build
```

根据实际环境修改 `.env`。生产环境应更换数据库密码、JWT 密钥和身份证加密密钥，不要提交真实 `.env`、Token 或数据库备份。

### 身份证加密配置

Docker Compose 使用以下环境变量：

```dotenv
PLAYEDU_ID_CARD_ENCRYPTION_KEY=<Base64编码的32字节密钥>
PLAYEDU_ID_CARD_MIGRATION_ENABLED=false
```

密钥解码后必须正好为 32 字节，可使用 `openssl rand -base64 32` 生成。密钥为空、Base64 格式错误或长度不正确时，后端会拒绝启动。真实密钥只保存在部署环境的 `.env`，不要写入 README、Git、聊天记录或测试数据。更换密钥前必须制定既有密文的迁移方案，不能直接覆盖。

数据库升级文件位于 `docker/mysql/upgrade`。已有环境升级前必须备份并核对现有字段、索引和数据；当前二开虚拟机已完成手机号、身份证号字段升级及旧数据迁移，不要重复执行这些 SQL 或迁移 Runner。正常运行时保持 `PLAYEDU_ID_CARD_MIGRATION_ENABLED=false`，不要删除数据库数据卷。

| 端 | 默认地址 |
| --- | --- |
| PC 学员端 | `http://localhost:9800` |
| H5 学员端 | `http://localhost:9801` |
| 管理后台 | `http://localhost:9900` |
| API | `http://localhost:9700` |

H5 使用 `VITE_APP_URL=/api/`。经过既有 Nginx 代理规则后，API 函数 `/api/v1/...` 会以 `/api/api/v1/...` 形式发出请求，这是当前项目的正常路径规则。

## 联调检查清单

### 空状态与分页

1. 没有完成记录的学员应看到总数 0 和空状态。
2. 使用 45 条跨月份记录时，首次显示 20 条。
3. 第一次加载更多后累计 40 条，第二次累计 45 条。
4. 加载下一页时旧记录不能消失。
5. 不同月份归入对应的 `YYYY-MM` 分组。
6. 全部加载后显示“没有更多了”。
7. 换另一名学员登录，确认只能看到自己的记录。

### 最近学习与课程进度

这些页面与学习轨迹的数据依赖并不完全相同：

- 学习轨迹读取课时完成记录。
- 最近学习还依赖课时记录的 `updated_at`。
- 课程级进度读取 `user_course_records`。
- 课程列表还受课程显示状态和指派范围影响。

只插入课时完成记录不能完整模拟其他页面。测试数据应分别核对依赖，不应随意修改真实业务数据。

### 学员管理

1. 创建和编辑学员时，分别验证空手机号、合法手机号、格式错误手机号和重复手机号。
2. 使用合法测试身份证号验证保存和精确筛选，确认数据库中只有密文与盲索引。
3. 分别使用有、无敏感字段数据权限的管理员，确认列表展示和脱敏结果。
4. 检查管理日志，确认请求参数和响应结果不记录身份证号明文。
5. 确认学员列表的搜索栏、表格左右边界一致，列显示设置可正常保存。

身份证校验、加密盲索引和日志脱敏已有以下单元测试：

```text
playedu-api/playedu-common/src/test/java/xyz/playedu/common/util/IdCardCryptoTest.java
playedu-api/playedu-common/src/test/java/xyz/playedu/common/util/IdCardValidatorTest.java
playedu-api/playedu-system/src/test/java/xyz/playedu/system/aspectj/AdminLogAspectTest.java
```

这些测试文件的存在不等同于当前环境已经执行通过，发布前仍需运行测试并保留结果。

## 使用注意事项

- 桌面浏览器访问 H5 时，如果 `system-pc-url` 非空，原项目会跳转到 PC 学员端。调试 H5 可使用手机模拟模式。
- 测试账号、Mock 数据脚本、数据库备份和私有部署脚本不纳入本仓库。
- 不要在生产环境直接执行测试数据 SQL。
- 不要提交真实密钥、学员隐私数据或数据卷备份。
- 当前未新增学习轨迹专项自动化测试，部署前仍应完成后端测试、H5 构建和浏览器联调。

---

# PlayEdu 原项目说明

<p align="center"><a href="https://www.sslbear.com?from=playedu">SSLBear - 云服务域名证书守护者|7x24小时监护让域名证书永不过期</a></p>

<p align="center">
<img src="https://meedu.cloud.oss.meedu.vip/playedu/%E5%A4%B4%E5%9B%BE.jpg"/>
</p>

<h4 align="center">
  <a href="http://www.playeduos.com">PlayEdu官网</a> |
  <a href="https://www.playeduos.com/function.html">PlayEdu商业版</a> |
  <a href="https://faq.playeduos.com/opensource-maintenance-handbook/article/t08o2iHfLR">部署文档</a> |
  <a href="https://www.playeduos.com/demo.html">开源版演示站</a>
</h4>

PlayEdu 是由白书科技团队经营多年线上教培领域打造出的一款业内领先的线上培训解决方案。PlayEdu 基于 Java + MySQL 开发，采用前后端分离模式，前端核心框架为 React18，后端核心框架为 SpringBoot3。开源版本提供部门管理、学员管理、在线视频学习、学员进度追踪、视频私有化存储等基础培训功能。  
**针对企业级培训场景，我们精心打造了“功能更多、响应更快、并发更强”的企业版本，满足企业多样化的培训需求。企业版本支持音视频学习、文档在线预览、线上考试、学习任务等多种学习方式，并提供多重安全防护，如视频转码加密、防盗链、学习防快进、防挂机等。同时，我们集成了企业微信、钉钉、飞书等主流办公系统，帮助企业快速部署专属培训平台！**

## 🚀 快速上手

拉取代码：

```
git clone --branch main https://gitee.com/playeduxyz/playedu.git playedu
```

构建镜像：

```
cd playedu && docker-compose up -d
```

命令执行完成以后，打开您的浏览器，输入 `http://localhost:9900` 即可访问后台管理界面，默认管理员账号和密码 `admin@playedu.xyz / playedu` 。

- PC 端口 `http://localhost:9800`
- H5 端口 `http://localhost:9801`
- API 端口 `http://localhost:9700`

## 🔰️ 软件安全

安全问题应该通过邮件私下报告给 tengyongzhi@playeduos.com。 您将在 24 小时内收到回复，如果因为某些原因您没有收到回复，请通过回复原始邮件的方式跟进，以确保我们收到了您的原始邮件。

## 👁 界面预览

![学员端口界面预览](https://meedu.cloud.oss.meedu.vip/playedu/%E5%89%8D%E5%8F%B0%E9%A1%B5%E9%9D%A2.jpg)

![管理后台界面预览](https://meedu.cloud.oss.meedu.vip/playedu/%E5%90%8E%E5%8F%B0%E9%A1%B5%E9%9D%A2.jpg)

## 📃 使用须知

- **1.版权归属**： 杭州白书科技有限公司对 PlayEdu 开源版拥有完整版权，所有使用权保留。
- **2.代码修改**： 在遵守相关开源协议的严格前提下，允许对 PlayEdu 开源版代码进行修改。修改时，必须在代码中加入明确备注，详细记录每一处修改的具体内容。
- **3.版权保护**： 严令禁止删除、修改或篡改源代码中的版权信息及开源说明文件，侵犯版权的行为将面临法律追究。
- 在任何使用场景下，必须严格保留 PlayEdu 开源版页面及代码中的原有版权信息，包括不限于 “Designed By PlayEdu” 页面版权标识、官网链接以及代码中的开源说明等，一旦出现侵犯版权的行为，将承担相应法律责任。
