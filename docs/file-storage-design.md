# ファイルストレージ設計書

## 設計思想

「投げたっていい。」アプリのファイルストレージ設計は、以下の原則に基づいています:

1. **一元管理**: すべてのファイルをアプリ専用ディレクトリに集約
2. **衝突回避**: UUIDベースのファイル名で重複を防止
3. **型別整理**: ファイルタイプごとにサブディレクトリで管理
4. **メタデータ分離**: ファイル名とメタデータを分離し、DBで管理
5. **セキュリティ**: プライベートストレージとパブリックストレージの適切な使い分け

## 現状の問題点

### 既存実装の課題

```
Downloads/
└── 投げたっていい。/
    ├── お気に入り/
    │   ├── お気に入り_20250113_120000.jpg
    │   └── お気に入り_20250113_120001.txt
    └── 買い物/
        ├── 買い物_20250113_130000.jpg
        └── 買い物_20250113_130001.txt
```

**問題点:**
1. タグ名でフォルダが散らばる（フラット構造の利点が失われる）
2. ファイル名にタグ名が含まれ、タグ変更時に対応困難
3. 複数ファイルを一つのアイテムとして紐付ける仕組みがない
4. 同じタイムスタンプで複数ファイルを保存すると衝突する可能性
5. メタデータがファイル名に依存

---

## 新しいディレクトリ構造

### ストレージ場所の選択

Android 10以降のScoped Storageに対応し、以下の2つの選択肢があります:

#### 1. アプリプライベートストレージ（推奨）

**パス**: `/data/data/jp.ac.meijou.android.nanndatteii/files/`

**メリット:**
- アプリ専用領域でセキュア
- 権限不要（`READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE`不要）
- アンインストール時に自動削除
- 高速アクセス

**デメリット:**
- 他のアプリからアクセス不可
- ユーザーが直接ファイルにアクセスしにくい

#### 2. メディアストア（パブリック）

**パス**: `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)`

**メリット:**
- ユーザーがファイルマネージャーからアクセス可能
- 他のアプリと共有しやすい
- アンインストール後もファイルが残る

**デメリット:**
- Android 10以降は`MediaStore` API必須
- 権限管理が複雑

### 推奨構造: ハイブリッドアプローチ

**プライベートストレージを基本とし、必要に応じてエクスポート機能を提供**

```
/data/data/jp.ac.meijou.android.nanndatteii/files/
└── nagetatte/
    ├── images/
    │   ├── a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
    │   └── b2c3d4e5-f6g7-8901-bcde-fg2345678901.jpg
    ├── texts/
    │   ├── c3d4e5f6-g7h8-9012-cdef-gh3456789012.txt
    │   └── d4e5f6g7-h8i9-0123-defg-hi4567890123.txt
    ├── documents/
    │   └── e5f6g7h8-i9j0-1234-efgh-ij5678901234.pdf
    ├── videos/
    │   └── f6g7h8i9-j0k1-2345-fghi-jk6789012345.mp4
    └── others/
        └── g7h8i9j0-k1l2-3456-ghij-kl7890123456.dat
```

#### ディレクトリ説明

| ディレクトリ  | 用途                          | 対応ファイルタイプ                      |
|--------------|------------------------------|-----------------------------------------|
| `images/`    | 写真・画像ファイル             | .jpg, .jpeg, .png, .gif, .webp, .heic   |
| `texts/`     | テキストメモ                  | .txt, .md                               |
| `documents/` | ドキュメント                  | .pdf, .doc, .docx, .xls, .xlsx          |
| `videos/`    | 動画ファイル                  | .mp4, .mov, .avi, .mkv                  |
| `others/`    | その他のファイル               | 上記以外                                |

---

## ファイル命名規則

### UUID v4 ベース

```
<UUID>.<extension>
```

**例:**
- `a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg`
- `c3d4e5f6-g7h8-9012-cdef-gh3456789012.txt`

### 命名の利点

1. **衝突回避**: UUID v4の衝突確率は極めて低い
2. **セキュリティ**: ファイル名から内容を推測不可
3. **タグ非依存**: タグ変更時にファイル名変更不要
4. **並列処理対応**: 複数ファイル同時保存でも安全

### Java実装例

```java
import java.util.UUID;

public class FileNameGenerator {
    public static String generateFileName(String extension) {
        UUID uuid = UUID.randomUUID();
        return uuid.toString() + "." + extension;
    }

    public static String getFileTypeDirectory(String mimeType) {
        if (mimeType.startsWith("image/")) {
            return "images";
        } else if (mimeType.equals("text/plain")) {
            return "texts";
        } else if (mimeType.startsWith("video/")) {
            return "videos";
        } else if (mimeType.equals("application/pdf") ||
                   mimeType.startsWith("application/msword") ||
                   mimeType.startsWith("application/vnd.ms-excel") ||
                   mimeType.startsWith("application/vnd.openxmlformats")) {
            return "documents";
        } else {
            return "others";
        }
    }
}
```

