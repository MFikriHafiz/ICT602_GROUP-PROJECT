package com.example.medialert.ui.map;

import android.annotation.SuppressLint;
import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.medialert.data.PlaceItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapViewModel extends ViewModel {

    private final MutableLiveData<List<PlaceItem>> _places = new MutableLiveData<>();
    public LiveData<List<PlaceItem>> places = _places;

    private static final String API_KEY = "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

    public void fetchNearbyPlaces(Location userLocation, String type) {
        if ("all".equalsIgnoreCase(type)) {
            fetchAllTypes(userLocation);
        } else {
            String googleType = convertToValidGoogleType(type);
            fetchType(userLocation, googleType);
        }
    }

    private void fetchAllTypes(Location userLocation) {
        new Thread(() -> {
            List<PlaceItem> combinedList = new ArrayList<>();
            String[] types = {"hospital", "pharmacy", "doctor"};
            for (String type : types) {
                combinedList.addAll(fetchPlacesForType(userLocation, type));
            }
            _places.postValue(combinedList);
        }).start();
    }

    private void fetchType(Location userLocation, String type) {
        new Thread(() -> {
            List<PlaceItem> placeList = fetchPlacesForType(userLocation, type);
            _places.postValue(placeList);
        }).start();
    }

    private String convertToValidGoogleType(String type) {
        switch (type.toLowerCase()) {
            case "clinic":
                return "doctor";
            default:
                return type.toLowerCase();
        }
    }

    private List<PlaceItem> fetchPlacesForType(Location userLocation, String type) {
        List<PlaceItem> placeList = new ArrayList<>();
        try {
            @SuppressLint("DefaultLocale") String urlString = String.format(
                    "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=3000&type=%s&key=%s",
                    userLocation.getLatitude(),
                    userLocation.getLongitude(),
                    type,
                    API_KEY
            );

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            reader.close();
            conn.disconnect();

            JSONObject jsonResponse = new JSONObject(jsonBuilder.toString());
            JSONArray results = jsonResponse.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);
                String name = place.optString("name", "Unknown Place");
                String address = place.optString("vicinity", "No address");

                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");

                float[] resultsDistance = new float[1];
                Location.distanceBetween(
                        userLocation.getLatitude(), userLocation.getLongitude(),
                        lat, lng,
                        resultsDistance
                );
                double distanceInKm = resultsDistance[0] / 1000.0;

                placeList.add(new PlaceItem(name, address, lat, lng, distanceInKm, type));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return placeList;
    }
}
