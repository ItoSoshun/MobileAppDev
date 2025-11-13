# 「投げたっていい。」設計ドキュメント

## 概要

このディレクトリには、「投げたっていい。」アプリのファイル保存・データベース設計に関するドキュメントが格納されています。

## 実装状況

現在、以下のフェーズが完了しています：

- Phase 0: Room依存関係の追加
- Phase 1: データベース実装（Entity、DAO、Relation、AppDatabase）
- Phase 2: ファイルストレージ実装（FileStorageManager）
- Phase 3: リポジトリパターン実装（ItemRepository、TagRepository）
- Phase 4: UI層更新（HomeFragment、DashboardFragment、ItemAdapter）

次のステップは Phase 5（テストと最適化）です。

---

## ドキュメント一覧

### 1. [database-design.md](./database-design.md)
**データベース設計書**

- Room Database 設計
- テーブル定義（Items, Files, Tags, Item_Tags）
- DAO（Data Access Object）の設計
- リレーションとクエリの実装例

**主なポイント:**
- 複数ファイルを一つのアイテムとして管理
- タグによる多対多の関連
- メタデータの完全な管理（投稿日時、閲覧日時など）
- LiveDataによるリアクティブなUI更新

### 2. [file-storage-design.md](./file-storage-design.md)
**ファイルストレージ設計書**

- プライベートストレージ設計
- UUID v4 ベースのファイル命名規則
- ファイルタイプ別ディレクトリ構造
- FileStorageManager の実装
- エクスポート機能とセキュリティ

**主なポイント:**
- タグ非依存のファイル管理
- ファイル名の衝突回避
- 型別整理（images, texts, documents, videos, others）
- セキュアなプライベートストレージの活用

### 3. [database-usage-guide.md](./database-usage-guide.md)
**データベース使用ガイド**（実装済み機能の使い方）

- 現在のデータベースの仕組み
- Repository パターンの使い方
- FileStorageManager の使い方
- 実装例とコードサンプル

**主なポイント:**
- ItemRepository と TagRepository の使い方
- アイテム作成・取得・削除の実装例
- ファイル保存・取得の実装例
- LiveData によるUI更新の実装方法

### 4. [implementation-guide.md](./implementation-guide.md)
**実装ガイド**（次のステップ）

- Phase 5 の詳細な実装手順（テストと最適化）
- テスト戦略
- パフォーマンス最適化
- エラーハンドリング

**主なポイント:**
- 単体テスト・統合テストの実装方法
- パフォーマンステストとクエリ最適化
- 孤立ファイルのクリーンアップ
- エラーハンドリングのベストプラクティス

---

## 設計の背景

### 旧実装の問題点

従来の実装では:
1. タグ別フォルダにファイルが散らばる
2. 複数ファイルを一つのアイテムとして紐付けられない
3. ファイル名にタグ名が含まれ、柔軟性に欠ける
4. メタデータがファイルシステムに依存
5. 素のSQLiteで型安全性が低い

### 新設計の利点

新しい設計により:
1. **アイテム単位の管理**: 写真+メモを一つの投稿として扱える
2. **タグの柔軟性**: 一つのアイテムに複数タグを付与可能
3. **メタデータ管理**: 投稿日時・閲覧日時などをDBで管理
4. **型安全性**: Roomによるコンパイル時検証
5. **リアクティブUI**: LiveDataによる自動UI更新
6. **セキュリティ**: プライベートストレージによる保護
7. **拡張性**: 将来の自動解析機能を見据えた構造

---

## アーキテクチャ概要

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  (Fragment, ViewModel, RecyclerView Adapter)             │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                         │
│  (Repository Pattern - ItemRepository, TagRepository)    │
└────────────┬────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                          │
│                                                           │
│  ┌─────────────────┐          ┌──────────────────────┐  │
│  │   Room Database │          │ FileStorageManager   │  │
│  │                 │          │                      │  │
│  │  - ItemDao      │          │  UUID-based naming   │  │
│  │  - FileDao      │          │  Type directories    │  │
│  │  - TagDao       │          │  Private storage     │  │
│  │  - ItemTagDao   │          │                      │  │
│  └─────────────────┘          └──────────────────────┘  │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

