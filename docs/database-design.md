# データベース設計書

## 設計思想

アプリのデータベース設計は、以下の原則に基づいています:

1. **アイテム中心の設計**: 複数のファイルを一つの「アイテム」として管理
2. **フラットな構造**: 階層を持たず、タグによる柔軟な分類
3. **メタデータ重視**: 投稿日時・閲覧日時などの情報を保持
4. **拡張性**: 将来の機能追加を見据えた設計

## 技術スタック

### Room Database

**Room** を使用してデータベースを実装しています。

**Roomの特徴:**
1. **型安全性**: コンパイル時のSQLクエリ検証
2. **LiveData統合**: リアクティブなUI更新が容易
3. **メンテナンス性**: ボイラープレートコードの削減
4. **Androidベストプラクティス**: Google推奨のJetpack Architecture Component

---

## データベーススキーマ

### ER図

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Items     │         │  Item_Tags   │         │    Tags     │
│             │         │ (中間テーブル) │         │             │
├─────────────┤         ├──────────────┤         ├─────────────┤
│ id (PK)     │◄───┐    │ item_id (FK) │    ┌───►│ id (PK)     │
│ title       │    │    │ tag_id (FK)  │────┘    │ name        │
│ description │    │    └──────────────┘         │ color       │
│ created_at  │    │                              │ created_at  │
│ updated_at  │    │                              └─────────────┘
│ last_viewed │    │
└─────────────┘    │
                   │
                   │    ┌──────────────┐
                   └────┤    Files     │
                        │              │
                        ├──────────────┤
                        │ id (PK)      │
                        │ item_id (FK) │
                        │ file_path    │
                        │ file_name    │
                        │ file_type    │
                        │ file_size    │
                        │ mime_type    │
                        │ created_at   │
                        └──────────────┘
```

---

## テーブル定義

### 1. Items テーブル

アプリに「投げられた」一つの投稿単位を表します。複数のファイルやタグを持つことができます。

| カラム名      | 型           | NULL | デフォルト      | 説明                                     |
|--------------|-------------|------|----------------|------------------------------------------|
| id           | INTEGER     | NO   | AUTO INCREMENT | 主キー                                   |
| title        | TEXT        | YES  | NULL           | アイテムのタイトル（オプション）          |
| description  | TEXT        | YES  | NULL           | アイテムの説明（メモ内容など）            |
| created_at   | INTEGER     | NO   | CURRENT_TIME   | 作成日時（Unix timestamp）                |
| updated_at   | INTEGER     | NO   | CURRENT_TIME   | 更新日時（Unix timestamp）                |
| last_viewed  | INTEGER     | YES  | NULL           | 最終閲覧日時（Unix timestamp）            |

**インデックス:**
- `created_at` (DESC): 作成日時での検索を高速化
- `last_viewed` (DESC): 閲覧履歴での検索を高速化

**Entity実装:**

```java
@Entity(tableName = "items",
        indices = {
            @Index(value = "created_at"),
            @Index(value = "last_viewed")
        })
public class Item {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private String description;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "last_viewed")
    private Long lastViewed;

    // getter, setter
}
```

---

### 2. Files テーブル

各アイテムに紐づくファイル（写真、メモテキスト、その他ファイル）を管理します。

| カラム名      | 型           | NULL | デフォルト      | 説明                                     |
|--------------|-------------|------|----------------|------------------------------------------|
| id           | INTEGER     | NO   | AUTO INCREMENT | 主キー                                   |
| item_id      | INTEGER     | NO   | -              | アイテムID（外部キー）                    |
| file_path    | TEXT        | NO   | -              | ファイルの相対パス                        |
| file_name    | TEXT        | NO   | -              | ファイル名（UUID）                        |
| file_type    | TEXT        | NO   | -              | ファイル種別（IMAGE/TEXT/VIDEO/DOCUMENT/OTHER）|
| file_size    | INTEGER     | NO   | 0              | ファイルサイズ（バイト）                  |
| mime_type    | TEXT        | NO   | -              | MIMEタイプ（image/jpeg, text/plain等）    |
| created_at   | INTEGER     | NO   | CURRENT_TIME   | 作成日時（Unix timestamp）                |

**外部キー制約:**
- `item_id` REFERENCES Items(id) ON DELETE CASCADE

**インデックス:**
- `item_id`: アイテムIDでの検索を高速化
- `file_type`: ファイルタイプでの検索を高速化

**Entity実装:**

```java
@Entity(tableName = "files",
        foreignKeys = @ForeignKey(
            entity = Item.class,
            parentColumns = "id",
            childColumns = "item_id",
            onDelete = ForeignKey.CASCADE
        ),
        indices = {
            @Index(value = "item_id"),
            @Index(value = "file_type")
        })
