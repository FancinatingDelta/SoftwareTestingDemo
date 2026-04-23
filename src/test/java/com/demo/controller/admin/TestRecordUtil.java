package com.demo.controller.admin;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 测试记录工具类 - 统一记录测试输出到文件
 */
public class TestRecordUtil {

    private static final String LOG_FILE = "src/test/ControllerTestRecord/test_execution_log.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        // 初始化时清空文件
        try (PrintWriter writer = new PrintWriter(LOG_FILE)) {
            writer.println("=== 测试执行记录 ===");
            writer.println("开始时间: " + LocalDateTime.now().format(FORMATTER));
            writer.println("==================================================");
            writer.println();
        } catch (IOException e) {
            System.err.println("初始化日志文件失败: " + e.getMessage());
        }
    }

    /**
     * 记录测试信息
     * @param testName 测试名称
     * @param message 记录消息
     */
    public static void record(String testName, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logLine = String.format("[%s] [%s] %s", timestamp, testName, message);

        // 同时输出到控制台和文件
        System.out.println(logLine);

        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter writer = new PrintWriter(fw)) {
            writer.println(logLine);
        } catch (IOException e) {
            System.err.println("写入日志文件失败: " + e.getMessage());
        }
    }

    /**
     * 记录异常信息
     * @param testName 测试名称
     * @param exception 异常对象
     */
    public static void recordException(String testName, Exception exception) {
        String message = String.format("抛出异常 - %s - %s",
                exception.getClass().getSimpleName(),
                exception.getMessage());
        record(testName, message);
    }

    /**
     * 记录正常响应
     * @param testName 测试名称
     */
    public static void recordSuccess(String testName) {
        record(testName, "无异常，返回正常响应");
    }
}
