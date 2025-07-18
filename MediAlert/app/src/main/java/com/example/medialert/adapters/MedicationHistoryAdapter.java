package com.example.medialert.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.medialert.R; // Make sure this R points to your project's R file
import com.example.medialert.model.Medication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicationHistoryAdapter extends RecyclerView.Adapter<MedicationHistoryAdapter.MedicationHistoryViewHolder> {

    private List<Medication> medicationList;
    private final Context context;
    private OnItemClickListener itemClickListener;
    private OnDeleteClickListener deleteClickListener;

    public MedicationHistoryAdapter(Context context) {
        this.context = context;
        this.medicationList = new ArrayList<>();
    }

    public void setMedicationList(List<Medication> medicationList) {
        this.medicationList = medicationList;
        notifyDataSetChanged(); // Notify adapter that data has changed
    }

    public interface OnItemClickListener {
        void onItemClick(Medication medication);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Medication medication);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    @NonNull
    @Override
    public MedicationHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medication_history, parent, false);
        return new MedicationHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationHistoryViewHolder holder, int position) {
        Medication medication = medicationList.get(position);
        holder.bind(medication);
    }

    @Override
    public int getItemCount() {
        return medicationList.size();
    }

    public class MedicationHistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMedicationPhoto;
        TextView textViewMedicationName;
        TextView textViewDosageFrequency;
        TextView textViewInstructions;
        TextView textViewDates;
        ImageButton buttonDelete;

        public MedicationHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewMedicationPhoto = itemView.findViewById(R.id.imageViewHistoryMedicationPhoto);
            textViewMedicationName = itemView.findViewById(R.id.textViewHistoryMedicationName);
            textViewDosageFrequency = itemView.findViewById(R.id.textViewHistoryDosageFrequency);
            textViewInstructions = itemView.findViewById(R.id.textViewHistoryInstructions);
            textViewDates = itemView.findViewById(R.id.textViewHistoryDates);
            buttonDelete = itemView.findViewById(R.id.buttonHistoryDelete);

            // Set up click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(medicationList.get(getAdapterPosition()));
                }
            });

            // Set up click listener for the delete button
            buttonDelete.setOnClickListener(v -> {
                if (deleteClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    deleteClickListener.onDeleteClick(medicationList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Medication medication) {
            textViewMedicationName.setText(medication.getName());

            String dosageFreq = String.format(Locale.getDefault(), "%s %s, %s",
                    medication.getDosageQuantity(), medication.getDosageUnit(), medication.getFrequency());
            textViewDosageFrequency.setText(dosageFreq);

            if (!TextUtils.isEmpty(medication.getInstructions())) {
                textViewInstructions.setText(medication.getInstructions());
                textViewInstructions.setVisibility(View.VISIBLE);
            } else {
                textViewInstructions.setVisibility(View.GONE);
            }

            // Format start and end dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String startDateStr = (medication.getStartDate() > 0) ? dateFormat.format(new Date(medication.getStartDate())) : "N/A";
            String endDateStr = (medication.getEndDate() > 0) ? dateFormat.format(new Date(medication.getEndDate())) : "N/A";
            textViewDates.setText(String.format("Taken: %s to %s", startDateStr, endDateStr));


            // Load image using Glide
            if (!TextUtils.isEmpty(medication.getImageUrl())) {
                Glide.with(context)
                        .load(Uri.parse(medication.getImageUrl()))
                        .placeholder(R.drawable.ic_medication_placeholder) // Your placeholder drawable
                        .error(R.drawable.ic_medication_placeholder) // Image to show if loading fails
                        .into(imageViewMedicationPhoto);
            } else {
                Glide.with(context)
                        .load(R.drawable.ic_medication_placeholder) // Load placeholder if no URL
                        .into(imageViewMedicationPhoto);
            }
        }
    }
}