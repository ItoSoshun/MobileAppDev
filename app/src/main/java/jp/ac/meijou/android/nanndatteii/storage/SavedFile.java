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
