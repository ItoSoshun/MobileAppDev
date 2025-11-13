package jp.ac.meijou.android.nanndatteii.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tags",
        indices = @Index(value = "name", unique = true))
public class Tag {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "name")
    @NonNull
    private String name;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructors
    public Tag() {
    }

    // Getters
    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
