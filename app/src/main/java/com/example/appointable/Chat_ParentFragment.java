package com.example.appointable;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class Chat_ParentFragment extends Fragment {

    private static final String ARG_OTHER_UID = "arg_other_uid";
    private static final String ARG_OTHER_NAME = "arg_other_name";
    private static final String ARG_FIRST_MESSAGE = "arg_first_message";

    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private ImageView btnSendMessage;
    private ChatMessagesAdapter adapter;
    private final List<ChatMessageModel> messages = new ArrayList<>();

    private String currentUid;
    private String otherUid;
    private String otherName;
    private String firstMessage;

    private FirebaseFirestore db;
    private CollectionReference messagesRef;
    private ListenerRegistration messagesListener;
    private ListenerRegistration convoListener;
    private String conversationId;

    private boolean messagesSeenByOther = false;

    private String currentUserImageUrl;
    private String otherUserImageUrl;

    private float startX, startY;

    public static Chat_ParentFragment newInstance(String otherUid,
                                                  String otherName,
                                                  @Nullable String firstMessage) {
        Chat_ParentFragment fragment = new Chat_ParentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OTHER_UID, otherUid);
        args.putString(ARG_OTHER_NAME, otherName);
        args.putString(ARG_FIRST_MESSAGE, firstMessage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUid = user.getUid();
        }

        if (getArguments() != null) {
            otherUid = getArguments().getString(ARG_OTHER_UID, "");
            otherName = getArguments().getString(ARG_OTHER_NAME, "");
            firstMessage = getArguments().getString(ARG_FIRST_MESSAGE);
        }

        db = FirebaseFirestore.getInstance();

        if (!TextUtils.isEmpty(currentUid) && !TextUtils.isEmpty(otherUid)) {
            conversationId = buildConversationId(currentUid, otherUid);
            messagesRef = db.collection("conversations")
                    .document(conversationId)
                    .collection("messages");
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeChat();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private String buildConversationId(String uid1, String uid2) {
        if (uid1 == null || uid2 == null) return "";
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_chat_parent_fragment, container, false);

        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etMessageInput = view.findViewById(R.id.etMessageInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        TextView tvChatName = view.findViewById(R.id.tvChatName);
        ImageView btnBackChat = view.findViewById(R.id.btnBackChat);

        tvChatName.setText(otherName);

        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(lm);

        adapter = new ChatMessagesAdapter(messages);
        rvChatMessages.setAdapter(adapter);

        // hide bottom nav while in chat
        if (getActivity() instanceof ParentsMainActivity) {
            ((ParentsMainActivity) getActivity()).hideBottomNav();
        }

        if (!TextUtils.isEmpty(conversationId)) {
            fetchProfileImages();
            listenForMessages();
            listenForConversationMeta();
            markConversationRead();
        }

        btnSendMessage.setOnClickListener(v -> {
            String text = etMessageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendMessage(text);
                etMessageInput.setText("");
            }
        });

        btnBackChat.setOnClickListener(v -> closeChat());

        view.setOnTouchListener(this::handleSwipe);

        if (!TextUtils.isEmpty(firstMessage)) {
            sendMessage(firstMessage);
            firstMessage = null;
        }

        return view;
    }

    private boolean handleSwipe(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float endX = event.getX();
                float endY = event.getY();
                float dx = endX - startX;
                float dy = Math.abs(endY - startY);

                if (startX < 80 && dx > 150 && dy < 100) {
                    closeChat();
                    return true;
                }
                break;
        }
        return false;
    }

    private void fetchProfileImages() {
        if (currentUid == null || otherUid == null) return;

        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserImageUrl = doc.getString("profileImageUrl");
                        adapter.setCurrentUserImageUrl(currentUserImageUrl);
                    }
                });

        db.collection("users")
                .document(otherUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        otherUserImageUrl = doc.getString("profileImageUrl");
                        adapter.setOtherUserImageUrl(otherUserImageUrl);
                    }
                });
    }

    private void listenForMessages() {
        if (messagesRef == null) return;

        messagesListener = messagesRef
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    messages.clear();

                    SimpleDateFormat timeFormat =
                            new SimpleDateFormat("h:mm a", Locale.getDefault());
                    SimpleDateFormat keyFormat =
                            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat headerFormat =
                            new SimpleDateFormat("MMM d", Locale.getDefault());

                    Calendar today = Calendar.getInstance();
                    Calendar yesterday = Calendar.getInstance();
                    yesterday.add(Calendar.DAY_OF_YEAR, -1);

                    String lastDateKey = null;

                    for (QueryDocumentSnapshot doc : value) {
                        String senderId = doc.getString("senderId");
                        String text = doc.getString("text");
                        Timestamp ts = doc.getTimestamp("timestamp");

                        if (ts == null) continue;

                        boolean isSender = senderId != null && senderId.equals(currentUid);
                        boolean isSeen = isSender && messagesSeenByOther;

                        String timeStr = timeFormat.format(ts.toDate());
                        String currentKey = keyFormat.format(ts.toDate());

                        if (!currentKey.equals(lastDateKey)) {
                            Calendar msgCal = Calendar.getInstance();
                            msgCal.setTime(ts.toDate());

                            String headerLabel;
                            if (isSameDay(msgCal, today)) {
                                headerLabel = "Today";
                            } else if (isSameDay(msgCal, yesterday)) {
                                headerLabel = "Yesterday";
                            } else {
                                headerLabel = headerFormat.format(ts.toDate());
                            }

                            messages.add(new ChatMessageModel(headerLabel));
                            lastDateKey = currentKey;
                        }

                        messages.add(new ChatMessageModel(text, timeStr, isSender, isSeen));
                    }

                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        rvChatMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void listenForConversationMeta() {
        if (TextUtils.isEmpty(conversationId)) return;

        convoListener = db.collection("conversations")
                .document(conversationId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;

                    Object unreadObj = doc.get("unread." + otherUid);
                    long unreadForOther = 0;
                    if (unreadObj instanceof Number) {
                        unreadForOther = ((Number) unreadObj).longValue();
                    }

                    boolean newSeen = (unreadForOther == 0);
                    if (newSeen != messagesSeenByOther) {
                        messagesSeenByOther = newSeen;

                        for (ChatMessageModel m : messages) {
                            if (!m.isDateHeader() && m.isSender()) {
                                m.setSeen(messagesSeenByOther);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void sendMessage(String text) {
        if (currentUid == null || otherUid == null ||
                currentUid.isEmpty() || otherUid.isEmpty() ||
                messagesRef == null) {
            return;
        }

        Timestamp now = Timestamp.now();

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUid);
        msg.put("receiverId", otherUid);
        msg.put("text", text);
        msg.put("timestamp", now);

        messagesRef.add(msg);

        Map<String, Object> convoUpdate = new HashMap<>();
        convoUpdate.put("members", Arrays.asList(currentUid, otherUid));
        convoUpdate.put("lastMessage", text);
        convoUpdate.put("lastMessageTime", now);
        convoUpdate.put("lastMessageSender", currentUid);
        convoUpdate.put("unread." + currentUid, 0);
        convoUpdate.put("unread." + otherUid, FieldValue.increment(1));

        db.collection("conversations")
                .document(conversationId)
                .set(convoUpdate, SetOptions.merge());
    }

    private void markConversationRead() {
        if (currentUid == null || TextUtils.isEmpty(conversationId)) return;

        Map<String, Object> update = new HashMap<>();
        update.put("unread." + currentUid, 0);

        db.collection("conversations")
                .document(conversationId)
                .set(update, SetOptions.merge());
    }

    private void closeChat() {
        if (getActivity() instanceof ParentsMainActivity) {
            ((ParentsMainActivity) requireActivity()).showBottomNav();
        }

        hideKeyboard();

        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (convoListener != null) {
            convoListener.remove();
            convoListener = null;
        }

        if (isAdded()) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    )
                    .remove(this)
                    .commit();

            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void hideKeyboard() {
        if (getActivity() == null || getView() == null) return;
        InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (convoListener != null) {
            convoListener.remove();
            convoListener = null;
        }

        if (getActivity() instanceof ParentsMainActivity) {
            ((ParentsMainActivity) getActivity()).showBottomNav();
        }
    }
}
