package service;

public class FileDownloadDTO {

    private long fileEntryId;
    private long downloads;

    public FileDownloadDTO(long fileEntryId, long downloads) {
        this.fileEntryId = fileEntryId;
        this.downloads = downloads;
    }

    public long getFileEntryId() {
        return fileEntryId;
    }

    public long getDownloads() {
        return downloads;
    }
}
