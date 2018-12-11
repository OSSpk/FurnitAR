package com.razi.furnitar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static GoogleApiClient mGoogleApiClient;
    private static Context c;
    @BindView(R.id.toolbar_main)
    public Toolbar toolBar;
    FirebaseAuth gAuth;
    Button ar, nonAR, searchButton;
    Drawable d;
    boolean showAR;
    String searchQuery;
    TextView searchbar;
    boolean cancel;
    FirebaseAuth.AuthStateListener aL;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private RecyclerViewAdapter adapter, ARadapter, NonARadapter, searchAdapter;
    private FirebaseAnalytics mFirebaseAnalytics;

    public static void signOut() {
        FirebaseAuth.getInstance().signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                status -> c.startActivity(new Intent(c, Login.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        gAuth.addAuthStateListener(aL);
        mGoogleApiClient.connect();
        adapter.startListening();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        toolBar.setTitle("");
        setSupportActionBar(toolBar);

        DrawerUtil.getDrawer(this, toolBar);
        gAuth = FirebaseAuth.getInstance();
        c = this;


        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        ar = findViewById(R.id.ar_filter);
        showAR = true;
        searchbar = (TextView) findViewById(R.id.searchbar);
        searchButton = (Button) findViewById(R.id.search_button);
        cancel = false;
        d = ar.getBackground();
        Query query;
        query = db.collection("items").whereGreaterThan("quantity", 0);
        FirestoreRecyclerOptions<Item> options;
        options = new FirestoreRecyclerOptions.Builder<Item>()
                .setQuery(query, Item.class)
                .build();
        adapter = new RecyclerViewAdapter(options);

        query = db.collection("items").whereGreaterThan("quantity", 0);

        options = new FirestoreRecyclerOptions.Builder<Item>()
                .setQuery(query, Item.class)
                .build();
        ARadapter = new RecyclerViewAdapter(options);

        query = db.collection("items")
                .whereEqualTo("isAR", false)
                .whereGreaterThan("quantity", 0);

        options = new FirestoreRecyclerOptions.Builder<Item>()
                .setQuery(query, Item.class)
                .build();
        NonARadapter = new RecyclerViewAdapter(options);





        initRecyclerView();
        aL = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(MainActivity.this, Login.class));
            }
        };
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, connectionResult -> Log.i("OK", "NOT OK"))
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
    }

    private void initRecyclerView() {

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DocumentSnapshot documentSnapshot, int pos) {
                Bundle bundle = new Bundle();
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                String path = documentSnapshot.getReference().getPath();
                String id = documentSnapshot.getId();
                String name = documentSnapshot.get("name").toString();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
                Intent intent = new Intent(getApplicationContext(), Item_Detail.class);
                intent.putExtra("path", path);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    public void seacrhItem(View view) {
        adapter.startListening();
        if(!cancel){
            searchQuery = searchbar.getText().toString();
            ArrayList<String> arr = new ArrayList<>(Arrays.asList(searchQuery.split(" ")));
            searchQuery = "";
            for(String s : arr){
                searchQuery += (" " + s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase());
            }
            searchQuery = searchQuery.substring(1);
            String s = searchQuery.substring(0,searchQuery.length() - 2);
            char c = searchQuery.charAt(searchQuery.length() - 1);
            c++;
            s += c;
            Query query = db.collection("items")
                    .whereEqualTo("name", searchQuery)
                    .whereGreaterThan("quantity", 0);

            FirestoreRecyclerOptions<Item> options = new FirestoreRecyclerOptions.Builder<Item>()
                    .setQuery(query, Item.class)
                    .build();
            searchAdapter = new RecyclerViewAdapter(options);
            adapter = searchAdapter;
            searchButton.setBackground(getBaseContext().getDrawable(R.drawable.ic_cancel_black_24dp));
        }
        else{
            searchbar.setText("");
            adapter = ARadapter;
            searchButton.setBackground(getBaseContext().getDrawable(R.drawable.ic_search_black_24dp));
        }
        adapter.stopListening();
        cancel = !cancel;
    }


    public void showAR(View view) {
        showAR = !showAR;
        adapter.stopListening();
        if(showAR){
            adapter = ARadapter;
        }
        else{
            adapter = NonARadapter;
        }
        adapter.startListening();
    }
}
