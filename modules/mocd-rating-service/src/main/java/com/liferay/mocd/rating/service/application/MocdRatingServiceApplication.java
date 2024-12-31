package com.liferay.mocd.rating.service.application;
import java.io.Serializable;
import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryTypeException;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.object.model.ObjectEntry;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import service.FileDownloadDTO;
import service.FileDownloadService;
import service.RatingService;
import util.ResponseUtil;
import util.StatisticsUtil;
@Component(
	property = {
		JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=/API",
		JaxrsWhiteboardConstants.JAX_RS_NAME + "=Ratings.Rest"
	},
	service = Application.class
)
public class MocdRatingServiceApplication extends Application {

	@Reference
	private RatingService ratingService;

	@Reference
	private DLFileEntryLocalService _dlFileEntryLocalService;

	@Reference
	private FileDownloadService _fileDownloadService;

	public Set<Object> getSingletons() {
		return Collections.<Object>singleton(this);
	}

	@POST
	@Path("/ratings/add_rating")
	@Consumes("application/json")
	@Produces("application/json")
	public Response addRating(String jsonInput) {
		try {
			JSONObject jsonObject = JSONFactoryUtil.createJSONObject(jsonInput);
			long fileEntryId = jsonObject.getLong("fileEntryId");
			int rating = jsonObject.getInt("rating");
			ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();

			Map<String, Serializable> values = new HashMap<>();
			values.put("fileEntryId", fileEntryId);
			values.put("rating", rating);

			if (jsonObject.has("usersId")) {
				return handleRegisteredUserRating(jsonObject.getLong("usersId"),jsonObject.getLong("fileEntryId") ,values, serviceContext);
			} else {
				return handleGuestRating(values, serviceContext);
			}
		} catch (Exception e) {
			return ResponseUtil.createErrorResponse(Response.Status.BAD_REQUEST, "Failed to process rating: " + e.getMessage());
		}
	}

