package jp.ac.meijou.android.nanndatteii.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import jp.ac.meijou.android.nanndatteii.db.entity.ItemFile;

@Dao
public interface FileDao {
    @Insert
    long insert(ItemFile file);

    @Update
    void update(ItemFile file);

    @Delete
    void delete(ItemFile file);

    @Query("SELECT * FROM files WHERE item_id = :itemId")
    LiveData<List<ItemFile>> getFilesByItemId(long itemId);

    @Query("SELECT * FROM files WHERE item_id = :itemId")
    List<ItemFile> getFilesByItemIdSync(long itemId);

    @Query("SELECT * FROM files WHERE file_type = :fileType")
    LiveData<List<ItemFile>> getFilesByType(String fileType);

    @Query("SELECT file_path FROM files")
    List<String> getAllFilePaths();
}
