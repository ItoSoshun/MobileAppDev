# 実装ガイド

## 実装状況

現在、以下のフェーズが完了しています：

- **Phase 0**: Room依存関係の追加 ✓
- **Phase 1**: データベース実装（Entity、DAO、Relation、AppDatabase） ✓
- **Phase 2**: ファイルストレージ実装（FileStorageManager） ✓
- **Phase 3**: リポジトリパターン実装（ItemRepository、TagRepository） ✓
- **Phase 4**: UI層更新（HomeFragment、DashboardFragment、ItemAdapter） ✓

次のステップは Phase 5（テストと最適化）です。

---

## Phase 5: テストと最適化

### 概要

アプリケーションの品質を確保するため、単体テスト、統合テスト、パフォーマンステストを実施します。

### ステップ5.1: 単体テスト

#### DAOのテスト

各DAOの基本的なCRUD操作をテストします。

**テスト対象:**
- ItemDao
- FileDao
- TagDao
- ItemTagDao

**テストケース例:**

```java
@RunWith(AndroidJUnit4.class)
public class ItemDaoTest {
    private AppDatabase database;
    private ItemDao itemDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        itemDao = database.itemDao();
    }

    @After
    public void closeDb() {
        database.close();
    }

    @Test
    public void insertAndGetItem() {
        Item item = new Item();
        item.setDescription("テストアイテム");
        item.setCreatedAt(System.currentTimeMillis());
        item.setUpdatedAt(System.currentTimeMillis());

        long itemId = itemDao.insert(item);
        Item retrieved = itemDao.getItemByIdSync(itemId);

        assertNotNull(retrieved);
        assertEquals("テストアイテム", retrieved.getDescription());
    }

    @Test
    public void deleteItem() {
        Item item = new Item();
        item.setDescription("削除テスト");
        item.setCreatedAt(System.currentTimeMillis());
        item.setUpdatedAt(System.currentTimeMillis());

        long itemId = itemDao.insert(item);
        Item retrieved = itemDao.getItemByIdSync(itemId);
        itemDao.delete(retrieved);

        Item deleted = itemDao.getItemByIdSync(itemId);
        assertNull(deleted);
    }
}
```

#### FileStorageManagerのテスト

ファイルの保存、取得、削除をテストします。

```java
@RunWith(AndroidJUnit4.class)
public class FileStorageManagerTest {
    private FileStorageManager storageManager;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        storageManager = new FileStorageManager(context);
    }

    @Test
    public void saveAndGetFile() throws IOException {
        String testContent = "テストコンテンツ";
        InputStream inputStream = new ByteArrayInputStream(testContent.getBytes());

        SavedFile savedFile = storageManager.saveFile(inputStream, "text/plain");

        assertNotNull(savedFile);
        assertNotNull(savedFile.getRelativePath());

        File retrievedFile = storageManager.getFile(savedFile.getRelativePath());
        assertTrue(retrievedFile.exists());
    }

    @Test
    public void deleteFile() throws IOException {
        String testContent = "削除テスト";
        InputStream inputStream = new ByteArrayInputStream(testContent.getBytes());

        SavedFile savedFile = storageManager.saveFile(inputStream, "text/plain");
        boolean deleted = storageManager.deleteFile(savedFile.getRelativePath());

        assertTrue(deleted);
        File file = storageManager.getFile(savedFile.getRelativePath());
        assertFalse(file.exists());
    }
}
```

#### Repositoryのテスト

ビジネスロジック層のテストを実施します。

```java
@RunWith(AndroidJUnit4.class)
public class ItemRepositoryTest {
    private ItemRepository repository;
    private FileStorageManager storageManager;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        repository = new ItemRepository(context);
        storageManager = new FileStorageManager(context);
    }

    @Test
    public void createItemWithFilesAndTags() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final long[] createdItemId = new long[1];

        Item item = new Item();
        item.setDescription("統合テスト");
        item.setCreatedAt(System.currentTimeMillis());
        item.setUpdatedAt(System.currentTimeMillis());

        List<ItemFile> files = new ArrayList<>();
        List<Long> tagIds = new ArrayList<>();
        // ファイルとタグを準備...

        repository.createItem(item, files, tagIds, new ItemRepository.OnItemCreatedListener() {
            @Override
            public void onSuccess(long itemId) {
                createdItemId[0] = itemId;
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                fail("アイテム作成に失敗: " + e.getMessage());
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        assertTrue(createdItemId[0] > 0);
    }
}
```

---

### ステップ5.2: 統合テスト

#### 新規アイテム作成テスト

写真とメモを含むアイテムの作成フローをテストします。

**テストシナリオ:**
1. カメラで写真を撮影
2. メモを入力
3. タグを選択
4. 保存ボタンをクリック
5. データベースとストレージに正しく保存されることを確認

#### タグによるフィルタリングテスト

**テストシナリオ:**
1. 複数のアイテムを異なるタグで作成
2. タグフィルターを適用
3. 正しいアイテムのみが表示されることを確認

#### ファイル削除テスト

**テストシナリオ:**
1. アイテムを作成
2. アイテムを削除
3. データベースから削除されることを確認
4. 物理ファイルも削除されることを確認

#### 複数タグの付与テスト

**テストシナリオ:**
1. 一つのアイテムに複数のタグを付与
2. 各タグでフィルタリングしたときに表示されることを確認

---

### ステップ5.3: パフォーマンステスト

#### 大量データのテスト

**テスト内容:**
- 1000件のアイテムを作成
- 一覧表示の速度を測定
- スクロール性能を確認
- メモリ使用量を監視

**最適化ポイント:**
1. RecyclerViewのViewHolderパターンの正しい実装
2. 画像の遅延読み込み
3. Paging Libraryの検討（将来的な改善）

