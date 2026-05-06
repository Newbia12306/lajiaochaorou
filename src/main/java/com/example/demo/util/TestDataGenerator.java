package com.example.demo.util;

import com.github.javafaker.Faker;
import java.sql.*;
import java.util.Locale;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class TestDataGenerator {

    private static final String URL = "jdbc:postgresql://localhost:5432/demo1";
    private static final String USER = "postgres";
    private static final String PASSWORD = "123456";

    private static final String[] SPICY_LEVELS = {"NOT_SPICY", "MEDIUM_SPICY", "EXTRA_SPICY"};
    private static Set<String> usedDishNames = new HashSet<>(); // 用于确保名称唯一

    public static void main(String[] args) {
        Faker faker = new Faker(new Locale("zh-CN"));
        Connection conn = null;
        Random random = new Random();

        try {
            // 1. 建立数据库连接
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            conn.setAutoCommit(false);

            // 2. 准备SQL语句
            String sql = "INSERT INTO menu (name, spicy, is_signature, is_deleted) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            int batchSize = 100;
            int totalRecords = 300;
            int successfullyInserted = 0;

            System.out.println("开始生成菜单测试数据...");

            for (int i = 1; i <= totalRecords; i++) {
                String dishName = generateUniqueChineseDishName(faker, random);
                String spicy = SPICY_LEVELS[random.nextInt(SPICY_LEVELS.length)];
                boolean isSignature = random.nextDouble() < 0.2;
                boolean isDeleted = random.nextDouble() < 0.05;

                pstmt.setString(1, dishName);
                pstmt.setString(2, spicy);
                pstmt.setBoolean(3, isSignature);
                pstmt.setBoolean(4, isDeleted);

                pstmt.addBatch();

                // 每 batchSize 条执行一次批处理
                if (i % batchSize == 0) {
                    try {
                        int[] results = pstmt.executeBatch();
                        conn.commit();
                        successfullyInserted += results.length;
                        System.out.println("已插入 " + i + " 条记录，本批次成功: " + results.length + " 条");
                    } catch (BatchUpdateException e) {
                        System.err.println("批处理插入失败，尝试逐条插入...");
                        // 如果批处理失败，尝试逐条插入
                        successfullyInserted += handleBatchFailure(pstmt, conn, i - batchSize + 1, i);
                    }
                }
            }

            // 执行最后剩余的批处理
            System.out.println("执行最后一批数据插入...");
            try {
                int[] finalResults = pstmt.executeBatch();
                conn.commit();
                successfullyInserted += finalResults.length;
                System.out.println("最后一批成功插入: " + finalResults.length + " 条");
            } catch (BatchUpdateException e) {
                System.err.println("最后一批插入失败，尝试逐条插入...");
                successfullyInserted += handleBatchFailure(pstmt, conn, (totalRecords / batchSize) * batchSize + 1, totalRecords);
            }

            System.out.println("菜单数据插入完成！共成功插入 " + successfullyInserted + " 条记录。");

            pstmt.close();
            conn.close();

        } catch (SQLException e) {
            System.err.println("数据库操作出错: " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("已回滚事务");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 处理批处理失败的情况：逐条插入
     */
    private static int handleBatchFailure(PreparedStatement pstmt, Connection conn, int start, int end) throws SQLException {
        int successCount = 0;
        System.out.println("尝试逐条插入第 " + start + " 到 " + end + " 条记录...");

        // 这里需要重新准备单条插入的语句
        String singleSql = "INSERT INTO menu (name, spicy, is_signature, is_deleted) VALUES (?, ?, ?, ?)";
        PreparedStatement singleStmt = conn.prepareStatement(singleSql);

        // 注意：这里简化处理，实际应该记录哪些记录失败了
        // 这里我们假设从start到end都重新插入
        for (int i = start; i <= end; i++) {
            try {
                // 重新生成数据（因为之前的数据可能已经丢失）
                Faker faker = new Faker(new Locale("zh-CN"));
                Random random = new Random();
                String dishName = generateUniqueChineseDishName(faker, random);
                String spicy = SPICY_LEVELS[random.nextInt(SPICY_LEVELS.length)];
                boolean isSignature = random.nextDouble() < 0.2;
                boolean isDeleted = random.nextDouble() < 0.05;

                singleStmt.setString(1, dishName);
                singleStmt.setString(2, spicy);
                singleStmt.setBoolean(3, isSignature);
                singleStmt.setBoolean(4, isDeleted);

                singleStmt.executeUpdate();
                successCount++;
            } catch (SQLException e) {
                System.err.println("第 " + i + " 条记录插入失败: " + e.getMessage());
            }
        }

        conn.commit();
        singleStmt.close();
        System.out.println("逐条插入完成，成功插入 " + successCount + " 条记录");
        return successCount;
    }

    /**
     * 生成唯一的中文菜品名称
     */
    private static String generateUniqueChineseDishName(Faker faker, Random random) {
        String dishName;
        int attempt = 0;

        do {
            dishName = generateChineseDishName(faker, random);
            attempt++;
            // 如果尝试多次还是重复，添加随机数确保唯一性
            if (attempt > 5) {
                dishName += "_" + random.nextInt(10000);
            }
        } while (usedDishNames.contains(dishName) && attempt < 10);

        usedDishNames.add(dishName);
        return dishName;
    }

    private static String generateChineseDishName(Faker faker, Random random) {
        String[] cookingMethods = {"红烧", "清蒸", "爆炒", "干煸", "水煮", "麻辣", "香辣", "糖醋", "椒盐", "蒜蓉",
                "油炸", "凉拌", "白灼", "酱爆", "葱爆", "铁板", "宫保", "鱼香"};

        String[] mainIngredients = {"牛肉", "鸡肉", "猪肉", "羊肉", "鱼肉", "虾仁", "豆腐", "茄子", "土豆", "青菜",
                "蘑菇", "鸡蛋", "排骨", "鸡翅", "鸭肉", "螃蟹", "贝类", "面条", "米饭"};

        String[] suffixes = {"", "煲", "丝", "片", "块", "球", "卷", "饼", "汤", "饭", "面", "粥", "拼盘"};

        String method = cookingMethods[random.nextInt(cookingMethods.length)];
        String ingredient = mainIngredients[random.nextInt(mainIngredients.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];

        if (random.nextDouble() < 0.3) {
            String secondIngredient = mainIngredients[random.nextInt(mainIngredients.length)];
            while (secondIngredient.equals(ingredient)) {
                secondIngredient = mainIngredients[random.nextInt(mainIngredients.length)];
            }
            return method + ingredient + "配" + secondIngredient + suffix;
        } else {
            return method + ingredient + suffix;
        }
    }
}