package jp.ac.meijou.android.nanndatteii.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import jp.ac.meijou.android.nanndatteii.db.entity.Item;

@Dao
public interface ItemDao {
    @Insert
    long insert(Item item);

    @Update
    void update(Item item);

    @Delete
    void delete(Item item);

    @Query("SELECT * FROM items ORDER BY created_at DESC")
    LiveData<List<Item>> getAllItems();

    @Query("SELECT * FROM items WHERE id = :itemId")
    LiveData<Item> getItemById(long itemId);

    @Query("SELECT * FROM items WHERE id = :itemId")
    Item getItemByIdSync(long itemId);

    @Query("UPDATE items SET last_viewed = :timestamp WHERE id = :itemId")
    void updateLastViewed(long itemId, long timestamp);

    @Query("SELECT * FROM items ORDER BY last_viewed DESC LIMIT 10")
    LiveData<List<Item>> getRecentlyViewedItems();

    @Query("SELECT * FROM items WHERE id IN " +
           "(SELECT item_id FROM item_tags WHERE tag_id = :tagId) " +
           "ORDER BY created_at DESC")
    LiveData<List<Item>> getItemsByTag(long tagId);
}
