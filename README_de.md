# 🚀 LogSyncPro
Die ultimative Analyse-Zentrale für verteilte Systeme und Client-Server-Architekturen.

LogSyncPro wurde entwickelt, um das größte Problem bei der Fehlersuche in komplexen Umgebungen zu lösen: Den Überblick über zeitliche Zusammenhänge zu behalten. Wenn der Server im Rechenzentrum und die Desktop-App beim Kunden unterschiedliche Logs schreiben, ist LogSyncPro die Brücke dazwischen.

## ✨ Key Features
🕒 Intelligente Zeit-Synchronisation
Klicke auf ein Ereignis in einem beliebigen Log-File, und alle anderen geöffneten Logs springen automatisch zum exakt selben Zeitpunkt. Kein manuelles Suchen und Vergleichen von Zeitstempeln mehr.

## 🧩 Multi-Node Merging
Führe Logs von verschiedenen Server-Nodes mit einem Klick zusammen.

Verschmolzene Ansicht: Alle Einträge in einer chronologischen Liste.

Herkunfts-Tracing: Eine farbige Kennung zeigt sofort, von welchem Node oder welcher Applikation die Zeile stammt.

Reversibel: Wechsle jederzeit zwischen der kombinierten und der Einzelansicht zurück.

## 📦 Universal Import & Auto-Detection
Drag & Drop: Ziehe Log-Files einfach direkt in das Fenster.

Archiv-Support: Direktes Lesen aus .zip, .7z und .gz Archiven (kein vorheriges Entpacken nötig).

Smart Recognition: LogSyncPro erkennt das Log-Format (z.B. Log4j, Logback, benutzerdefiniert) automatisch anhand des Inhalts.

## 🛠️ Dynamische Konfiguration
Log-Formate sind nicht hart codiert. Über eine flexible Konfiguration können neue Zeitstempel-Formate und Regex-Parser "on the fly" hinzugefügt werden, um auch proprietäre Legacy-Logs zu unterstützen.

## 🖥️ Power-User Interface
Zebra-Table-Design: Maximale Lesbarkeit durch kontrastierte Zeilen.

FlatLaf Dark Theme: Augenschonendes Arbeiten in Windows- und Linux-Umgebungen.

Dynamic Split-Panes: Jedes Log in einer eigenen, schließbaren Ansicht.

Echtzeit-Filter: Blitzschnelle Suche und Regex-Filterung pro Tabelle.

## 🛠 Tech-Stack
Engine: Java 21 (LTS)

UI: Swing mit FlatLaf & MigLayout

I/O: Apache Commons Compress für High-Performance Archiv-Zugriffe

Architektur: Strategy-Pattern für erweiterbare Parser-Logik

❓ Deine Meinung ist gefragt!
LogSyncPro soll das Schweizer Taschenmesser für Entwickler werden. Welche Features fehlen dir für deinen Workflow?

Remote-Logs: Direkte Anbindung an SSH/SFTP oder Kubernetes-Pods?

Highlighting: Markierung von bestimmten Schlüsselwörtern (z.B. Error-IDs) über alle Logs hinweg?

Export: Speichern eines "Synchronisierten Ausschnitts" als PDF oder neue Textdatei?

DICOM-Support: Spezielle Darstellung für SOP-Instance-UIDs oder Patient-IDs?

Lass uns die Fehleranalyse gemeinsam beschleunigen!
