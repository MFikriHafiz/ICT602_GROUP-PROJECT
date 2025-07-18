package com.example.medialert.adapters;

import android.content.Context;
import android.net.Uri; // NEW: Import Uri
import android.util.Log; // NEW: For logging errors
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView; // NEW: Import ImageView
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // NEW: Import Glide
import com.example.medialert.R;
import com.example.medialert.model.Medication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {

    private List<Medication> medications = new ArrayList<>();
    private OnMedicationActionsListener actionsListener;
    private Context context;

    public interface OnMedicationActionsListener {
        void onEditClick(Medication medication);
        void onDeleteClick(Medication medication);
        void onToggleActive(Medication medication, boolean isActive);
        void onItemClick(Medication medication);
    }

    public void setOnMedicationActionsListener(OnMedicationActionsListener listener) {
        this.actionsListener = listener;
    }

    public void setMedications(List<Medication> medications) {
        this.medications = medications;
        notifyDataSetChanged();
    }

    public Medication getMedicationAt(int position) {
        if (position >= 0 && position < medications.size()) {
            return medications.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_medication, parent, false);
        return new MedicationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Medication currentMedication = medications.get(position);

        holder.medicationNameTextView.setText(currentMedication.getName());
        holder.medicationTypeTextView.setText("Type: " + currentMedication.getType());

        String dosageText = String.format(Locale.getDefault(), "Dosage: %.1f%s",
                currentMedication.getDosageQuantity(),
                currentMedication.getDosageUnit());
        holder.medicationDosageTextView.setText(dosageText);

        String frequencyAndTimes = currentMedication.getFrequency();
        if (currentMedication.getAlarmTimes() != null && !currentMedication.getAlarmTimes().isEmpty()) {
            StringBuilder timesBuilder = new StringBuilder();
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            // --- MODIFIED: Use Malaysia TimeZone for consistency ---
            timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));

            List<Long> sortedAlarmTimes = new ArrayList<>(currentMedication.getAlarmTimes());
            Collections.sort(sortedAlarmTimes, Comparator.naturalOrder());

            for (Long timeInMillis : sortedAlarmTimes) {
                // --- MODIFIED: Use Malaysia TimeZone for consistency ---
                Calendar tempCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                tempCalendar.setTimeInMillis(timeInMillis);
                timesBuilder.append(timeFormat.format(tempCalendar.getTime())).append(", ");
            }
            if (timesBuilder.length() > 0) {
                timesBuilder.setLength(timesBuilder.length() - 2);
                frequencyAndTimes += " (" + timesBuilder.toString() + ")";
            }
        }
        holder.medicationFrequencyTextView.setText(frequencyAndTimes);

        if (currentMedication.getInstructions() != null && !currentMedication.getInstructions().isEmpty()) {
            holder.medicationInstructionsTextView.setText("Instructions: " + currentMedication.getInstructions());
            holder.medicationInstructionsTextView.setVisibility(View.VISIBLE);
        } else {
            holder.medicationInstructionsTextView.setVisibility(View.GONE);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String startDate = sdf.format(new Date(currentMedication.getStartDate()));
        String endDateText;

        if (currentMedication.getEndDate() != 0) {
            endDateText = sdf.format(new Date(currentMedication.getEndDate()));
            holder.medicationDateRangeTextView.setText(String.format("Dates: %s - %s", startDate, endDateText));
        } else {
            holder.medicationDateRangeTextView.setText(String.format("Dates: %s - Ongoing", startDate));
        }

        holder.activeSwitch.setChecked(currentMedication.isActive());
        holder.activeSwitch.setOnCheckedChangeListener(null);
        holder.activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (actionsListener != null) {
                actionsListener.onToggleActive(currentMedication, isChecked);
            }
        });

        holder.editButton.setOnClickListener(v -> {
            if (actionsListener != null) {
                actionsListener.onEditClick(currentMedication);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (actionsListener != null) {
                actionsListener.onDeleteClick(currentMedication);
            }
        });

        // --- START OF NEW/MODIFIED: Load and display image using Glide ---
        if (currentMedication.getImageUrl() != null && !currentMedication.getImageUrl().isEmpty()) {
            try {
                Uri imageUri = Uri.parse(currentMedication.getImageUrl());
                Glide.with(context) // Use the adapter's context
                        .load(imageUri)
                        .placeholder(R.drawable.ic_medication_placeholder) // Your placeholder drawable
                        .error(R.drawable.ic_image_error) // Your error drawable (optional)
                        .into(holder.medicationImageView); // Reference to the new ImageView
                holder.medicationImageView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e("MedicationAdapter", "Error loading medication image for " + currentMedication.getName() + ": " + e.getMessage());
                holder.medicationImageView.setImageResource(R.drawable.ic_image_error); // Fallback to error drawable
                holder.medicationImageView.setVisibility(View.VISIBLE);
            }
        } else {
            // No image URL, hide the ImageView or show a generic placeholder
            holder.medicationImageView.setImageResource(R.drawable.ic_medication_placeholder); // Show placeholder by default
            holder.medicationImageView.setVisibility(View.VISIBLE); // Always visible to show placeholder if no image
        }
        // --- END OF NEW/MODIFIED IMAGE LOADING ---


        long currentTime = System.currentTimeMillis();
        int defaultTextColor = ContextCompat.getColor(context, R.color.text_color_default);
        int secondaryTextColor = ContextCompat.getColor(context, R.color.text_color_secondary);
        int finishedTextColor = ContextCompat.getColor(context, R.color.text_color_finished);

        if (currentMedication.getEndDate() != 0 && currentTime > currentMedication.getEndDate()) {
            holder.medicationNameTextView.setTextColor(finishedTextColor);
            holder.medicationTypeTextView.setTextColor(finishedTextColor);
            holder.medicationDosageTextView.setTextColor(finishedTextColor);
            holder.medicationFrequencyTextView.setTextColor(finishedTextColor);
            holder.medicationInstructionsTextView.setTextColor(finishedTextColor);
            holder.medicationDateRangeTextView.setTextColor(finishedTextColor);

            holder.activeSwitch.setEnabled(false);
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.activeSwitch.setEnabled(true);

            if (currentMedication.isActive()) {
                holder.medicationNameTextView.setTextColor(defaultTextColor);
                holder.medicationTypeTextView.setTextColor(secondaryTextColor);
                holder.medicationDosageTextView.setTextColor(secondaryTextColor);
                holder.medicationFrequencyTextView.setTextColor(secondaryTextColor);
                holder.medicationInstructionsTextView.setTextColor(secondaryTextColor);
                holder.medicationDateRangeTextView.setTextColor(secondaryTextColor);
                holder.itemView.setAlpha(1.0f);
            } else {
                holder.medicationNameTextView.setTextColor(finishedTextColor);
                holder.medicationTypeTextView.setTextColor(finishedTextColor);
                holder.medicationDosageTextView.setTextColor(finishedTextColor);
                holder.medicationFrequencyTextView.setTextColor(finishedTextColor);
                holder.medicationInstructionsTextView.setTextColor(finishedTextColor);
                holder.medicationDateRangeTextView.setTextColor(finishedTextColor);
                holder.itemView.setAlpha(0.8f);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (actionsListener != null) {
                actionsListener.onItemClick(currentMedication);
            }
        });
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        private TextView medicationNameTextView;
        private TextView medicationTypeTextView;
        private TextView medicationDosageTextView;
        private TextView medicationFrequencyTextView;
        private TextView medicationInstructionsTextView;
        private TextView medicationDateRangeTextView;
        private SwitchCompat activeSwitch;
        private ImageButton editButton;
        private ImageButton deleteButton;
        private ImageView medicationImageView; // --- NEW: ImageView member ---

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            medicationNameTextView = itemView.findViewById(R.id.medication_name_text_view);
            medicationTypeTextView = itemView.findViewById(R.id.medication_type_text_view);
            medicationDosageTextView = itemView.findViewById(R.id.medication_dosage_text_view);
            medicationFrequencyTextView = itemView.findViewById(R.id.medication_frequency_text_view);
            medicationInstructionsTextView = itemView.findViewById(R.id.medication_instructions_text_view);
            medicationDateRangeTextView = itemView.findViewById(R.id.medication_date_range_text_view);
            activeSwitch = itemView.findViewById(R.id.active_switch);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            medicationImageView = itemView.findViewById(R.id.medication_image_view); // --- NEW: Find ImageView by ID ---
        }
    }
}