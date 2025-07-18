package com.example.medialert.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medialert.R;
import com.example.medialert.data.PlaceItem;

import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    private List<PlaceItem> placeList;
    private OnPlaceClickListener listener;

    public interface OnPlaceClickListener {
        void onPlaceClick(PlaceItem place);
    }

    public PlacesAdapter(List<PlaceItem> placeList, OnPlaceClickListener listener) {
        this.placeList = placeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceItem place = placeList.get(position);
        holder.placeName.setText(place.getName());
        holder.placeAddress.setText(place.getAddress());
        holder.placeDistance.setText(String.format("%.2f km away", place.getDistanceInKm()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaceClick(place);
            }
        });
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView placeName, placeAddress, placeDistance;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.placeName);
            placeAddress = itemView.findViewById(R.id.placeAddress);
            placeDistance = itemView.findViewById(R.id.placeDistance);
        }
    }
}
