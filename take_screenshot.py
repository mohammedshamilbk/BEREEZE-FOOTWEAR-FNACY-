"""
Launches the POS app, waits for it to appear, takes a screenshot,
then saves it as app_screenshot.png in the project root.
"""
import subprocess, sys, time, os

# Ensure PIL is available
try:
    from PIL import ImageGrab
except ImportError:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pillow", "-q"])
    from PIL import ImageGrab

import threading
import tkinter as tk

# ── Patch sys.path so we can import pos_billing ───────────────────────────
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from pos_billing.database.db_init import initialize_database
from pos_billing.ui.login_frame import LoginFrame

screenshots = []

def take_screenshot_after_delay(root, delay=2.0):
    time.sleep(delay)
    try:
        x = root.winfo_rootx()
        y = root.winfo_rooty()
        w = root.winfo_width()
        h = root.winfo_height()
        img = ImageGrab.grab(bbox=(x, y, x + w, y + h))
        path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app_screenshot.png")
        img.save(path)
        screenshots.append(path)
        print(f"Screenshot saved: {path}")
        root.after(500, root.destroy)
    except Exception as e:
        print(f"Screenshot error: {e}")
        root.after(500, root.destroy)

initialize_database()
root = LoginFrame()

# Take screenshot 2 seconds after window opens
t = threading.Thread(target=take_screenshot_after_delay, args=(root, 2.0), daemon=True)
t.start()

root.mainloop()
print("Done.")