	@GET
	@Path("/ratings/average/{fileEntryId}")
	@Produces("application/json")
	public Response getAverageRating(@PathParam("fileEntryId") long fileEntryId) {
		try {
			ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
			List<ObjectEntry> entries = ratingService.getEntries("C_Rating", serviceContext);

			if (entries.isEmpty()) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			double[] stats = StatisticsUtil.calculateRatingStats(entries, fileEntryId);
			if (stats[0] == 0) { // totalRatings
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			JSONObject response = JSONFactoryUtil.createJSONObject();
			response.put("fileEntryId", fileEntryId);
			response.put("averageRating", Math.round((stats[1] / stats[0]) * 10.0) / 10.0);
			response.put("totalRatings", (int)stats[0]);
			response.put("sumRatings", stats[1]);

			return Response.ok(response.toString()).build();
		} catch (Exception e) {
			return ResponseUtil.createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}


	@GET
	@Path("/ratings/average")
	@Produces("application/json")
	public Response getAllAverageRatings() {
		try {
			ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
			List<ObjectEntry> entries = ratingService.getEntries("C_Rating", serviceContext);

			if (entries.isEmpty()) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			Map<Long, List<Integer>> ratingsMap = new HashMap<>();
			for (ObjectEntry entry : entries) {
				Map<String, Serializable> values = entry.getValues();
				if (values.containsKey("fileEntryId") && values.containsKey("rating")) {
					long fileEntryId = ((Number) values.get("fileEntryId")).longValue();
					int value = ((Number) values.get("rating")).intValue();
					ratingsMap.computeIfAbsent(fileEntryId, k -> new ArrayList<>()).add(value);
				}
			}

			JSONArray responseArray = StatisticsUtil.createRatingResponseArray(ratingsMap);
			return Response.ok(responseArray.toString()).build();
		} catch (Exception e) {
			return ResponseUtil.createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@POST
	@Path("/likes/add_like")
	@Consumes("application/json")
	@Produces("application/json")
	public Response addLike(String jsonInput) {
		try {
			JSONObject jsonObject = JSONFactoryUtil.createJSONObject(jsonInput);
			long fileEntryId = jsonObject.getLong("fileEntryId");

			ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
			List<ObjectEntry> entries = ratingService.getEntries("C_DocumentLike", serviceContext);

			Map<String, Serializable> values = new HashMap<>();
			values.put("fileEntryId", fileEntryId);

			boolean entryExists = false;
			for (ObjectEntry entry : entries) {
				Map<String, Serializable> existingValues = entry.getValues();
				if (existingValues.containsKey("fileEntryId") &&
						fileEntryId == ((Number) existingValues.get("fileEntryId")).longValue()) {
					int currentLikes = ((Number) existingValues.get("likeCount")).intValue();
					values.put("likeCount", currentLikes + 1);
					ratingService.updateRating(20096, entry.getObjectEntryId(), values, serviceContext);
					entryExists = true;
					break;
				}
			}
			if (!entryExists) {
				values.put("likeCount", 1);
				ratingService.addLike(20096, serviceContext.getScopeGroupId(), values, serviceContext);
			}

			return ResponseUtil.createSuccessResponse(entryExists ? "Like count updated" : "First like added");
		} catch (Exception e) {
			return ResponseUtil.createErrorResponse(Response.Status.BAD_REQUEST, "Failed to process like: " + e.getMessage());
		}
	}

	@GET
	@Path("/likes/all")
	@Produces("application/json")
	public Response getAllLikes() {
		try {

			ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
			List<ObjectEntry> entries = ratingService.getEntries("C_DocumentLike", serviceContext);
			if (entries.isEmpty()) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			JSONArray responseArray = JSONFactoryUtil.createJSONArray();
			for (ObjectEntry entry : entries) {
				Map<String, Serializable> values = entry.getValues();
				if (values.containsKey("fileEntryId") && values.containsKey("likeCount")) {
					JSONObject response = JSONFactoryUtil.createJSONObject();
					response.put("fileEntryId", ((Number) values.get("fileEntryId")).longValue());
					response.put("likeCount", ((Number) values.get("likeCount")).intValue());
					responseArray.put(response);
				}
			}

			return Response.ok(responseArray.toString()).build();
		} catch (Exception e) {
			return ResponseUtil.createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@GET
	@Path("/downloads/by-folder-id/{folderId}")
	@Produces("application/json")
	public Response getDownloadsFolder(@PathParam("folderId") long folderId) {
		try {
			// Create dynamic query to get files in folder
			DynamicQuery fileQuery = _dlFileEntryLocalService.dynamicQuery();
			fileQuery.add(RestrictionsFactoryUtil.eq("folderId", folderId));

			List<DLFileEntry> files = _dlFileEntryLocalService.dynamicQuery(fileQuery);
			JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

			for (DLFileEntry file : files) {
				JSONObject fileObj = JSONFactoryUtil.createJSONObject();
				fileObj.put("fileEntryId", String.valueOf(file.getFileEntryId()));
				fileObj.put("downloads", file.getReadCount());
				jsonArray.put(fileObj);
			}

			return Response.ok(jsonArray.toString()).build();

		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path("/downloads/{fileEntryId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDownloads(@PathParam("fileEntryId") long fileEntryId) throws PortalException {
		try {
			long downloadCount = _fileDownloadService.getDownloadCount(fileEntryId);

			JSONObject response = JSONFactoryUtil.createJSONObject();
			response.put("downloads", String.valueOf(downloadCount));

			return Response.ok(response.toString()).build();
		} catch (NoSuchFileEntryException nsfe) {
			return ResponseUtil.createErrorResponse(
					Response.Status.NOT_FOUND,
					"File entry not found with id: " + fileEntryId
			);
		} catch (PrincipalException pe) {
			return ResponseUtil.createErrorResponse(
					Response.Status.FORBIDDEN,
					"Not authorized to access file entry: " + fileEntryId
			);
		}
	}

	@GET
	@Path("/downloads/by-document-type/{documentTypeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDownloadsByDocumentType(@PathParam("documentTypeId") long documentTypeId) {
		try {
			List<FileDownloadDTO> downloads = _fileDownloadService.getDownloadsByDocumentType(documentTypeId);

			JSONArray responseArray = JSONFactoryUtil.createJSONArray();
			for (FileDownloadDTO download : downloads) {
				JSONObject response = JSONFactoryUtil.createJSONObject();
				response.put("fileEntryId", String.valueOf(download.getFileEntryId()));
				response.put("downloads", String.valueOf(download.getDownloads()));
				responseArray.put(response);
			}

			return Response.ok(responseArray.toString()).build();

		} catch (NoSuchFileEntryTypeException nsfte) {
			return ResponseUtil.createErrorResponse(
					Response.Status.NOT_FOUND,
					"Document type not found with id: " + documentTypeId
			);
		}  catch (PortalException pe) {
			return ResponseUtil.createErrorResponse(
					Response.Status.INTERNAL_SERVER_ERROR,
					"Error retrieving document type information"
			);
		}
	}

	private Response handleRegisteredUserRating(long usersId, long fileEntryId,Map<String, Serializable> values, ServiceContext serviceContext) {
		try {
			User user = UserLocalServiceUtil.getUser(usersId);
			if (user == null) {
				return ResponseUtil.createErrorResponse(Response.Status.BAD_REQUEST, "Invalid user ID");
			}
			values.put("usersId", usersId);
			ObjectEntry existingRating = ratingService.findExistingRating(
					usersId,
					fileEntryId,
					serviceContext
			);

			// Add or update the rating
			ObjectEntry result = ratingService.addOrUpdateRating(
					usersId,
					serviceContext.getScopeGroupId(),
					values,
					serviceContext
			);

			return ResponseUtil.createSuccessResponse(
					existingRating != null ? "Rating updated successfully" : "Rating added successfully"
			);
		} catch (Exception e) {
			return ResponseUtil.createErrorResponse(Response.Status.BAD_REQUEST, "Failed to handle user rating: " + e.getMessage());
		}
	}

	private Response handleGuestRating(Map<String, Serializable> values, ServiceContext serviceContext) {
		try {
			Company company = CompanyLocalServiceUtil.getCompany(CompanyThreadLocal.getCompanyId());
			long guestUserId = company.getDefaultUser().getUserId();
			values.put("usersId", guestUserId);

			ratingService.addRating(guestUserId, serviceContext.getScopeGroupId(), values, serviceContext);
			return ResponseUtil.createSuccessResponse("Guest rating added successfully");
		} catch (Exception e) {
			return ResponseUtil.createErrorResponse(Response.Status.BAD_REQUEST, "Failed to handle guest rating: " + e.getMessage());
		}
	}

}