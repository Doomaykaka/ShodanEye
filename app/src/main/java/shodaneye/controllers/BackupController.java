package shodaneye.controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import shodaneye.App;
import shodaneye.dao.BackupDAO;
import shodaneye.models.Backup;
import shodaneye.models.BackupDescriptor;
import shodaneye.models.BackupStrategy;
import shodaneye.models.BackupStrategyType;
import shodaneye.models.Workspace;
import shodaneye.utils.Config.WorkspaceConfig;
import shodaneye.utils.Constants;
import shodaneye.utils.Logger;
import shodaneye.utils.SupportFunctions;

public class BackupController {
    private List<Workspace> workspaces;
    private Map<Workspace, BackupDAO> workspacesDAO;

    public BackupController(List<Workspace> managedWorkspaces) {
        this.workspaces = managedWorkspaces;

        init();
        runBackupCheckerLoop();
    }

    public void init() {
        Logger.printApplicationLog("BackupController init", "BackupController");

        this.workspacesDAO = new HashMap<Workspace, BackupDAO>();

        for (int workspaceIdx = Constants.getStartIndex(); workspaceIdx < this.workspaces.size(); workspaceIdx++) {
            Workspace workspace = this.workspaces.get(workspaceIdx);
            prepareWorkspaceWithDao(workspace);
        }
    }

