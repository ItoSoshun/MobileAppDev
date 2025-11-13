package jp.ac.meijou.android.nanndatteii.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import jp.ac.meijou.android.nanndatteii.db.entity.Tag;

@Dao
public interface TagDao {
    @Insert
    long insert(Tag tag);

    @Update
    void update(Tag tag);

    @Delete
    void delete(Tag tag);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    LiveData<List<Tag>> getAllTags();

    @Query("SELECT * FROM tags WHERE name = :name")
    Tag getTagByName(String name);

    @Query("SELECT * FROM tags WHERE name = :name")
    Tag getTagByNameSync(String name);

    @Query("SELECT * FROM tags WHERE id = :tagId")
    LiveData<Tag> getTagById(long tagId);

    @Query("SELECT * FROM tags WHERE id = :tagId")
    Tag getTagByIdSync(long tagId);
}
