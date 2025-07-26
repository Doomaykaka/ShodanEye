package shodaneye.models;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import shodaneye.utils.Constants;
import shodaneye.utils.Logger;
import shodaneye.utils.SupportFunctions;

public class BackupDescriptor implements Cloneable {
    private static final String CHECKSUM_ALGORITHM_NAME = "sha256";
    private static final String REPRESENTATION_PART_1 = "BackupDescriptor [filePaths=";
    private static final String REPRESENTATION_PART_2 = ", foldersPaths=";
    private static final String REPRESENTATION_PART_3 = ", checksum=";
    private static final String REPRESENTATION_PART_4 = ", filesCount=";
    private static final String REPRESENTATION_PART_5 = ", foldersCount=";
    private static final String REPRESENTATION_PART_6 = ", filesCountTotal=";
    private static final String REPRESENTATION_PART_7 = ", foldersCountTotal=";
    private static final String REPRESENTATION_PART_8 = ", isSecured=";
    private static final String REPRESENTATION_PART_9 = ", createdOn=";
    private static final String REPRESENTATION_PART_10 = ", version=";
    private static final String REPRESENTATION_PART_11 = "]";

    private List<String> filePaths;
    private List<String> foldersPaths;
    private byte[] checksum;
    private int filesCount = Constants.getIntDefault();
    private int foldersCount = Constants.getIntDefault();
    private int filesCountTotal = Constants.getIntDefault();
    private int foldersCountTotal = Constants.getIntDefault();
    private boolean isSecured = Constants.getBoolDefault();
    private Date createdOn;
    private long version = Constants.getIntDefault();

    private Backup backup;

    public BackupDescriptor(BackupDescriptor backupDescriptorToClone) {
        this.filePaths = List.copyOf(backupDescriptorToClone.getFilePaths());
        this.foldersPaths = List.copyOf(backupDescriptorToClone.getFilePaths());
        this.checksum =
                Arrays.copyOf(backupDescriptorToClone.getChecksum(), backupDescriptorToClone.getChecksum().length);
        this.filesCount = backupDescriptorToClone.getFilesCount();
        this.foldersCount = backupDescriptorToClone.getFoldersCount();
        this.filesCountTotal = backupDescriptorToClone.getFilesCountTotal();
        this.foldersCountTotal = backupDescriptorToClone.getFilesCountTotal();
        this.isSecured = backupDescriptorToClone.isSecured();
        this.createdOn = Date.from(backupDescriptorToClone.getCreatedOn().toInstant());
        this.version = backupDescriptorToClone.getVersion();
    }

    public BackupDescriptor(
            List<File> foldersToBackup, List<File> filesToBackup, boolean isSecured, boolean checkData) {
        this.isSecured = isSecured;
        this.createdOn = Date.from(Instant.now());

        init(foldersToBackup, filesToBackup, checkData);
    }

    private void init(List<File> foldersToBackup, List<File> filesToBackup, boolean checkData) {
        Logger.printApplicationLog("Backup descriptor init start", "BackupDescriptor");

        saveFilesDescription(filesToBackup);
        saveFoldersDescription(foldersToBackup);

        if (checkData) {
            calculateChecksum();
        }
    }

    private void saveFilesDescription(List<File> filesToBackup) {
        filePaths = new ArrayList<String>();

        if (filesToBackup == null) {
            return;
        }

        for (File fileToBackup : filesToBackup) {
            filePaths.add(fileToBackup.getAbsolutePath());
        }

        filesCount = filesToBackup.size();
        filesCountTotal = filesToBackup.size();
    }

    private void saveFoldersDescription(List<File> foldersToBackup) {
        foldersPaths = new ArrayList<String>();

        if (foldersToBackup == null) {
            return;
        }

        for (File folderToBackup : foldersToBackup) {
            generateFolderDescriptionPart(folderToBackup);
        }

        foldersCount = foldersToBackup.size();
    }

    private void generateFolderDescriptionPart(File folder) {
        if (folder == null) {
            return;
        }

        if (folder.listFiles() == null) {
            foldersCountTotal++;
            foldersPaths.add(folder.getAbsolutePath());

            return;
        }

        for (File folderEntry : folder.listFiles()) {
            if (folderEntry.isFile()) {
                filesCountTotal++;
                filePaths.add(folderEntry.getAbsolutePath());
            } else {
                foldersCountTotal++;
                foldersPaths.add(folderEntry.getAbsolutePath());

                generateFolderDescriptionPart(folderEntry);
            }
        }

        foldersCountTotal++;
        foldersPaths.add(folder.getAbsolutePath());
    }

