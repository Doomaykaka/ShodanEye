package shodaneye;

import java.util.List;
import shodaneye.controllers.BackupController;
import shodaneye.gui.Tray;
import shodaneye.models.Workspace;
import shodaneye.utils.Config;
import shodaneye.utils.Constants;
import shodaneye.utils.Logger;
import shodaneye.utils.SupportFunctions;

public class App {
    private static boolean isClosed = Constants.getBoolDefault();

    public static void main(String[] args) {
        start();
    }

    private static void start() {
        Logger.printApplicationLog("App started", "App");

        Config appConfig = Config.getConfig();
        List<Workspace> workspaces = SupportFunctions.findWorkspaces(appConfig);

        BackupController controller = new BackupController(workspaces);

        Tray tray = new Tray(controller);
        tray.show();
    }

    public static boolean isClosed() {
        return isClosed;
    }

    public static void setIsClosed(boolean isClosed) {
        App.isClosed = isClosed;
    }
}
