package com.lab.deser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 反序列化漏洞攻防实验
 *
 * 简历中 "Java 反序列化漏洞攻防实践" 条目对应工程。
 *
 * 实验目标：
 * 1. 使用 ysoserial 生成 CC1/CC5/CC6 Payload 投递至 /api/import 复现 RCE
 * 2. 部署白名单防御（SafeObjectInputStream）并验证拦截效果
 * 3. 自动化覆盖率测试确保业务类与白名单不脱节
 */
@SpringBootApplication
public class DeserApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeserApplication.class, args);
    }
}
