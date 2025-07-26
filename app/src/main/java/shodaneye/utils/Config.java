package shodaneye.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.Properties;
import shodaneye.models.Workspace;

public class Config {
    private boolean logApp;
    private boolean useLAF;
    private boolean useDark;
    private boolean useRussianLanguage;
    private String encoding;
    private String filesCheckDelayMs;
    private String trayShowTimeMs;

    private String backupsFolderPath;

    private String systemEncoding;

    private Path pathToConfig;

    private static volatile Config instance;

    private Config() throws IOException, FileNotFoundException {
        this.systemEncoding = System.getProperty(Constants.getSystemEncodingProperty());

        String configParentFolder = System.getProperty(Constants.getConfigParentFolderName());

        this.pathToConfig = Path.of(Path.of(configParentFolder, Constants.getConfigFilename())
                .toFile()
                .getAbsolutePath());

        if (!this.pathToConfig.toFile().exists()) {
            throw new FileNotFoundException(Constants.getConfigNotFoundMessage() + this.pathToConfig.toString());
        }

        FileInputStream configFIS = new FileInputStream(this.pathToConfig.toString());
        InputStreamReader reader = new InputStreamReader(configFIS, this.systemEncoding);
        Properties properties = new Properties();
        properties.load(reader);

        this.filesCheckDelayMs =
                SupportFunctions.getStringProperty(properties, Constants.getPropertyNameFilesCheckDelay());
        this.trayShowTimeMs = SupportFunctions.getStringProperty(properties, Constants.getPropertyNameTrayShowTimeMs());
        this.encoding = SupportFunctions.getStringProperty(properties, Constants.getPropertyNameEncoding());
        this.logApp = SupportFunctions.getBooleanProperty(properties, Constants.getPropertyNameLogApp());
        this.useLAF = SupportFunctions.getBooleanProperty(properties, Constants.getPropertyNameLafIsNeeded());
        this.useDark = SupportFunctions.getBooleanProperty(properties, Constants.getPropertyNameDarkThemeIsNeeded());
        this.useRussianLanguage =
                SupportFunctions.getBooleanProperty(properties, Constants.getPropertyNameRussianLanguageIsNeeded());
        this.backupsFolderPath =
                SupportFunctions.getStringProperty(properties, Constants.getPropertyNameBackupFolderPath());

        if (this.encoding != null && !this.encoding.isEmpty()) {
            this.systemEncoding = encoding;
        }

        if (this.filesCheckDelayMs == null || this.filesCheckDelayMs.isEmpty()) {
            this.filesCheckDelayMs = Long.toString(Constants.getBackupCheckerDelayMs());
        }

        if (this.trayShowTimeMs == null || this.trayShowTimeMs.isEmpty()) {
            this.trayShowTimeMs = Long.toString(Constants.getTrayHideDelay());
        }
    }

    public static Config getConfig() {
        if (instance == null) {
            try {
                instance = new Config();
            } catch (IOException e) {
                Logger.printApplicationLog("config initialize error", "SupportFunctions");
                Logger.printApplicationLog(e.getMessage(), "SupportFunctions");
                e.printStackTrace();
            }
        }

        return instance;
    }

    public void save() throws IOException {
        String configParentFolder = System.getProperty(Constants.getConfigParentFolderName());

        this.pathToConfig = Path.of(Path.of(configParentFolder, Constants.getConfigFilename())
                .toFile()
                .getAbsolutePath());

        if (!this.pathToConfig.toFile().exists()) {
            throw new FileNotFoundException(Constants.getConfigNotFoundMessage() + this.pathToConfig.toString());
        }

        FileOutputStream configFOS = new FileOutputStream(this.pathToConfig.toString());
        OutputStreamWriter writer = new OutputStreamWriter(configFOS, this.systemEncoding);
        Properties properties = new Properties();

        SupportFunctions.setStringProperty(properties, Constants.getPropertyNameFilesCheckDelay(), filesCheckDelayMs);
        SupportFunctions.setStringProperty(properties, Constants.getPropertyNameTrayShowTimeMs(), trayShowTimeMs);
        SupportFunctions.setStringProperty(properties, Constants.getPropertyNameEncoding(), encoding);
        SupportFunctions.setBooleanProperty(properties, Constants.getPropertyNameLogApp(), logApp);
        SupportFunctions.setBooleanProperty(properties, Constants.getPropertyNameLafIsNeeded(), useLAF);
        SupportFunctions.setBooleanProperty(properties, Constants.getPropertyNameDarkThemeIsNeeded(), useDark);
        SupportFunctions.setBooleanProperty(
                properties, Constants.getPropertyNameRussianLanguageIsNeeded(), useRussianLanguage);
        SupportFunctions.setStringProperty(properties, Constants.getPropertyNameBackupFolderPath(), backupsFolderPath);

        properties.store(writer, Constants.getTextDefault());
        configFOS.flush();
        configFOS.close();
    }

