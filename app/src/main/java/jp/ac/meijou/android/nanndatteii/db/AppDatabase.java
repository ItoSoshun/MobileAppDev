package jp.ac.meijou.android.nanndatteii.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import jp.ac.meijou.android.nanndatteii.db.dao.FileDao;
import jp.ac.meijou.android.nanndatteii.db.dao.ItemDao;
import jp.ac.meijou.android.nanndatteii.db.dao.ItemTagDao;
import jp.ac.meijou.android.nanndatteii.db.dao.TagDao;
import jp.ac.meijou.android.nanndatteii.db.entity.Item;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemFile;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemTag;
import jp.ac.meijou.android.nanndatteii.db.entity.Tag;

@Database(
    entities = {
        Item.class,
        ItemFile.class,
        Tag.class,
        ItemTag.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract ItemDao itemDao();
    public abstract FileDao fileDao();
    public abstract TagDao tagDao();
    public abstract ItemTagDao itemTagDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "app_data.db"
                    )
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // 初期データの挿入（オプション）
                            // 必要に応じてデフォルトタグなどを挿入できます
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
