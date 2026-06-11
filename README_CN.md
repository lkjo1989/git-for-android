# Git For Android（中文说明）

一款双模式 Android Git 客户端，同时支持 GUI 和 CLI 终端界面，基于 JGit 实现——无需原生 git 二进制文件。

> English version: [README.md](README.md)

## 功能特性

- **GUI 模式** — Jetpack Compose + Material 3 界面，覆盖常用 Git 操作：状态查看、提交、日志、分支、推送/拉取
- **CLI 终端模式** — 完整的 git 命令行 REPL：init、clone、status、add、commit、push/pull、log、branch、checkout、merge、diff、stash 等。init/clone 无需预先选择仓库即可使用
- **Git 引擎** — 基于 JGit 6.10，完全在设备端运行，无外部依赖
- **仓库管理** — 克隆、列出、切换仓库；任意模式下添加的仓库在另一模式中立即可见
- **SSH 与凭证** — 支持 SSH 密钥认证及用户名/密码凭证
- **Room 数据库** — 本地存储仓库元数据和设置

## 终端模式

终端提供一个 git 命令行 REPL（读取-求值-输出循环）。它解析标准 git 命令并通过 JGit 执行——不需要原生 git。

### 快速上手

1. 打开应用，切换到**终端（Terminal）**标签页
2. 创建第一个仓库：`git init myproject` 或 `git clone https://github.com/user/repo.git`
3. 新建的仓库会被自动选中——`git status`、`git add`、`git commit` 立即可用
4. 使用 `repo list` 和 `repo use <id>` 在仓库之间切换

### 命令分发逻辑

```
用户输入
  ├── "" (空输入) → 无操作
  ├── "help" → 显示命令参考
  ├── "clear" / "cls" → 清屏
  ├── "pwd" → 显示当前仓库路径
  ├── "repo <子命令>" → 仓库管理（无需预先选择仓库）
  │   ├── "repo list" → 显示数据库中所有仓库
  │   ├── "repo use <id>" → 通过 ID 选择仓库
  │   └── "repo current" → 显示当前选中的仓库信息及路径
  ├── "git init [名称]" → 创建新仓库（无需预先选择！）→ 自动选中
  ├── "git clone <url> [路径]" → 克隆仓库（无需预先选择！）→ 自动选中
  ├── "git config <键> [值]" → 设置/查看作者信息（无需预先选择）
  └── "git <命令>" → 需要已选中仓库（仅有一个仓库时自动选中）
      ├── status, add, commit, push, pull, fetch
      ├── log, branch, checkout, merge, diff, stash
      └── remote
```

### 核心行为

- **`git init` 和 `git clone`** 无需预先选择仓库。新创建的仓库自动选中，后续命令立即可用。
- **自动选中**：如果数据库中只有一个仓库，输入任意 git 命令时无需 `repo use`，会自动选中该仓库。
- **双模式同步**：在 GUI（仓库标签页）中克隆的仓库会立即出现在终端的 `repo list` 中。在终端通过 `git init`/`git clone` 创建的仓库也会立即出现在 GUI 仓库列表中。
- **命令历史**：上下箭头键浏览命令历史（最多 500 条）。正在输入的内容会被保留——向下翻过历史末尾会恢复你正在输入的文字。
- **进度反馈**：克隆操作会在终端中显示实时进度信息。

### 内置命令

| 命令 | 说明 |
|------|------|
| `help` | 显示完整命令参考 |
| `clear` / `cls` | 清空终端输出 |
| `pwd` | 显示当前仓库的文件系统路径 |
| `repo list` | 列出所有仓库（ID 和名称） |
| `repo use <id>` | 通过 ID 选择仓库 |
| `repo current` | 显示当前选中的仓库信息 |

### Git 命令

