# Administrator Manual — Bereeze Footwear Fancy Cloud System

This manual provides instructions for System Administrators and Store Owners managing the cloud platform.

## 1. User & Role Management
1. Log in with `ADMIN` or `SUPER_ADMIN` credentials.
2. Navigate to **Users Management** via API or Desktop Admin panel.
3. Supported Roles:
   - `SUPER_ADMIN`: Full root access to all configurations, database backups, and audit logs.
   - `ADMIN`: Full access to user management, inventory, reports, and billing.
   - `MANAGER`: Can modify inventory prices, view P&L reports, and manage expenses.
   - `STAFF`: Restricted to POS billing and customer check-in/out.

---

## 2. Financial Reports & Exports
1. Navigate to **Reports & Exports**.
2. Click **Download Multi-Tab Excel Workbook** to export full historical sales, day-closing cash tallies, and store expenses.

---

## 3. Database Backups & Recovery
- **Manual Backup**: Trigger via `/api/v1/backups/create`.
- **Restore**: Restore database snapshot via `/api/v1/backups/restore/{filename}`.

---

## 4. Security & Audit Trail Inspection
1. Navigate to **Security Audit** view to inspect full audit timeline of user logins, bill creations, inventory modifications, and security actions.
