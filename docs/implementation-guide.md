# 実装ガイド・マイグレーション計画

## 概要

このドキュメントでは、既存の「投げたっていい。」アプリを新しいデータベース・ファイルストレージ設計に移行する手順を説明します。

## 実装フェーズ

### Phase 0: 準備（1-2日）

#### 依存関係の追加

`gradle/libs.versions.toml` に Room の依存関係を追加します。

```toml
[versions]
room = "2.6.1"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
```

`app/build.gradle` に追加:

```gradle
dependencies {
    // 既存の依存関係...

    // Room Database
    implementation libs.androidx.room.runtime
    annotationProcessor libs.androidx.room.compiler
    implementation libs.androidx.room.ktx
}
```

#### プロジェクト構造の準備

```
app/src/main/java/jp/ac/meijou/android/nanndatteii/
├── db/
│   ├── AppDatabase.java (新規)
│   ├── dao/
│   │   ├── ItemDao.java (新規)
│   │   ├── FileDao.java (新規)
│   │   ├── TagDao.java (新規)
│   │   └── ItemTagDao.java (新規)
│   ├── entity/
│   │   ├── Item.java (新規)
│   │   ├── ItemFile.java (新規)
│   │   ├── Tag.java (新規)
│   │   └── ItemTag.java (新規)
│   └── relation/
│       ├── ItemWithFiles.java (新規)
│       ├── ItemWithTags.java (新規)
│       └── ItemWithFilesAndTags.java (新規)
├── storage/
│   ├── FileStorageManager.java (新規)
│   └── SavedFile.java (新規)
├── repository/
│   ├── ItemRepository.java (新規)
│   └── TagRepository.java (新規)
└── migration/
    └── DataMigrationHelper.java (新規)
```

---

### Phase 1: データベース実装（3-5日）

#### ステップ1.1: Entityクラスの作成

`database-design.md` を参照して、以下のEntityクラスを作成します:

- `Item.java`
- `ItemFile.java`
- `Tag.java`
- `ItemTag.java`

#### ステップ1.2: DAOインターフェースの作成

各エンティティに対応するDAOを作成します:

- `ItemDao.java`
- `FileDao.java`
- `TagDao.java`
- `ItemTagDao.java`

#### ステップ1.3: AppDatabaseクラスの作成

```java
@Database(
    entities = {Item.class, ItemFile.class, Tag.class, ItemTag.class},
    version = 2, // 既存のDB_VERSIONが1なので2にする
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
                    .addMigrations(MIGRATION_1_2)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    // マイグレーション定義（Phase 2で実装）
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Phase 2で実装
        }
    };
}
```

#### ステップ1.4: Relationクラスの作成

- `ItemWithFiles.java`
- `ItemWithTags.java`
- `ItemWithFilesAndTags.java`

---

### Phase 2: ファイルストレージ実装（2-3日）

#### ステップ2.1: FileStorageManagerの作成

`file-storage-design.md` を参照して、`FileStorageManager.java` と `SavedFile.java` を作成します。

#### ステップ2.2: FileProviderの更新

`res/xml/file_paths.xml` を更新し、新しいストレージディレクトリを追加します。

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 新しいプライベートストレージ -->
    <files-path
        name="app_files"
        path="nagetatte/" />

    <!-- 既存の外部ストレージ（マイグレーション用に残す） -->
    <external-path
        name="external_files"
        path="Download/投げたっていい。/" />
</paths>
```

---

### Phase 3: リポジトリパターンの実装（2-3日）

#### ステップ3.1: ItemRepositoryの作成

```java
package jp.ac.meijou.android.nanndatteii.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.ac.meijou.android.nanndatteii.db.AppDatabase;
import jp.ac.meijou.android.nanndatteii.db.dao.*;
import jp.ac.meijou.android.nanndatteii.db.entity.*;
import jp.ac.meijou.android.nanndatteii.db.relation.ItemWithFilesAndTags;

