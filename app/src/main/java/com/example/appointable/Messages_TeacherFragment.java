package com.example.appointable;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Messages_TeacherFragment extends Fragment {

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

    // lastMessage, time, unreadCount per otherUid
    private final Map<String, ConversationMeta> convoMetaMap = new HashMap<>();

    private ListenerRegistration conversationsListener;

    private String currentQuery = "";

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

        View view = inflater.inflate(R.layout.fragment_messages_teacher, container, false);

        rvTeacherMessages = view.findViewById(R.id.rvTeacherMessages);
        rvTeacherMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        tvEmptyMessages = view.findViewById(R.id.tvEmptyMessages);
        etSearchMessages = view.findViewById(R.id.etSearchMessages);
        btnNewMessage = view.findViewById(R.id.btnNewMessage);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUid = user.getUid();
        }

        adapter = new TeacherMessagesAdapter(shownUsers, model -> {
            openChatScreen(model.getUserId(), model.getName(), null);
        });
        rvTeacherMessages.setAdapter(adapter);

        loadUsersFromFirestore();

        if (currentUid != null && !currentUid.isEmpty()) {
            listenToConversations();
        }

        setupSearch();
        setupNewMessage();

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
        allUsers.clear();
        shownUsers.clear();
        adapter.notifyDataSetChanged();

        db.collection("users")
                .whereIn("status", Arrays.asList("Active", "Irregular"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        String uid = doc.getId();

                        // skip self
                        if (currentUid != null && currentUid.equals(uid)) {
                            continue;
                        }

                        String firstName = safe(doc.getString("firstName"));
                        String middleName = safe(doc.getString("middleName"));
                        String lastName = safe(doc.getString("lastName"));
                        String suffix = safe(doc.getString("suffix"));
                        String profileUrl = doc.getString("profileImageUrl");

                        String fullName = buildFullName(firstName, middleName, lastName, suffix);

                        TeacherMessageModel user = new TeacherMessageModel(
                                uid,
                                fullName,
                                "Tap to start a conversation",
                                "",
                                0,
                                profileUrl
                        );

                        allUsers.add(user);
                    }

                    mergeConversationMetaIntoUsers();
                    applySearch(currentQuery);
                    refreshUnreadBadge();
                    updateEmptyState();
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
        return sb.toString();
    }

    private void listenToConversations() {
        if (currentUid == null || currentUid.isEmpty()) return;

        if (conversationsListener != null) {
            conversationsListener.remove();
        }

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
                            if (!m.equals(currentUid)) {
                                otherUid = m;
                                break;
                            }
                        }
                        if (otherUid == null) continue;

                        String lastMessage = doc.getString("lastMessage");
                        Timestamp lastTime = doc.getTimestamp("lastMessageTime");

                        int unread = 0;
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            String targetKey = "unread." + currentUid;
                            Object val = data.get(targetKey);
                            if (val instanceof Number) {
                                unread = ((Number) val).intValue();
                            }
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
        SimpleDateFormat sdf =
                new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

        for (TeacherMessageModel user : allUsers) {
            ConversationMeta meta = convoMetaMap.get(user.getUserId());
            if (meta != null) {
                String lm = meta.lastMessage;
                if (lm == null || lm.isEmpty()) {
                    lm = "Tap to start a conversation";
                }
                user.setLastMessage(lm);

                String timeStr = "";
                if (meta.lastTime != null) {
                    timeStr = sdf.format(meta.lastTime.toDate());
                }
                user.setTime(timeStr);

                user.setUnreadCount(meta.unreadCount);
            } else {
                if (user.getLastMessage() == null || user.getLastMessage().isEmpty()) {
                    user.setLastMessage("Tap to start a conversation");
                }
                if (user.getTime() == null) {
                    user.setTime("");
                }
                user.setUnreadCount(0);
            }
        }
    }

    private void setupSearch() {
        etSearchMessages.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                applySearch(currentQuery);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // Only show users that have a conversation (convoMetaMap contains their uid)
    private void applySearch(String query) {
        shownUsers.clear();

        boolean hasQuery = query != null && !query.trim().isEmpty();
        String q = hasQuery ? query.toLowerCase().trim() : "";

        for (TeacherMessageModel user : allUsers) {
            if (!convoMetaMap.containsKey(user.getUserId())) {
                continue;
            }

            if (!hasQuery) {
                shownUsers.add(user);
            } else {
                if (user.getName().toLowerCase().contains(q)
                        || user.getLastMessage().toLowerCase().contains(q)) {
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

            View dialogView = LayoutInflater.from(getContext())
                    .inflate(R.layout.dialog_new_message, null, false);

            AutoCompleteTextView actvUserSearch =
                    dialogView.findViewById(R.id.actvUserSearch);
            EditText etNewMessage = dialogView.findViewById(R.id.etNewMessage);
            TextView btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
            TextView btnSend = dialogView.findViewById(R.id.btnDialogSend);

            UserSearchAdapter userAdapter =
                    new UserSearchAdapter(requireContext(), allUsers);
            actvUserSearch.setAdapter(userAdapter);
            actvUserSearch.setThreshold(1);

            final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            btnCancel.setOnClickListener(viewCancel -> dialog.dismiss());

            btnSend.setOnClickListener(viewSend -> {
                String name = actvUserSearch.getText().toString().trim();
                String msg = etNewMessage.getText().toString().trim();

                if (name.isEmpty() || msg.isEmpty()) {
                    return;
                }

                TeacherMessageModel target = null;
                for (TeacherMessageModel u : allUsers) {
                    if (u.getName().equalsIgnoreCase(name)) {
                        target = u;
                        break;
                    }
                }
                if (target == null) return;

                dialog.dismiss();
                openChatScreen(target.getUserId(), target.getName(), msg);
            });

            dialog.show();
        });
    }

    private void openChatScreen(String otherUid, String otherName, @Nullable String firstMessage) {
        ((TeachersMainActivity) requireActivity()).hideBottomNav();

        Fragment chatFragment = Chat_TeacherFragment.newInstance(otherUid, otherName, firstMessage);

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
        if (getActivity() instanceof TeachersMainActivity) {
            ((TeachersMainActivity) getActivity()).updateMessagesBadge(totalUnread);
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
