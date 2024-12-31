package service;

import com.liferay.object.model.ObjectEntry;
import com.liferay.portal.kernel.exception.PortalException;

import java.util.List;

public interface FileDownloadService {


    long getDownloadCount(long fileEntryId) throws PortalException;
    List<FileDownloadDTO> getDownloadsByDocumentType(long documentTypeId) throws PortalException;


}
