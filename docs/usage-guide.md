# 使い方ガイド

## 概要

このドキュメントでは、データベースとファイルストレージシステムの使い方を説明します。

---

## ItemRepository の使い方

### 初期化

```java
ItemRepository repository = new ItemRepository(requireContext());
```

### アイテムの作成

```java
// アイテムを作成
Item item = new Item();
item.setDescription("桜の写真");
item.setCreatedAt(System.currentTimeMillis());
item.setUpdatedAt(System.currentTimeMillis());

// ファイルリストを作成
List<ItemFile> files = new ArrayList<>();

ItemFile photoFile = new ItemFile();
photoFile.setFilePath("images/abc123.jpg");
photoFile.setFileName("abc123.jpg");
photoFile.setFileType("IMAGE");
photoFile.setFileSize(1024000);
photoFile.setMimeType("image/jpeg");
photoFile.setCreatedAt(System.currentTimeMillis());
files.add(photoFile);

// タグIDリストを作成
List<Long> tagIds = new ArrayList<>();
tagIds.add(1L); // 風景タグ

// アイテムを保存
repository.createItem(item, files, tagIds, new ItemRepository.OnItemCreatedListener() {
    @Override
    public void onSuccess(long itemId) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "保存しました", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(Exception e) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "保存に失敗しました", Toast.LENGTH_SHORT).show();
        });
    }
});
```

### すべてのアイテムを取得

```java
LiveData<List<Item>> allItems = repository.getAllItems();

allItems.observe(getViewLifecycleOwner(), items -> {
    adapter.setItems(items);
});
```

### タグでフィルタリング

```java
long tagId = 1L;
LiveData<List<Item>> filteredItems = repository.getItemsByTag(tagId);

filteredItems.observe(getViewLifecycleOwner(), items -> {
    adapter.setItems(items);
});
```

### アイテムの削除

```java
long itemId = 123L;
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
        });
    }
});
```

### 最終閲覧日時の更新

```java
long itemId = 123L;
repository.updateLastViewed(itemId, System.currentTimeMillis(),
    new ItemRepository.OnItemUpdatedListener() {
        @Override
        public void onSuccess() {
            Log.d("TAG", "最終閲覧日時を更新しました");
        }

        @Override
        public void onError(Exception e) {
            Log.e("TAG", "更新に失敗しました", e);
        }
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
tag.setColor("#FF5722");
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
            Toast.makeText(requireContext(), "タグの追加に失敗しました", Toast.LENGTH_SHORT).show();
        });
    }
});
```

### タグの削除

```java
Tag tag = ...;

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

---

## FileStorageManager の使い方

### 初期化

```java
FileStorageManager storageManager = new FileStorageManager(requireContext());
```

### ファイルの保存

```java
InputStream inputStream = requireContext().getContentResolver().openInputStream(photoUri);
String mimeType = "image/jpeg";

try {
    SavedFile savedFile = storageManager.saveFile(inputStream, mimeType);

    // ItemFileを作成してデータベースに登録
    ItemFile itemFile = new ItemFile();
    itemFile.setFilePath(savedFile.getRelativePath());
    itemFile.setFileName(savedFile.getFileName());
    itemFile.setFileType("IMAGE");
    itemFile.setFileSize(savedFile.getFileSize());
    itemFile.setMimeType(savedFile.getMimeType());
    itemFile.setCreatedAt(System.currentTimeMillis());

} catch (IOException e) {
    Toast.makeText(requireContext(), "ファイルの保存に失敗しました", Toast.LENGTH_SHORT).show();
}
```

### テキストの保存

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
    Log.e("TAG", "テキスト保存エラー", e);
}
```

### ファイルを開く

```java
ItemFile itemFile = ...; // データベースから取得
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
}
```

### ファイルの削除

```java
String relativePath = "images/abc123.jpg";
boolean deleted = storageManager.deleteFile(relativePath);

if (deleted) {
    Log.d("TAG", "ファイルを削除しました");
} else {
    Log.w("TAG", "ファイル削除に失敗しました");
}
```

### ストレージ使用量の取得

```java
long usedBytes = storageManager.getUsedStorageSize();
double usedMB = usedBytes / (1024.0 * 1024.0);

Log.d("TAG", String.format("使用量: %.2f MB", usedMB));
```

---

## 実装例

### 例1: 写真とメモを保存する

```java
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
            InputStream photoStream =
                requireContext().getContentResolver().openInputStream(photoUri);
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
        itemRepository.createItem(item, files, tagIds,
            new ItemRepository.OnItemCreatedListener() {
                @Override
                public void onSuccess(long itemId) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "保存しました",
                            Toast.LENGTH_SHORT).show();
                        binding.memoEditText.setText("");
                        photoUri = null;
                    });
                }

                @Override
                public void onError(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "保存に失敗しました",
                            Toast.LENGTH_SHORT).show();
                    });
                }
            });

    } catch (IOException e) {
        Toast.makeText(requireContext(), "ファイル保存エラー", Toast.LENGTH_SHORT).show();
    }
}
```

### 例2: タグでフィルタリングして表示する

```java
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
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        binding.tagSpinner.setAdapter(spinnerAdapter);

        binding.tagSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
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
```

---

## よくある質問

### LiveDataを使わずにデータを取得できますか？

可能ですが推奨しません。同期メソッドはバックグラウンドスレッドで実行してください。LiveDataを使用すると自動的に適切なスレッドで実行されます。

### ファイルのUUIDはどのように生成されますか？

`FileStorageManager.saveFile()` メソッド内で `UUID.randomUUID().toString()` を使用して自動生成されます。

### アイテムを削除すると、関連ファイルも削除されますか？

はい。`ItemRepository.deleteItem()` メソッドは、データベースから削除するとともに、物理ファイルもストレージから削除します。

### 同じタグ名を重複して登録できますか？

できません。`Tag` エンティティの `name` フィールドには一意制約が設定されているため、同じ名前のタグを登録すると `SQLiteConstraintException` が発生します。

### ファイルタイプはどのように判定されますか？

MIMEタイプに基づいて判定されます：
- `image/*` → IMAGE
- `text/*` → TEXT
- `video/*` → VIDEO
- `application/pdf` など → DOCUMENT
- その他 → OTHER

### タグの色はどのように指定しますか？

カラーコード文字列（例: "#FF5722"）を `Tag.setColor()` で設定します。UIでは `Color.parseColor(tag.getColor())` で色を取得できます。

### ストレージ容量不足を検出できますか？

`FileStorageManager.getUsedStorageSize()` でアプリが使用している容量を取得できます。システムの空き容量は `StatFs` クラスを使用して取得できます。

---

## トラブルシューティング

### "Cannot access database on the main thread"

**原因:** メインスレッドでデータベース操作を実行しています。

**解決策:** LiveDataを使用するか、Repositoryのコールバックメソッドを使用してください。

### "FileNotFoundException"

**原因:** 指定されたファイルパスが存在しません。

**解決策:** `File.exists()` でファイルの存在を確認してからアクセスしてください。

### LiveDataが更新されない

**原因:** `observe()` のLifecycleOwnerが正しくありません。

**解決策:** Fragmentでは `getViewLifecycleOwner()` を使用してください。
