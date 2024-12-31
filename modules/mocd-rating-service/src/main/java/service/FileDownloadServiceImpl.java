package service;

import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.object.model.ObjectEntry;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(
        immediate = true,
        service = FileDownloadService.class
)
public class FileDownloadServiceImpl implements FileDownloadService {


    @Override
    public long getDownloadCount(long fileEntryId) throws PortalException {
        DLFileEntry fileEntry = _dlFileEntryLocalService.getDLFileEntry(fileEntryId);
        return fileEntry.getReadCount();
    }


    @Override
    public List<FileDownloadDTO> getDownloadsByDocumentType(long documentTypeId) throws PortalException {
        List<FileDownloadDTO> downloads = new ArrayList<>();

        try {
            DynamicQuery fileQuery = _dlFileEntryLocalService.dynamicQuery();
            fileQuery.add(RestrictionsFactoryUtil.eq("fileEntryTypeId", documentTypeId));

            List<DLFileEntry> files = _dlFileEntryLocalService.dynamicQuery(fileQuery);

            for (DLFileEntry file : files) {
                downloads.add(new FileDownloadDTO(
                        file.getFileEntryId(),
                        file.getReadCount()
                ));
            }
            return downloads;
        } catch (Exception e) {
            _log.error("Error getting download counts by document type: " + e.getMessage(), e);
            throw new PortalException(e);
        }
    }
    @Reference
    private DLFileEntryLocalService _dlFileEntryLocalService;

    private static final Log _log = LogFactoryUtil.getLog(FileDownloadServiceImpl.class);

}