public class ItemFile {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "item_id")
    private long itemId;

    @ColumnInfo(name = "file_path")
    @NonNull
    private String filePath;

    @ColumnInfo(name = "file_name")
    @NonNull
    private String fileName;

    @ColumnInfo(name = "file_type")
    @NonNull
    private String fileType;

    @ColumnInfo(name = "file_size")
    private long fileSize;

    @ColumnInfo(name = "mime_type")
    @NonNull
    private String mimeType;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // getter, setter
}
```

---

### 3. Tags テーブル

タグのマスターデータを格納します。

| カラム名      | 型           | NULL | デフォルト      | 説明                                     |
|--------------|-------------|------|----------------|------------------------------------------|
| id           | INTEGER     | NO   | AUTO INCREMENT | 主キー                                   |
| name         | TEXT        | NO   | -              | タグ名（UNIQUE制約）                      |
| color        | TEXT        | YES  | NULL           | タグの表示色（#RRGGBB形式）               |
| created_at   | INTEGER     | NO   | CURRENT_TIME   | 作成日時（Unix timestamp）                |

**制約:**
- `UNIQUE(name)`: タグ名の重複を防ぐ

**インデックス:**
- `name` (UNIQUE): タグ名での検索を高速化、重複防止

**Entity実装:**

```java
@Entity(tableName = "tags",
        indices = @Index(value = "name", unique = true))
public class Tag {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    @NonNull
    private String name;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // getter, setter
}
```

---

### 4. Item_Tags テーブル

アイテムとタグの多対多の関係を管理する中間テーブルです。

| カラム名      | 型           | NULL | デフォルト      | 説明                                     |
|--------------|-------------|------|----------------|------------------------------------------|
| item_id      | INTEGER     | NO   | -              | アイテムID（外部キー、主キー）            |
| tag_id       | INTEGER     | NO   | -              | タグID（外部キー、主キー）                |

**外部キー制約:**
- `item_id` REFERENCES Items(id) ON DELETE CASCADE
- `tag_id` REFERENCES Tags(id) ON DELETE CASCADE

**主キー:**
- `(item_id, tag_id)` の複合主キー: 同じ組み合わせの重複を防ぐ

**インデックス:**
- `item_id`: アイテムIDでの検索を高速化
- `tag_id`: タグIDでの検索を高速化

**Entity実装:**

```java
@Entity(tableName = "item_tags",
        primaryKeys = {"item_id", "tag_id"},
        foreignKeys = {
            @ForeignKey(
                entity = Item.class,
                parentColumns = "id",
                childColumns = "item_id",
                onDelete = ForeignKey.CASCADE
            ),
            @ForeignKey(
                entity = Tag.class,
                parentColumns = "id",
                childColumns = "tag_id",
                onDelete = ForeignKey.CASCADE
            )
        },
        indices = {
            @Index(value = "item_id"),
            @Index(value = "tag_id")
        })
public class ItemTag {
    @ColumnInfo(name = "item_id")
    private long itemId;

    @ColumnInfo(name = "tag_id")
    private long tagId;

    // getter, setter
}
```

---

## DAO（Data Access Object）

DAOインターフェースを通じてデータベース操作を行います。

### ItemDao

```java
@Dao
public interface ItemDao {
    @Insert
    long insert(Item item);

    @Update
    void update(Item item);

    @Delete
    void delete(Item item);

    @Query("SELECT * FROM items ORDER BY created_at DESC")
    LiveData<List<Item>> getAllItems();

    @Query("SELECT * FROM items WHERE id = :itemId")
    Item getItemByIdSync(long itemId);

    @Query("UPDATE items SET last_viewed = :timestamp WHERE id = :itemId")
    void updateLastViewed(long itemId, long timestamp);

    @Query("SELECT * FROM items ORDER BY last_viewed DESC LIMIT :limit")
    LiveData<List<Item>> getRecentlyViewedItems(int limit);