    private void prepareWorkspaceWithDao(Workspace workspace) {
        WorkspaceConfig workspaceConfig = null;
        try {
            workspaceConfig = new WorkspaceConfig(workspace);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<BackupStrategyType> backupStartegyTypes = SupportFunctions
                .parseBackupStrategyTypes(workspaceConfig.getBackupsStrategyTypes());

        BackupStrategy strategy = new BackupStrategy(backupStartegyTypes);
        BackupDAO newDAO = new BackupDAO(strategy, workspace);
        this.workspacesDAO.put(workspace, newDAO);

        updateWorkspace(workspace);
    }

    private void runBackupCheckerLoop() {
        Logger.printApplicationLog("Run backup checker loop", "BackupController");

        BackupCheckerTask checkerTask = new BackupCheckerTask();
        Thread checkerThread = new Thread(checkerTask);
        checkerThread.start();
    }

    public void createNewBackup(Workspace backupWorkspace) {
        Logger.printApplicationLog("Create new backup", "BackupController");

        updateWorkspace(backupWorkspace);

        BackupDAO dao = workspacesDAO.get(backupWorkspace);

        List<Backup> allBackups = dao.getAll();

        if (allBackups.isEmpty() || SupportFunctions.getLastWorkspaceBackup(backupWorkspace) == null) {
            createFirstBackupWithDescriptor(backupWorkspace, dao);
        } else {
            createLastBackupWithDescriptor(backupWorkspace, dao);
        }
    }

    private void createFirstBackupWithDescriptor(Workspace backupWorkspace, BackupDAO dao) {
        BackupDescriptor descriptor = SupportFunctions.createNewBackupDescriptor(backupWorkspace);
        Backup backupToSave = new Backup(null, descriptor);
        dao.saveBackup(backupToSave, null);
    }

    private void createLastBackupWithDescriptor(Workspace backupWorkspace, BackupDAO dao) {
        WorkspaceConfig workspaceConfig = null;

        Backup backupToSave = null;

        try {
            workspaceConfig = new WorkspaceConfig(backupWorkspace);
        } catch (IOException e) {
            Logger.printApplicationLog("backup creating error", "BackupController");
            Logger.printApplicationLog(e.getMessage(), "BackupController");
            e.printStackTrace();
        }

        List<String> filesToBackupRerpr = SupportFunctions.listRepresentationToList(workspaceConfig.getFilesToBackup());
        List<String> foldersToBackupRerpr = SupportFunctions
                .listRepresentationToList(workspaceConfig.getFoldersToBackup());
        List<File> filesToBackup = SupportFunctions.listOfPathsToListOfFiles(filesToBackupRerpr);
        List<File> foldersToBackup = SupportFunctions.listOfPathsToListOfFiles(foldersToBackupRerpr);

        boolean isSecured = workspaceConfig.getBackupPassword() != null
                && !workspaceConfig.getBackupPassword().isEmpty();

        Backup lastBackup = SupportFunctions.getLastWorkspaceBackup(backupWorkspace);

        boolean needDataCheck = !Constants.getBoolDefault();

        BackupDescriptor descriptor = new BackupDescriptor(foldersToBackup, filesToBackup, isSecured, needDataCheck);

        descriptor.setChecksum(descriptor.calculateCurrentChecksum());

        Backup newBackup = new Backup(null, descriptor);
        newBackup.setWorkspace(backupWorkspace);
        backupWorkspace.addBackup(newBackup);
        descriptor.setBackup(backupToSave);

        dao.saveBackup(newBackup, lastBackup);
    }

    public void restore(Workspace backupWorkspace) {
        Logger.printApplicationLog("Restore data from backup", "BackupController");

        updateWorkspace(backupWorkspace);

        Backup backup = SupportFunctions.getLastWorkspaceBackup(backupWorkspace);

        WorkspaceConfig workspaceConfig = null;

        try {
            workspaceConfig = new WorkspaceConfig(backupWorkspace);
        } catch (IOException e) {
            Logger.printApplicationLog("backup restoring error", "SupportFunctions");
            Logger.printApplicationLog(e.getMessage(), "SupportFunctions");
            e.printStackTrace();
        }

        List<String> backupFilePaths = backup.getDescriptor().getFilePaths();
        List<String> backupFolderPaths = backup.getDescriptor().getFoldersPaths();

        List<File> backupFiles = SupportFunctions.listOfPathsToListOfFiles(backupFilePaths);
        List<File> backupFolders = SupportFunctions.listOfPathsToListOfFiles(backupFolderPaths);

        for (File file : backupFiles) {
            SupportFunctions.removeFilesAndFolders(file);
        }

        for (File folder : backupFolders) {
            SupportFunctions.removeFilesAndFolders(folder);
        }

        if (workspaceConfig.isBackupInArchive()) {
            restoreBackupFromArchive(backup, backupFiles, backupFolders, workspaceConfig);
        } else {
            restoreBackupFromFolder(backup, backupFiles, backupFolders);
        }
    }

    private void restoreBackupFromArchive(Backup backup, List<File> backupFiles, List<File> backupFolders,
            WorkspaceConfig workspaceConfig) {
        File backupZipFile = backup.getData();

        if (backupZipFile == null || !backupZipFile.exists() || backupZipFile.isDirectory()) {
            Logger.printApplicationLog("cant restore backup, bad data zip file", "BackupController");

            return;
        }

        List<File> allData = List.copyOf(backupFiles);
        allData.addAll(backupFolders);
        File relativePath = SupportFunctions.getRelativePath(allData);

        String password = workspaceConfig.getBackupPassword();

        SupportFunctions.unzipFilesAndFoldersFromArchive(relativePath, backupZipFile, password);
    }

    private void restoreBackupFromFolder(Backup backup, List<File> backupFiles, List<File> backupFolders) {
        File backupDataFolder = backup.getData();

        if (backupDataFolder == null || !backupDataFolder.exists() || !backupDataFolder.isDirectory()) {
            Logger.printApplicationLog("cant restore backup, bad data folder", "BackupController");

            return;
        }

        List<File> allData = new ArrayList<File>();
        allData.addAll(backupFiles);
        allData.addAll(backupFolders);
        File relativePath = SupportFunctions.getRelativePath(allData);

        for (File element : backupDataFolder.listFiles()) {
            SupportFunctions.copyFilesAndFolders(element, relativePath);
        }
    }

    private void updateWorkspace(Workspace workspaceToUpdate) {
        Logger.printApplicationLog("Workspace update", "BackupDAO");

        workspaceToUpdate.setBackups(workspacesDAO.get(workspaceToUpdate).getAll());
    }

    private class BackupCheckerTask implements Runnable {

        @Override
        public void run() {
            Logger.printApplicationLog("Backup checker loop started", "BackupController");

            Thread currentThread = Thread.currentThread();

            while (!App.isClosed()) {
                tryBackup(currentThread);
            }
        }

        private void tryBackup(Thread currentThread) {
            List<Entry<Workspace, BackupDAO>> daos = List.copyOf(workspacesDAO.entrySet());

            for (Entry<Workspace, BackupDAO> workspaceDAOEntry : daos) {
                WorkspaceConfig config = null;

                try {
                    config = new WorkspaceConfig(workspaceDAOEntry.getKey());
                } catch (IOException e) {
                    Logger.printApplicationLog("backup restoring error", "SupportFunctions");
                    Logger.printApplicationLog(e.getMessage(), "SupportFunctions");
                    e.printStackTrace();
                }

                Set<BackupStrategyType> strategies = SupportFunctions
                        .parseBackupStrategyTypes(config.getBackupsStrategyTypes());

                if (strategies.contains(BackupStrategyType.MANUAL)) {
                    return;
                }

                createNewBackup(workspaceDAOEntry.getKey());
            }

            try {
                Thread.sleep(Constants.getBackupCheckerDelayMs());
            } catch (InterruptedException e) {
                Logger.printApplicationLog("waiting error", "BackupController");
                Logger.printApplicationLog(e.getMessage(), "BackupController");
                e.printStackTrace();
            }
        }
    }

}
