import 'dart:convert';
import 'dart:io';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/trash_item.dart';

class TrashService {
  static const String _keyTrashManifest = "trash_manifest_items";
  final SharedPreferences _prefs;
  late final Directory _trashDir;

  TrashService(this._prefs);

  static Future<TrashService> create() async {
    final sp = await SharedPreferences.getInstance();
    final service = TrashService(sp);
    await service._init();
    return service;
  }

  Future<void> _init() async {
    final appDir = await getApplicationSupportDirectory();
    _trashDir = Directory(p.join(appDir.path, "ZevixTrash"));
    if (!_trashDir.existsSync()) {
      _trashDir.createSync(recursive: true);
    }
    // Automatically purge items older than 30 days on startup
    await cleanExpiredTrash();
  }

  List<TrashItem> getTrashItems() {
    final raw = _prefs.getStringList(_keyTrashManifest) ?? [];
    final items = <TrashItem>[];
    for (final str in raw) {
      try {
        final item = TrashItem.fromJson(jsonDecode(str));
        if (File(item.trashPath).existsSync()) {
          items.add(item);
        }
      } catch (_) {}
    }
    return items;
  }

  Future<void> _saveTrashItems(List<TrashItem> items) async {
    final list = items.map((e) => jsonEncode(e.toJson())).toList();
    await _prefs.setStringList(_keyTrashManifest, list);
  }

  Future<bool> moveToTrash(String filePath, {bool isVideo = true}) async {
    final file = File(filePath);
    if (!file.existsSync()) return false;

    try {
      final fileName = p.basename(filePath);
      final uniqueTrashName = "${DateTime.now().millisecondsSinceEpoch}_$fileName";
      final targetPath = p.join(_trashDir.path, uniqueTrashName);

      // Move the physical file to the trash folder
      await file.rename(targetPath);

      final trashItem = TrashItem(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        title: fileName,
        originalPath: filePath,
        trashPath: targetPath,
        size: File(targetPath).lengthSync(),
        deletedAt: DateTime.now(),
        isVideo: isVideo,
      );

      final current = getTrashItems();
      current.insert(0, trashItem);
      await _saveTrashItems(current);
      return true;
    } catch (e) {
      // Fallback copy & delete if across drives
      try {
        final fileName = p.basename(filePath);
        final uniqueTrashName = "${DateTime.now().millisecondsSinceEpoch}_$fileName";
        final targetPath = p.join(_trashDir.path, uniqueTrashName);

        await file.copy(targetPath);
        await file.delete();

        final trashItem = TrashItem(
          id: DateTime.now().millisecondsSinceEpoch.toString(),
          title: fileName,
          originalPath: filePath,
          trashPath: targetPath,
          size: File(targetPath).lengthSync(),
          deletedAt: DateTime.now(),
          isVideo: isVideo,
        );

        final current = getTrashItems();
        current.insert(0, trashItem);
        await _saveTrashItems(current);
        return true;
      } catch (_) {
        return false;
      }
    }
  }

  Future<bool> restoreItem(TrashItem item) async {
    final trashFile = File(item.trashPath);
    if (!trashFile.existsSync()) return false;

    try {
      final destDir = Directory(p.dirname(item.originalPath));
      if (!destDir.existsSync()) {
        destDir.createSync(recursive: true);
      }

      var targetDest = item.originalPath;
      if (File(targetDest).existsSync()) {
        final name = p.basenameWithoutExtension(targetDest);
        final ext = p.extension(targetDest);
        targetDest = p.join(destDir.path, "${name}_restored$ext");
      }

      await trashFile.rename(targetDest);

      final current = getTrashItems();
      current.removeWhere((e) => e.id == item.id);
      await _saveTrashItems(current);
      return true;
    } catch (e) {
      try {
        await trashFile.copy(item.originalPath);
        await trashFile.delete();

        final current = getTrashItems();
        current.removeWhere((e) => e.id == item.id);
        await _saveTrashItems(current);
        return true;
      } catch (_) {
        return false;
      }
    }
  }

  Future<bool> permanentDelete(TrashItem item) async {
    try {
      final file = File(item.trashPath);
      if (file.existsSync()) {
        await file.delete();
      }
      final current = getTrashItems();
      current.removeWhere((e) => e.id == item.id);
      await _saveTrashItems(current);
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<void> emptyTrash() async {
    final current = getTrashItems();
    for (final item in current) {
      try {
        final f = File(item.trashPath);
        if (f.existsSync()) await f.delete();
      } catch (_) {}
    }
    await _saveTrashItems([]);
  }

  Future<void> cleanExpiredTrash() async {
    final current = getTrashItems();
    final remaining = <TrashItem>[];
    for (final item in current) {
      if (item.daysRemaining <= 0) {
        try {
          final f = File(item.trashPath);
          if (f.existsSync()) await f.delete();
        } catch (_) {}
      } else {
        remaining.add(item);
      }
    }
    await _saveTrashItems(remaining);
  }
}

