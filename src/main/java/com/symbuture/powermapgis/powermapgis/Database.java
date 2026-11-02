/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.symbuture.powermapgis.powermapgis;

/**
 *
 * @author GIS
 */
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class Database {

   private static final Path DB_PATH = Paths.get(
        System.getenv("LOCALAPPDATA"),
        "PowerMapGIS",
        "powermapgis.db"
);

public static Connection getConnection() throws SQLException {
    try {
        Files.createDirectories(DB_PATH.getParent());
    } catch (IOException e) {
        throw new SQLException("تعذر إنشاء مجلد قاعدة البيانات", e);
    }

    return DriverManager.getConnection(
            "jdbc:sqlite:" + DB_PATH.toAbsolutePath()
    );
}

    // إنشاء جدول الطلاب إذا لم يكن موجوداً
    public static void initialize() throws SQLException {

        String sql = """
            CREATE TABLE IF NOT EXISTS students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                mother_name TEXT NOT NULL,
                parent_phone TEXT,
                national_id TEXT,
                residence_card TEXT,
                student_class TEXT NOT NULL,
                section TEXT NOT NULL,
                birth_date TEXT NOT NULL,
                image_path TEXT,
                notes TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
        }
    }}