---

## ファイル操作ユーティリティ

### FileStorageManager

アプリ全体でファイル操作を統一管理するクラスです。

```java
package jp.ac.meijou.android.nanndatteii.storage;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class FileStorageManager {
    private static final String ROOT_DIR = "nagetatte";
    private final Context context;

    public FileStorageManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * ファイルを保存
     * @param inputStream 入力ストリーム
     * @param mimeType MIMEタイプ
     * @return 保存されたファイル情報
     */
    public SavedFile saveFile(InputStream inputStream, String mimeType) throws IOException {
        // 1. ファイルタイプディレクトリを決定
        String typeDir = getFileTypeDirectory(mimeType);

        // 2. 拡張子を取得
        String extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType);
        if (extension == null) extension = "dat";

        // 3. UUIDファイル名を生成
        String fileName = UUID.randomUUID().toString() + "." + extension;

        // 4. ディレクトリを作成
        File directory = new File(context.getFilesDir(), ROOT_DIR + "/" + typeDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 5. ファイルに書き込み
        File file = new File(directory, fileName);
        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // 6. 保存情報を返す
        return new SavedFile(
            typeDir + "/" + fileName,  // 相対パス
            fileName,                    // ファイル名
            file.length(),               // ファイルサイズ
            mimeType                     // MIMEタイプ
        );
    }

    /**
     * ファイルを読み込み
     * @param relativePath 相対パス（例: "images/uuid.jpg"）
     * @return Fileオブジェクト
     */
    public File getFile(String relativePath) {
        return new File(context.getFilesDir(), ROOT_DIR + "/" + relativePath);
    }

    /**
     * ファイルを削除
     * @param relativePath 相対パス
     * @return 削除成功したか
     */
    public boolean deleteFile(String relativePath) {
        File file = getFile(relativePath);
        return file.exists() && file.delete();
    }

    /**
     * ファイルをコピー
     * @param sourceFile コピー元
     * @param mimeType MIMEタイプ
     * @return 新しいファイル情報
     */
    public SavedFile copyFile(File sourceFile, String mimeType) throws IOException {
        try (InputStream inputStream = new FileInputStream(sourceFile)) {
            return saveFile(inputStream, mimeType);
        }
    }

    /**
     * MIMEタイプからディレクトリ名を取得
     */
    private String getFileTypeDirectory(String mimeType) {
        if (mimeType.startsWith("image/")) {
            return "images";
        } else if (mimeType.equals("text/plain")) {
            return "texts";
        } else if (mimeType.startsWith("video/")) {
            return "videos";
        } else if (mimeType.equals("application/pdf") ||
                   mimeType.startsWith("application/msword") ||
                   mimeType.startsWith("application/vnd.ms-excel") ||
                   mimeType.startsWith("application/vnd.openxmlformats")) {
            return "documents";
        } else {
            return "others";
        }
    }

    /**
     * ストレージのルートディレクトリを取得
     */
    public File getRootDirectory() {
        return new File(context.getFilesDir(), ROOT_DIR);
    }

    /**
     * 使用容量を計算（バイト）
     */
    public long getUsedStorageSize() {
        return calculateDirectorySize(getRootDirectory());
    }

    private long calculateDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
}
```

### SavedFile データクラス

```java
package jp.ac.meijou.android.nanndatteii.storage;

public class SavedFile {
    private final String relativePath;
    private final String fileName;
    private final long fileSize;
    private final String mimeType;

    public SavedFile(String relativePath, String fileName, long fileSize, String mimeType) {
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }
}
```

---

## 使用例

### 1. カメラ写真の保存

```java
// HomeFragment.java
private void saveCameraPhoto(Uri photoUri) {
    try {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(photoUri);
        FileStorageManager storageManager = new FileStorageManager(requireContext());

        SavedFile savedFile = storageManager.saveFile(inputStream, "image/jpeg");

        // データベースに保存
        ItemFile itemFile = new ItemFile();
        itemFile.setItemId(currentItemId);
        itemFile.setFilePath(savedFile.getRelativePath());
        itemFile.setFileName(savedFile.getFileName());
        itemFile.setFileType("IMAGE");
        itemFile.setFileSize(savedFile.getFileSize());
        itemFile.setMimeType(savedFile.getMimeType());
        itemFile.setCreatedAt(System.currentTimeMillis());

        fileDao.insert(itemFile);

        Toast.makeText(requireContext(), "写真を保存しました", Toast.LENGTH_SHORT).show();

    } catch (IOException e) {
        Toast.makeText(requireContext(), "保存に失敗しました", Toast.LENGTH_SHORT).show();
    }
}
```

