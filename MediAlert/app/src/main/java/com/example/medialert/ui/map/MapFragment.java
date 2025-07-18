package com.example.medialert.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medialert.R;
import com.example.medialert.adapters.PlacesAdapter;
import com.example.medialert.data.PlaceItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private Spinner filterSpinner;
    private RecyclerView recyclerView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient locationProvider;
    private Location currentLocation;
    private MapViewModel mapViewModel;
    private final Map<String, Marker> markerMap = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        filterSpinner = view.findViewById(R.id.filterSpinner);
        recyclerView = view.findViewById(R.id.placesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity());
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        setupSpinner();
        setupMap();

        return view;
    }

    private void setupSpinner() {
        String[] options = {"All", "Hospital", "Pharmacy", "Clinic"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (currentLocation != null) {
                    String type = parent.getItemAtPosition(position).toString();
                    mapViewModel.fetchNearbyPlaces(currentLocation, type);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = new SupportMapFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.mapContainer, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        googleMap.setMyLocationEnabled(true);
        locationProvider.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f));

                // Trigger fetch for default filter
                String defaultType = filterSpinner.getSelectedItem().toString();
                mapViewModel.fetchNearbyPlaces(currentLocation, defaultType);
            }
        });

        mapViewModel.places.observe(getViewLifecycleOwner(), places -> {
            googleMap.clear();
            markerMap.clear();

            for (PlaceItem place : places) {
                LatLng latLng = new LatLng(place.getLatitude(), place.getLongitude());
                Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                if (marker != null) {
                    markerMap.put(place.getName(), marker);
                }
            }

            PlacesAdapter adapter = new PlacesAdapter(places, place -> {
                Marker selectedMarker = markerMap.get(place.getName());
                if (selectedMarker != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedMarker.getPosition(), 16f));
                    selectedMarker.showInfoWindow();
                }
            });

            recyclerView.setAdapter(adapter);
        });
    }
}
