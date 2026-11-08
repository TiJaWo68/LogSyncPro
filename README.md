# 🚀 LogSyncPro

**The ultimate analysis hub for distributed systems and client-server architectures.**

LogSyncPro bridges the gap in troubleshooting complex environments. When your data center server and client desktop app write different logs, LogSyncPro brings them together in one synchronized view.

## ✨ Key Features

### 🕒 Intelligent Time Synchronization
Click on an event in any log file, and **all other open logs automatically jump to the exact same timestamp**. Say goodbye to manual searching and comparing timestamps.

### 🧩 Multi-Node Merging
Merge logs from different server nodes or applications with a single click.
*   **Unified View**: All entries displayed in one chronological list.
*   **Source Tracing**: Color-coded indicators show exactly which node or application an entry originated from.
*   **Reversible**: Switch back to individual views at any time.

### 📦 Universal Import & Auto-Detection
*   **Drag & Drop**: Simply drag log files directly into the window.
*   **Archive Support**: Read directly from `.zip`, `.7z`, and `.gz` archives without unpacking.
*   **Smart Recognition**: Automatically detects log formats (e.g., Log4j, Logback, custom) based on content.

### 🛠️ Dynamic Configuration
Log formats are not hard-coded. Add new timestamp formats and Regex parsers "on the fly" via flexible configuration to support proprietary legacy logs.

### 🖥️ Power-User Interface
*   **Zebra-Table Design**: High-contrast rows for maximum readability.
*   **FlatLaf Dark Theme**: Eye-friendly interface for Windows and Linux users.
*   **Dynamic Split-Panes**: Each log lives in its own closable view.
*   **Real-time Filtering**: Fast search and Regex filtering per table.

## 🚀 Command Line Interface

LogSyncPro supports various startup parameters to automate your workflow:

*   **`--open=path1,path2`**: Opens the specified files or directories immediately on startup.
*   **`--ssh=[user[:password]@]host`**: Initiates a K8s log discovery via SSH.
    *   If password or host is missing, the connection dialog will appear pre-filled.
    *   Example: `--ssh=admin:secret@192.168.1.10`
*   **`--fetch="pattern1,pattern2"`**: Automatically selects and streams containers matching the patterns.
    *   Used in combination with `--ssh`.
    *   Supports wildcards (e.g., `*/web*/*`) or substring matching.
    *   Example: `--fetch="production/frontend-*,backend"`

## 🔌 Receivers (TCP Log Streams)

LogSyncPro can receive live log streams over TCP:

| Protocol | Port | Description |
| --- | --- | --- |
| Universal (serialized) | 4444, 4445 | Serialized events from Logback / Log4j 1.x / Log4j 2.x |
| Logstash JSON | 4560 | Line-delimited JSON (NDJSON), e.g. MoneyPoly `LogstashTcpSocketAppender` |
| XML | 4561 | Log4j/Logback `XMLLayout` events |

To stream MoneyPoly logs, set `LOGGING_TCP_ENABLED=true`, `LOGGING_TCP_HOST=<LogSyncPro host>` and `LOGGING_TCP_PORT=4560`.

## 🛠 Tech Stack
*   **Engine**: Java 21 (LTS)
*   **UI**: Swing with FlatLaf & MigLayout
*   **I/O**: Apache Commons Compress for high-performance archive access
*   **Architecture**: Strategy Pattern for extensible parser logic

---

*LogSyncPro is designed to be the Swiss Army Knife for developers.*