### 2. テキストメモの保存

```java
// HomeFragment.java
private void saveTextMemo(String memoText) {
    try {
        byte[] textBytes = memoText.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(textBytes);

        FileStorageManager storageManager = new FileStorageManager(requireContext());
        SavedFile savedFile = storageManager.saveFile(inputStream, "text/plain");

        // データベースに保存
        ItemFile itemFile = new ItemFile();
        itemFile.setItemId(currentItemId);
        itemFile.setFilePath(savedFile.getRelativePath());
        itemFile.setFileName(savedFile.getFileName());
        itemFile.setFileType("TEXT");
        itemFile.setFileSize(savedFile.getFileSize());
        itemFile.setMimeType(savedFile.getMimeType());
        itemFile.setCreatedAt(System.currentTimeMillis());

        fileDao.insert(itemFile);

        Toast.makeText(requireContext(), "メモを保存しました", Toast.LENGTH_SHORT).show();

    } catch (IOException e) {
        Toast.makeText(requireContext(), "保存に失敗しました", Toast.LENGTH_SHORT).show();
    }
}
```

### 3. ファイルの読み込み

```java
// DashboardFragment.java (ファイルビューア)
private void openFile(ItemFile itemFile) {
    FileStorageManager storageManager = new FileStorageManager(requireContext());
    File file = storageManager.getFile(itemFile.getFilePath());

    if (!file.exists()) {
        Toast.makeText(requireContext(), "ファイルが見つかりません", Toast.LENGTH_SHORT).show();
        return;
    }

    // FileProviderでUriを取得
    Uri fileUri = FileProvider.getUriForFile(
        requireContext(),
        requireContext().getPackageName() + ".fileprovider",
        file
    );

    // 外部アプリで開く
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(fileUri, itemFile.getMimeType());
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
        startActivity(intent);
    } else {
        Toast.makeText(requireContext(), "ファイルを開けるアプリがありません", Toast.LENGTH_SHORT).show();
    }
}
```

### 4. ファイルの削除

```java
// アイテム削除時にファイルも削除
private void deleteItem(long itemId) {
    // 1. データベースからファイル一覧を取得
    List<ItemFile> files = fileDao.getFilesByItemId(itemId).getValue();

    // 2. 物理ファイルを削除
    FileStorageManager storageManager = new FileStorageManager(requireContext());
    if (files != null) {
        for (ItemFile file : files) {
            storageManager.deleteFile(file.getFilePath());
        }
    }

    // 3. データベースからアイテムを削除（CASCADE設定により関連ファイルレコードも自動削除）
    itemDao.delete(item);
}
```

---

## FileProvider設定

外部アプリとファイルを共有するために、`file_paths.xml`を更新します。

### res/xml/file_paths.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- アプリ内部ストレージ -->
    <files-path
        name="app_files"
        path="nagetatte/" />

    <!-- 外部ストレージ（既存の設定も維持） -->
    <external-path
        name="external_files"
        path="Download/投げたっていい。/" />
