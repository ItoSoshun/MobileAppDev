# データベース・ファイルストレージ ドキュメント

## 概要

このディレクトリには、アプリのデータベースとファイルストレージシステムに関するドキュメントが格納されています。

---

## ドキュメント一覧

### 1. [database-design.md](./database-design.md)
データベース設計書

- テーブル定義（Items, Files, Tags, Item_Tags）
- Entity、DAO、Relationの説明
- データモデルとリレーション

### 2. [file-storage-design.md](./file-storage-design.md)
ファイルストレージ設計書

- UUID ベースのファイル命名規則
- ファイルタイプ別ディレクトリ構造
- プライベートストレージの仕組み

### 3. [usage-guide.md](./usage-guide.md)
使い方ガイド

- ItemRepository の使い方
- TagRepository の使い方
- FileStorageManager の使い方
- 実装例とコードサンプル

---

## アーキテクチャ

```
┌─────────────────────────────────────────────┐
│         UI Layer (Fragment, Adapter)        │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│    Domain Layer (Repository Pattern)        │
│  - ItemRepository                            │
│  - TagRepository                             │
└────────────┬────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│           Data Layer                         │
│  ┌──────────────┐    ┌──────────────────┐  │
│  │ Room DB      │    │ FileStorage      │  │
│  │ - DAO        │    │ - UUID naming    │  │
│  │ - Entity     │    │ - Type dirs      │  │
│  └──────────────┘    └──────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## データモデル

### Entity
- **Item**: 投稿の基本情報
- **ItemFile**: ファイルの詳細情報
- **Tag**: タグマスター
- **ItemTag**: アイテムとタグの関連

### Relation
- **ItemWithFiles**: アイテム + ファイル一覧
- **ItemWithTags**: アイテム + タグ一覧
- **ItemWithFilesAndTags**: 完全な情報

---

## ストレージ構造

```
/data/data/jp.ac.meijou.android.nanndatteii/files/nagetatte/
├── images/
│   └── <UUID>.jpg
├── texts/
│   └── <UUID>.txt
├── documents/
│   └── <UUID>.pdf
├── videos/
│   └── <UUID>.mp4
└── others/
    └── <UUID>.dat
```

---

## データベース構造

```
app_data.db (Room Database)
├── items
├── files
├── tags
└── item_tags
```

---

## 参考リンク

- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)
- [LiveData Overview](https://developer.android.com/topic/libraries/architecture/livedata)
- [Repository Pattern](https://developer.android.com/topic/architecture/data-layer)
