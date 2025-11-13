package jp.ac.meijou.android.nanndatteii.db.relation;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

import jp.ac.meijou.android.nanndatteii.db.entity.Item;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemTag;
import jp.ac.meijou.android.nanndatteii.db.entity.Tag;

public class ItemWithTags {
    @Embedded
    public Item item;

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = @Junction(
            value = ItemTag.class,
            parentColumn = "item_id",
            entityColumn = "tag_id"
        )
    )
    public List<Tag> tags;
}