public class ItemRepository {
    private final ItemDao itemDao;
    private final FileDao fileDao;
    private final ItemTagDao itemTagDao;
    private final ExecutorService executorService;

    public ItemRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        itemDao = db.itemDao();
        fileDao = db.fileDao();
        itemTagDao = db.itemTagDao();
        executorService = Executors.newFixedThreadPool(2);
    }

    // アイテム作成
    public void createItem(Item item, List<ItemFile> files, List<Long> tagIds, OnItemCreatedListener listener) {
        executorService.execute(() -> {
            try {
                // 1. アイテムを挿入
                long itemId = itemDao.insert(item);

                // 2. ファイルを挿入
                for (ItemFile file : files) {
                    file.setItemId(itemId);
                    fileDao.insert(file);
                }

                // 3. タグを関連付け
                for (Long tagId : tagIds) {
                    ItemTag itemTag = new ItemTag();
                    itemTag.setItemId(itemId);
                    itemTag.setTagId(tagId);
                    itemTagDao.insert(itemTag);
                }

                if (listener != null) {
                    listener.onSuccess(itemId);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // アイテム取得
    public LiveData<List<ItemWithFilesAndTags>> getAllItems() {
        return itemDao.getAllItemsWithFilesAndTags();
    }

    // タグでフィルタリング
    public LiveData<List<ItemWithFilesAndTags>> getItemsByTag(long tagId) {
        return itemDao.getItemsByTagWithFilesAndTags(tagId);
    }

    // アイテム削除
    public void deleteItem(long itemId, FileStorageManager storageManager, OnItemDeletedListener listener) {
        executorService.execute(() -> {
            try {
                // 1. ファイルを取得
                List<ItemFile> files = fileDao.getFilesByItemIdSync(itemId);

                // 2. 物理ファイルを削除
                for (ItemFile file : files) {
                    storageManager.deleteFile(file.getFilePath());
                }

                // 3. データベースから削除（CASCADE設定により関連レコードも自動削除）
                Item item = itemDao.getItemByIdSync(itemId);
                itemDao.delete(item);

                if (listener != null) {
                    listener.onSuccess();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public interface OnItemCreatedListener {
        void onSuccess(long itemId);
        void onError(Exception e);
    }

    public interface OnItemDeletedListener {
        void onSuccess();
        void onError(Exception e);
    }
}
```

#### ステップ3.2: TagRepositoryの作成

```java
package jp.ac.meijou.android.nanndatteii.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.ac.meijou.android.nanndatteii.db.AppDatabase;
import jp.ac.meijou.android.nanndatteii.db.dao.TagDao;
import jp.ac.meijou.android.nanndatteii.db.entity.Tag;

public class TagRepository {
    private final TagDao tagDao;
    private final ExecutorService executorService;

    public TagRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        tagDao = db.tagDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Tag>> getAllTags() {
        return tagDao.getAllTags();
    }

    public void insertTag(Tag tag, OnTagInsertedListener listener) {
        executorService.execute(() -> {
            try {
                long tagId = tagDao.insert(tag);
                if (listener != null) {
                    listener.onSuccess(tagId);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void deleteTag(Tag tag, OnTagDeletedListener listener) {
        executorService.execute(() -> {
            try {
                tagDao.delete(tag);
                if (listener != null) {
                    listener.onSuccess();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public interface OnTagInsertedListener {
        void onSuccess(long tagId);
        void onError(Exception e);
    }

    public interface OnTagDeletedListener {
        void onSuccess();
        void onError(Exception e);
    }
}
```

---

### Phase 4: データマイグレーション（3-4日）

#### ステップ4.1: データベーススキーママイグレーション

`AppDatabase.java` の `MIGRATION_1_2` を実装します（`database-design.md` 参照）。

#### ステップ4.2: ファイルマイグレーションヘルパーの作成

```java
package jp.ac.meijou.android.nanndatteii.migration;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ac.meijou.android.nanndatteii.db.AppDatabase;
import jp.ac.meijou.android.nanndatteii.db.dao.*;
import jp.ac.meijou.android.nanndatteii.db.entity.*;
import jp.ac.meijou.android.nanndatteii.storage.FileStorageManager;
import jp.ac.meijou.android.nanndatteii.storage.SavedFile;

public class DataMigrationHelper {
    private static final String TAG = "DataMigrationHelper";
    private static final String OLD_FOLDER_NAME = "投げたっていい。";

    private final Context context;
    private final AppDatabase database;
    private final FileStorageManager storageManager;

    public DataMigrationHelper(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.storageManager = new FileStorageManager(context);
    }

    /**
     * 既存ファイルを新しい構造にマイグレーション
     */
    public void migrateOldFiles(MigrationProgressListener listener) {
        new Thread(() -> {
            try {
                File oldBaseDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    OLD_FOLDER_NAME
                );

                if (!oldBaseDir.exists() || !oldBaseDir.isDirectory()) {
                    if (listener != null) {
                        listener.onComplete(0, 0);
                    }
                    return;
                }

                // タグフォルダごとにファイルをグループ化
                Map<String, List<File>> tagFileMap = scanOldFiles(oldBaseDir);

                int totalFiles = 0;
                for (List<File> files : tagFileMap.values()) {
                    totalFiles += files.size();
                }

                if (listener != null) {
                    listener.onStart(totalFiles);
                }

                int migratedCount = 0;

                // タグごとにアイテムを作成
                for (Map.Entry<String, List<File>> entry : tagFileMap.entrySet()) {
                    String tagName = entry.getKey();
                    List<File> files = entry.getValue();

                    // タグをDBに登録（既存チェック）
                    Tag tag = database.tagDao().getTagByNameSync(tagName);
                    if (tag == null) {
                        tag = new Tag();
                        tag.setName(tagName);
                        tag.setCreatedAt(System.currentTimeMillis());
                        long tagId = database.tagDao().insert(tag);
                        tag.setId(tagId);
                    }

                    // ファイルを日時でグループ化（同じタイムスタンプ = 同じアイテム）
                    Map<String, List<File>> timestampGroups = groupFilesByTimestamp(files);

                    for (Map.Entry<String, List<File>> group : timestampGroups.entrySet()) {
                        String timestamp = group.getKey();
                        List<File> groupFiles = group.getValue();

                        // アイテムを作成
                        Item item = new Item();
                        item.setTitle(tagName + " - " + timestamp);
                        item.setCreatedAt(parseTimestamp(timestamp));
                        item.setUpdatedAt(item.getCreatedAt());
                        long itemId = database.itemDao().insert(item);

                        // タグを関連付け
                        ItemTag itemTag = new ItemTag();
                        itemTag.setItemId(itemId);
                        itemTag.setTagId(tag.getId());
                        database.itemTagDao().insert(itemTag);

                        // ファイルをコピー
                        for (File file : groupFiles) {
                            try {
                                migrateFile(file, itemId);
                                migratedCount++;

                                if (listener != null) {
                                    listener.onProgress(migratedCount, totalFiles);
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Failed to migrate file: " + file.getName(), e);
                            }
                        }
                    }
                }

                if (listener != null) {
                    listener.onComplete(migratedCount, totalFiles);
                }

            } catch (Exception e) {
                Log.e(TAG, "Migration failed", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        }).start();
    }

    /**
     * 旧ファイルをスキャンしてタグごとに分類
     */
    private Map<String, List<File>> scanOldFiles(File baseDir) {
        Map<String, List<File>> tagFileMap = new HashMap<>();

        File[] tagDirs = baseDir.listFiles();
        if (tagDirs == null) return tagFileMap;

        for (File tagDir : tagDirs) {
            if (!tagDir.isDirectory()) continue;

            String tagName = tagDir.getName();
            List<File> files = new ArrayList<>();

            File[] filesInDir = tagDir.listFiles();
            if (filesInDir != null) {
                for (File file : filesInDir) {
                    if (file.isFile()) {
                        files.add(file);
                    }
                }
            }

            if (!files.isEmpty()) {
                tagFileMap.put(tagName, files);
            }
        }

        return tagFileMap;
    }

    /**
     * ファイルをタイムスタンプでグループ化
     * ファイル名: タグ名_yyyyMMdd_HHmmss.拡張子
     */
    private Map<String, List<File>> groupFilesByTimestamp(List<File> files) {
        Map<String, List<File>> groups = new HashMap<>();
        Pattern pattern = Pattern.compile(".*_(\\d{8}_\\d{6})\\..+");

        for (File file : files) {
            Matcher matcher = pattern.matcher(file.getName());
            String timestamp;
            if (matcher.matches()) {
                timestamp = matcher.group(1);
            } else {
                // タイムスタンプが抽出できない場合は最終更新日時を使用
                timestamp = String.valueOf(file.lastModified());
            }

            groups.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(file);
        }

        return groups;
    }

    /**
     * タイムスタンプ文字列をUnix timestampに変換
     */
    private long parseTimestamp(String timestamp) {
        try {
            // yyyyMMdd_HHmmss形式
            if (timestamp.matches("\\d{8}_\\d{6}")) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
                return sdf.parse(timestamp).getTime();
            } else {
                // 数値のみの場合はそのまま使用
                return Long.parseLong(timestamp);
            }
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    /**
     * 単一ファイルをマイグレーション
     */
    private void migrateFile(File oldFile, long itemId) throws IOException {
        // MIMEタイプを推測
        String mimeType = guessMimeType(oldFile);

        // 新しいストレージにコピー
        try (InputStream inputStream = new FileInputStream(oldFile)) {
            SavedFile savedFile = storageManager.saveFile(inputStream, mimeType);

            // データベースに登録
            ItemFile itemFile = new ItemFile();
            itemFile.setItemId(itemId);
            itemFile.setFilePath(savedFile.getRelativePath());
            itemFile.setFileName(oldFile.getName()); // 元のファイル名を保持
            itemFile.setFileType(getFileType(mimeType));
            itemFile.setFileSize(savedFile.getFileSize());
            itemFile.setMimeType(savedFile.getMimeType());
            itemFile.setCreatedAt(oldFile.lastModified());

            database.fileDao().insert(itemFile);
        }
    }

    /**
     * ファイルからMIMEタイプを推測
     */
    private String guessMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".png")) {
            return "image/png";
        } else if (name.endsWith(".txt")) {
            return "text/plain";
        } else if (name.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * MIMEタイプからファイルタイプを判定
     */
    private String getFileType(String mimeType) {
        if (mimeType.startsWith("image/")) {
            return "IMAGE";
        } else if (mimeType.equals("text/plain")) {
            return "TEXT";
        } else if (mimeType.startsWith("video/")) {
            return "VIDEO";
        } else {
            return "OTHER";
        }
    }

    public interface MigrationProgressListener {
        void onStart(int totalFiles);
        void onProgress(int migratedCount, int totalFiles);
        void onComplete(int migratedCount, int totalFiles);
        void onError(Exception e);
    }
}
```

#### ステップ4.3: マイグレーション画面の作成

ユーザーに対して、データマイグレーションを実行するためのUI（ダイアログまたは専用画面）を作成します。

```java
// マイグレーション実行例
DataMigrationHelper migrationHelper = new DataMigrationHelper(requireContext());
migrationHelper.migrateOldFiles(new DataMigrationHelper.MigrationProgressListener() {
    @Override
    public void onStart(int totalFiles) {
        // プログレスバーを表示
        requireActivity().runOnUiThread(() -> {
            progressDialog.setMax(totalFiles);
            progressDialog.show();
        });
    }

    @Override
    public void onProgress(int migratedCount, int totalFiles) {
        requireActivity().runOnUiThread(() -> {
            progressDialog.setProgress(migratedCount);
        });
    }

    @Override
    public void onComplete(int migratedCount, int totalFiles) {
        requireActivity().runOnUiThread(() -> {
            progressDialog.dismiss();
            Toast.makeText(requireContext(),
                migratedCount + " 個のファイルを移行しました",
                Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onError(Exception e) {
        requireActivity().runOnUiThread(() -> {
            progressDialog.dismiss();
            Toast.makeText(requireContext(),
                "移行に失敗しました: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        });
    }
});
```

---

### Phase 5: UI層の更新（5-7日）

#### ステップ5.1: HomeFragmentの更新

既存のファイル保存ロジックを新しいRepositoryとFileStorageManagerを使用するように変更します。

**主な変更点:**
1. カメラ写真の保存先を新しいストレージに変更
2. テキストメモの保存先を新しいストレージに変更
3. 複数ファイルを一つのアイテムとして保存
4. タグの関連付け処理を追加

**実装例:**

```java
// 新しい保存処理（写真 + メモ）
private void saveNewItem(Uri photoUri, String memoText, long tagId) {
    FileStorageManager storageManager = new FileStorageManager(requireContext());
    ItemRepository repository = new ItemRepository(requireContext());

    List<ItemFile> files = new ArrayList<>();
    List<Long> tagIds = new ArrayList<>();
    tagIds.add(tagId);

    try {
        // 1. 写真を保存
        if (photoUri != null) {
            InputStream photoStream = requireContext().getContentResolver().openInputStream(photoUri);
            SavedFile savedPhoto = storageManager.saveFile(photoStream, "image/jpeg");

            ItemFile photoFile = new ItemFile();
            photoFile.setFilePath(savedPhoto.getRelativePath());
            photoFile.setFileName(savedPhoto.getFileName());
            photoFile.setFileType("IMAGE");
            photoFile.setFileSize(savedPhoto.getFileSize());
            photoFile.setMimeType(savedPhoto.getMimeType());
            photoFile.setCreatedAt(System.currentTimeMillis());
            files.add(photoFile);
        }

        // 2. メモを保存
        if (memoText != null && !memoText.isEmpty()) {
            byte[] textBytes = memoText.getBytes(StandardCharsets.UTF_8);
            InputStream textStream = new ByteArrayInputStream(textBytes);
            SavedFile savedText = storageManager.saveFile(textStream, "text/plain");

            ItemFile textFile = new ItemFile();
            textFile.setFilePath(savedText.getRelativePath());
            textFile.setFileName(savedText.getFileName());
            textFile.setFileType("TEXT");
            textFile.setFileSize(savedText.getFileSize());
            textFile.setMimeType(savedText.getMimeType());
            textFile.setCreatedAt(System.currentTimeMillis());
            files.add(textFile);
        }

        // 3. アイテムを作成
        Item item = new Item();
        item.setDescription(memoText);
        item.setCreatedAt(System.currentTimeMillis());
        item.setUpdatedAt(System.currentTimeMillis());

        repository.createItem(item, files, tagIds, new ItemRepository.OnItemCreatedListener() {
            @Override
            public void onSuccess(long itemId) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "保存しました", Toast.LENGTH_SHORT).show();
                    // UIをリセット
                    binding.Textbox.setText("");
                    photoUri = null;
                });
            }

            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "保存に失敗しました", Toast.LENGTH_SHORT).show();
                });
            }
        });

    } catch (IOException e) {
        Toast.makeText(requireContext(), "ファイル保存エラー", Toast.LENGTH_SHORT).show();
    }
}
```

#### ステップ5.2: DashboardFragmentの更新

ファイル一覧の取得ロジックを新しいRepositoryを使用するように変更します。

```java
// 新しい一覧取得処理
private void loadItems(Long tagId) {
    ItemRepository repository = new ItemRepository(requireContext());
    LiveData<List<ItemWithFilesAndTags>> itemsLiveData;

    if (tagId == null) {
        // すべてのアイテムを取得
        itemsLiveData = repository.getAllItems();
    } else {
        // タグでフィルタリング
        itemsLiveData = repository.getItemsByTag(tagId);
    }

    itemsLiveData.observe(getViewLifecycleOwner(), items -> {
        // RecyclerViewを更新
        adapter.setItems(items);
    });
}
```

#### ステップ5.3: RecyclerViewアダプターの更新

`FileAdapter` を `ItemAdapter` に変更し、アイテム単位の表示に対応します。

```java
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private List<ItemWithFilesAndTags> items;
    private final Context context;
    private final OnItemClickListener listener;

    public ItemAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.items = new ArrayList<>();
    }

    public void setItems(List<ItemWithFilesAndTags> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemWithFilesAndTags item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // ViewHolder実装
        public void bind(ItemWithFilesAndTags item, OnItemClickListener listener) {
            // アイテム情報をバインド
        }
    }

    public interface OnItemClickListener {
        void onItemClick(ItemWithFilesAndTags item);
    }
}
```

---

### Phase 6: テストと最適化（3-5日）

#### ステップ6.1: 単体テスト

- DAOのテスト
- Repositoryのテスト
- FileStorageManagerのテスト

#### ステップ6.2: 統合テスト

- 既存データのマイグレーションテスト
- 新規アイテム作成テスト
- ファイル削除テスト

#### ステップ6.3: パフォーマンステスト

- 大量ファイルの読み込み速度
- データベースクエリの最適化
- メモリ使用量の確認

---

## マイグレーション実行計画

### ユーザー視点のマイグレーションフロー

1. **アプリ起動時**:
   - 旧データベース構造を検出
   - マイグレーション通知を表示

2. **マイグレーション実行**:
   - ユーザーが「開始」ボタンをタップ
   - プログレスバーで進捗を表示
   - バックグラウンドでファイルを移行

3. **完了後**:
   - 成功メッセージを表示
   - 新しいUIで継続

### ロールバック戦略

万が一マイグレーションに失敗した場合のために:

1. **既存ファイルは削除しない** (Phase 7で手動削除)
2. **データベースバックアップ** を事前に作成
3. **エラーログ** を記録

---

## Phase 7: クリーンアップ（1-2日）

### 旧ファイルの削除

マイグレーション成功後、一定期間経過したら旧ファイルを削除するオプションを提供します。

```java
public void deleteOldFiles() {
    File oldBaseDir = new File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "投げたっていい。"
    );

    if (oldBaseDir.exists()) {
        deleteRecursive(oldBaseDir);
    }
}

private void deleteRecursive(File fileOrDirectory) {
    if (fileOrDirectory.isDirectory()) {
        File[] children = fileOrDirectory.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursive(child);
            }
        }
    }
    fileOrDirectory.delete();
}
```

### 旧データベースヘルパーの削除

`AppDatabaseHelper.java` を削除し、すべての参照を `AppDatabase` + DAOに置き換えます。

---

## タイムライン

| フェーズ | 作業内容 | 期間 |
|---------|---------|------|
| Phase 0 | 準備 | 1-2日 |
| Phase 1 | データベース実装 | 3-5日 |
| Phase 2 | ファイルストレージ実装 | 2-3日 |
| Phase 3 | リポジトリパターン実装 | 2-3日 |
| Phase 4 | データマイグレーション | 3-4日 |
| Phase 5 | UI層更新 | 5-7日 |
| Phase 6 | テスト・最適化 | 3-5日 |
| Phase 7 | クリーンアップ | 1-2日 |
| **合計** | | **20-31日** |

---

## 注意事項

1. **バックアップの重要性**: 既存データを必ずバックアップしてからマイグレーションを実行
2. **段階的実装**: 各Phaseごとに動作確認を行い、問題があれば前のPhaseに戻る
3. **ユーザーテスト**: Phase 5完了後、少数のユーザーでベータテストを実施
4. **ドキュメント更新**: 実装中に発見した問題や変更点をドキュメントに反映

---

## 次のステップ

1. `database-design.md` を読み、Entity・DAOの実装を開始
2. `file-storage-design.md` を読み、FileStorageManagerの実装を開始
3. Phase 0の依存関係追加から着手

実装中に不明点があれば、設計書を参照するか、追加の質問をしてください。
