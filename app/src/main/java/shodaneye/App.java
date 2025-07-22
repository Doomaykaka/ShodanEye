package shodaneye;

import java.util.List;

import shodaneye.controllers.BackupController;
import shodaneye.models.Workspace;
import shodaneye.utils.Config;
import shodaneye.utils.Constants;

public class App {
    private static boolean isClosed = Constants.getBoolDefault();

    public static void main(String[] args) {
        Config appConfig = Config.getConfig();

        Workspace testWorkspace = new Workspace("testWorkspace");

        BackupController controller = new BackupController(List.of(testWorkspace));

        // controller.createNewBackup(testWorkspace);

        controller.restore(testWorkspace);

    }

    public static boolean isClosed() {
        return isClosed;
    }

    public static void setIsClosed(boolean isClosed) {
        App.isClosed = isClosed;
    }
}
