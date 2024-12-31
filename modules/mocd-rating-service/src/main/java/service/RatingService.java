package service;

import com.liferay.object.model.ObjectEntry;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface RatingService {

    ObjectEntry addRating(long userId, long groupId, Map<String, Serializable> values, ServiceContext serviceContext) throws PortalException;
    boolean updateRating(long userId, long objectEntryId, Map<String, Serializable> values, ServiceContext serviceContext) throws PortalException;
    ObjectEntry addLike(long userId, long groupId, Map<String, Serializable> values, ServiceContext serviceContext) throws PortalException;
    List<ObjectEntry> getEntries(String definitionName, ServiceContext serviceContext) throws PortalException;
    ObjectEntry findExistingRating(long userId, long fileEntryId, ServiceContext serviceContext) throws PortalException;
    ObjectEntry addOrUpdateRating(long userId, long groupId, Map<String, Serializable> values, ServiceContext serviceContext) throws PortalException;

}