    private void calculateChecksum() {
        this.checksum = calculateCurrentChecksum();
    }

    public byte[] calculateCurrentChecksum() {
        MessageDigest encoder = null;

        try {
            encoder = MessageDigest.getInstance(CHECKSUM_ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            Logger.printApplicationLog("calculate checksum error", "BackupDescriptor");
            Logger.printApplicationLog(e.getMessage(), "BackupDescriptor");
            e.printStackTrace();
        }

        if (encoder == null) {
            Logger.printApplicationLog("calculate checksum error", "BackupDescriptor");
            SupportFunctions.nonCorrectExit();
        }

        updateChecksumWithFiles(encoder);
        updateChecksumWithFolders(encoder);

        return encoder.digest();
    }

    private void updateChecksumWithFiles(MessageDigest encoder) {
        for (String filepath : this.filePaths) {
            File fileToCalc = new File(filepath);

            if (!fileToCalc.exists()) {
                Logger.printApplicationLog("file not exists to calculate checksum", "BackupDescriptor");
                SupportFunctions.nonCorrectExit();
            }

            encoder.update(fileToCalc.getName().getBytes());

            List<String> fileContent = SupportFunctions.readFileContent(fileToCalc);

            for (String fileRow : fileContent) {
                encoder.update(fileRow.getBytes());
            }
        }
    }

    private void updateChecksumWithFolders(MessageDigest encoder) {
        for (String folderpath : this.foldersPaths) {
            File folderToCalc = new File(folderpath);

            if (!folderToCalc.exists()) {
                SupportFunctions.nonCorrectExit();
            }

            encoder.update(folderToCalc.getName().getBytes());
        }
    }

    public boolean backupIsValid() {
        String oldChecksum = new String(this.checksum);
        String currentChecksum = new String(calculateCurrentChecksum());

        return currentChecksum.equals(oldChecksum);
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public List<String> getFoldersPaths() {
        return foldersPaths;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public int getFoldersCount() {
        return foldersCount;
    }

    public int getFilesCountTotal() {
        return filesCountTotal;
    }

    public int getFoldersCountTotal() {
        return foldersCountTotal;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public long getVersion() {
        return version;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public void setFoldersCount(int foldersCount) {
        this.foldersCount = foldersCount;
    }

    public void setFilesCountTotal(int filesCountTotal) {
        this.filesCountTotal = filesCountTotal;
    }

    public void setFoldersCountTotal(int foldersCountTotal) {
        this.foldersCountTotal = foldersCountTotal;
    }

    public Backup getBackup() {
        return backup;
    }

    public void setBackup(Backup backup) {
        this.backup = backup;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(checksum);
        result = prime * result
                + Objects.hash(
                        createdOn,
                        filePaths,
                        filesCount,
                        filesCountTotal,
                        foldersCount,
                        foldersCountTotal,
                        foldersPaths,
                        isSecured,
                        version);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BackupDescriptor other = (BackupDescriptor) obj;
        return Arrays.equals(checksum, other.checksum)
                && Objects.equals(createdOn, other.createdOn)
                && Objects.equals(filePaths, other.filePaths)
                && filesCount == other.filesCount
                && filesCountTotal == other.filesCountTotal
                && foldersCount == other.foldersCount
                && foldersCountTotal == other.foldersCountTotal
                && Objects.equals(foldersPaths, other.foldersPaths)
                && isSecured == other.isSecured
                && version == other.version;
    }

    @Override
    public String toString() {
        return REPRESENTATION_PART_1
                + filePaths
                + REPRESENTATION_PART_2
                + foldersPaths
                + REPRESENTATION_PART_3
                + Arrays.toString(checksum)
                + REPRESENTATION_PART_4
                + filesCount
                + REPRESENTATION_PART_5
                + foldersCount
                + REPRESENTATION_PART_6
                + filesCountTotal
                + REPRESENTATION_PART_7
                + foldersCountTotal
                + REPRESENTATION_PART_8
                + isSecured
                + REPRESENTATION_PART_9
                + createdOn
                + REPRESENTATION_PART_10
                + version
                + REPRESENTATION_PART_11;
    }
}
