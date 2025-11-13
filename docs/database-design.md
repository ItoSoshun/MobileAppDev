# データベース設計書

## 設計思想

「投げたっていい。」アプリのデータベース設計は、以下の原則に基づいています:

1. **アイテム中心の設計**: 複数のファイルを一つの「アイテム」として管理
2. **フラットな構造**: 階層を持たず、タグによる柔軟な分類
3. **メタデータ重視**: 投稿日時・閲覧日時などの情報を保持
4. **拡張性**: 将来の自動解析・自動タグ付け機能を見据えた設計

## 技術選定

### Room Database への移行

現在の素のSQLiteから **Room** への移行を推奨します。

#### Roomを選択する理由

1. **型安全性**: コンパイル時のSQLクエリ検証
2. **LiveData統合**: リアクティブなUI更新が容易
3. **メンテナンス性**: ボイラープレートコードの削減
4. **Androidベストプラクティス**: Google推奨のJetpack Architecture Component

## データベーススキーマ

### ER図（概念図）

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Items     │         │  Item_Tags   │         │    Tags     │
│             │         │ (中間テーブル) │         │             │
├─────────────┤         ├──────────────┤         ├─────────────┤
│ id (PK)     │◄───┐    │ id (PK)      │    ┌───►│ id (PK)     │
│ title       │    │    │ item_id (FK) │────┘    │ name        │
│ description │    │    │ tag_id (FK)  │         │ color       │
│ created_at  │    │    └──────────────┘         │ created_at  │
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

### 1. Items テーブル（アイテム）

アプリに「投げられた」一つの投稿単位を表します。複数のファイルやタグを持つことができます。

| カラム名      | 型           | NULL | デフォルト      | 説明                                     |
|--------------|-------------|------|----------------|------------------------------------------|
| id           | INTEGER     | NO   | AUTO INCREMENT | 主キー                                   |
| title        | TEXT        | YES  | NULL           | アイテムのタイトル（オプション）          |
| description  | TEXT        | YES  | NULL           | アイテムの説明（メモ内容など）            |
| created_at   | INTEGER     | NO   | CURRENT_TIME   | 作成日時（Unix timestamp）                |
| updated_at   | INTEGER     | NO   | CURRENT_TIME   | 更新日時（Unix timestamp）                |
| last_viewed  | INTEGER     | YES  | NULL           | 最終閲覧日時（Unix timestamp）            |

#### インデックス
- `idx_items_created_at` ON (created_at DESC)
- `idx_items_last_viewed` ON (last_viewed DESC)

#### Room Entity定義例

```java
@Entity(tableName = "items",
        indices = {
            @Index(value = "created_at"),
            @Index(value = "last_viewed")
        })
public class Item {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "last_viewed")
    private Long lastViewed;

    // コンストラクタ、getter、setter
}
```

---

### 2. Files テーブル（ファイル）

各アイテムに紐づくファイル（写真、メモテキスト、その他ファイル）を管理します。

| カラム名      | 型           | NULL | デフォルト      | 説明                                     |
|--------------|-------------|------|----------------|------------------------------------------|
| id           | INTEGER     | NO   | AUTO INCREMENT | 主キー                                   |
| item_id      | INTEGER     | NO   | -              | アイテムID（外部キー）                    |
| file_path    | TEXT        | NO   | -              | ファイルの相対パス                        |
| file_name    | TEXT        | NO   | -              | オリジナルファイル名                      |
| file_type    | TEXT        | NO   | -              | ファイル種別（IMAGE/TEXT/OTHER）          |
| file_size    | INTEGER     | NO   | 0              | ファイルサイズ（バイト）                  |
| mime_type    | TEXT        | NO   | -              | MIMEタイプ（image/jpeg, text/plain等）    |
| created_at   | INTEGER     | NO   | CURRENT_TIME   | 作成日時（Unix timestamp）                |

#### 外部キー制約
- `item_id` REFERENCES Items(id) ON DELETE CASCADE

#### インデックス
- `idx_files_item_id` ON (item_id)
- `idx_files_file_type` ON (file_type)

#### Room Entity定義例

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
    @ColumnInfo(name = "id")
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
    private String fileType; // IMAGE, TEXT, OTHER

    @ColumnInfo(name = "file_size")
    private long fileSize;

    @ColumnInfo(name = "mime_type")
    @NonNull
    private String mimeType;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // コンストラクタ、getter、setter
}
```

---

### 3. Tags テーブル（タグマスター）

既存のタグテーブルを拡張し、タグの色情報などを追加します。

| カラム名      | 型           | NULL | デフォルト      | 説明                                     |
|--------------|-------------|------|----------------|------------------------------------------|
| id           | INTEGER     | NO   | AUTO INCREMENT | 主キー                                   |
| name         | TEXT        | NO   | -              | タグ名（UNIQUE制約）                      |
| color        | TEXT        | YES  | NULL           | タグの表示色（#RRGGBB形式）               |
| created_at   | INTEGER     | NO   | CURRENT_TIME   | 作成日時（Unix timestamp）                |

#### 制約
- `UNIQUE(name)`: タグ名の重複を防ぐ

#### インデックス
- `idx_tags_name` ON (name)

#### Room Entity定義例

```java
@Entity(tableName = "tags",
        indices = @Index(value = "name", unique = true))
