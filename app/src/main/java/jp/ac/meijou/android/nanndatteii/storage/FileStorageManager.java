package jp.ac.meijou.android.nanndatteii.storage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class FileStorageManager {
    private static final String TAG = "FileStorageManager";
    private static final String ROOT_DIR = "nagetatteii";
    private final Context context;

    public FileStorageManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * ファイルを保存（MediaStore APIを使用してDownloadディレクトリに保存）
     * @param inputStream 入力ストリーム
     * @param mimeType MIMEタイプ
     * @return 保存されたファイル情報
     */
    public SavedFile saveFile(InputStream inputStream, String mimeType) throws IOException {
        Log.d(TAG, "saveFile: 開始。MIMEタイプ: " + mimeType);

        // 1. ファイルタイプディレクトリを決定
        String typeDir = getFileTypeDirectory(mimeType);
        Log.d(TAG, "saveFile: ファイルタイプディレクトリ: " + typeDir);

        // 2. 拡張子を取得
        String extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType);
        if (extension == null) extension = "dat";

        // 3. UUIDファイル名を生成
        String fileName = UUID.randomUUID().toString() + "." + extension;
        Log.d(TAG, "saveFile: 生成されたファイル名: " + fileName);

        // 4. MediaStore APIを使用して保存
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10以降: MediaStore API使用
            return saveFileWithMediaStore(inputStream, fileName, mimeType, typeDir);
        } else {
            // Android 9以下: 従来の方法
            return saveFileLegacy(inputStream, fileName, mimeType, typeDir);
        }
    }

    /**
     * MediaStore APIを使用してファイルを保存（Android 10+）
     */
    private SavedFile saveFileWithMediaStore(InputStream inputStream, String fileName,
                                             String mimeType, String typeDir) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        // ContentValuesを設定
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + ROOT_DIR + "/" + typeDir);
        values.put(MediaStore.Downloads.IS_PENDING, 1); // 書き込み中フラグ

        // MediaStoreに挿入
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Failed to create MediaStore entry");
        }

        Log.d(TAG, "saveFileWithMediaStore: MediaStore URI: " + uri);

        long fileSize = 0;
        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Failed to open output stream");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                fileSize += bytesRead;
            }
            Log.d(TAG, "saveFileWithMediaStore: 書き込み完了。サイズ: " + fileSize + " bytes");
        }

        // 書き込み完了フラグを解除
        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);

        // 相対パスを返す
        String relativePath = typeDir + "/" + fileName;
        Log.d(TAG, "saveFileWithMediaStore: 完了。相対パス: " + relativePath);

        return new SavedFile(relativePath, fileName, fileSize, mimeType);
    }

    /**
     * 従来の方法でファイルを保存（Android 9以下）
     */
    private SavedFile saveFileLegacy(InputStream inputStream, String fileName,
                                     String mimeType, String typeDir) throws IOException {
        // Downloadsディレクトリ配下に保存
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File directory = new File(downloadDir, ROOT_DIR + "/" + typeDir);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            Log.d(TAG, "saveFileLegacy: ディレクトリ作成: " + directory.getAbsolutePath() + ", 成功: " + created);
        }

        File file = new File(directory, fileName);
        Log.d(TAG, "saveFileLegacy: ファイルに書き込み: " + file.getAbsolutePath());

        long fileSize = 0;
        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                fileSize += bytesRead;
            }
            Log.d(TAG, "saveFileLegacy: 書き込み完了。サイズ: " + fileSize + " bytes");
        }

        String relativePath = typeDir + "/" + fileName;
        return new SavedFile(relativePath, fileName, fileSize, mimeType);
    }

    /**
     * ファイルを読み込み
     * @param relativePath 相対パス（例: "images/uuid.jpg"）
     * @return Fileオブジェクト（Android 9以下のみ有効）
     */
    public File getFile(String relativePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.w(TAG, "getFile: Android 10+ではMediaStore経由でアクセスしてください");
            // フォールバック: Downloadディレクトリから取得を試みる
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            return new File(downloadDir, ROOT_DIR + "/" + relativePath);
        } else {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            return new File(downloadDir, ROOT_DIR + "/" + relativePath);
        }
    }

    /**
     * ファイルを削除
     * @param relativePath 相対パス
     * @return 削除成功したか
     */
    public boolean deleteFile(String relativePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10以降: MediaStoreから削除
            return deleteFileWithMediaStore(relativePath);
        } else {
            // Android 9以下: 直接削除
            File file = getFile(relativePath);
            boolean deleted = file.exists() && file.delete();
            Log.d(TAG, "deleteFile: " + relativePath + ", 削除成功: " + deleted);
            return deleted;
        }
    }

    /**
     * MediaStore経由でファイルを削除（Android 10+）
     */
    private boolean deleteFileWithMediaStore(String relativePath) {
        ContentResolver resolver = context.getContentResolver();

        // ファイル名を取得
        String fileName = new File(relativePath).getName();

        // MediaStoreから検索
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Downloads.DISPLAY_NAME + "=?";
        String[] selectionArgs = new String[]{fileName};

        int deletedRows = resolver.delete(collection, selection, selectionArgs);
        Log.d(TAG, "deleteFileWithMediaStore: " + relativePath + ", 削除行数: " + deletedRows);
        return deletedRows > 0;
    }

    /**
     * ファイルをコピー
     * @param sourceFile コピー元
     * @param mimeType MIMEタイプ
     * @return 新しいファイル情報
     */
    public SavedFile copyFile(File sourceFile, String mimeType) throws IOException {
        Log.d(TAG, "copyFile: " + sourceFile.getAbsolutePath() + ", MIMEタイプ: " + mimeType);
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
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(downloadDir, ROOT_DIR);
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

    /**
     * 使用容量をフォーマットして取得
     */
    public String getStorageUsageFormatted() {
        long usedBytes = getUsedStorageSize();

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
}
