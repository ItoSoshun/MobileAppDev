package jp.ac.meijou.android.nanndatteii.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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

    // Constructors
    public ItemFile() {
    }

    // Getters
    public long getId() {
        return id;
    }

    public long getItemId() {
        return itemId;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    @NonNull
    public String getFileName() {
        return fileName;
    }

    @NonNull
    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public void setFilePath(@NonNull String filePath) {
        this.filePath = filePath;
    }

    public void setFileName(@NonNull String fileName) {
        this.fileName = fileName;
    }

    public void setFileType(@NonNull String fileType) {
        this.fileType = fileType;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setMimeType(@NonNull String mimeType) {
        this.mimeType = mimeType;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