public class Tag {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "name")
    @NonNull
    private String name;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // コンストラクタ、getter、setter
}
```

---

### 4. Item_Tags テーブル（アイテム-タグ関連）

アイテムとタグの多対多の関係を管理する中間テーブルです。

| カラム名      | 型           | NULL | デフォルト      | 説明                                     |
|--------------|-------------|------|----------------|------------------------------------------|
| id           | INTEGER     | NO   | AUTO INCREMENT | 主キー                                   |
| item_id      | INTEGER     | NO   | -              | アイテムID（外部キー）                    |
| tag_id       | INTEGER     | NO   | -              | タグID（外部キー）                        |

#### 外部キー制約
- `item_id` REFERENCES Items(id) ON DELETE CASCADE
- `tag_id` REFERENCES Tags(id) ON DELETE CASCADE

#### ユニーク制約
- `UNIQUE(item_id, tag_id)`: 同じ組み合わせの重複を防ぐ

#### インデックス
- `idx_item_tags_item_id` ON (item_id)
- `idx_item_tags_tag_id` ON (tag_id)

#### Room Entity定義例

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

    // コンストラクタ、getter、setter
}
```

---

## データアクセスオブジェクト（DAO）

Roomでは、DAOインターフェースを通じてデータベース操作を行います。

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
    LiveData<Item> getItemById(long itemId);

    @Query("UPDATE items SET last_viewed = :timestamp WHERE id = :itemId")
    void updateLastViewed(long itemId, long timestamp);

    @Query("SELECT * FROM items ORDER BY last_viewed DESC LIMIT 10")
    LiveData<List<Item>> getRecentlyViewedItems();

    @Transaction
    @Query("SELECT * FROM items WHERE id IN " +
           "(SELECT item_id FROM item_tags WHERE tag_id = :tagId) " +
           "ORDER BY created_at DESC")
    LiveData<List<ItemWithFiles>> getItemsByTag(long tagId);
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

    @Query("SELECT * FROM files WHERE file_type = :fileType")
    LiveData<List<ItemFile>> getFilesByType(String fileType);
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
    Tag getTagByName(String name);

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

## データベースクラス

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
                    )
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // 初期データの挿入（オプション）
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
```

---

## リレーション用データクラス

複数テーブルを結合した結果を受け取るためのデータクラスです。

### ItemWithFiles（アイテム + ファイル一覧）

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

### ItemWithTags（アイテム + タグ一覧）

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

### ItemWithFilesAndTags（完全な情報）

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

## 使用例

### アイテムの作成（写真 + メモ）

```java
// 1. アイテムを作成
Item item = new Item();
item.setTitle("外出先メモ");
item.setDescription("買い物リスト");
item.setCreatedAt(System.currentTimeMillis());
item.setUpdatedAt(System.currentTimeMillis());

long itemId = itemDao.insert(item);

// 2. 写真ファイルを追加
ItemFile photoFile = new ItemFile();
photoFile.setItemId(itemId);
photoFile.setFilePath("items/abc123-def456.jpg");
photoFile.setFileName("photo.jpg");
photoFile.setFileType("IMAGE");
photoFile.setMimeType("image/jpeg");
photoFile.setFileSize(1024000);
photoFile.setCreatedAt(System.currentTimeMillis());

fileDao.insert(photoFile);

// 3. テキストメモを追加
ItemFile memoFile = new ItemFile();
memoFile.setItemId(itemId);
memoFile.setFilePath("items/abc123-def456.txt");
memoFile.setFileName("memo.txt");
memoFile.setFileType("TEXT");
memoFile.setMimeType("text/plain");
memoFile.setFileSize(512);
memoFile.setCreatedAt(System.currentTimeMillis());

fileDao.insert(memoFile);

// 4. タグを関連付け
Tag tag = tagDao.getTagByName("買い物");
ItemTag itemTag = new ItemTag();
itemTag.setItemId(itemId);
itemTag.setTagId(tag.getId());

itemTagDao.insert(itemTag);
```

### タグでフィルタリング

```java
// LiveDataでリアクティブに監視
itemDao.getItemsByTag(tagId).observe(getViewLifecycleOwner(), items -> {
    // RecyclerViewを更新
    adapter.setItems(items);
});
```

---

## パフォーマンス最適化

1. **インデックスの活用**: 頻繁に検索されるカラムにインデックスを設定
2. **バッチ処理**: 複数件の挿入は `@Insert List<T>` を使用
3. **非同期処理**: Room操作は必ずバックグラウンドスレッドで実行
4. **ページング**: 大量データは `Paging 3` ライブラリを併用

---

## 将来の拡張

### 自動解析機能のためのテーブル追加例

```java
@Entity(tableName = "auto_analysis")
public class AutoAnalysis {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "file_id")
    private long fileId; // 外部キー

    @ColumnInfo(name = "detected_objects")
    private String detectedObjects; // JSON形式

    @ColumnInfo(name = "suggested_tags")
    private String suggestedTags; // カンマ区切り

    @ColumnInfo(name = "ocr_text")
    private String ocrText; // OCRで抽出したテキスト

    @ColumnInfo(name = "analyzed_at")
    private long analyzedAt;
}
```

---

## まとめ

この設計により、以下が実現できます:

1. **複数ファイルを一つのアイテムとして管理**
2. **タグによる柔軟な分類（多対多）**
3. **メタデータの完全な管理**
4. **リアクティブなUI更新（LiveData）**
5. **型安全なデータベース操作（Room）**
6. **将来の拡張に対応した構造**

次のステップとして、ファイルストレージ設計書をご覧ください。
