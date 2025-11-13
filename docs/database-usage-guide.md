# データベース使用ガイド

## 概要

このドキュメントでは、実装済みのデータベースとファイルストレージシステムの仕組みと使い方を説明します。

---

## 目次

1. [データベースの仕組み](#データベースの仕組み)
2. [Repository パターン](#repository-パターン)
3. [ItemRepository の使い方](#itemrepository-の使い方)
4. [TagRepository の使い方](#tagrepository-の使い方)
5. [FileStorageManager の使い方](#filestoragemanager-の使い方)
6. [実装例](#実装例)
7. [よくある質問](#よくある質問)

---

## データベースの仕組み

### アーキテクチャ概要

アプリケーションは以下の3層アーキテクチャで構成されています：

```
UI Layer (Fragment)
    ↓
Domain Layer (Repository)
    ↓
Data Layer (DAO + FileStorageManager)
```

### データモデル

#### 1. Item（アイテム）

投稿の基本情報を格納します。

**フィールド:**
- `id`: アイテムのID（自動採番）
- `title`: タイトル（オプション）
- `description`: 説明文・メモ
- `created_at`: 作成日時（UnixTime）
- `updated_at`: 更新日時（UnixTime）
- `last_viewed`: 最終閲覧日時（UnixTime）

#### 2. ItemFile（ファイル）

アイテムに紐づくファイルの情報を格納します。

**フィールド:**
- `id`: ファイルID（自動採番）
- `item_id`: 所属するアイテムのID（外部キー）
- `file_path`: ファイルの相対パス
- `file_name`: ファイル名（UUID）
- `file_type`: ファイルタイプ（IMAGE, TEXT, VIDEO, DOCUMENT, OTHER）
- `file_size`: ファイルサイズ（バイト）
- `mime_type`: MIMEタイプ（例: image/jpeg）
- `created_at`: 作成日時（UnixTime）

#### 3. Tag（タグ）

タグのマスターデータを格納します。

**フィールド:**
- `id`: タグID（自動採番）
- `name`: タグ名（一意制約）
- `color`: 表示色（カラーコード）
- `created_at`: 作成日時（UnixTime）

#### 4. ItemTag（アイテム-タグ関連）

アイテムとタグの多対多関係を表現する中間テーブルです。

**フィールド:**
- `item_id`: アイテムID（外部キー）
- `tag_id`: タグID（外部キー）

**主キー:** `(item_id, tag_id)` の複合キー

### リレーション

#### ItemWithFiles

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

#### ItemWithTags

アイテムとそれに紐づくタグ一覧を一度に取得します。

```java
public class ItemWithTags {
    @Embedded
    public Item item;

    @Relation(
        parentColumn = "id",
        entityColumn = "item_id",
        associateBy = @Junction(
            value = ItemTag.class,
            parentColumn = "item_id",
            entityColumn = "tag_id"
        )
    )
    public List<Tag> tags;
}
```

#### ItemWithFilesAndTags

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
        entityColumn = "item_id",
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

## Repository パターン

### 概要

Repository パターンは、データアクセスロジックをカプセル化し、UI層からデータ層の詳細を隠蔽するデザインパターンです。

### メリット

1. **関心の分離**: UI層はビジネスロジックのみに集中できる
2. **テスタビリティ**: Repositoryをモック化してテストが容易
3. **保守性**: データアクセスロジックの変更がUI層に影響しない
4. **再利用性**: 複数のFragmentから同じRepositoryを利用可能

### 実装済みのRepository

- **ItemRepository**: アイテムとファイルの操作
- **TagRepository**: タグの操作

---

## ItemRepository の使い方

### 初期化

```java
ItemRepository repository = new ItemRepository(requireContext());
```

### アイテムの作成

写真とメモを含むアイテムを作成します。

```java
// 1. アイテムを作成
Item item = new Item();
item.setDescription("桜の写真");
item.setCreatedAt(System.currentTimeMillis());
item.setUpdatedAt(System.currentTimeMillis());

// 2. ファイルリストを作成
List<ItemFile> files = new ArrayList<>();

// 写真ファイル
ItemFile photoFile = new ItemFile();
photoFile.setFilePath("images/abc123-def456.jpg");
photoFile.setFileName("abc123-def456.jpg");
photoFile.setFileType("IMAGE");
photoFile.setFileSize(1024000);
photoFile.setMimeType("image/jpeg");
photoFile.setCreatedAt(System.currentTimeMillis());
files.add(photoFile);

// メモファイル
ItemFile textFile = new ItemFile();
textFile.setFilePath("texts/xyz789.txt");
textFile.setFileName("xyz789.txt");
textFile.setFileType("TEXT");
textFile.setFileSize(512);
textFile.setMimeType("text/plain");
textFile.setCreatedAt(System.currentTimeMillis());
files.add(textFile);

// 3. タグIDリストを作成
List<Long> tagIds = new ArrayList<>();
tagIds.add(1L); // 例: "風景"タグ
tagIds.add(2L); // 例: "春"タグ

// 4. アイテムを保存
repository.createItem(item, files, tagIds, new ItemRepository.OnItemCreatedListener() {
    @Override
    public void onSuccess(long itemId) {
        // 成功時の処理
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "保存しました", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(Exception e) {
        // エラー時の処理
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "保存に失敗しました", Toast.LENGTH_SHORT).show();
            Log.e("MyFragment", "Error creating item", e);
        });
    }
});
```

### すべてのアイテムを取得

```java
LiveData<List<Item>> allItems = repository.getAllItems();

// LiveDataを監視してUIを更新
allItems.observe(getViewLifecycleOwner(), items -> {
    // RecyclerViewアダプターを更新
    adapter.setItems(items);
});
```

### ファイルとタグを含むアイテムを取得

```java
LiveData<List<ItemWithFilesAndTags>> itemsWithData = repository.getAllItemsWithFilesAndTags();

itemsWithData.observe(getViewLifecycleOwner(), items -> {
    for (ItemWithFilesAndTags itemData : items) {
        Item item = itemData.item;
        List<ItemFile> files = itemData.files;
        List<Tag> tags = itemData.tags;

        // アイテム情報を表示
        Log.d("MyFragment", "Item: " + item.getDescription());
        Log.d("MyFragment", "Files: " + files.size());
        Log.d("MyFragment", "Tags: " + tags.size());
    }
});
```

### タグでフィルタリング

```java
long tagId = 1L; // 特定のタグID
LiveData<List<ItemWithFilesAndTags>> filteredItems = repository.getItemsByTagWithFilesAndTags(tagId);

filteredItems.observe(getViewLifecycleOwner(), items -> {
    // フィルタリングされたアイテムを表示
    adapter.setItems(items);
});
```

### アイテムの削除

```java
long itemId = 123L; // 削除するアイテムのID
FileStorageManager storageManager = new FileStorageManager(requireContext());

repository.deleteItem(itemId, storageManager, new ItemRepository.OnItemDeletedListener() {
    @Override
    public void onSuccess() {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "削除しました", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(Exception e) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "削除に失敗しました", Toast.LENGTH_SHORT).show();
            Log.e("MyFragment", "Error deleting item", e);
        });
    }
});
```

### 最終閲覧日時の更新

```java
long itemId = 123L;
repository.updateLastViewed(itemId, System.currentTimeMillis(), new ItemRepository.OnItemUpdatedListener() {
    @Override
    public void onSuccess() {
        Log.d("MyFragment", "Last viewed updated");
    }

    @Override
    public void onError(Exception e) {
        Log.e("MyFragment", "Error updating last viewed", e);
    }
});
```

### 最近閲覧したアイテムを取得

```java
int limit = 10; // 取得件数
LiveData<List<ItemWithFilesAndTags>> recentItems = repository.getRecentlyViewedItems(limit);

recentItems.observe(getViewLifecycleOwner(), items -> {
    // 最近閲覧したアイテムを表示
    recentAdapter.setItems(items);
});
```

---

## TagRepository の使い方

### 初期化

```java
TagRepository repository = new TagRepository(requireContext());
```

### すべてのタグを取得

```java
LiveData<List<Tag>> allTags = repository.getAllTags();

allTags.observe(getViewLifecycleOwner(), tags -> {
    // Spinnerにタグを表示
    List<String> tagNames = new ArrayList<>();
    tagNames.add("すべて");
    for (Tag tag : tags) {
        tagNames.add(tag.getName());
    }

    ArrayAdapter<String> adapter = new ArrayAdapter<>(
        requireContext(),
        android.R.layout.simple_spinner_item,
        tagNames
    );
    spinner.setAdapter(adapter);
});
```

### タグの作成

```java
Tag tag = new Tag();
tag.setName("風景");
tag.setColor("#FF5722"); // オレンジ色
tag.setCreatedAt(System.currentTimeMillis());

repository.insertTag(tag, new TagRepository.OnTagInsertedListener() {
    @Override
    public void onSuccess(long tagId) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "タグを追加しました", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(Exception e) {
        requireActivity().runOnUiThread(() -> {
            if (e instanceof SQLiteConstraintException) {
                Toast.makeText(requireContext(), "すでに同じ名前のタグが存在します", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "タグの追加に失敗しました", Toast.LENGTH_SHORT).show();
            }
        });
    }
});
```

### タグの削除

```java
Tag tag = ...; // 削除するタグ

repository.deleteTag(tag, new TagRepository.OnTagDeletedListener() {
    @Override
    public void onSuccess() {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "タグを削除しました", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(Exception e) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "タグの削除に失敗しました", Toast.LENGTH_SHORT).show();
        });
    }
});
```

### タグ名で検索

```java
String tagName = "風景";

repository.getTagByName(tagName, new TagRepository.OnTagRetrievedListener() {
    @Override
    public void onSuccess(Tag tag) {
        if (tag != null) {
            // タグが見つかった
            Log.d("MyFragment", "Found tag: " + tag.getName());
        } else {
            // タグが見つからない
            Log.d("MyFragment", "Tag not found");
        }
    }

    @Override
    public void onError(Exception e) {
        Log.e("MyFragment", "Error retrieving tag", e);
    }
});
```

---

## FileStorageManager の使い方

### 初期化

```java
FileStorageManager storageManager = new FileStorageManager(requireContext());
```

### ファイルの保存

```java
// InputStreamからファイルを保存
InputStream inputStream = requireContext().getContentResolver().openInputStream(photoUri);
String mimeType = "image/jpeg";

try {
    SavedFile savedFile = storageManager.saveFile(inputStream, mimeType);

    // 保存されたファイル情報
    String relativePath = savedFile.getRelativePath(); // "images/abc123-def456.jpg"
    String fileName = savedFile.getFileName();         // "abc123-def456.jpg"
    long fileSize = savedFile.getFileSize();           // 1024000
    String savedMimeType = savedFile.getMimeType();    // "image/jpeg"

    // ItemFileを作成してデータベースに登録
    ItemFile itemFile = new ItemFile();
    itemFile.setFilePath(relativePath);
    itemFile.setFileName(fileName);
    itemFile.setFileType("IMAGE");
    itemFile.setFileSize(fileSize);
    itemFile.setMimeType(savedMimeType);
    itemFile.setCreatedAt(System.currentTimeMillis());

} catch (IOException e) {
    Toast.makeText(requireContext(), "ファイルの保存に失敗しました", Toast.LENGTH_SHORT).show();
    Log.e("MyFragment", "Error saving file", e);
}
```

### テキストデータの保存

```java
String memoText = "今日は良い天気だった";
byte[] textBytes = memoText.getBytes(StandardCharsets.UTF_8);
InputStream textStream = new ByteArrayInputStream(textBytes);

try {
    SavedFile savedFile = storageManager.saveFile(textStream, "text/plain");

    ItemFile itemFile = new ItemFile();
    itemFile.setFilePath(savedFile.getRelativePath());
    itemFile.setFileName(savedFile.getFileName());
    itemFile.setFileType("TEXT");
    itemFile.setFileSize(savedFile.getFileSize());
    itemFile.setMimeType(savedFile.getMimeType());
    itemFile.setCreatedAt(System.currentTimeMillis());

} catch (IOException e) {
    Log.e("MyFragment", "Error saving text", e);
}
```

### ファイルの取得

```java
String relativePath = "images/abc123-def456.jpg";
File file = storageManager.getFile(relativePath);

if (file.exists()) {
    // ファイルが存在する
    Log.d("MyFragment", "File path: " + file.getAbsolutePath());
    Log.d("MyFragment", "File size: " + file.length());
} else {
    // ファイルが存在しない
    Log.w("MyFragment", "File not found: " + relativePath);
}
```

### ファイルをアプリで開く

```java
ItemFile itemFile = ...; // データベースから取得したファイル情報
File file = storageManager.getFile(itemFile.getFilePath());

if (!file.exists()) {
    Toast.makeText(requireContext(), "ファイルが見つかりません", Toast.LENGTH_SHORT).show();
    return;
}

try {
    Uri fileUri = FileProvider.getUriForFile(
        requireContext(),
        "jp.ac.meijou.android.nanndatteii.fileprovider",
        file
    );

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(fileUri, itemFile.getMimeType());
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    startActivity(intent);

} catch (Exception e) {
    Toast.makeText(requireContext(), "ファイルを開けません", Toast.LENGTH_SHORT).show();
    Log.e("MyFragment", "Error opening file", e);
}
```

### ファイルの削除

```java
String relativePath = "images/abc123-def456.jpg";
boolean deleted = storageManager.deleteFile(relativePath);

if (deleted) {
    Log.d("MyFragment", "File deleted successfully");
} else {
    Log.w("MyFragment", "Failed to delete file");
}
```

### ストレージ使用量の取得

```java
long usedBytes = storageManager.getUsedStorageSize();
double usedMB = usedBytes / (1024.0 * 1024.0);

Log.d("MyFragment", String.format("Storage used: %.2f MB", usedMB));
```

---

## 実装例

### 例1: 写真とメモを保存する（HomeFragment）

```java
public class HomeFragment extends Fragment {
    private ItemRepository itemRepository;
    private TagRepository tagRepository;
    private FileStorageManager fileStorageManager;
    private Uri photoUri;
    private long selectedTagId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // 初期化
        itemRepository = new ItemRepository(requireContext());
        tagRepository = new TagRepository(requireContext());
        fileStorageManager = new FileStorageManager(requireContext());

        // 保存ボタンのクリックリスナー
        binding.saveButton.setOnClickListener(v -> saveNewItem());

        return binding.getRoot();
    }

    private void saveNewItem() {
        String memoText = binding.memoEditText.getText().toString();

        if (photoUri == null && memoText.isEmpty()) {
            Toast.makeText(requireContext(), "写真かメモを入力してください", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ItemFile> files = new ArrayList<>();

        try {
            // 写真を保存
            if (photoUri != null) {
                InputStream photoStream = requireContext().getContentResolver().openInputStream(photoUri);
                SavedFile savedPhoto = fileStorageManager.saveFile(photoStream, "image/jpeg");

                ItemFile photoFile = new ItemFile();
                photoFile.setFilePath(savedPhoto.getRelativePath());
                photoFile.setFileName(savedPhoto.getFileName());
                photoFile.setFileType("IMAGE");
                photoFile.setFileSize(savedPhoto.getFileSize());
                photoFile.setMimeType(savedPhoto.getMimeType());
                photoFile.setCreatedAt(System.currentTimeMillis());
                files.add(photoFile);
            }

            // メモを保存
            if (!memoText.isEmpty()) {
                byte[] textBytes = memoText.getBytes(StandardCharsets.UTF_8);
                InputStream textStream = new ByteArrayInputStream(textBytes);
                SavedFile savedText = fileStorageManager.saveFile(textStream, "text/plain");

                ItemFile textFile = new ItemFile();
                textFile.setFilePath(savedText.getRelativePath());
                textFile.setFileName(savedText.getFileName());
                textFile.setFileType("TEXT");
                textFile.setFileSize(savedText.getFileSize());
                textFile.setMimeType(savedText.getMimeType());
                textFile.setCreatedAt(System.currentTimeMillis());
                files.add(textFile);
            }

            // アイテムを作成
            Item item = new Item();
            item.setDescription(memoText);
            item.setCreatedAt(System.currentTimeMillis());
            item.setUpdatedAt(System.currentTimeMillis());

            List<Long> tagIds = new ArrayList<>();
            if (selectedTagId > 0) {
                tagIds.add(selectedTagId);
            }

            // データベースに保存
            itemRepository.createItem(item, files, tagIds, new ItemRepository.OnItemCreatedListener() {
                @Override
                public void onSuccess(long itemId) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "保存しました", Toast.LENGTH_SHORT).show();
                        binding.memoEditText.setText("");
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
            Log.e("HomeFragment", "Error saving files", e);
        }
    }
}
```

### 例2: タグでフィルタリングして表示（DashboardFragment）

```java
public class DashboardFragment extends Fragment {
    private ItemRepository itemRepository;
    private TagRepository tagRepository;
    private ItemAdapter adapter;
    private List<Tag> allTags;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        // 初期化
        itemRepository = new ItemRepository(requireContext());
        tagRepository = new TagRepository(requireContext());

        // RecyclerViewの設定
        adapter = new ItemAdapter(requireContext(), this::openItemFiles);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // タグSpinnerの設定
        setupTagSpinner();

        // 初期データ読み込み
        loadAllItems();

        return binding.getRoot();
    }

    private void setupTagSpinner() {
        tagRepository.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            allTags = tags;

            List<String> tagNames = new ArrayList<>();
            tagNames.add("すべて");
            for (Tag tag : tags) {
                tagNames.add(tag.getName());
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tagNames
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.tagSpinner.setAdapter(spinnerAdapter);

            binding.tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        loadAllItems();
                    } else {
                        loadItemsByTag(allTags.get(position - 1).getId());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void loadAllItems() {
        itemRepository.getAllItems().observe(getViewLifecycleOwner(), items -> {
            adapter.setItems(items);
        });
    }

    private void loadItemsByTag(long tagId) {
        itemRepository.getItemsByTag(tagId).observe(getViewLifecycleOwner(), items -> {
            adapter.setItems(items);
        });
    }

    private void openItemFiles(Item item) {
        FileStorageManager storageManager = new FileStorageManager(requireContext());

        itemRepository.getFilesForItem(item.getId(), new ItemRepository.OnFilesRetrievedListener() {
            @Override
            public void onSuccess(List<ItemFile> files) {
                requireActivity().runOnUiThread(() -> {
                    if (files.isEmpty()) {
                        Toast.makeText(requireContext(), "ファイルがありません", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 最初のファイルを開く
                    ItemFile firstFile = files.get(0);
                    File file = storageManager.getFile(firstFile.getFilePath());

                    if (!file.exists()) {
                        Toast.makeText(requireContext(), "ファイルが見つかりません", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        Uri fileUri = FileProvider.getUriForFile(
                            requireContext(),
                            "jp.ac.meijou.android.nanndatteii.fileprovider",
                            file
                        );

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(fileUri, firstFile.getMimeType());
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(intent);

                        // 閲覧日時を更新
                        itemRepository.updateLastViewed(item.getId(), System.currentTimeMillis(), null);

                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "ファイルを開けません", Toast.LENGTH_SHORT).show();
                        Log.e("DashboardFragment", "Error opening file", e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "ファイル取得エラー", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
```

---

## よくある質問

### Q1. LiveDataを使わずに同期的にデータを取得できますか？

A. 可能ですが推奨しません。同期メソッド（例: `getAllItemsSync()`）はメインスレッドでの実行を避け、必ずバックグラウンドスレッドで実行してください。LiveDataを使用すると、自動的にバックグラウンドで実行され、結果がメインスレッドで通知されるため、より安全です。

### Q2. ファイルのUUIDはどのように生成されますか？

A. `FileStorageManager.saveFile()` メソッド内で `UUID.randomUUID().toString()` を使用して自動生成されます。ファイル拡張子はMIMEタイプから自動判定されます。

### Q3. アイテムを削除すると、関連ファイルも削除されますか？

A. はい。`ItemRepository.deleteItem()` メソッドは、データベースからアイテムを削除するとともに、関連する物理ファイルもストレージから削除します。ただし、CASCADE設定により、ItemFileレコードとItemTagレコードは自動的に削除されます。

### Q4. 同じタグ名を重複して登録できますか？

A. できません。`Tag` エンティティの `name` フィールドには一意制約（UNIQUE）が設定されているため、同じ名前のタグを登録しようとすると `SQLiteConstraintException` が発生します。

### Q5. ファイルタイプはどのように判定されますか？

A. MIMEタイプに基づいて判定されます：
- `image/*` → IMAGE
- `text/*` → TEXT
- `video/*` → VIDEO
- `application/pdf` など → DOCUMENT
- その他 → OTHER

### Q6. 大量のアイテムを効率的に表示するには？

A. 以下の方法を検討してください：
1. RecyclerViewのViewHolderパターンを正しく実装
2. 画像の遅延読み込みを実装
3. Paging Libraryの導入（将来的な改善）
4. インデックスを活用したクエリの最適化

### Q7. ストレージ容量不足を検出できますか？

A. `FileStorageManager` に空き容量チェック機能を追加することで可能です。`implementation-guide.md` のエラーハンドリングセクションを参照してください。

### Q8. タグの色はどのように指定しますか？

A. カラーコード文字列（例: "#FF5722"）を `Tag.setColor()` で設定します。UIでは `Color.parseColor(tag.getColor())` で色を取得できます。

### Q9. 複数のタグでOR検索はできますか？

A. 現在の実装では、単一タグによるフィルタリングのみサポートしています。複数タグのOR検索が必要な場合は、`ItemDao` に新しいクエリメソッドを追加してください。

### Q10. ファイルをエクスポートするには？

A. FileProviderを使用してファイルURIを取得し、`Intent.ACTION_SEND` でファイルを共有できます。詳細は `file-storage-design.md` のエクスポート機能セクションを参照してください。

---

## 参考資料

- [database-design.md](./database-design.md) - データベース設計の詳細
- [file-storage-design.md](./file-storage-design.md) - ファイルストレージの詳細
- [implementation-guide.md](./implementation-guide.md) - テストと最適化

---

## トラブルシューティング

### エラー: "Cannot access database on the main thread"

**原因:** メインスレッドでデータベース操作を実行しようとしています。

**解決策:** LiveDataを使用するか、Repositoryのコールバックメソッドを使用してください。

### エラー: "FileNotFoundException"

**原因:** 指定されたファイルパスが存在しません。

**解決策:** `File.exists()` でファイルの存在を確認してからアクセスしてください。

### エラー: "SQLiteConstraintException: UNIQUE constraint failed"

**原因:** 同じ名前のタグを重複登録しようとしています。

**解決策:** タグ登録前に `TagRepository.getTagByName()` で存在チェックを行ってください。

### LiveDataが更新されない

**原因:** `observe()` のLifecycleOwnerが正しくありません。

**解決策:** Fragmentでは `getViewLifecycleOwner()` を使用してください。`this` を使用すると、View破棄後も監視が継続されメモリリークの原因になります。

---

このガイドを参考に、データベースとファイルストレージシステムを効果的に活用してください。
