package jp.ac.meijou.android.nanndatteii.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(tableName = "item_tags",
        primaryKeys = {"item_id", "tag_id"},
        foreignKeys = {
            @ForeignKey(
                entity = Item.class,
                parentColumns = "id",
                childColumns = "item_id",
                onDelete = ForeignKey.CASCADE
            ),
            @ForeignKey(
                entity = Tag.class,
                parentColumns = "id",
                childColumns = "tag_id",
                onDelete = ForeignKey.CASCADE
            )
        },
        indices = {
            @Index(value = "item_id"),
            @Index(value = "tag_id")
        })
public class ItemTag {
    @ColumnInfo(name = "item_id")
    private long itemId;

    @ColumnInfo(name = "tag_id")
    private long tagId;

    // Constructors
    public ItemTag() {
    }

    @Ignore
    public ItemTag(long itemId, long tagId) {
        this.itemId = itemId;
        this.tagId = tagId;
    }

    // Getters
    public long getItemId() {
        return itemId;
    }

    public long getTagId() {
        return tagId;
    }

    // Setters
    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }
}