| 命令 | 说明 |
|------|------|
| `git init [名称]` | 创建新仓库。未指定名称时默认为 `my-repo`。仓库创建在应用的私有文件目录下 |
| `git clone <url> [路径]` | 克隆远程仓库。传输过程中显示进度。完成后自动选中 |
| `git config user.name <名称>` | 设置提交作者名称 |
| `git config user.email <邮箱>` | 设置提交作者邮箱 |
| `git config user.name` | 查看当前作者名称 |
| `git config user.email` | 查看当前作者邮箱 |
| `git status` | 显示分支、已暂存/未暂存/未跟踪的变更 |
| `git add <文件...>` | 暂存文件（无参数时默认暂存全部 `.`） |
| `git commit -m <消息>` | 提交已暂存的变更 |
| `git push [远程] [分支]` | 推送到远程（默认 `origin`） |
| `git pull [远程] [分支]` | 从远程拉取 |
| `git fetch [远程]` | 从远程获取 |
| `git log [-n <数量>] [--oneline]` | 显示提交历史 |
| `git branch [-a] [-d <名称>]` | 列出分支，或创建/删除分支 |
| `git checkout <分支> [-b]` | 切换分支（支持远程跟踪分支） |
| `git merge <分支>` | 将分支合并到当前分支 |
| `git diff [--staged] [路径]` | 显示工作区变更 |
| `git stash [pop\|list]` | 暂存工作区、恢复暂存、或列出暂存列表 |

### 使用场景示例

**场景 1：首次使用（无仓库）**
```
git init hello-world
  → "Initialized empty Git repository in .../repos/hello-world"
  → "Auto-selected repo 'hello-world' [id=1]"
git status
  → "On branch main\nnothing to commit, working tree clean"
```

**场景 2：只有一个仓库（GUI 克隆或终端创建）**
```
git status
  → "Auto-selected repo 'my-repo' [id=1]"
  → "On branch main\nChanges:\n  modified: README.md"
```

**场景 3：有多个仓库**
```
git status
  → "No repository selected. Use 'repo list' and 'repo use <id>' to select one."
repo list
  → "  [1] project-a\n  [2] project-b"
repo use 2
  → "Switched to repo: project-b [id=2]"
```

**场景 4：设置作者并提交**
```
git config user.name "Alice"
  → "Set user.name = Alice"
git config user.email "alice@example.com"
  → "Set user.email = alice@example.com"
git add -A
git commit -m "feat: 初始实现"
  → "Committed: a1b2c3d - feat: 初始实现"
```

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.24 |
| UI 框架 | Jetpack Compose + Material 3 |
| 构建系统 | Gradle 8.8 + AGP 8.4.1 |
| 依赖注入 | Hilt 2.51.1 (KSP) |
| Git 引擎 | JGit 6.10.0 |
| 数据库 | Room 2.6.1 |
| 架构 | MVVM + Clean Architecture (UI → Domain → Data) |

## 系统要求

- Android 8.0+ (API 26)
- JDK 17

## 构建

```bash
# 设置 JDK 17 到 PATH
source set-jdk17.sh

# 或使用包装脚本（JDK 17 + Gradle + Android SDK）
./use-gradle.sh

# 构建 Debug APK
./gradlew assembleDebug --no-daemon

# 构建 Release APK（已签名）— 一键脚本
./build-release.sh
# 输出文件: GitForAndroid-release.apk
# 安装命令: adb install GitForAndroid-release.apk

# 或手动构建：
./gradlew assembleRelease --no-daemon
```

## 运行测试

```bash
# 所有单元测试
./gradlew test --no-daemon

# 单个测试类
./gradlew test --tests "com.gitforandroid.domain.parser.GitCliParserTest" --no-daemon
```

## 项目结构

```
app/src/main/java/com/gitforandroid/
├── ui/                    # Compose 界面 + ViewModel
│   ├── gui/               # GUI 模式界面（Status、Commit、Log 等）
│   ├── cli/               # CLI 终端界面 + ViewModel
│   ├── common/            # 共享 UI 组件
│   └── theme/             # Material 3 主题
├── domain/                # 用例 + CLI 解析器（无 Android 依赖）
│   ├── parser/            # GitCliParser → CommandAST
│   └── usecase/           # 业务逻辑用例
├── data/
│   ├── git/               # GitService 接口 + JGit 实现
│   ├── local/             # Room 数据库、DAO、实体
│   └── repository/        # AppRepository（协调数据库和 GitService）
├── di/                    # Hilt 依赖注入模块
├── navigation/            # Compose 导航图
├── util/                  # 工具类（FileUtils、SSH、权限、日志）
├── GitApp.kt              # Application 类
└── MainActivity.kt        # 单一 Activity 宿主
```

## 许可证

MIT

## 作者

GitForAndroid
