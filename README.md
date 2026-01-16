# MobHealth

[English](#english) | [日本語](#japanese)

---

<a name="english"></a>
## English

**MobHealth** is a Minecraft (Paper 1.21.1) plugin that displays mob health and damage indicators using Text Displays. It is designed to be lightweight and high-performance using ProtocolLib.

### Features
- **HP Bar Display**: Shows the mob's current HP above their head when damaged.
- **Damage Indicators**: Displays damage values floating from the mob's location.
- **Configurable**: Customize messages, display formats, view distance, and blacklisted mobs.
- **Optimized**: Uses packet-based Text Displays (ProtocolLib) for high performance.
- **Toggleable**: Players can toggle the display on/off individually.

### Requirements
- **Paper 1.21.1** (or compatible 1.21.x server)
- **[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)** (Latest build for 1.21)

### Commands
| Command | Description | Permission | Default |
|---|---|---|---|
| `/health toggle` | Toggle the MobHealth display for yourself | `mobhealth.see` | `true` (Everyone) |
| `/health reload` | Reload the plugin configuration | `mobhealth.reload` | `op` (Operators) |

### Configuration
You can customize the plugin in `config.yml`.
```yaml
# Display Settings
display:
  hp-format: "&aHP: &a{hp}"
  damage-format: "&c- {damage}"
  view-distance: 16.0 # Max distance to see HP bars

# Blacklist
blacklist:
  entities:
    - ARMOR_STAND
```

### Installation
1. Download `mobhealth-1.0.2.jar`.
2. Place it in your server's `plugins` folder.
3. Make sure you have **ProtocolLib** installed.
4. Restart the server.

---

<a name="japanese"></a>
## 日本語

**MobHealth** は、ProtocolLib を使用して Mob の HP とダメージ表記を軽量かつ高性能に表示する Minecraft (Paper 1.21.1) 用プラグインです。Text Display エンティティを使用しているため、視認性が高くサーバーへの負荷も最小限に抑えられています。

### 機能
- **HPバー表示**: Mob がダメージを受けた際、頭上に現在の HP を表示します。
- **ダメージ表記**: ダメージを与えた際、その数値をポップアップ表示します。
- **設定可能**: メッセージ、表示フォーマット、表示距離、除外する Mob などを `config.yml` で自由に変更可能です。
- **最適化**: ProtocolLib を使用したパケットベースの処理により、大量の Mob がいても軽量に動作します。
- **切り替え機能**: プレイヤーごとに表示の ON/OFF を切り替えることができます。

### 必須要件
- **Paper 1.21.1** (または互換性のある 1.21.x サーバー)
- **[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)** (1.21 対応の最新ビルド)

### コマンド
| コマンド | 説明 | 権限 (Permission) | デフォルト |
|---|---|---|---|
| `/health toggle` | 自分の HP 表示の ON/OFF を切り替えます | `mobhealth.see` | `true` (全員) |
| `/health reload` | 設定ファイルを再読み込みします | `mobhealth.reload` | `op` (管理者のみ) |

### 設定 (Configuration)
`config.yml` で以下の設定が可能です。
```yaml
# 表示設定
display:
  hp-format: "&aHP: &a{hp}"      # HPバーのフォーマット
  damage-format: "&c- {damage}"  # ダメージ表記のフォーマット
  view-distance: 16.0            # HPバーを表示する最大距離 (ブロック)

# 除外設定 (ブラックリスト)
blacklist:
  entities:
    - ARMOR_STAND                # 表示しないエンティティタイプ
    - VILLAGER                   # 例: 村人を除外する場合
```

### インストール方法
1. `mobhealth-1.0.2.jar` をダウンロードします。
2. サーバーの `plugins` フォルダに入れます。
3. **ProtocolLib** が導入されていることを確認してください。
4. サーバーを再起動します。

## Author
- [naonao](https://github.com/naonao0319)
- Repository: [https://github.com/naonao0319/mobhealth](https://github.com/naonao0319/mobhealth)