</paths>
```

---

## エクスポート機能

ユーザーがファイルを外部に共有・エクスポートする機能を提供します。

### 単一ファイルのエクスポート

```java
public void exportFile(ItemFile itemFile) {
    FileStorageManager storageManager = new FileStorageManager(requireContext());
    File file = storageManager.getFile(itemFile.getFilePath());

    if (!file.exists()) {
        Toast.makeText(requireContext(), "ファイルが見つかりません", Toast.LENGTH_SHORT).show();
        return;
    }

    // 共有インテント
    Uri fileUri = FileProvider.getUriForFile(
        requireContext(),
        requireContext().getPackageName() + ".fileprovider",
        file
    );

    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType(itemFile.getMimeType());
    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    startActivity(Intent.createChooser(shareIntent, "ファイルをエクスポート"));
}
```

### 複数ファイルのエクスポート（ZIP）

```java
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public void exportItemAsZip(long itemId) throws IOException {
    List<ItemFile> files = fileDao.getFilesByItemId(itemId).getValue();
    if (files == null || files.isEmpty()) {
        Toast.makeText(requireContext(), "エクスポートするファイルがありません", Toast.LENGTH_SHORT).show();
        return;
    }

    FileStorageManager storageManager = new FileStorageManager(requireContext());

    // 一時ZIPファイルを作成
    File tempZip = new File(requireContext().getCacheDir(), "export_" + itemId + ".zip");

    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempZip))) {
        for (ItemFile itemFile : files) {
            File file = storageManager.getFile(itemFile.getFilePath());
            if (!file.exists()) continue;

            ZipEntry zipEntry = new ZipEntry(itemFile.getFileName());
            zipOut.putNextEntry(zipEntry);

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    zipOut.write(buffer, 0, bytesRead);
                }
            }
            zipOut.closeEntry();
        }
    }

    // ZIPファイルを共有
    Uri zipUri = FileProvider.getUriForFile(
        requireContext(),
        requireContext().getPackageName() + ".fileprovider",
        tempZip
    );

    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("application/zip");
    shareIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    startActivity(Intent.createChooser(shareIntent, "ZIPファイルをエクスポート"));
}
```

---

## ストレージ容量管理

### 容量表示

```java
public String getStorageUsageFormatted() {
    FileStorageManager storageManager = new FileStorageManager(requireContext());
    long usedBytes = storageManager.getUsedStorageSize();

    if (usedBytes < 1024) {
        return usedBytes + " B";
    } else if (usedBytes < 1024 * 1024) {
        return String.format("%.2f KB", usedBytes / 1024.0);
    } else if (usedBytes < 1024 * 1024 * 1024) {
        return String.format("%.2f MB", usedBytes / (1024.0 * 1024.0));
    } else {
        return String.format("%.2f GB", usedBytes / (1024.0 * 1024.0 * 1024.0));
    }
}
```

### キャッシュクリーニング

```java
public void clearOrphanedFiles() {
    FileStorageManager storageManager = new FileStorageManager(requireContext());
    File rootDir = storageManager.getRootDirectory();

    // データベースに登録されているファイルパスの一覧を取得
    List<String> registeredPaths = fileDao.getAllFilePaths(); // 要実装
    Set<String> registeredSet = new HashSet<>(registeredPaths);

    // 物理ファイルをスキャン
    int deletedCount = 0;
    for (String typeDir : new String[]{"images", "texts", "documents", "videos", "others"}) {
        File dir = new File(rootDir, typeDir);
        if (!dir.exists()) continue;

        File[] files = dir.listFiles();
        if (files == null) continue;

        for (File file : files) {
            String relativePath = typeDir + "/" + file.getName();
            if (!registeredSet.contains(relativePath)) {
                // データベースに登録されていないファイル = 孤立ファイル
                if (file.delete()) {
                    deletedCount++;
                }
            }
        }
    }

    Toast.makeText(requireContext(),
        deletedCount + " 個の孤立ファイルを削除しました",
        Toast.LENGTH_SHORT).show();
}
```

---

## セキュリティ考慮事項

### 1. ファイル名の予測不可能性

UUID v4を使用することで、ファイル名から内容を推測されることを防ぎます。

### 2. プライベートストレージの利用

`context.getFilesDir()`を使用することで、他のアプリからの直接アクセスを防ぎます。

### 3. FileProvider による安全な共有

直接ファイルパスを公開せず、`FileProvider`経由でURIを発行することで、一時的なアクセス権限のみを付与します。

### 4. 権限の最小化

プライベートストレージを使用することで、`READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE`権限が不要になります。

---

## パフォーマンス最適化

### 1. サムネイル生成（画像）

```java
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ThumbnailGenerator {
    private static final int THUMBNAIL_SIZE = 200; // px

    public static File generateThumbnail(File originalFile, Context context) throws IOException {
        Bitmap original = BitmapFactory.decodeFile(originalFile.getAbsolutePath());

        float scale = Math.min(
            (float) THUMBNAIL_SIZE / original.getWidth(),
            (float) THUMBNAIL_SIZE / original.getHeight()
        );

        int newWidth = Math.round(original.getWidth() * scale);
        int newHeight = Math.round(original.getHeight() * scale);

        Bitmap thumbnail = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        original.recycle();

        File thumbnailFile = new File(context.getCacheDir(), "thumb_" + originalFile.getName());
        try (FileOutputStream out = new FileOutputStream(thumbnailFile)) {
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out);
        }
        thumbnail.recycle();

        return thumbnailFile;
    }
}
```

### 2. 画像のキャッシング

Glideライブラリを使用することで、画像の読み込みとキャッシングを効率化できます。

```gradle
// build.gradle
implementation 'com.github.bumptech.glide:glide:4.16.0'
annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
```

```java
// 使用例
Glide.with(context)
    .load(file)
    .placeholder(R.drawable.placeholder)
    .into(imageView);
```

---

## まとめ

この設計により、以下が実現できます:

1. **UUID v4によるファイル名の衝突回避**
2. **タイプ別ディレクトリによる整理**
3. **プライベートストレージによるセキュリティ向上**
4. **FileStorageManagerによる一元管理**
5. **エクスポート機能による外部共有**
6. **孤立ファイルのクリーニング機能**

次のステップとして、実装ガイドをご覧ください。
