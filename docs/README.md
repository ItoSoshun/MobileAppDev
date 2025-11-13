# 「投げたっていい。」設計ドキュメント

## 概要

このディレクトリには、「投げたっていい。」アプリのファイル保存・データベース設計に関するドキュメントが格納されています。

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

### 3. [implementation-guide.md](./implementation-guide.md)
**実装ガイド**

- Phase 0～5 の詳細な実装手順
- Repository パターンの実装
- UI層の更新ガイド
- テスト戦略

**主なポイント:**
- 段階的な実装アプローチ
- 16-25日の実装タイムライン
- 既存コードとの並行動作による安全な移行

---

## 設計の背景

### 現状の問題点

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

- **Item**: 投稿の基本情報
- **ItemFile**: ファイルの詳細情報
- **Tag**: タグマスター
- **ItemTag**: アイテムとタグの関連

### Relation（リレーション）

- **ItemWithFiles**: アイテム + ファイル一覧
- **ItemWithTags**: アイテム + タグ一覧
- **ItemWithFilesAndTags**: 完全な情報（アイテム + ファイル + タグ）

---

## ファイル構造

### 新しいストレージ構造

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

## 実装の開始方法

### 1. ドキュメントを読む

1. **database-design.md** でデータベース設計を理解
2. **file-storage-design.md** でファイルストレージを理解
3. **implementation-guide.md** で実装手順を確認

### 2. Phase 0 から開始

```gradle
// gradle/libs.versions.toml に Room を追加
[versions]
room = "2.6.1"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

### 3. Entity クラスから実装

```java
// db/entity/Item.java から作成開始
@Entity(tableName = "items")
public class Item {
    @PrimaryKey(autoGenerate = true)
    private long id;
    // ...
}
```

### 4. 段階的に実装

Phase 0 → Phase 1 → ... → Phase 5 の順に実装し、各Phaseごとに動作確認を行います。

---

## テスト計画

### 単体テスト

- [ ] ItemDao のCRUD操作
- [ ] FileDao のCRUD操作
- [ ] TagDao のCRUD操作
- [ ] ItemTagDao のCRUD操作
- [ ] FileStorageManager のファイル操作

### 統合テスト

- [ ] アイテム作成（複数ファイル + タグ）
- [ ] タグによるフィルタリング
- [ ] アイテム削除（ファイルも削除）
- [ ] 孤立ファイルのクリーニング

### パフォーマンステスト

- [ ] 1000件のアイテム読み込み速度
- [ ] 画像のキャッシュ動作
- [ ] メモリ使用量の確認

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

---

## 参考リンク

- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Android Data Storage](https://developer.android.com/training/data-storage)
- [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)
- [LiveData Overview](https://developer.android.com/topic/libraries/architecture/livedata)

---

## 変更履歴

| 日付 | バージョン | 変更内容 |
|------|----------|---------|
| 2025-01-13 | 1.0.0 | 初版作成（データベース・ファイルストレージ設計） |

---

## お問い合わせ

実装中に不明点や問題が発生した場合は、以下を参照してください:

1. 各ドキュメントの該当セクション
2. 実装ガイドのトラブルシューティング
3. プロジェクトのIssueトラッカー

---

## ライセンス

このドキュメントは「投げたっていい。」プロジェクトの一部であり、プロジェクトと同じライセンスが適用されます。
