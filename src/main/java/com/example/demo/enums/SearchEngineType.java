package com.example.demo.enums;

public enum SearchEngineType {
    DATABASE("database", "数据库搜索"),
    ELASTICSEARCH("elasticsearch", "Elasticsearch搜索"),
    PG_VECTOR("pg_vector", "PG Vector语义搜索");

    private final String code;
    private final String description;

    SearchEngineType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SearchEngineType fromCode(String code) {
        for (SearchEngineType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的搜索引擎类型: " + code);
    }
}
