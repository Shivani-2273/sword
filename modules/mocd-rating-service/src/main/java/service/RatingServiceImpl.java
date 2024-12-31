package service;

import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalServiceUtil;
import com.liferay.object.service.ObjectEntryLocalServiceUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
import org.osgi.service.component.annotations.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
@Component(
        immediate = true,
        service = RatingService.class
)
public class RatingServiceImpl implements RatingService{

    private static final String RATING_OBJECT_DEFINITION_NAME = "C_Rating";
    private static final String LIKE_OBJECT_DEFINITION_NAME = "C_DocumentLike";

    @Override
    public ObjectEntry addRating(long userId, long groupId, Map<String, Serializable> values, ServiceContext serviceContext) throws PortalException {
        long objectDefinitionId = getObjectDefinitionId(serviceContext, RATING_OBJECT_DEFINITION_NAME);
        return ObjectEntryLocalServiceUtil.addObjectEntry(
                userId,
                groupId,
                objectDefinitionId,
                values,
                serviceContext
        );
    }

    public boolean updateRating(long userId, long objectEntryId, Map<String, Serializable> values, ServiceContext serviceContext) throws PortalException {
        ObjectEntryLocalServiceUtil.updateObjectEntry(
                userId,
                objectEntryId,
                values,
                serviceContext
        );
        return true;
    }


    public ObjectEntry addLike(long userId, long groupId, Map<String, Serializable> values, ServiceContext serviceContext) throws PortalException {
        long objectDefinitionId = getObjectDefinitionId(serviceContext, LIKE_OBJECT_DEFINITION_NAME);
        return ObjectEntryLocalServiceUtil.addObjectEntry(
                userId,
                groupId,
                objectDefinitionId,
                values,
                serviceContext
        );
    }

    @Override
    public List<ObjectEntry> getEntries(String definitionName, ServiceContext serviceContext) throws PortalException {
        long objectDefinitionId = getObjectDefinitionId(serviceContext, definitionName);
        DynamicQuery query = ObjectEntryLocalServiceUtil.dynamicQuery()
                .add(RestrictionsFactoryUtil.eq("objectDefinitionId", objectDefinitionId));
        return ObjectEntryLocalServiceUtil.dynamicQuery(query);
    }


    private long getObjectDefinitionId(ServiceContext serviceContext, String definitionName)
            throws PortalException {
        ObjectDefinition objectDefinition = ObjectDefinitionLocalServiceUtil.fetchObjectDefinition(
                serviceContext.getCompanyId(),
                definitionName
        );
        if (objectDefinition == null) {
            throw new PortalException("Object definition not found for name: " + definitionName);
        }
        return objectDefinition.getObjectDefinitionId();
    }

    @Override
    public ObjectEntry findExistingRating(long userId, long fileEntryId, ServiceContext serviceContext)
            throws PortalException {
        List<ObjectEntry> entries = getEntries(RATING_OBJECT_DEFINITION_NAME, serviceContext);

        for (ObjectEntry entry : entries) {
            Map<String, Serializable> values = entry.getValues();
            if (entry.getUserId() == userId &&
                    values.containsKey("fileEntryId") &&
                    fileEntryId == ((Number) values.get("fileEntryId")).longValue()) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public ObjectEntry addOrUpdateRating(long userId, long groupId, Map<String, Serializable> values, ServiceContext serviceContext)
            throws PortalException {
        long fileEntryId = ((Number) values.get("fileEntryId")).longValue();
        ObjectEntry existingRating = findExistingRating(userId, fileEntryId, serviceContext);

        if (existingRating != null) {
            // Update existing entry
            return ObjectEntryLocalServiceUtil.updateObjectEntry(
                    userId,
                    existingRating.getObjectEntryId(),
                    values,
                    serviceContext
            );
        } else {
            // Create new entry
            long objectDefinitionId = getObjectDefinitionId(serviceContext, RATING_OBJECT_DEFINITION_NAME);
            return ObjectEntryLocalServiceUtil.addObjectEntry(
                    userId,
                    groupId,
                    objectDefinitionId,
                    values,
                    serviceContext
            );
        }
    }
}
