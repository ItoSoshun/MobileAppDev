package jp.ac.meijou.android.nanndatteii.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.ac.meijou.android.nanndatteii.db.AppDatabase;
import jp.ac.meijou.android.nanndatteii.db.dao.FileDao;
import jp.ac.meijou.android.nanndatteii.db.dao.ItemDao;
import jp.ac.meijou.android.nanndatteii.db.dao.ItemTagDao;
import jp.ac.meijou.android.nanndatteii.db.entity.Item;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemFile;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemTag;
import jp.ac.meijou.android.nanndatteii.storage.FileStorageManager;

public class ItemRepository {
    private final ItemDao itemDao;
    private final FileDao fileDao;
    private final ItemTagDao itemTagDao;
    private final ExecutorService executorService;

    public ItemRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        itemDao = db.itemDao();
        fileDao = db.fileDao();
        itemTagDao = db.itemTagDao();
        executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * アイテムを作成
     * @param item アイテム
     * @param files ファイルリスト
     * @param tagIds タグIDリスト
     * @param listener コールバック
     */
    public void createItem(Item item, List<ItemFile> files, List<Long> tagIds, OnItemCreatedListener listener) {
        executorService.execute(() -> {
            try {
                // 1. アイテムを挿入
                long itemId = itemDao.insert(item);

                // 2. ファイルを挿入
                if (files != null) {
                    for (ItemFile file : files) {
                        file.setItemId(itemId);
                        fileDao.insert(file);
                    }
                }

                // 3. タグを関連付け
                if (tagIds != null) {
                    for (Long tagId : tagIds) {
                        ItemTag itemTag = new ItemTag();
                        itemTag.setItemId(itemId);
                        itemTag.setTagId(tagId);
                        itemTagDao.insert(itemTag);
                    }
                }

                if (listener != null) {
                    listener.onSuccess(itemId);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * すべてのアイテムを取得
     */
    public LiveData<List<Item>> getAllItems() {
        return itemDao.getAllItems();
    }

    /**
     * タグでフィルタリングしたアイテムを取得
     */
    public LiveData<List<Item>> getItemsByTag(long tagId) {
        return itemDao.getItemsByTag(tagId);
    }

    /**
     * アイテムIDでアイテムを取得
     */
    public LiveData<Item> getItemById(long itemId) {
        return itemDao.getItemById(itemId);
    }

    /**
     * 最近閲覧したアイテムを取得
     */
    public LiveData<List<Item>> getRecentlyViewedItems() {
        return itemDao.getRecentlyViewedItems();
    }

    /**
     * 閲覧日時を更新
     */
    public void updateLastViewed(long itemId) {
        executorService.execute(() -> {
            itemDao.updateLastViewed(itemId, System.currentTimeMillis());
        });
    }

    /**
     * アイテムを更新
     */
    public void updateItem(Item item, OnItemUpdatedListener listener) {
        executorService.execute(() -> {
            try {
                item.setUpdatedAt(System.currentTimeMillis());
                itemDao.update(item);
                if (listener != null) {
                    listener.onSuccess();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * アイテムを削除（ファイルも削除）
     */
    public void deleteItem(long itemId, FileStorageManager storageManager, OnItemDeletedListener listener) {
        executorService.execute(() -> {
            try {
                // 1. ファイルを取得
                List<ItemFile> files = fileDao.getFilesByItemIdSync(itemId);

                // 2. 物理ファイルを削除
                if (files != null && storageManager != null) {
                    for (ItemFile file : files) {
                        storageManager.deleteFile(file.getFilePath());
                    }
                }

                // 3. データベースから削除（CASCADE設定により関連レコードも自動削除）
                Item item = itemDao.getItemByIdSync(itemId);
                if (item != null) {
                    itemDao.delete(item);
                }

                if (listener != null) {
                    listener.onSuccess();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * アイテムにタグを追加
     */
    public void addTagToItem(long itemId, long tagId, OnTagAddedListener listener) {
        executorService.execute(() -> {
            try {
                ItemTag itemTag = new ItemTag(itemId, tagId);
                itemTagDao.insert(itemTag);
                if (listener != null) {
                    listener.onSuccess();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * アイテムからタグを削除
     */
    public void removeTagFromItem(long itemId, long tagId, OnTagRemovedListener listener) {
        executorService.execute(() -> {
            try {
                ItemTag itemTag = new ItemTag(itemId, tagId);
                itemTagDao.delete(itemTag);
                if (listener != null) {
                    listener.onSuccess();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * アイテムのファイル一覧を取得
     */
    public LiveData<List<ItemFile>> getFilesByItemId(long itemId) {
        return fileDao.getFilesByItemId(itemId);
    }

    // コールバックインターフェース
    public interface OnItemCreatedListener {
        void onSuccess(long itemId);
        void onError(Exception e);
    }

    public interface OnItemUpdatedListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnItemDeletedListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnTagAddedListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnTagRemovedListener {
        void onSuccess();
        void onError(Exception e);
    }
}
