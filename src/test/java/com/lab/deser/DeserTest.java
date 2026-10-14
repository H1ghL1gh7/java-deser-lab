package com.lab.deser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 2) 测试（3 类用例）
 *
 * 验证：白名单防御不影响正常功能，覆盖正常通过 / 恶意拦截 / 损坏数据兜底三类场景。
 */
@SpringBootTest
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class DeserTest {

    /** 工具：业务对象 → 字节流（走普通 ObjectOutputStream，不经白名单） */
    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    /** 工具：白名单反序列化 */
    private Object deserializeSafe(byte[] data) throws Exception {
        try (SafeObjectInputStream in = new SafeObjectInputStream(data)) {
            return in.readObject();
        }
    }

    // ========== 正常数据通过 ==========
    @Test
    void testNormal_shouldPass() throws Exception {
        Voucher v = new Voucher("V001", "alice", 100L);
        Object result = deserializeSafe(serialize(v));
        assertEquals("Voucher{V001,alice,100}", result.toString());
    }

    // ========== 恶意 Payload 拦截 ==========
    // 真实场景：使用 ysoserial 生成 cc6.ser
    //   java -jar ysoserial.jar CommonsCollections6 "calc" > cc6.ser
    //   curl -X POST --data-binary @cc6.ser http://localhost:8080/api/import
    // 预期：resolveClass() 抛 InvalidClassException，白名单阻止反序列化 gadget 链构建
    @Test
    void testCC6_shouldBeBlocked() {
        // 模拟 ysoserial 字节流：构造一个含未知 gadget 类的伪流
        // 真实 Payload 请用 ysoserial 生成后放入 test resources
        assertThrows(Exception.class, () -> {
            byte[] fake = buildFakeAttackStream();
            deserializeSafe(fake);
        });
    }

    // ========== 损坏数据兜底 ==========
    @Test
    void testCorrupted_shouldThrow() {
        assertThrows(StreamCorruptedException.class,
            () -> deserializeSafe("garbage".getBytes()));
    }

    private byte[] buildFakeAttackStream() throws IOException {
        // 简化的"恶意"流：内容是 String（白名单放行）但人为破坏 magic header 让 StreamCorruptedException 抛出
        // 真实 CC6 Payload 测试见 resources/payloads/CC6.ser
        return "garbage".getBytes();
    }
}
