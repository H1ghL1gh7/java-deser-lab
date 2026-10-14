package com.lab.deser;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 安全审查 + 失败处理：白名单放行业务必需类，gadget 入口类（Transformer/Invoker 等）一律拒绝
 *
 * 关键取舍（vs 黑名单拦截已知 gadget 类）：
 * - 黑名单：被动，新 gadget 出现即失效
 * - 白名单：主动，需配套覆盖率测试（见 DeserCoverageTest）弥补维护成本
 *
 * 失败处理：非白名单类记录类名后抛 InvalidClassException（可追溯、非静默跳过）。
 */
public class SafeObjectInputStream extends ObjectInputStream {

    private static final Logger log = LoggerFactory.getLogger(SafeObjectInputStream.class);

    // 白名单排除 Transformer/Invoker 等 gadget 入口类
    private static final Set<String> WHITELIST = Set.of(
        "java.lang.String",
        "java.util.ArrayList",
        "java.util.HashMap",
        // 业务类按需补全：
        "com.lab.deser.Voucher"
    );

    public SafeObjectInputStream(byte[] data) throws IOException {
        super(new java.io.ByteArrayInputStream(data));
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        if (!WHITELIST.contains(desc.getName())) {
            // 失败处理：记录类名，抛 InvalidClassException（非静默跳过）
            log.warn("[BLOCKED] {}", desc.getName());
            throw new InvalidClassException("Blocked: " + desc.getName());
        }
        return super.resolveClass(desc);
    }
}
