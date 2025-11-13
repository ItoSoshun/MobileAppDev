package jp.ac.meijou.android.nanndatteii.db.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import jp.ac.meijou.android.nanndatteii.db.entity.Item;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemFile;

public class ItemWithFiles {
    @Embedded
    public Item item;

    @Relation(
        parentColumn = "id",
        entityColumn = "item_id"
    )
    public List<ItemFile> files;
}
