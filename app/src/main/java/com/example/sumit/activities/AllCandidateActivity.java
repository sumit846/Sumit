package com.example.sumit.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;

import com.example.sumit.R;
import com.example.sumit.adapter.CandidateAdapter;
import com.example.sumit.model.Candidate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AllCandidateActivity extends AppCompatActivity {

    private RecyclerView candidateRV;
    private Button startBtn;
    private List<Candidate> list;
    private CandidateAdapter adapter;

    private FirebaseFirestore firebaseFirestore;
    private int retryAttempts = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_candidate);

        candidateRV = findViewById(R.id.candidates_rv);
        startBtn = findViewById(R.id.start);
        firebaseFirestore = FirebaseFirestore.getInstance();

        list = new ArrayList<>();
        adapter = new CandidateAdapter(this, list);
        candidateRV.setLayoutManager(new LinearLayoutManager(this));
        candidateRV.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            performFirestoreOperation();
        }
    }

    private void performFirestoreOperation() {
        final int MAX_RETRY_ATTEMPTS = 3;

        firebaseFirestore.collection("Candidate")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot snapshot : Objects.requireNonNull(task.getResult())) {
                                list.add(new Candidate(
                                        snapshot.getString("name"),
                                        snapshot.getString("party"),
                                        snapshot.getString("post"),
                                        snapshot.getString("image"),
                                        snapshot.getString("id")
                                ));
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            // Retry operation if the client is offline and retryAttempts is less than MAX_RETRY_ATTEMPTS
                            if (isFirestoreOffline(task.getException()) && retryAttempts < MAX_RETRY_ATTEMPTS) {
                                retryAttempts++;
                                // Retry the operation after a short delay
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        performFirestoreOperation();
                                    }
                                }, 1000); // Retry after 1 second (adjust as needed)
                            } else {
                                Toast.makeText(AllCandidateActivity.this, "Candidate not found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private boolean isFirestoreOffline(Exception exception) {
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
            return firestoreException.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firebaseFirestore.collection("Users")
                .document(uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                        String finish = task.getResult().getString("finish");

                        if (finish != null) {
                            if (finish.equals("voted")) {
                                Toast.makeText(AllCandidateActivity.this, "Your vote is counted already", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(AllCandidateActivity.this, ResultActivity.class));
                                finish();
                            }
                        }
                    }
                });

    }
}
