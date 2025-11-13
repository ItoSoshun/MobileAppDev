package jp.ac.meijou.android.nanndatteii.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.ac.meijou.android.nanndatteii.db.AppDatabase;
import jp.ac.meijou.android.nanndatteii.db.dao.ItemTagDao;
import jp.ac.meijou.android.nanndatteii.db.dao.TagDao;
import jp.ac.meijou.android.nanndatteii.db.entity.Tag;

public class TagRepository {
    private final TagDao tagDao;
    private final ItemTagDao itemTagDao;
    private final ExecutorService executorService;

    public TagRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        tagDao = db.tagDao();
        itemTagDao = db.itemTagDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * すべてのタグを取得
     */
    public LiveData<List<Tag>> getAllTags() {
        return tagDao.getAllTags();
    }

    /**
     * タグIDでタグを取得
     */
    public LiveData<Tag> getTagById(long tagId) {
        return tagDao.getTagById(tagId);
    }

    /**
     * タグ名でタグを取得（同期）
     */
    public Tag getTagByNameSync(String name) {
        return tagDao.getTagByNameSync(name);
    }

    /**
     * アイテムに紐づくタグを取得
     */
    public LiveData<List<Tag>> getTagsForItem(long itemId) {
        return itemTagDao.getTagsForItem(itemId);
    }

    /**
     * タグに紐づくアイテム数を取得
     */
    public LiveData<Integer> getItemCountForTag(long tagId) {
        return itemTagDao.getItemCountForTag(tagId);
    }

    /**
     * タグを挿入
     */
    public void insertTag(Tag tag, OnTagInsertedListener listener) {
        executorService.execute(() -> {
            try {
                // 重複チェック
                Tag existing = tagDao.getTagByNameSync(tag.getName());
                if (existing != null) {
                    if (listener != null) {
                        listener.onError(new Exception("タグは既に存在します"));
                    }
                    return;
                }

                tag.setCreatedAt(System.currentTimeMillis());
                long tagId = tagDao.insert(tag);
                if (listener != null) {
                    listener.onSuccess(tagId);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * タグを更新
     */
    public void updateTag(Tag tag, OnTagUpdatedListener listener) {
        executorService.execute(() -> {
            try {
                tagDao.update(tag);
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
     * タグを削除
     */
    public void deleteTag(Tag tag, OnTagDeletedListener listener) {
        executorService.execute(() -> {
            try {
                tagDao.delete(tag);
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
     * タグを名前で削除
     */
    public void deleteTagByName(String name, OnTagDeletedListener listener) {
        executorService.execute(() -> {
            try {
                Tag tag = tagDao.getTagByNameSync(name);
                if (tag != null) {
                    tagDao.delete(tag);
                    if (listener != null) {
                        listener.onSuccess();
                    }
                } else {
                    if (listener != null) {
                        listener.onError(new Exception("タグが見つかりません"));
                    }
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // コールバックインターフェース
    public interface OnTagInsertedListener {
        void onSuccess(long tagId);
        void onError(Exception e);
    }

    public interface OnTagUpdatedListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnTagDeletedListener {
        void onSuccess();
        void onError(Exception e);
    }
}
