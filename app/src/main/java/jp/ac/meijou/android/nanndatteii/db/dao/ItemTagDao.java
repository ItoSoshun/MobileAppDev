package jp.ac.meijou.android.nanndatteii.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import jp.ac.meijou.android.nanndatteii.db.entity.ItemTag;
import jp.ac.meijou.android.nanndatteii.db.entity.Tag;

@Dao
public interface ItemTagDao {
    @Insert
    void insert(ItemTag itemTag);

    @Delete
    void delete(ItemTag itemTag);

    @Query("DELETE FROM item_tags WHERE item_id = :itemId")
    void deleteByItemId(long itemId);

    @Query("SELECT * FROM tags WHERE id IN " +
           "(SELECT tag_id FROM item_tags WHERE item_id = :itemId)")
    LiveData<List<Tag>> getTagsForItem(long itemId);

    @Query("SELECT * FROM tags WHERE id IN " +
           "(SELECT tag_id FROM item_tags WHERE item_id = :itemId)")
    List<Tag> getTagsForItemSync(long itemId);

    @Query("SELECT COUNT(*) FROM item_tags WHERE tag_id = :tagId")
    LiveData<Integer> getItemCountForTag(long tagId);
}
