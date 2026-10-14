# Java 反序列化漏洞攻防实验

> 个人安全研究 | 2025.06–2025.07
> 简历中"Java 反序列化漏洞攻防实践"条目对应源码、测试与利用链分析

## 漏洞复现

- 使用 ysoserial 生成 CC1/CC5/CC6 Payload，通过 HTTP POST 投递至 `/api/import` 触发 RCE
- 三条 Payload 全部触发（基线确认）
- CC6 利用链调用链：
  ```
  HashSet.readObject()
    → TiedMapEntry.hashCode()
      → LazyMap.get()
        → ChainedTransformer.transform()
          → InvokerTransformer → Runtime.exec()
  ```

## 防御实现（关键取舍）

| 方案 | 优点 | 缺点 | 选择 |
|---|---|---|---|
| 黑名单拦截已知 gadget 类 | 实现简单 | 被动，新 gadget 出现即失效 | ✗ |
| **白名单仅放行业务必需类** | 主动，零信任 | 需配套覆盖率测试 | **✓** |

**白名单**：
- 重写 `ObjectInputStream.resolveClass()`：非白名单类记录类名 + 抛 `InvalidClassException`
- Controller 层统一错误响应不泄露堆栈
- 覆盖率测试：反射扫描业务包全部 `Serializable` 子类与白名单做差集，遗漏即失败

## 测试结果

| 用例 | 期望 | 结果 |
|---|---|---|
| 正常业务数据（Voucher） | 通过 | ✅ |
| CC1 Payload | 拦截（InvalidClassException） | ✅ |
| CC5 Payload | 拦截 | ✅ |
| CC6 Payload | 拦截 | ✅ |
| 损坏数据 | 兜底（StreamCorruptedException） | ✅ |
| 覆盖率扫描 | 业务类全在白名单 | ✅ |
| 正常数据误拦截 | 0 | ✅ |

## 复现命令

```bash
# 1. 启动应用
mvn spring-boot:run

# 2. 生成 ysoserial Payload（CC6）
java -jar ysoserial.jar CommonsCollections6 "calc" > payloads/cc6.ser

# 3. 投递利用（无防御时触发 calc；本工程有防御，会返回 400）
curl -i -X POST --data-binary @payloads/cc6.ser http://localhost:8080/api/import

# 4. 跑全部测试
mvn test
```

## 仓库结构

```
java-deser-lab/
├── pom.xml
├── README.md
├── src/main/java/com/lab/deser/
│   ├── DeserApplication.java        # 启动类
│   ├── DeserController.java         # /api/import 入口 + 失败处理
│   ├── SafeObjectInputStream.java   # 白名单 resolveClass
│   └── Voucher.java                 # 业务样例类
├── src/test/java/com/lab/deser/
│   ├── DeserTest.java               # 3 类用例
│   └── DeserCoverageTest.java       # 覆盖率扫描
└── payloads/                        # ysoserial 生成的 .ser（运行时填充）
```

## 工程化落地

- 覆盖率测试集成到 `mvn test` 自动运行
- Controller 层 `try/catch` 三分支（InvalidClass / IOException / ClassNotFoundException），失败处理不泄露堆栈
- Code Review 检查点：确认无 `readResolve` 嵌套绕过
