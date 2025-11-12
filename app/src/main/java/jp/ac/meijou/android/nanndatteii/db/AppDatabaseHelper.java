package jp.ac.meijou.android.nanndatteii.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "app_data.db";
    private static final int DB_VERSION = 1;

    public AppDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // [必須] テーブル作成例
        db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // [必須] バージョンアップ時の処理
        db.execSQL("DROP TABLE IF EXISTS tags");
        onCreate(db);
    }

    public boolean insertTag(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM tags WHERE name = ?", new String[]{name});
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        cursor.close();
        if (exists) {
            return false; // すでに存在する場合は追加しない
        }
        db.execSQL("INSERT INTO tags (name) VALUES (?)", new Object[]{name});
        return true;
    }

    public void deleteTag(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM tags WHERE name = ?", new Object[]{name});
    }

    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM tags", null);
        while (cursor.moveToNext()) {
            tags.add(cursor.getString(0));
        }
        cursor.close();
        return tags;
    }
}