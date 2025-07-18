package com.example.medialert.ui.emergencycontacts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medialert.adapters.EmergencyContactsAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.medialert.R;
import com.example.medialert.data.EmergencyContact;

public class EmergencyContactsFragment extends Fragment implements EmergencyContactsAdapter.OnContactActionListener {

    private EmergencyContactsViewModel emergencyContactsViewModel;
    private EmergencyContactsAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyStateView;
    private NavController navController;

    private static final String TAG = "EmergencyContactsFrag";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Apply Material 3 theme (ensure Theme_MediAlert_EmergencyContacts is defined or use Theme_MediAlert)
        Context contextThemeWrapper = new ContextThemeWrapper(getContext(), R.style.Theme_MediAlert); // Reverted to main app theme for consistency, or define custom if needed.
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        View root = localInflater.inflate(R.layout.fragment_emergency_contacts, container, false);

        emergencyContactsViewModel = new ViewModelProvider(this).get(EmergencyContactsViewModel.class);

        // Initialize views
        recyclerView = root.findViewById(R.id.recyclerViewEmergencyContacts);
        FloatingActionButton fabAddContact = root.findViewById(R.id.fabAddContact);
        emptyStateView = root.findViewById(R.id.emptyStateView);

        // Setup RecyclerView
        adapter = new EmergencyContactsAdapter(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Set the listener for actions on contacts (call, WhatsApp, delete, edit)
        adapter.setOnContactActionListener(this);

        // Observe contacts LiveData from ViewModel
        emergencyContactsViewModel.getEmergencyContacts().observe(getViewLifecycleOwner(), contacts -> {
            Log.d(TAG, "Contacts updated. Size: " + contacts.size());
            adapter.setContacts(contacts);
            // Show/hide empty state based on contact list
            if (contacts.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyStateView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        // Set up click listener for FAB to add new contact
        fabAddContact.setOnClickListener(v -> {
            // Navigate to AddEditContactFragment for 'add' mode
            navController.navigate(R.id.action_emergencyContactsFragment_to_addEditContactFragment);
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get NavController after view is created to avoid null pointer
        navController = Navigation.findNavController(view);
    }

    // --- EmergencyContactsAdapter.OnContactActionListener implementations ---

    @Override
    public void onItemClick(EmergencyContact contact) {
        // Handle click on the contact item (for editing)
        Log.d(TAG, "Clicked on contact for edit: " + contact.getName() + ", ID: " + contact.getId());
        Bundle bundle = new Bundle();
        // --- FIX START ---
        // Instead of just ID, pass the entire EmergencyContact object
        bundle.putSerializable("emergencyContact", contact);
        // --- FIX END ---
        navController.navigate(R.id.action_emergencyContactsFragment_to_addEditContactFragment, bundle);
    }

    @Override
    public void onCallClick(EmergencyContact contact) {
        Log.d(TAG, "Call button clicked for: " + contact.getName() + ", Phone: " + contact.getPhoneNumber());
        if (contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty()) {
            try {
                String phoneNumber = contact.getPhoneNumber().trim();
                // Ensure URI scheme
                if (!phoneNumber.startsWith("tel:")) {
                    phoneNumber = "tel:" + phoneNumber;
                }
                Intent callIntent = new Intent(Intent.ACTION_DIAL); // ACTION_DIAL to open dialer
                callIntent.setData(Uri.parse(phoneNumber));
                startActivity(callIntent);
            } catch (Exception e) {
                Log.e(TAG, "Error initiating call for " + contact.getPhoneNumber(), e);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Could not initiate call.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Phone number not available.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onWhatsAppClick(EmergencyContact contact) {
        Log.d(TAG, "WhatsApp button clicked for: " + contact.getName() + ", Phone: " + contact.getPhoneNumber());
        if (contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty()) {
            try {
                String phoneNumber = contact.getPhoneNumber().trim();
                // WhatsApp API expects numbers without '+' sign for wa.me link
                if (phoneNumber.startsWith("+")) {
                    phoneNumber = phoneNumber.substring(1);
                }
                Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                whatsappIntent.setData(Uri.parse("https://wa.me/" + phoneNumber));
                whatsappIntent.setPackage("com.whatsapp"); // Try to open in WhatsApp app first

                startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException e) {
                Log.e(TAG, "WhatsApp app not found, opening in browser.", e);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "WhatsApp app not found, opening in browser.", Toast.LENGTH_LONG).show();
                }
                try { // Fallback to browser if WhatsApp app is not installed
                    String phoneNumber = contact.getPhoneNumber().trim();
                    if (phoneNumber.startsWith("+")) {
                        phoneNumber = phoneNumber.substring(1);
                    }
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + phoneNumber));
                    startActivity(browserIntent);
                } catch (Exception ex) {
                    Log.e(TAG, "Could not open WhatsApp via browser either.", ex);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Could not open WhatsApp.", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening WhatsApp for " + contact.getPhoneNumber(), e);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Could not open WhatsApp.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Phone number not available for WhatsApp.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDeleteClick(EmergencyContact contact) {
        Log.d(TAG, "Delete button clicked for: " + contact.getName());
        emergencyContactsViewModel.deleteContact(contact.getId());
        if (getContext() != null) {
            Toast.makeText(getContext(), contact.getName() + " deleted.", Toast.LENGTH_SHORT).show();
        }
    }
}