# ファイルストレージ設計書

## 設計思想

ファイルストレージ設計は、以下の原則に基づいています:

1. **一元管理**: すべてのファイルをアプリ専用ディレクトリに集約
2. **衝突回避**: UUIDベースのファイル名で重複を防止
3. **型別整理**: ファイルタイプごとにサブディレクトリで管理
4. **メタデータ分離**: ファイル名とメタデータを分離し、DBで管理
5. **セキュリティ**: プライベートストレージによる保護

---

## ディレクトリ構造

### アプリプライベートストレージ

**パス**: `/data/data/jp.ac.meijou.android.nanndatteii/files/nagetatte/`

**メリット:**
- アプリ専用領域でセキュア
- 権限不要（`READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE`不要）
- アンインストール時に自動削除
- 高速アクセス

**ディレクトリ構成:**

```
/data/data/jp.ac.meijou.android.nanndatteii/files/nagetatte/
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

### ディレクトリ分類

| ディレクトリ  | 用途                | 対応ファイルタイプ                    |
|--------------|---------------------|--------------------------------------|
| `images/`    | 写真・画像ファイル   | .jpg, .jpeg, .png, .gif, .webp, .heic |
| `texts/`     | テキストメモ        | .txt, .md                             |
| `documents/` | ドキュメント        | .pdf, .doc, .docx, .xls, .xlsx        |
| `videos/`    | 動画ファイル        | .mp4, .mov, .avi, .mkv                |
| `others/`    | その他のファイル    | 上記以外                              |

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

### MIMEタイプからディレクトリを判定

```java
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
```

---

## FileStorageManager

ファイル操作を一元管理するクラスです。

### 主な機能

1. **ファイルの保存**: `saveFile(InputStream, String mimeType)`
2. **ファイルの取得**: `getFile(String relativePath)`
3. **ファイルの削除**: `deleteFile(String relativePath)`
4. **ストレージ使用量の取得**: `getUsedStorageSize()`

### クラス構造

```java
public class FileStorageManager {
    private static final String STORAGE_ROOT = "nagetatte";
    private final Context context;

    public FileStorageManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * ファイルを保存
     * @param inputStream 入力ストリーム
     * @param mimeType MIMEタイプ
     * @return SavedFile 保存情報
     */
    public SavedFile saveFile(InputStream inputStream, String mimeType) throws IOException {
        // 1. ファイルタイプディレクトリを決定
        // 2. UUID生成と拡張子取得
        // 3. ディレクトリ作成
        // 4. ファイル書き込み
        // 5. SavedFileを返す
    }

    /**
     * ファイルを取得
     * @param relativePath 相対パス（例: "images/uuid.jpg"）
     * @return File
     */
    public File getFile(String relativePath) {
        return new File(context.getFilesDir(), STORAGE_ROOT + "/" + relativePath);
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
     * ストレージ使用量を取得
     * @return 使用量（バイト）
     */
    public long getUsedStorageSize() {
        File rootDir = new File(context.getFilesDir(), STORAGE_ROOT);
        return calculateDirectorySize(rootDir);
    }

    private long calculateDirectorySize(File directory) {
        // ディレクトリの合計サイズを計算
    }
}
```

### SavedFile データクラス

ファイル保存結果を表すデータクラスです。

```java
public class SavedFile {
    private final String relativePath; // 例: "images/uuid.jpg"
    private final String fileName;     // 例: "uuid.jpg"
    private final long fileSize;       // バイト数
    private final String mimeType;     // 例: "image/jpeg"

    public SavedFile(String relativePath, String fileName,
                     long fileSize, String mimeType) {
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    // getter...
}
```

---

## FileProvider設定

外部アプリとファイルを安全に共有するための設定です。

### res/xml/file_paths.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- アプリ内部ストレージ -->
    <files-path
        name="app_files"
        path="nagetatte/" />
</paths>
```

### AndroidManifest.xml

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### ファイルを開く例

```java
File file = storageManager.getFile(itemFile.getFilePath());

Uri fileUri = FileProvider.getUriForFile(
    context,
    context.getPackageName() + ".fileprovider",
    file
);

Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setDataAndType(fileUri, itemFile.getMimeType());
intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

startActivity(intent);
```

---

## セキュリティ

### 1. ファイル名の予測不可能性

UUID v4を使用することで、ファイル名から内容を推測されることを防ぎます。

### 2. プライベートストレージの利用

`context.getFilesDir()`を使用することで、他のアプリからの直接アクセスを防ぎます。

### 3. FileProvider による安全な共有

直接ファイルパスを公開せず、`FileProvider`経由でURIを発行することで、一時的なアクセス権限のみを付与します。

### 4. 権限の最小化

プライベートストレージを使用することで、`READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE`権限が不要になります。

---

## まとめ

この設計により、以下が実現できます:

1. UUID v4によるファイル名の衝突回避
2. タイプ別ディレクトリによる整理
3. プライベートストレージによるセキュリティ向上
4. FileStorageManagerによる一元管理
5. FileProviderによる安全なファイル共有
6. 権限不要のシンプルな実装
