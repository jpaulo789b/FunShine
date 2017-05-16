package jpaulo789b.com.github.funshine.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import jpaulo789b.com.github.funshine.R;
import jpaulo789b.com.github.funshine.model.DadosDiasClima;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener{

    final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast";
    final String URL_CORD = "/?lat="; //"-16.642&lon=-49.402";
    final String URL_API_KEY = "&lang=pt&appid=cc80b837f2b0dfdfc88bc907eb018556&units=metric";
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<DadosDiasClima> mListaDiasClima;
    final int GPSPERMISSION = 2727;
    private AlertDialog.Builder mDialog;

    // UI itens
    private TextView mtexTextViewStatus;
    private TextView mtexTextViewCidade;
    private TextView mtexTextViewTemp;
    private TextView mtexTextViewTemp_max;
    private TextView mtexTextViewData;
    private ImageView mImageViewWeatherSymbol;
    private ImageView mImageViewWeatherSymbolmini;
    private RecyclerView recyclerView;
    private WeatherAdapter mAdapterDiasReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mtexTextViewStatus = (TextView)findViewById(R.id.textViewStatus);
        mtexTextViewCidade = (TextView)findViewById(R.id.textViewCidadePais);
        mtexTextViewTemp = (TextView)findViewById(R.id.textViewTemperaturaAtual);
        mtexTextViewTemp_max = (TextView)findViewById(R.id.textViewTemperaturaMaxima);
        mtexTextViewData = (TextView)findViewById(R.id.textViewData);
        mImageViewWeatherSymbol = (ImageView)findViewById(R.id.imageViewWeatherSymbol);
        mImageViewWeatherSymbolmini = (ImageView)findViewById(R.id.imageViewWeatherSymbolmini);

        setContentView(R.layout.activity_weather);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .enableAutoManage(this,this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build();
        mListaDiasClima = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.containerWeatherReport);
        mAdapterDiasReport = new WeatherAdapter(mListaDiasClima);
        recyclerView.setAdapter(mAdapterDiasReport);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(linearLayoutManager);
    }


    public void donwloadDataWeather(Location location){
        final String fullcords = URL_CORD + location.getLatitude() + "&lon="+location.getLongitude();
        final String url = URL_BASE + fullcords + URL_API_KEY;
        Log.v("URLAPI", url);
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try{
                    JSONObject cidade = response.getJSONObject("city");
                    JSONArray listaDias = response.getJSONArray("list");

                    // Pega dados dos dias
                    for(int i = 0; i < 5; i++){
                        DadosDiasClima mDadosDiasClima = new DadosDiasClima();
                        mDadosDiasClima.setCidade(cidade.getString("name"));
                        mDadosDiasClima.setPais(cidade.getString("country"));
                        JSONObject obj = listaDias.getJSONObject(i);
                        JSONObject main = obj.getJSONObject("main");
                        JSONObject weather = obj.getJSONArray("weather").getJSONObject(0);
                        mDadosDiasClima.setTemp(main.getDouble("temp"));
                        mDadosDiasClima.setTemp_min(main.getDouble("temp_min"));
                        mDadosDiasClima.setTemp_max(main.getDouble("temp_max"));
                        mDadosDiasClima.setStatus(weather.getString("description"));
                        mDadosDiasClima.setIcon(weather.getString("icon"));
                        //
                        mDadosDiasClima.setData(obj.getString("dt_txt"));
                        mListaDiasClima.add(mDadosDiasClima);
                        Log.v("mDadosDiasClima", mDadosDiasClima.toString());


                    }
                    updateUI();
                    mAdapterDiasReport.notifyDataSetChanged();
                }catch (JSONException je){
                    Log.v("ERRORJSON",je.toString());
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("FUN","error: " + error.toString());
            }
        });
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION},GPSPERMISSION);
            }else{
                startLocationServices();
            }
    }

    private void startLocationServices() {
        try {
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,req,this);
        }catch (SecurityException se){
            Log.v("DEU ERRO;",se.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
            donwloadDataWeather(location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case GPSPERMISSION : {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startLocationServices();
                }else {
                    mDialog = new AlertDialog.Builder(this);
                    mDialog.setMessage("Permissão de GPS não concedida")
                            .setTitle("Sem autorização");
                    AlertDialog dialog = mDialog.create();
                    dialog.show();
                }
            }
        }
    }



    public void updateUI(){
        mImageViewWeatherSymbol = (ImageView)findViewById(R.id.imageViewWeatherSymbol);
        mImageViewWeatherSymbolmini = (ImageView)findViewById(R.id.imageViewWeatherSymbolmini);
        mtexTextViewStatus = (TextView)findViewById(R.id.textViewStatus);
        mtexTextViewCidade = (TextView)findViewById(R.id.textViewCidadePais);
        mtexTextViewTemp = (TextView)findViewById(R.id.textViewTemperaturaAtual);
        mtexTextViewTemp_max = (TextView)findViewById(R.id.textViewTemperaturaMaxima);
        mtexTextViewData = (TextView)findViewById(R.id.textViewData);

        if(mListaDiasClima.size() > 0){
            DadosDiasClima dadosClima = mListaDiasClima.get(0);

            switch (dadosClima.getStatus()){
                default:{
                    mImageViewWeatherSymbol.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    mImageViewWeatherSymbolmini.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
                    break;
                }
                case DadosDiasClima.WEATHER_TYPE_CLEAR :{
                    mImageViewWeatherSymbol.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    mImageViewWeatherSymbolmini.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
                    break;
                }
                case DadosDiasClima.WEATHER_TYPE_RAIN :{
                    mImageViewWeatherSymbol.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    mImageViewWeatherSymbolmini.setImageDrawable(getResources().getDrawable(R.drawable.rainy_mini));
                    break;
                }
                case DadosDiasClima.WEATHER_TYPE_WIND:{
                    mImageViewWeatherSymbol.setImageDrawable(getResources().getDrawable(R.drawable.partially_cloudy));
                    mImageViewWeatherSymbolmini.setImageDrawable(getResources().getDrawable(R.drawable.partially_cloudy_mini));
                    break;
                }
                case DadosDiasClima.WEATHER_TYPE_SNOW:{
                    mImageViewWeatherSymbol.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    mImageViewWeatherSymbolmini.setImageDrawable(getResources().getDrawable(R.drawable.snow_mini));
                    break;
                }
            }
            mtexTextViewTemp.setText(String.valueOf(dadosClima.getTemp()));
            mtexTextViewTemp_max.setText(String.valueOf(dadosClima.getTemp_max()));
            mtexTextViewCidade.setText(dadosClima.getCidade()+", "+dadosClima.getPais());
            mtexTextViewStatus.setText(dadosClima.getStatus());

        }
    }

    public class WeatherAdapter extends RecyclerView.Adapter<WeatherViewHolder>{

        private ArrayList<DadosDiasClima> mListaDias;

        public WeatherAdapter(ArrayList<DadosDiasClima> listaDias) {
            this.mListaDias = listaDias;
        }

        @Override
        public WeatherViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_dia_weather,parent,false);
            return new WeatherViewHolder(card);
        }

        @Override
        public void onBindViewHolder(WeatherViewHolder holder, int position) {
                DadosDiasClima dadosDiasClima = mListaDias.get(position);
                holder.updateUI(dadosDiasClima);
        }

        @Override
        public int getItemCount() {
            return mListaDias.size();
        }
    }

    public class WeatherViewHolder extends RecyclerView.ViewHolder{

        private TextView weatherDia;
        private TextView weatherStatus;
        private TextView weatherTemp;
        private TextView weatherTemp_max;
        private ImageView wearther_icon;

        public WeatherViewHolder(View itemView) {
            super(itemView);
            weatherDia = (TextView) itemView.findViewById(R.id.weatherDia);
            weatherStatus = (TextView) itemView.findViewById(R.id.weatherStatus);
            weatherTemp = (TextView) itemView.findViewById(R.id.weatherTemp);
            weatherTemp_max = (TextView) itemView.findViewById(R.id.weatherTemp_max);
            wearther_icon = (ImageView) itemView.findViewById(R.id.wearther_icon);
        }
        public void updateUI(DadosDiasClima report){

            weatherDia.setText(report.getData().toString());
            weatherStatus.setText(report.getStatus().toString());
            weatherTemp.setText(String.valueOf(report.getTemp()));
            weatherTemp_max.setText(String.valueOf(report.getTemp_max()));

            switch (report.getStatus()){
                default:{
                    wearther_icon.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
                    break;
                }
                case DadosDiasClima.WEATHER_TYPE_CLEAR :{
                    wearther_icon.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
                    break;
                }
                case DadosDiasClima.WEATHER_TYPE_RAIN :{
                    wearther_icon.setImageDrawable(getResources().getDrawable(R.drawable.rainy_mini));
                    break;
                }
                case DadosDiasClima.WEATHER_TYPE_WIND:{
                    wearther_icon.setImageDrawable(getResources().getDrawable(R.drawable.partially_cloudy_mini));
                    break;
                }
                case DadosDiasClima.WEATHER_TYPE_SNOW:{
                    wearther_icon.setImageDrawable(getResources().getDrawable(R.drawable.snow_mini));
                    break;
                }
            }

        }
    }
}
