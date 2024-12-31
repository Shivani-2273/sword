package util;

import com.liferay.object.model.ObjectEntry;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class StatisticsUtil {

    public static double[] calculateRatingStats(List<ObjectEntry> entries, long fileEntryId) {
        double totalRatings = 0;
        double sumRatings = 0;

        for (ObjectEntry entry : entries) {
            Map<String, Serializable> values = entry.getValues();
            if (values.containsKey("fileEntryId") &&
                    fileEntryId == ((Number) values.get("fileEntryId")).longValue() &&
                    values.containsKey("rating")) {
                int rating = ((Number) values.get("rating")).intValue();
                sumRatings += rating;
                totalRatings++;
            }
        }

        return new double[]{totalRatings, sumRatings};
    }

    public static JSONArray createRatingResponseArray(Map<Long, List<Integer>> ratingsMap) {
        JSONArray responseArray = JSONFactoryUtil.createJSONArray();
        for (Map.Entry<Long, List<Integer>> entry : ratingsMap.entrySet()) {
            JSONObject response = JSONFactoryUtil.createJSONObject();
            List<Integer> ratings = entry.getValue();
            double average = ratings.stream().mapToDouble(Integer::doubleValue).sum() / ratings.size();

            response.put("fileEntryId", entry.getKey());
            response.put("averageRating", Math.round(average * 10.0) / 10.0);
            response.put("totalRatings", ratings.size());
            responseArray.put(response);
        }
        return responseArray;
    }
}