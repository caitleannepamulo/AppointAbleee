package com.example.appointable;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentMessagesFragment extends Fragment {

    private RecyclerView rvTeacherMessages;
    private TeacherMessagesAdapter adapter;
    private TextView tvEmptyMessages;

    private TextInputEditText etSearchMessages;
    private ImageView btnNewMessage;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String currentUid;

    private final List<TeacherMessageModel> allUsers = new ArrayList<>();
    private final List<TeacherMessageModel> shownUsers = new ArrayList<>();
    private final Map<String, ConversationMeta> convoMetaMap = new HashMap<>();

    private ListenerRegistration conversationsListener;
    private String currentQuery = "";
    private boolean usersLoaded = false;

    private static class ConversationMeta {
        String otherUid;
        String lastMessage;
        Timestamp lastTime;
        int unreadCount;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_messages_fragment, container, false);

        rvTeacherMessages = view.findViewById(R.id.rvTeacherMessages);
        rvTeacherMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        tvEmptyMessages = view.findViewById(R.id.tvEmptyMessages);
        etSearchMessages = view.findViewById(R.id.etSearchMessages);
        btnNewMessage = view.findViewById(R.id.btnNewMessage);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) currentUid = user.getUid();

        adapter = new TeacherMessagesAdapter(shownUsers, model -> {
            openChatScreen(model.getUserId(), model.getName(), null);
        });
        rvTeacherMessages.setAdapter(adapter);

        setupSearch();
        setupNewMessage();

        loadUsersFromFirestore();

        if (!TextUtils.isEmpty(currentUid)) {
            listenToConversations();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUnreadBadge();
        updateEmptyState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }
    }

    private void loadUsersFromFirestore() {
        usersLoaded = false;

        allUsers.clear();
        shownUsers.clear();
        adapter.notifyDataSetChanged();

        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        String uid = doc.getId();
                        if (!TextUtils.isEmpty(currentUid) && currentUid.equals(uid)) continue;

                        String profileUrl = doc.getString("profileImageUrl");

                        String firstName = safe(doc.getString("firstName"));
                        String middleName = safe(doc.getString("middleName"));
                        String lastName = safe(doc.getString("lastName"));
                        String suffix = safe(doc.getString("suffix"));

                        String fullName = buildFullName(firstName, middleName, lastName, suffix);

                        if (TextUtils.isEmpty(fullName)) {
                            fullName = safe(doc.getString("name"));
                        }

                        if (TextUtils.isEmpty(fullName)) continue;

                        allUsers.add(new TeacherMessageModel(
                                uid,
                                fullName,
                                "Tap to start a conversation",
                                "",
                                0,
                                profileUrl
                        ));
                    }

                    usersLoaded = true;

                    mergeConversationMetaIntoUsers();
                    applySearch(currentQuery);
                    refreshUnreadBadge();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    usersLoaded = true;
                    updateEmptyState();
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Users load failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildFullName(String first, String middle, String last, String suffix) {
        StringBuilder sb = new StringBuilder();
        if (!first.isEmpty()) sb.append(first);
        if (!middle.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middle);
        }
        if (!last.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(last);
        }
        if (!suffix.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(suffix);
        }
        return sb.toString().trim();
    }

    private void listenToConversations() {
        if (TextUtils.isEmpty(currentUid)) return;

        if (conversationsListener != null) conversationsListener.remove();

        conversationsListener = db.collection("conversations")
                .whereArrayContains("members", currentUid)
                .addSnapshotListener((QuerySnapshot value,
                                      com.google.firebase.firestore.FirebaseFirestoreException error) -> {
                    if (error != null || value == null) return;

                    convoMetaMap.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        List<String> members = (List<String>) doc.get("members");
                        if (members == null || members.size() != 2) continue;

                        String otherUid = null;
                        for (String m : members) {
                            if (m != null && !m.equals(currentUid)) {
                                otherUid = m;
                                break;
                            }
                        }
                        if (TextUtils.isEmpty(otherUid)) continue;

                        String lastMessage = doc.getString("lastMessage");
                        Timestamp lastTime = doc.getTimestamp("lastMessageTime");

                        int unread = 0;
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            Object val = data.get("unread." + currentUid);
                            if (val instanceof Number) unread = ((Number) val).intValue();
                        }

                        ConversationMeta meta = new ConversationMeta();
                        meta.otherUid = otherUid;
                        meta.lastMessage = lastMessage;
                        meta.lastTime = lastTime;
                        meta.unreadCount = unread;

                        convoMetaMap.put(otherUid, meta);
                    }

                    mergeConversationMetaIntoUsers();
                    applySearch(currentQuery);
                    refreshUnreadBadge();
                    updateEmptyState();
                });
    }

    private void mergeConversationMetaIntoUsers() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

        for (TeacherMessageModel user : allUsers) {
            ConversationMeta meta = convoMetaMap.get(user.getUserId());

            if (meta != null) {
                String lm = meta.lastMessage;
                if (TextUtils.isEmpty(lm)) lm = "Tap to start a conversation";
                user.setLastMessage(lm);

                String timeStr = "";
                if (meta.lastTime != null) timeStr = sdf.format(meta.lastTime.toDate());
                user.setTime(timeStr);

                user.setUnreadCount(meta.unreadCount);
            } else {
                if (TextUtils.isEmpty(user.getLastMessage())) user.setLastMessage("Tap to start a conversation");
                if (user.getTime() == null) user.setTime("");
                user.setUnreadCount(0);
            }
        }
    }

    private void setupSearch() {
        etSearchMessages.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString();
                applySearch(currentQuery);
            }
        });
    }

    private void applySearch(String query) {
        shownUsers.clear();

        boolean hasQuery = query != null && !query.trim().isEmpty();
        String q = hasQuery ? query.toLowerCase(Locale.getDefault()).trim() : "";

        for (TeacherMessageModel user : allUsers) {

            // Only show users you already have a conversation with
            if (!convoMetaMap.containsKey(user.getUserId())) continue;

            if (!hasQuery) {
                shownUsers.add(user);
            } else {
                String name = user.getName() == null ? "" : user.getName().toLowerCase(Locale.getDefault());
                String last = user.getLastMessage() == null ? "" : user.getLastMessage().toLowerCase(Locale.getDefault());

                if (name.contains(q) || last.contains(q)) {
                    shownUsers.add(user);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void setupNewMessage() {
        btnNewMessage.setOnClickListener(v -> {
            if (getContext() == null) return;

            if (!usersLoaded) {
                Toast.makeText(getContext(), "Loading users...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (allUsers.isEmpty()) {
                Toast.makeText(getContext(), "No users found.", Toast.LENGTH_SHORT).show();
                return;
            }

            showNewMessageDialog();
        });
    }

    /**
     * UPDATED:
     * - Always show dropdown on focus/click
     * - Accepts exact typed name (even if user didn't tap suggestion)
     * - Still blocks sending if typed name doesn't match any user
     */
    private void showNewMessageDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_message, null, false);

        AutoCompleteTextView actvUserSearch = dialogView.findViewById(R.id.actvUserSearch);
        EditText etNewMessage = dialogView.findViewById(R.id.etNewMessage);
        TextView btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        TextView btnSend = dialogView.findViewById(R.id.btnDialogSend);

        final List<TeacherMessageModel> baseList = new ArrayList<>(allUsers);
        final List<TeacherMessageModel> filteredList = new ArrayList<>(baseList);

        final UserSearchAdapter suggestionAdapter = new UserSearchAdapter(requireContext(), filteredList);
        actvUserSearch.setAdapter(suggestionAdapter);
        actvUserSearch.setThreshold(1);

        final TeacherMessageModel[] selectedUser = new TeacherMessageModel[1];

        actvUserSearch.setOnItemClickListener((parent, view, position, id) -> {
            TeacherMessageModel picked = (TeacherMessageModel) parent.getItemAtPosition(position);
            selectedUser[0] = picked;
            actvUserSearch.setText(picked.getName(), false);
        });

        actvUserSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) actvUserSearch.showDropDown();
        });

        actvUserSearch.setOnClickListener(v -> actvUserSearch.showDropDown());

        actvUserSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedUser[0] = null;

                String q = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());

                filteredList.clear();

                if (q.isEmpty()) {
                    filteredList.addAll(baseList);
                } else {
                    for (TeacherMessageModel u : baseList) {
                        String name = u.getName() == null ? "" : u.getName().toLowerCase(Locale.getDefault());
                        if (name.contains(q)) filteredList.add(u);
                    }
                }

                suggestionAdapter.notifyDataSetChanged();

                if (!filteredList.isEmpty()) {
                    actvUserSearch.showDropDown();
                }
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            String typedName = actvUserSearch.getText().toString().trim();
            String msg = etNewMessage.getText().toString().trim();

            if (TextUtils.isEmpty(typedName)) {
                actvUserSearch.setError("Select a user");
                return;
            }

            if (TextUtils.isEmpty(msg)) {
                etNewMessage.setError("Type a message");
                return;
            }

            TeacherMessageModel picked = selectedUser[0];

            // If user didn't click suggestion, try exact match by name
            if (picked == null) {
                for (TeacherMessageModel u : baseList) {
                    if (u.getName() != null && u.getName().equalsIgnoreCase(typedName)) {
                        picked = u;
                        break;
                    }
                }
            }

            if (picked == null) {
                actvUserSearch.setError("Select a user from the list");
                return;
            }

            dialog.dismiss();
            openChatScreen(picked.getUserId(), picked.getName(), msg);
        });

        dialog.show();
    }

    private void openChatScreen(String otherUid, String otherName, @Nullable String firstMessage) {
        if (getActivity() instanceof ParentsMainActivity) {
            ((ParentsMainActivity) requireActivity()).hideBottomNav();
        }

        Fragment chatFragment = Chat_ParentFragment.newInstance(otherUid, otherName, firstMessage);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null)
                .commit();
    }

    private void refreshUnreadBadge() {
        int totalUnread = 0;
        for (TeacherMessageModel user : allUsers) {
            totalUnread += user.getUnreadCount();
        }
        if (getActivity() instanceof ParentsMainActivity) {
            ((ParentsMainActivity) getActivity()).updateMessagesBadge(totalUnread);
        }
    }

    private void updateEmptyState() {
        if (tvEmptyMessages == null) return;

        if (shownUsers.isEmpty()) {
            tvEmptyMessages.setVisibility(View.VISIBLE);
            rvTeacherMessages.setVisibility(View.GONE);
        } else {
            tvEmptyMessages.setVisibility(View.GONE);
            rvTeacherMessages.setVisibility(View.VISIBLE);
        }
    }
}
