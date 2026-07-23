"""
Unit tests for Backup, Restore, and Directory safety.
"""

import os
import unittest
from pathlib import Path

from pos_billing.utils import backup_manager, path_manager


class TestExportsAndBackup(unittest.TestCase):

    def test_required_directories_exist(self):
        path_manager.ensure_directories_exist()
        self.assertTrue(path_manager.RECORDS_DIR.exists())
        self.assertTrue(path_manager.EXPORTS_DIR.exists())
        self.assertTrue(path_manager.BACKUPS_DIR.exists())
        self.assertTrue(path_manager.CONFIG_DIR.exists())
        self.assertTrue(path_manager.TEMP_DIR.exists())
        self.assertTrue(path_manager.LOGS_DIR.exists())

    def test_sanitize_filename(self):
        clean = path_manager.sanitize_filename("../../../etc/passwd")
        self.assertEqual(clean, "passwd")

        clean_normal = path_manager.sanitize_filename("report_2026.csv")
        self.assertEqual(clean_normal, "report_2026.csv")

    def test_backup_and_list(self):
        backup_path = backup_manager.create_backup(custom_prefix="unittest")
        self.assertIsNotNone(backup_path)
        self.assertTrue(backup_path.exists())

        backups = backup_manager.list_backups()
        self.assertTrue(len(backups) > 0)
        self.assertIn(backup_path, backups)


if __name__ == "__main__":
    unittest.main()