    @Query("SELECT * FROM items WHERE id IN " +
           "(SELECT item_id FROM item_tags WHERE tag_id = :tagId) " +
           "ORDER BY created_at DESC")
    LiveData<List<Item>> getItemsByTag(long tagId);
}
```

### FileDao

```java
@Dao
public interface FileDao {
    @Insert
    long insert(ItemFile file);

    @Update
    void update(ItemFile file);

    @Delete
    void delete(ItemFile file);

    @Query("SELECT * FROM files WHERE item_id = :itemId")
    LiveData<List<ItemFile>> getFilesByItemId(long itemId);

    @Query("SELECT * FROM files WHERE item_id = :itemId")
    List<ItemFile> getFilesByItemIdSync(long itemId);

    @Query("SELECT * FROM files WHERE file_type = :fileType")
    LiveData<List<ItemFile>> getFilesByType(String fileType);

    @Query("SELECT file_path FROM files")
    List<String> getAllFilePaths();
}
```

### TagDao

```java
@Dao
public interface TagDao {
    @Insert
    long insert(Tag tag);

    @Update
    void update(Tag tag);

    @Delete
    void delete(Tag tag);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    LiveData<List<Tag>> getAllTags();

    @Query("SELECT * FROM tags WHERE name = :name")
    Tag getTagByNameSync(String name);

    @Query("SELECT * FROM tags WHERE id = :tagId")
    LiveData<Tag> getTagById(long tagId);
}
```

### ItemTagDao

```java
@Dao
public interface ItemTagDao {
    @Insert
    void insert(ItemTag itemTag);

    @Delete
    void delete(ItemTag itemTag);

    @Query("DELETE FROM item_tags WHERE item_id = :itemId")
    void deleteByItemId(long itemId);

    @Query("SELECT * FROM tags WHERE id IN " +
           "(SELECT tag_id FROM item_tags WHERE item_id = :itemId)")
    LiveData<List<Tag>> getTagsForItem(long itemId);

    @Query("SELECT COUNT(*) FROM item_tags WHERE tag_id = :tagId")
    LiveData<Integer> getItemCountForTag(long tagId);
}
```

---

## リレーション用データクラス

複数テーブルを結合した結果を受け取るためのデータクラスです。

### ItemWithFiles

アイテムとそれに紐づくファイル一覧を一度に取得します。

```java
public class ItemWithFiles {
    @Embedded
    public Item item;

    @Relation(
        parentColumn = "id",
        entityColumn = "item_id"
    )
    public List<ItemFile> files;
}
```

### ItemWithTags

アイテムとそれに紐づくタグ一覧を一度に取得します。

```java
public class ItemWithTags {
    @Embedded
    public Item item;

    @Relation(
        parentColumn = "id",
        entityColumn = "tag_id",
        associateBy = @Junction(
            value = ItemTag.class,
            parentColumn = "item_id",
            entityColumn = "tag_id"
        )
    )
    public List<Tag> tags;
}
```

### ItemWithFilesAndTags

アイテム、ファイル、タグの全情報を一度に取得します。

```java
public class ItemWithFilesAndTags {
    @Embedded
    public Item item;

    @Relation(
        parentColumn = "id",
        entityColumn = "item_id"
    )
    public List<ItemFile> files;

    @Relation(
        parentColumn = "id",
        entityColumn = "tag_id",
        associateBy = @Junction(
            value = ItemTag.class,
            parentColumn = "item_id",
            entityColumn = "tag_id"
        )
    )
    public List<Tag> tags;
}
```

---

## AppDatabase クラス

```java
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
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
```

---

## パフォーマンス最適化

1. **インデックスの活用**: 頻繁に検索されるカラムにインデックスを設定
2. **バッチ処理**: 複数件の挿入は `@Insert List<T>` を使用
3. **非同期処理**: Room操作は必ずバックグラウンドスレッドで実行
4. **JOIN の代わりに Relation**: RoomのRelation機能を使用して効率的に結合

---

## まとめ

この設計により、以下が実現できます:

1. 複数ファイルを一つのアイテムとして管理
2. タグによる柔軟な分類（多対多関係）
3. メタデータの完全な管理
4. リアクティブなUI更新（LiveData）
5. 型安全なデータベース操作（Room）
6. 効率的なクエリとパフォーマンス
