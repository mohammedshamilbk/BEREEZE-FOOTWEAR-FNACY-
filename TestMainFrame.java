import database.User;
import ui.frames.MainFrame;
public class TestMainFrame {
    public static void main(String[] args) {
        try {
            User u = new User("admin", "admin", "Admin", "ADMIN");
            MainFrame m = new MainFrame(u);
            System.out.println("MainFrame instantiated successfully.");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