---

## データモデル概要

### Entity（エンティティ）

- **Item**: 投稿の基本情報（id, description, created_at, updated_at, last_viewed）
- **ItemFile**: ファイルの詳細情報（id, item_id, file_path, file_name, file_type, file_size, mime_type, created_at）
- **Tag**: タグマスター（id, name, color, created_at）
- **ItemTag**: アイテムとタグの関連（item_id, tag_id）

### Relation（リレーション）

- **ItemWithFiles**: アイテム + ファイル一覧
- **ItemWithTags**: アイテム + タグ一覧
- **ItemWithFilesAndTags**: 完全な情報（アイテム + ファイル + タグ）

---

## ファイル構造

### ストレージ構造

```
/data/data/jp.ac.meijou.android.nanndatteii/files/
└── nagetatte/
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

### データベース構造

```
app_data.db (Room Database)
├── items (アイテム)
├── files (ファイル)
├── tags (タグ)
└── item_tags (中間テーブル)
```

---

## プロジェクト構造

```
app/src/main/java/jp/ac/meijou/android/nanndatteii/
├── db/
│   ├── AppDatabase.java
│   ├── dao/
│   │   ├── ItemDao.java
│   │   ├── FileDao.java
│   │   ├── TagDao.java
│   │   └── ItemTagDao.java
│   ├── entity/
│   │   ├── Item.java
│   │   ├── ItemFile.java
│   │   ├── Tag.java
│   │   └── ItemTag.java
│   └── relation/
│       ├── ItemWithFiles.java
│       ├── ItemWithTags.java
│       └── ItemWithFilesAndTags.java
├── storage/
│   ├── FileStorageManager.java
│   └── SavedFile.java
├── repository/
│   ├── ItemRepository.java
│   └── TagRepository.java
└── ui/
    ├── home/
    │   └── HomeFragment.java
    └── dashboard/
        ├── DashboardFragment.java
        └── ItemAdapter.java
```

---

## 使い方

### 1. 現在の機能を理解する

`database-usage-guide.md` を読んで、実装済みのデータベースとRepositoryの使い方を理解してください。

### 2. 開発を進める

アプリケーションの開発を継続し、必要に応じて機能を追加してください。

### 3. テストと最適化

`implementation-guide.md` を参照して、Phase 5（テストと最適化）を実施してください。

---

## 将来の拡張

### 自動解析機能（構想）

1. **画像認識**: ML Kit による物体検出
2. **OCR**: テキスト抽出
3. **自動タグ付け**: 検出結果から自動タグ生成
4. **自動キャプション**: 画像内容の説明文生成

### 追加テーブル例

```java
@Entity(tableName = "auto_analysis")
public class AutoAnalysis {
    private long id;
    private long fileId;
    private String detectedObjects; // JSON
    private String suggestedTags;   // CSV
    private String ocrText;
    private long analyzedAt;
}
```

### その他の拡張機能

1. **検索機能**: 全文検索の実装
2. **エクスポート機能**: ZIPファイルでのバックアップ
3. **クラウド同期**: Firebase Storageとの連携
4. **画像編集**: トリミング・フィルター機能
5. **共有機能**: SNSへの投稿

---

## 参考リンク

- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Android Data Storage](https://developer.android.com/training/data-storage)
- [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)
- [LiveData Overview](https://developer.android.com/topic/libraries/architecture/livedata)
- [Repository Pattern](https://developer.android.com/topic/architecture/data-layer)

---

## 変更履歴

| 日付 | バージョン | 変更内容 |
|------|----------|---------|
| 2025-01-13 | 1.0.0 | 初版作成（データベース・ファイルストレージ設計） |
| 2025-01-13 | 2.0.0 | Phase 0-4 実装完了、ドキュメント更新 |

---

## お問い合わせ

実装中に不明点や問題が発生した場合は、以下を参照してください:

1. 各ドキュメントの該当セクション
2. 実装ガイドのトラブルシューティング
3. プロジェクトのIssueトラッカー

---

## ライセンス

このドキュメントは「投げたっていい。」プロジェクトの一部であり、プロジェクトと同じライセンスが適用されます。
