package com.kangwon.macaronproject.notice_board;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.BaseBundle;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kangwon.macaronproject.databinding.ActivityNewPostBinding;
import com.kangwon.macaronproject.login.BaseActivity;
import com.kangwon.macaronproject.models.Post;
import com.kangwon.macaronproject.models.User;

import java.util.HashMap;
import java.util.Map;

public class NewPostActivity extends BaseActivity {

    private static final String TAG = "NewPostActivity";
    private static final String REQUIRED = "Required";

    private DatabaseReference mDatabase;

    private ActivityNewPostBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // [START initialize_database_ref]
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // [END initialize_database_ref]

        binding.fabSubmitPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPost();
            }
        });

    }

    private void submitPost() {
        final String title = binding.fieldTitle.getText().toString();
        final String body = binding.fieldBody.getText().toString();

        // Title is required
        if(TextUtils.isEmpty(title)){
            binding.fieldTitle.setError(REQUIRED);
            return;
        }

        // Body is required
        if(TextUtils.isEmpty(body)){
            binding.fieldBody.setError(REQUIRED);
            return;
        }

        // Disable button so there are no multi-posts
        setEditingEnable(false);
        Toast.makeText(this, "Posting...", Toast.LENGTH_SHORT).show();

        // [START single_value_read]
        final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get user value
                User user = snapshot.getValue(User.class);

                // [START_EXCLUDE]
                if(user == null){
                    // User is null, error out
                    Log.e(TAG, "User " + userId + " is unexpectedly null");
                    Toast.makeText(NewPostActivity.this, "ERR: could not fetch user", Toast.LENGTH_SHORT).show();
                } else {
                    // Write new post
                    writeNewPost(userId, user.username, title, body);
                }

                // Finish this Activity, back to the stream
                setEditingEnable(true);
                finish();
                // [END_EXCLUDE]
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "getUser:onCancelled", error.toException());
                // [START_EXCLUDE]
                setEditingEnable(true);
                // [END_EXCLUDE]
            }
        });
        // [END single_value_read]
    }

    private void setEditingEnable(boolean enabled) {
        binding.fieldTitle.setEnabled(enabled);
        binding.fieldBody.setEnabled(enabled);
        if (enabled) {
            binding.fabSubmitPost.show();
        } else {
            binding.fabSubmitPost.hide();
        }
    }

    // [START write_fan_out]
    private void writeNewPost(String userId, String username, String title, String body) {
        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        String key = mDatabase.child("posts").push().getKey();
        Post post = new Post(userId, username, title, body);
        Map<String, Object> postValues = post.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/" + key, postValues);
        childUpdates.put("/user-posts/" + userId + "/" + key, postValues);

        mDatabase.updateChildren(childUpdates);
    }
    // [END write_fan_out]
}