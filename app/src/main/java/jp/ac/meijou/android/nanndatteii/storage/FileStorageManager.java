package jp.ac.meijou.android.nanndatteii.storage;

import android.content.Context;
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
