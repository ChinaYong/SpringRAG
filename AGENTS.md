# SpringRAG 编码代理指引

## 安全与协作

不允许读取`secrets.properties`文件，里面存放了项目需要使用的密钥。

- 禁读规则覆盖所有目录中的同名副本；不得通过全文搜索、配置转储或日志间接获取、输出密钥。需要确认凭据时，由用户在本地检查，不要求把密钥发到聊天中。
- 遵循[学习协作规则](.agents/rules/study-with-exercise.md)：默认中文，说明做法、原因和易错点
- 开始新方向前参考[开发路线](.agents/rules/project-framework-and-analysis.md)和[需求分析](项目需求分析.md)。文档中的版本、阶段勾选和规划不等于当前实现，事实以源码和构建配置为准。

## 构建与验证

- 这是 Maven/Spring Boot 后端，使用 JDK 25；依赖版本以 [pom.xml](pom.xml) 为准。不要因需求文档提到 Vue 就假设已有前端工程。
- 在仓库根目录的 PowerShell 使用 Maven Wrapper；首次运行需要可用网络下载 Maven/依赖，离线模式仅适用于已缓存环境。

```powershell
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd -DskipTests test-compile
```

- 上述命令只检查编译，不证明应用启动或外部服务连接正常。
- [现有测试](src/test/java/com/aiplus/spring_rag/SpringRagApplicationTests.java)使用完整 Spring 上下文，并有写固定 Redis key、插入固定用户名的测试，缺少清理；未经确认隔离环境，不直接运行完整 `test`。
- 仅检查上下文时可用 `.\mvnw.cmd "-Dtest=SpringRagApplicationTests#contextLoads" test`，仍需 MySQL、Redis、Qdrant 和必要模型配置；文件向量化还会调用 Embedding 服务，可能产生费用。
- [SQL 建表脚本](SQL/spring_rag.sql)含 `DROP TABLE`，不是增量迁移；不得作为默认验证步骤执行。

## 关键实现边界
- [FileService](src/main/java/com/aiplus/spring_rag/service/FileService.java)用 SHA-256 唯一约束处理物理文件去重；保留原子引用计数和存储字段局部更新，避免整体更新覆盖并发变化。用户归属存于 `user_file_info`，不能把首次上传者当作共享文件的唯一拥有者。
- [FileHandleService](src/main/java/com/aiplus/spring_rag/service/FileHandleService.java)通过独立 Bean 的 `@Async("fileHandleExecutor")` 执行解析、分块和 `vectorStore.add`；不要改成同 Bean 内自调用而使异步失效。
- 增加文件类型必须同时检查入口校验、[解析策略](src/main/java/com/aiplus/spring_rag/service/FileParser.java)、[分块策略](src/main/java/com/aiplus/spring_rag/service/FileSplitter.java)及文件归属元数据。允许上传不代表支持完整向量化；当前 Markdown 解析也不代表存在 Markdown 分块策略。
- [进度服务](src/main/java/com/aiplus/spring_rag/service/FileProgressService.java)以 `taskId` 表示任务、以 `fileId` 共享处理进度；不要混用数据库数字状态、上传响应小写状态和 SSE 大写状态。[SSE 连接管理](src/main/java/com/aiplus/spring_rag/manager/SseEmitterManager.java)仅在进程内保存连接，重连补最新快照，不提供历史重放或跨实例广播。
- [JWT 拦截器](src/main/java/com/aiplus/spring_rag/interceptor/JwtInterceptor.java)当前读取原始 `Authorization` token，不剥离 `Bearer `。用户身份从服务端请求属性获取；修改文件、任务或智能体接口时分别核对归属，不信任客户端传入的用户 ID。
- 修改持久化逻辑前对照[表结构](SQL/spring_rag.sql)与实体/Mapper，注意 JSON 字段映射、联合主键及非空约束；不要假设磁盘、MySQL、Redis、Qdrant 操作具有整体事务回滚。

## 排错约定

- 先区分编译失败、Spring 上下文初始化失败和外部服务调用失败，再沿 `Caused by` 定位具体原因；给出一个最小检查验证判断，不凭顶层 Bean 创建失败信息下结论。
- 相对文件路径基于应用进程工作目录，不保证基于源码目录或仓库根目录；先确认启动工作目录，再判断配置导入和存储路径。