    public boolean isLogApp() {
        return this.logApp;
    }

    public boolean isUseLAF() {
        return this.useLAF;
    }

    public boolean isUseDark() {
        return this.useDark;
    }

    public boolean isUseRussianLanguage() {
        return useRussianLanguage;
    }

    public void setLogApp(boolean logApp) {
        this.logApp = logApp;
    }

    public void setUseLAF(boolean useLAF) {
        this.useLAF = useLAF;
    }

    public void setUseDark(boolean useDark) {
        this.useDark = useDark;
    }

    public void setUseRussianLanguage(boolean useRussianLanguage) {
        this.useRussianLanguage = useRussianLanguage;
    }

    public String getBackupsFolderPath() {
        return this.backupsFolderPath;
    }

    public void setBackupsFolderPath(String backupsFolderPath) {
        this.backupsFolderPath = backupsFolderPath;
    }

    public String getSystemEncoding() {
        return this.systemEncoding;
    }

    public String getFilesCheckDelayMs() {
        return filesCheckDelayMs;
    }

    public void setFilesCheckDelayMs(String filesCheckDelayMs) {
        this.filesCheckDelayMs = filesCheckDelayMs;
    }

    public String getTrayShowTimeMs() {
        return trayShowTimeMs;
    }

    public void setTrayShowTimeMs(String trayShowTimeMs) {
        this.trayShowTimeMs = trayShowTimeMs;
    }

    public static class WorkspaceConfig {
        private String backupPassword;
        private String backupDateDiff;
        private boolean backupUseTimestamps;
        private boolean backupUseVersion;
        private String backupPreffix;
        private String backupPostfix;
        private boolean backupInArchive;
        private String filesToBackup;
        private String foldersToBackup;

        private String backupsStrategyTypes;

        private Path pathToConfig;

        public WorkspaceConfig(Workspace workspace) throws IOException {
            File backupsFolder = new File(Config.getConfig().getBackupsFolderPath());
            Path backupsFolderPath = backupsFolder.toPath();
            Path workspaceFolderPath = backupsFolderPath.resolve(workspace.getName());

            if (!workspaceFolderPath.toFile().exists()) {
                throw new FileNotFoundException(
                        Constants.getConfigFolderNotFoundMessage() + workspaceFolderPath.toString());
            }

            Path workspaceConfigFilePath = workspaceFolderPath.resolve(Constants.getConfigFilename());

            if (!workspaceConfigFilePath.toFile().exists()) {
                throw new FileNotFoundException(
                        Constants.getConfigNotFoundMessage() + workspaceConfigFilePath.toString());
            }

            pathToConfig = workspaceConfigFilePath;

            FileInputStream configFIS = new FileInputStream(workspaceConfigFilePath.toString());
            InputStreamReader reader =
                    new InputStreamReader(configFIS, Config.getConfig().getSystemEncoding());
            Properties properties = new Properties();
            properties.load(reader);

            this.backupPassword =
                    SupportFunctions.getStringProperty(properties, Constants.getPropertyNameBackupPassword());
            this.backupDateDiff =
                    SupportFunctions.getStringProperty(properties, Constants.getPropertyNameBackupDateDiff());
            this.backupUseTimestamps =
                    SupportFunctions.getBooleanProperty(properties, Constants.getPropertyNameBackupUseTimestamp());
            this.backupUseVersion =
                    SupportFunctions.getBooleanProperty(properties, Constants.getPropertyNameBackupUseVersion());
            this.backupPreffix =
                    SupportFunctions.getStringProperty(properties, Constants.getPropertyNameBackupPreffix());
            this.backupPostfix =
                    SupportFunctions.getStringProperty(properties, Constants.getPropertyNameBackupPostfix());
            this.backupInArchive =
                    SupportFunctions.getBooleanProperty(properties, Constants.getPropertyNameBackupInArchive());
            this.backupsStrategyTypes =
                    SupportFunctions.getStringProperty(properties, Constants.getPropertyNameBackupsStrategyTypes());
            this.filesToBackup =
                    SupportFunctions.getStringProperty(properties, Constants.getPropertyNameFilesToBackup());
            this.foldersToBackup =
                    SupportFunctions.getStringProperty(properties, Constants.getPropertyNameFoldersToBackup());
        }

