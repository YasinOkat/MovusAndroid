package com.example.sql2;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ArabaListe extends AppCompatActivity {

    private static final String URL_GET_KULLANILAN_ARABALAR = "http://192.168.1.193:5000/getKullanilanArabalar";
    private ArabaAdapter adapter;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.araba_listesi);

        compositeDisposable = new CompositeDisposable();
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArabaAdapter();
        recyclerView.setAdapter(adapter);

        fetchArabalar();
    }

    private void fetchArabalar() {
        Disposable disposable = Observable.fromCallable(this::getArabalarFromJsonArray)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateArabalar, Throwable::printStackTrace);
        compositeDisposable.add(disposable);
    }

    private List<Araba> getArabalarFromJsonArray() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(URL_GET_KULLANILAN_ARABALAR).build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String json = response.body().string();
            JSONArray jsonArray = new JSONArray(json);

            List<Araba> arabalar = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String plaka = jsonObject.getString("kullanilan_plaka").trim();
                String kullaniciAdi = jsonObject.getString("kullanici_adi").trim();
                String gidilenYer = jsonObject.getString("gidilen_yer").trim();
                String cikisSaatiString = jsonObject.getString("cikis_saati").trim();
                String cikisSaati_eksi = cikisSaatiString.substring(5, 22).trim();

                arabalar.add(new Araba(plaka, kullaniciAdi, gidilenYer, cikisSaati_eksi));
            }

            return arabalar;
        }
    }

    private void updateArabalar(List<Araba> arabalar) {
        if (adapter.getItemCount() == 0) {
            adapter.setArabalar(arabalar);
        } else {
            int startPosition = adapter.getItemCount();
            adapter.addArabalar(arabalar);
            adapter.notifyItemRangeInserted(startPosition, arabalar.size());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }

    private static class ArabaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;

        private List<Araba> arabalar;

        public void setArabalar(List<Araba> arabalar) {
            this.arabalar = arabalar;
            notifyDataSetChanged();
        }

        public void addArabalar(List<Araba> arabalar) {
            if (this.arabalar == null) {
                this.arabalar = new ArrayList<>();
            }
            this.arabalar.addAll(arabalar);
        }

        @androidx.annotation.NonNull
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_HEADER) {
                View headerView = inflater.inflate(R.layout.araba_list_header, parent, false);
                return new HeaderViewHolder(headerView);
            } else {
                View itemView = inflater.inflate(R.layout.araba_list_item, parent, false);
                return new ArabaViewHolder(itemView);
            }
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull @NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ArabaViewHolder) {
                ArabaViewHolder itemViewHolder = (ArabaViewHolder) holder;
                Araba araba = arabalar.get(position - 1);
                itemViewHolder.bind(araba);

                int backgroundColor = (position % 2 == 0) ? Color.WHITE : Color.LTGRAY;
                itemViewHolder.itemView.setBackgroundColor(backgroundColor);
            }
        }

        @Override
        public int getItemCount() {
            return arabalar != null ? arabalar.size() + 1 : 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        private static class HeaderViewHolder extends RecyclerView.ViewHolder {

            public HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        private static class ArabaViewHolder extends RecyclerView.ViewHolder {
            TextView txtPlaka, txtKullaniciAdi, txtGidilenYer, txtCikisSaati;

            public ArabaViewHolder(@NonNull View itemView) {
                super(itemView);
                txtPlaka = itemView.findViewById(R.id.txtPlaka);
                txtKullaniciAdi = itemView.findViewById(R.id.txtKullaniciAdi);
                txtGidilenYer = itemView.findViewById(R.id.txtGidilenYer);
                txtCikisSaati = itemView.findViewById(R.id.txtCikisSaati);
            }

            public void bind(Araba araba) {
                txtPlaka.setText(araba.getPlaka());
                txtKullaniciAdi.setText(araba.getKullaniciAdi());
                txtGidilenYer.setText(araba.getGidilenYer());
                txtCikisSaati.setText(araba.getCikisSaati());
            }
        }
    }

    public static class Araba {
        private final String plaka;
        private final String kullaniciAdi;
        private final String gidilenYer;
        private final String cikisSaati;

        public Araba(String plaka, String kullaniciAdi, String gidilenYer, String cikisSaati) {
            this.plaka = plaka;
            this.kullaniciAdi = kullaniciAdi;
            this.gidilenYer = gidilenYer;
            this.cikisSaati = cikisSaati;
        }

        public String getPlaka() {
            return plaka;
        }

        public String getKullaniciAdi() {
            return kullaniciAdi;
        }

        public String getGidilenYer() {
            return gidilenYer;
        }

        public String getCikisSaati() {
            return cikisSaati;
        }
    }
}