#### データベースクエリの最適化

**確認項目:**
- インデックスが正しく設定されているか
- 不要なクエリが発生していないか
- N+1問題が発生していないか

**最適化例:**

```java
// 悪い例：N+1問題
List<Item> items = itemDao.getAllItemsSync();
for (Item item : items) {
    List<ItemFile> files = fileDao.getFilesByItemIdSync(item.getId());
    // ...
}

// 良い例：Relationを使用
List<ItemWithFiles> itemsWithFiles = itemDao.getAllItemsWithFiles();
// 一度のクエリで全データ取得
```

#### ストレージ使用量の監視

**実装例:**

```java
// ストレージ使用量の取得
public long getUsedStorageSize() {
    File rootDir = new File(context.getFilesDir(), STORAGE_ROOT);
    return calculateDirectorySize(rootDir);
}

private long calculateDirectorySize(File directory) {
    long size = 0;
    if (directory.isDirectory()) {
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                size += file.length();
            } else {
                size += calculateDirectorySize(file);
            }
        }
    }
    return size;
}
```

---

### ステップ5.4: 孤立ファイルのクリーンアップ

データベースに登録されていない物理ファイルを検出・削除する機能を実装します。

**実装例:**

```java
public void cleanupOrphanedFiles(OnCleanupCompletedListener listener) {
    executorService.execute(() -> {
        try {
            // 1. データベースに登録されている全ファイルパスを取得
            List<String> registeredPaths = fileDao.getAllFilePaths();
            Set<String> pathSet = new HashSet<>(registeredPaths);

            // 2. 物理ストレージの全ファイルを列挙
            List<File> allFiles = storageManager.listAllFiles();

            // 3. 孤立ファイルを検出して削除
            int deletedCount = 0;
            for (File file : allFiles) {
                String relativePath = storageManager.getRelativePath(file);
                if (!pathSet.contains(relativePath)) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            }

            if (listener != null) {
                listener.onSuccess(deletedCount);
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e);
            }
        }
    });
}

public interface OnCleanupCompletedListener {
    void onSuccess(int deletedCount);
    void onError(Exception e);
}
```

---

### ステップ5.5: エラーハンドリングの改善

**実装すべきエラーハンドリング:**

1. **ストレージ容量不足**
```java
public SavedFile saveFile(InputStream inputStream, String mimeType) throws IOException {
    // 空き容量チェック
    StatFs stat = new StatFs(context.getFilesDir().getPath());
    long availableBytes = stat.getAvailableBytes();

    if (availableBytes < MIN_REQUIRED_SPACE) {
        throw new IOException("ストレージ容量が不足しています");
    }

    // ファイル保存処理...
}
```

2. **データベースエラー**
```java
public void createItem(Item item, List<ItemFile> files, List<Long> tagIds,
                      OnItemCreatedListener listener) {
    executorService.execute(() -> {
        try {
            // トランザクション処理
            database.runInTransaction(() -> {
                long itemId = itemDao.insert(item);

                for (ItemFile file : files) {
                    file.setItemId(itemId);
                    fileDao.insert(file);
                }

                for (Long tagId : tagIds) {
                    ItemTag itemTag = new ItemTag();
                    itemTag.setItemId(itemId);
                    itemTag.setTagId(tagId);
                    itemTagDao.insert(itemTag);
                }
            });

            if (listener != null) {
                listener.onSuccess(itemId);
            }
        } catch (Exception e) {
            Log.e("ItemRepository", "アイテム作成エラー", e);
            if (listener != null) {
                listener.onError(e);
            }
        }
    });
}
```

3. **ファイル読み込みエラー**
```java
private void openFile(ItemFile file) {
    try {
        File physicalFile = storageManager.getFile(file.getFilePath());

        if (!physicalFile.exists()) {
            Toast.makeText(requireContext(), "ファイルが見つかりません", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(
            requireContext(),
            "jp.ac.meijou.android.nanndatteii.fileprovider",
            physicalFile
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, file.getMimeType());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);

    } catch (Exception e) {
        Log.e("DashboardFragment", "ファイルオープンエラー", e);
        Toast.makeText(requireContext(), "ファイルを開けません", Toast.LENGTH_SHORT).show();
    }
}
```

---

## テストチェックリスト

### 単体テスト
- [ ] ItemDaoのCRUD操作
- [ ] FileDaoのCRUD操作
- [ ] TagDaoのCRUD操作
- [ ] ItemTagDaoのCRUD操作
- [ ] FileStorageManagerのファイル保存
- [ ] FileStorageManagerのファイル取得
- [ ] FileStorageManagerのファイル削除

### 統合テスト
- [ ] 新規アイテム作成（写真+メモ+タグ）
- [ ] タグによるフィルタリング
- [ ] アイテム削除（DB+物理ファイル）
- [ ] 複数タグの付与と検索
- [ ] 閲覧履歴の更新

### パフォーマンステスト
- [ ] 1000件のアイテム読み込み速度
- [ ] RecyclerViewのスクロール性能
- [ ] メモリ使用量の確認
- [ ] データベースクエリの最適化確認

### エラーハンドリング
- [ ] ストレージ容量不足時の処理
- [ ] データベースエラー時のロールバック
- [ ] ファイル読み込み失敗時の処理
- [ ] ネットワークエラー時の処理（将来的な拡張）

---

## 次のステップ

Phase 5のテストと最適化を完了したら、以下の拡張機能を検討できます：

1. **自動解析機能**: ML Kitを使用した画像認識
2. **クラウド同期**: Firebase Storageとの連携
3. **エクスポート機能**: ZIPファイルでのバックアップ
4. **検索機能**: 全文検索の実装

詳細は `database-usage-guide.md` と各設計書を参照してください。