        public void save() throws IOException {
            FileOutputStream configFOS = new FileOutputStream(this.pathToConfig.toString());
            OutputStreamWriter writer =
                    new OutputStreamWriter(configFOS, Config.getConfig().getSystemEncoding());
            Properties properties = new Properties();

            SupportFunctions.setStringProperty(properties, Constants.getPropertyNameBackupPassword(), backupPassword);
            SupportFunctions.setStringProperty(properties, Constants.getPropertyNameBackupDateDiff(), backupDateDiff);
            SupportFunctions.setBooleanProperty(
                    properties, Constants.getPropertyNameBackupUseTimestamp(), backupUseTimestamps);
            SupportFunctions.setBooleanProperty(
                    properties, Constants.getPropertyNameBackupUseVersion(), backupUseVersion);
            SupportFunctions.setStringProperty(properties, Constants.getPropertyNameBackupPreffix(), backupPreffix);
            SupportFunctions.setStringProperty(properties, Constants.getPropertyNameBackupPostfix(), backupPostfix);
            SupportFunctions.setBooleanProperty(
                    properties, Constants.getPropertyNameBackupInArchive(), backupInArchive);
            SupportFunctions.setStringProperty(
                    properties, Constants.getPropertyNameBackupsStrategyTypes(), backupsStrategyTypes);
            SupportFunctions.setStringProperty(properties, Constants.getPropertyNameFilesToBackup(), filesToBackup);
            SupportFunctions.setStringProperty(properties, Constants.getPropertyNameFoldersToBackup(), foldersToBackup);

            properties.store(writer, Constants.getTextDefault());
            configFOS.flush();
            configFOS.close();
        }

        public void setBackupPassword(String backupPassword) {
            this.backupPassword = backupPassword;
        }

        public void setBackupDateDiff(String backupDateDiff) {
            this.backupDateDiff = backupDateDiff;
        }

        public String getBackupPassword() {
            return this.backupPassword;
        }

        public String getBackupDateDiff() {
            return this.backupDateDiff;
        }

        public boolean isBackupInArchive() {
            return backupInArchive;
        }

        public void setBackupInArchive(boolean backupInArchive) {
            this.backupInArchive = backupInArchive;
        }

        public boolean isBackupUseTimestamps() {
            return backupUseTimestamps;
        }

        public void setBackupUseTimestamps(boolean backupUseTimestamps) {
            this.backupUseTimestamps = backupUseTimestamps;
        }

        public boolean isBackupUseVersion() {
            return backupUseVersion;
        }

        public void setBackupUseVersion(boolean backupUseVersion) {
            this.backupUseVersion = backupUseVersion;
        }

        public String getBackupPreffix() {
            return backupPreffix;
        }

        public void setBackupPreffix(String backupPreffix) {
            this.backupPreffix = backupPreffix;
        }

        public String getBackupPostfix() {
            return backupPostfix;
        }

        public void setBackupPostfix(String backupPostfix) {
            this.backupPostfix = backupPostfix;
        }

        public String getBackupsStrategyTypes() {
            return backupsStrategyTypes;
        }

        public void setBackupsStrategyTypes(String backupsStrategyTypes) {
            this.backupsStrategyTypes = backupsStrategyTypes;
        }

        public String getFilesToBackup() {
            return filesToBackup;
        }

        public void setFilesToBackup(String filesToBackup) {
            this.filesToBackup = filesToBackup;
        }

        public String getFoldersToBackup() {
            return foldersToBackup;
        }

        public void setFoldersToBackup(String foldersToBackup) {
            this.foldersToBackup = foldersToBackup;
        }
    }
}
