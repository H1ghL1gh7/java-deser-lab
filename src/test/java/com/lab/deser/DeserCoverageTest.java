package com.lab.deser;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

/**
 * 3) 自动化验证（白名单覆盖率扫描）
 *
 * 反射扫描业务包下所有 Serializable 子类，与白名单做差集——
 * 遗漏即失败，避免白名单"漏配"导致业务类无法反序列化。
 *
 * 集成到构建流程自动运行：mvn test 会执行此测试，
 * CI 上即"白名单与业务类同步"的一道卡口。
 */
class DeserCoverageTest {

    private static final Set<String> WHITELIST = Set.of(
        "java.lang.String",
        "java.util.ArrayList",
        "java.util.HashMap",
        "com.lab.deser.Voucher"
    );

    @Test
    void testWhitelistCoverage() {
        // 反射扫描业务包（com.lab 下的所有 Serializable 子类）
        Reflections reflections = new Reflections("com.lab");
        Set<Class<? extends Serializable>> subTypes =
            reflections.getSubTypesOf(Serializable.class);

        List<String> bizClasses = subTypes.stream()
            .map(Class::getName)
            .filter(name -> !name.contains("$"))   // 排除内部类
            .collect(Collectors.toList());

        List<String> missing = bizClasses.stream()
            .filter(c -> !WHITELIST.contains(c))
            .collect(Collectors.toList());

        assertTrue(missing.isEmpty(),
            "遗漏业务类（白名单必须包含）: " + missing);
    }
}
