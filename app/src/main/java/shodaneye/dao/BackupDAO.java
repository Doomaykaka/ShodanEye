package shodaneye.dao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import shodaneye.models.Backup;
import shodaneye.models.BackupDescriptor;
import shodaneye.models.BackupStrategy;
import shodaneye.models.Workspace;
import shodaneye.utils.Constants;
import shodaneye.utils.Logger;
import shodaneye.utils.SupportFunctions;

public class BackupDAO {
    private BackupStrategy strategy;
    private Workspace workspace;

    public BackupDAO(BackupStrategy strategy, Workspace workspace) {
        this.strategy = strategy;
        this.workspace = workspace;
    }

    public Backup getBackup(String name) {
        Logger.printApplicationLog("Get backup by name", "BackupDAO");

        Backup result = null;

        File backupsFolder = new File(shodaneye.utils.Config.getConfig().getBackupsFolderPath());

        if (!backupsFolder.exists()) {
            backupsFolder.mkdir();
        }

        File workspaceFolder = new File(backupsFolder, this.workspace.getName());

        if (!workspaceFolder.exists()) {
            workspaceFolder.mkdir();
        }

        for (File backupFolder : workspaceFolder.listFiles()) {
            if (backupFolder.getName().equals(name)) {
                result = SupportFunctions.parseBackupFolder(backupFolder, this.workspace);
                break;
            }
        }

        return result;
    }

    public Backup getBackup(Date date) {
        Logger.printApplicationLog("Get backup by date", "BackupDAO");

        Backup result = null;

        File backupsFolder = new File(shodaneye.utils.Config.getConfig().getBackupsFolderPath());

        if (!backupsFolder.exists()) {
            backupsFolder.mkdir();
        }

        File workspaceFolder = new File(backupsFolder, this.workspace.getName());

        if (!workspaceFolder.exists()) {
            workspaceFolder.mkdir();
        }

        for (File backupFolder : workspaceFolder.listFiles()) {
            Backup parsed = SupportFunctions.parseBackupFolder(backupFolder, this.workspace);

            if (parsed.getDescriptor().getCreatedOn().equals(date)) {
                result = parsed;
                break;
            }
        }

        return result;
    }

    public Backup getBackup(long version) {
        Logger.printApplicationLog("Get backup by version", "BackupDAO");

        Backup result = null;

        File backupsFolder = new File(shodaneye.utils.Config.getConfig().getBackupsFolderPath());

        if (!backupsFolder.exists()) {
            backupsFolder.mkdir();
        }

        File workspaceFolder = new File(backupsFolder, this.workspace.getName());

        if (!workspaceFolder.exists()) {
            workspaceFolder.mkdir();
        }

        for (File backupFolder : workspaceFolder.listFiles()) {
            Backup parsed = SupportFunctions.parseBackupFolder(backupFolder, this.workspace);

            if (parsed.getDescriptor().getVersion() == version) {
                result = parsed;
                break;
            }
        }

        return result;
    }

    public List<Backup> getAll() {
        Logger.printApplicationLog("Get all backups", "BackupDAO");

        List<Backup> result = new ArrayList<Backup>();

        File backupsFolder = new File(shodaneye.utils.Config.getConfig().getBackupsFolderPath());

        if (!backupsFolder.exists()) {
            backupsFolder.mkdir();
        }

        File workspaceFolder = new File(backupsFolder, this.workspace.getName());

        if (!workspaceFolder.exists()) {
            workspaceFolder.mkdir();
        }

        for (File backupFolder : workspaceFolder.listFiles()) {
            if (backupFolder.getName().equals(Constants.getConfigFilename())) {
                continue;
            }

            Backup parsed = SupportFunctions.parseBackupFolder(backupFolder, this.workspace);
            result.add(parsed);
        }

        return result;
    }

    public boolean saveBackup(Backup backupToSave, Backup lastBackup) {
        Logger.printApplicationLog("Save backup", "BackupDAO");

        boolean saveIsCorrect = Constants.getBoolDefault();

        updateWorkspace();

        BackupDescriptor lastBackupDescriptor = null;

        if (lastBackup != null) {
            lastBackupDescriptor = lastBackup.getDescriptor();
        }

        try {
            saveIsCorrect = strategy.startBackupProcess(workspace, backupToSave.getDescriptor(),
                    lastBackupDescriptor) != null;
        } catch (IOException e) {
            Logger.printApplicationLog("backup process error", "SupportFunctions");
            Logger.printApplicationLog(e.getMessage(), "SupportFunctions");
            e.printStackTrace();
        }

        return saveIsCorrect;
    }

    public boolean updateBackup(Backup backupToUpdate) {
        Logger.printApplicationLog("Update backup", "BackupDAO");

        boolean backupIsUpdated = Constants.getBoolDefault();

        updateWorkspace();

        BackupDescriptor oldDescriptor = new BackupDescriptor(backupToUpdate.getDescriptor());

        Backup lastBackup = SupportFunctions.getLastWorkspaceBackup(workspace);

        backupIsUpdated = removeBackup(backupToUpdate);
        backupIsUpdated = backupIsUpdated && saveBackup(backupToUpdate, lastBackup);

        backupToUpdate.setDescriptor(oldDescriptor);

        return backupIsUpdated;
    }

    public boolean removeBackup(Backup backupToRemove) {
        Logger.printApplicationLog("Remove backup", "BackupDAO");

        boolean backupIsRemoved = Constants.getBoolDefault();

        updateWorkspace();

        File backupsFolder = new File(shodaneye.utils.Config.getConfig().getBackupsFolderPath());

        if (!backupsFolder.exists()) {
            backupsFolder.mkdir();
        }

        File workspaceFolder = new File(backupsFolder, this.workspace.getName());

        if (!workspaceFolder.exists()) {
            workspaceFolder.mkdir();
        }

        String backupName = null;
        try {
            backupName = strategy.getBackupName(backupToRemove.getDescriptor(), workspace);
        } catch (IOException e) {
            Logger.printApplicationLog("cant get backup name", "SupportFunctions");
            Logger.printApplicationLog(e.getMessage(), "SupportFunctions");
            e.printStackTrace();
        }

        File currentBackupFolder = new File(workspaceFolder, backupName);

        SupportFunctions.clearFolder(currentBackupFolder);

        backupIsRemoved = currentBackupFolder.delete();

        return backupIsRemoved;
    }

    private void updateWorkspace() {
        Logger.printApplicationLog("Workspace update", "BackupDAO");

        workspace.setBackups(getAll());
    }
}
