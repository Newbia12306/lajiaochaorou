package com.example.demo.util;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class TestDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(TestDataGenerator.class);

    // Fix: read credentials from environment variables, no hardcoded passwords
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/demo1");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    private static final String[] SPICY_LEVELS = {"NOT_SPICY", "MEDIUM_SPICY", "EXTRA_SPICY"};
    private static final Set<String> usedDishNames = new HashSet<>();

    // Fix: reuse Random instance instead of creating new ones repeatedly
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        if (PASSWORD.isEmpty()) {
            log.error("DB_PASSWORD environment variable must be set");
            System.exit(1);
        }

        Faker faker = new Faker(new Locale("zh-CN"));

        // Fix: use try-with-resources for Connection
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            String sql = "INSERT INTO menu (name, spicy, is_signature, is_deleted) VALUES (?, ?, ?, ?)";

            // Fix: use try-with-resources for PreparedStatement
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int batchSize = 100;
                int totalRecords = 300;
                int successfullyInserted = 0;

                log.info("开始生成菜单测试数据...");

                for (int i = 1; i <= totalRecords; i++) {
                    String dishName = generateUniqueChineseDishName(faker);
                    String spicy = SPICY_LEVELS[RANDOM.nextInt(SPICY_LEVELS.length)];
                    boolean isSignature = RANDOM.nextDouble() < 0.2;
                    boolean isDeleted = RANDOM.nextDouble() < 0.05;

                    pstmt.setString(1, dishName);
                    pstmt.setString(2, spicy);
                    pstmt.setBoolean(3, isSignature);
                    pstmt.setBoolean(4, isDeleted);
                    pstmt.addBatch();

                    if (i % batchSize == 0) {
                        try {
                            int[] results = pstmt.executeBatch();
                            conn.commit();
                            successfullyInserted += results.length;
                            log.info("已插入 {} 条记录，本批次成功: {} 条", i, results.length);
                        } catch (BatchUpdateException e) {
                            log.warn("批处理插入失败，尝试逐条插入...");
                            successfullyInserted += handleBatchFailure(conn, i - batchSize + 1, i);
                        }
                    }
                }

                // Execute remaining batch
                log.info("执行最后一批数据插入...");
                try {
                    int[] finalResults = pstmt.executeBatch();
                    conn.commit();
                    successfullyInserted += finalResults.length;
                    log.info("最后一批成功插入: {} 条", finalResults.length);
                } catch (BatchUpdateException e) {
                    log.warn("最后一批插入失败，尝试逐条插入...");
                    successfullyInserted += handleBatchFailure(conn,
                            (totalRecords / batchSize) * batchSize + 1, totalRecords);
                }

                log.info("菜单数据插入完成！共成功插入 {} 条记录。", successfullyInserted);
            }

        } catch (SQLException e) {
            log.error("数据库操作出错: {}", e.getMessage(), e);
            // Connection is auto-closed by try-with-resources, rollback handled
        }
    }

    /**
     * Handle batch insert failure by inserting records one by one.
     */
    private static int handleBatchFailure(Connection conn, int start, int end) throws SQLException {
        int successCount = 0;
        log.info("尝试逐条插入第 {} 到 {} 条记录...", start, end);

        String singleSql = "INSERT INTO menu (name, spicy, is_signature, is_deleted) VALUES (?, ?, ?, ?)";

        // Fix: use try-with-resources for PreparedStatement
        try (PreparedStatement singleStmt = conn.prepareStatement(singleSql)) {
            Faker faker = new Faker(new Locale("zh-CN"));

            for (int i = start; i <= end; i++) {
                try {
                    String dishName = generateUniqueChineseDishName(faker);
                    String spicy = SPICY_LEVELS[RANDOM.nextInt(SPICY_LEVELS.length)];
                    boolean isSignature = RANDOM.nextDouble() < 0.2;
                    boolean isDeleted = RANDOM.nextDouble() < 0.05;

                    singleStmt.setString(1, dishName);
                    singleStmt.setString(2, spicy);
                    singleStmt.setBoolean(3, isSignature);
                    singleStmt.setBoolean(4, isDeleted);
                    singleStmt.executeUpdate();
                    successCount++;
                } catch (SQLException e) {
                    log.error("第 {} 条记录插入失败: {}", i, e.getMessage());
                }
            }

            conn.commit();
        }

        log.info("逐条插入完成，成功插入 {} 条记录", successCount);
        return successCount;
    }

    /**
     * Generate a unique Chinese dish name.
     */
    private static String generateUniqueChineseDishName(Faker faker) {
        String dishName;
        int attempt = 0;

        do {
            dishName = generateChineseDishName(faker);
            attempt++;
            if (attempt > 5) {
                dishName += "_" + RANDOM.nextInt(10000);
            }
        } while (usedDishNames.contains(dishName) && attempt < 10);

        usedDishNames.add(dishName);
        return dishName;
    }

    private static String generateChineseDishName(Faker faker) {
        String[] cookingMethods = {"红烧", "清蒸", "爆炒", "干煸", "水煮", "麻辣", "香辣", "糖醋", "椒盐", "蒜蓉",
                "油炸", "凉拌", "白灼", "酱爆", "葱爆", "铁板", "宫保", "鱼香"};

        String[] mainIngredients = {"牛肉", "鸡肉", "猪肉", "羊肉", "鱼肉", "虾仁", "豆腐", "茄子", "土豆", "青菜",
                "蘑菇", "鸡蛋", "排骨", "鸡翅", "鸭肉", "螃蟹", "贝类", "面条", "米饭"};

        String[] suffixes = {"", "煲", "丝", "片", "块", "球", "卷", "饼", "汤", "饭", "面", "粥", "拼盘"};

        String method = cookingMethods[RANDOM.nextInt(cookingMethods.length)];
        String ingredient = mainIngredients[RANDOM.nextInt(mainIngredients.length)];
        String suffix = suffixes[RANDOM.nextInt(suffixes.length)];

        if (RANDOM.nextDouble() < 0.3) {
            String secondIngredient = mainIngredients[RANDOM.nextInt(mainIngredients.length)];
            while (secondIngredient.equals(ingredient)) {
                secondIngredient = mainIngredients[RANDOM.nextInt(mainIngredients.length)];
            }
            return method + ingredient + "配" + secondIngredient + suffix;
        } else {
            return method + ingredient + suffix;
        }
    }
}
