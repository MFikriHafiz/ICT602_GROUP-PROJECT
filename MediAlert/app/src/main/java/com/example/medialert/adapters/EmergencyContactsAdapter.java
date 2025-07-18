package com.example.medialert.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.medialert.R;
import com.example.medialert.data.EmergencyContact;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder> {

    private List<EmergencyContact> contacts = new ArrayList<>();
    private final Context context; // Use final as context isn't expected to change
    private OnContactActionListener listener;

    // Interface for specific actions on contact items
    public interface OnContactActionListener {
        void onItemClick(EmergencyContact contact); // For clicking the whole card (e.g., to edit)
        void onCallClick(EmergencyContact contact);
        void onWhatsAppClick(EmergencyContact contact);
        void onDeleteClick(EmergencyContact contact);
    }

    public EmergencyContactsAdapter(Context context) {
        this.context = context;
    }

    public void setContacts(List<EmergencyContact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    public void setOnContactActionListener(OnContactActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact currentContact = contacts.get(position);

        holder.textViewName.setText(currentContact.getName());
        holder.textViewRelationship.setText(currentContact.getRelationship());
        holder.textViewPhoneNumber.setText(currentContact.getPhoneNumber());

        // Load image using Glide
        if (currentContact.getImageUrl() != null && !currentContact.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(Uri.parse(currentContact.getImageUrl()))
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(holder.imageViewProfile);
        } else {
            holder.imageViewProfile.setImageResource(R.drawable.ic_default_profile);
        }

        // Set primary contact indicator visibility
        holder.imageViewPrimaryIndicator.setVisibility(currentContact.isPrimary() ? View.VISIBLE : View.GONE);

        // Click listeners for the whole item and individual buttons
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentContact);
            }
        });

        holder.buttonCall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCallClick(currentContact);
            }
        });

        holder.buttonWhatsApp.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWhatsAppClick(currentContact);
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(currentContact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView imageViewProfile;
        private final TextView textViewName;
        private final TextView textViewRelationship;
        private final TextView textViewPhoneNumber;
        private final ImageButton buttonCall;
        private final ImageButton buttonWhatsApp;
        private final ImageButton buttonDelete;
        private final ImageView imageViewPrimaryIndicator;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            textViewName = itemView.findViewById(R.id.textViewContactName);
            textViewRelationship = itemView.findViewById(R.id.textViewContactRelationship);
            textViewPhoneNumber = itemView.findViewById(R.id.textViewContactPhoneNumber);
            buttonCall = itemView.findViewById(R.id.buttonCall);
            buttonWhatsApp = itemView.findViewById(R.id.buttonWhatsApp);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            imageViewPrimaryIndicator = itemView.findViewById(R.id.imageViewPrimaryIndicator);
        }
    }
}