package com.lab.deser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 4) DevSecOps：Controller 层失败处理
 *
 * 三类异常分支：
 * - InvalidClassException：白名单拒绝 → 400 + 统一错误（不泄露堆栈）
 * - IOException / ClassNotFoundException：I/O 兜底 → 500 + 统一错误
 * - 正常业务类 → 200 + 业务处理结果
 */
@RestController
public class DeserController {

    private static final Logger log = LoggerFactory.getLogger(DeserController.class);

    @PostMapping("/api/import")
    public ResponseEntity<?> importData(@RequestBody byte[] data) {
        try {
            return ResponseEntity.ok(
                process(new SafeObjectInputStream(data).readObject()));
        } catch (InvalidClassException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "数据不合法"));
        } catch (IOException | ClassNotFoundException e) {
            log.error("反序列化失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "处理失败"));
        }
    }

    private String process(Object obj) {
        return "imported: " + obj;
    }

    /** 工具：把业务对象序列化成可 POST 的字节流（供测试与演示用） */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }
}
