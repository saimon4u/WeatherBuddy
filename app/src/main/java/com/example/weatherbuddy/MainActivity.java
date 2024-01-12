package com.example.weatherbuddy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private SearchView searchView;
    private TextView temperature, cityName, feelsLike, condition, day, date, humidity, windSpeed, boxCondition, sunrise, sunset, seaLevel, today;
    private LottieAnimationView lottieAnimationView;
    private ConstraintLayout mainLayout;
    private String url = "https://api.openweathermap.org/data/2.5/weather?q=cityName&appid=80e95bd6fd32761bc9733ddcb06837fd";
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private ProgressDialog progressDialog;
    private final DecimalFormat dF = new DecimalFormat("0.00");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchView = findViewById(R.id.search_view);
        temperature = findViewById(R.id.temperature);
        cityName = findViewById(R.id.city_name);
        feelsLike = findViewById(R.id.feels_like);
        condition = findViewById(R.id.condition);
        day = findViewById(R.id.day);
        date = findViewById(R.id.date);
        humidity = findViewById(R.id.humidity);
        windSpeed = findViewById(R.id.wind_speed);
        boxCondition = findViewById(R.id.box_condition);
        sunrise = findViewById(R.id.sunrise);
        sunset = findViewById(R.id.sunset);
        seaLevel = findViewById(R.id.sea_level);
        today = findViewById(R.id.today);
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        mainLayout = findViewById(R.id.main_layout);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait....");
        progressDialog.setCancelable(false);


        requestLocationPermission();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getWeatherUpdate(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
    }

    private void requestLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle("Location Permission Required")
                    .setMessage("This app requires access to your location to provide accurate weather information.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setCancelable(false)
                    .show();
        }else{
            requestLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                requestLocationPermission();
            }
        }
    }

    private void requestLocationUpdates() {
        if (locationManager != null){
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0,
                        0,
                        locationListener
                );
            } else {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            String cityName = getCityName(location.getLatitude(), location.getLongitude());
            getWeatherUpdate(cityName);
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private String getCityName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List <Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                return address.getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getWeatherUpdate(String name) {
        String mainUrl = url.replace("cityName", name);
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, mainUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            progressDialog.show();
                            String country = "Bangladesh";
                            JSONObject mainObject = new JSONObject(response);
                            cityName.setText(mainObject.getString("name"));
                            long timestamp = mainObject.getLong("dt");
                            date.setText(dateGenerator(timestamp, "dd MMMM yyyy"));
                            day.setText(dateGenerator(timestamp, "EEEE"));




                            JSONObject coordinateObject = mainObject.getJSONObject("coord");
                            double latitude = coordinateObject.getDouble("lat");
                            double longitude = coordinateObject.getDouble("lon");
                            Geocoder geocoder = new Geocoder(MainActivity.this,Locale.getDefault());
                            List <Address> addresses = geocoder.getFromLocation(latitude,longitude,1);
                            if(addresses != null && addresses.size() > 0){
                                Address address = addresses.get(0);
                                if(!address.getCountryName().equals("")){
                                    country = address.getCountryName();
                                }
                            }


                            JSONObject tempObject = mainObject.getJSONObject("main");
                            double temp = tempObject.getDouble("temp");
                            temp -= 273.15;
                            temperature.setText(dF.format(temp) + " °C");
                            double feels = tempObject.getDouble("feels_like");
                            feels -= 273.15;
                            feelsLike.setText("Feels Like: " + dF.format(feels) + " °C");

                            double humidity_val = tempObject.getDouble("humidity");
                            humidity.setText(String.valueOf(humidity_val) + " %");

                            int pressure = tempObject.getInt("pressure");
                            seaLevel.setText(String.valueOf(pressure) + " hPa");


                            JSONObject sunObject = mainObject.getJSONObject("sys");
                            long timestamp1 = sunObject.getLong("sunrise");
                            long timestamp2 = sunObject.getLong("sunset");
                            sunrise.setText(dateGenerator(timestamp1, "hh:mm a"));
                            sunset.setText(dateGenerator(timestamp2, "hh:mm a"));
                            cityName.setText(cityName.getText() + "," + country);

                            JSONObject windObject = mainObject.getJSONObject("wind");
                            double speed = windObject.getDouble("speed");
                            windSpeed.setText(String.valueOf(speed) + " m/s");


                            JSONArray weatherArray = mainObject.getJSONArray("weather");
                            JSONObject weatherObject = (JSONObject) weatherArray.get(0);
                            String presentCondition = weatherObject.getString("main");
                            boxCondition.setText(presentCondition);
                            condition.setText(presentCondition);
                            switch (presentCondition) {
                                case "Foggy":
                                case "Mist":
                                case "Overcast":
                                case "Partly Clouds":
                                case "Clouds":
                                    loadNewAnimation(R.raw.cloud);
                                    mainLayout.setBackground(getResources().getDrawable(R.drawable.cloudy_weather));
                                    setTextColor(false);
                                    break;
                                case "Light Snow":
                                case "Moderate Snow":
                                case "Heave Snow":
                                case "Blizzard":
                                case "Snow":
                                    loadNewAnimation(R.raw.snow);
                                    mainLayout.setBackground(getResources().getDrawable(R.drawable.snow_weather));
                                    setTextColor(true);
                                    break;
                                case "Light Rain":
                                case "Moderate Rain":
                                case "Heave Rain":
                                case "Drizzle":
                                case "Showers":
                                case "Rain":
                                    loadNewAnimation(R.raw.rain);
                                    mainLayout.setBackground(getResources().getDrawable(R.drawable.rainy_weather));
                                    setTextColor(true);
                                    break;
                                case "Haze":
                                    loadNewAnimation(R.raw.haze);
                                    mainLayout.setBackground(getResources().getDrawable(R.drawable.cloudy_weather));
                                    setTextColor(false);
                                    break;
                                default:
                                    loadNewAnimation(R.raw.sun);
                                    mainLayout.setBackground(getResources().getDrawable(R.drawable.sunny_weather));
                                    setTextColor(true);
                                    break;
                            }
                            searchView.setQuery("", false);
                            searchView.clearFocus();
                            progressDialog.cancel();
                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "Please Enter a Valid City Name...", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.d("error", e.getLocalizedMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Opps you misspelled the city name....", Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(stringRequest);
    }

    private String dateGenerator(long timestamp, String type) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(type, Locale.getDefault());
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

    private void loadNewAnimation(int id) {
        lottieAnimationView.cancelAnimation();
        lottieAnimationView.clearAnimation();
        lottieAnimationView.setAnimation(id);
        lottieAnimationView.playAnimation();
    }
    private void setTextColor(boolean white){
        if(white) {
            cityName.setTextColor(Color.parseColor("#FFFFFF"));
            today.setTextColor(Color.parseColor("#FFFFFF"));
            temperature.setTextColor(Color.parseColor("#FFFFFF"));
            condition.setTextColor(Color.parseColor("#FFFFFF"));
            feelsLike.setTextColor(Color.parseColor("#FFFFFF"));
            day.setTextColor(Color.parseColor("#FFFFFF"));
            date.setTextColor(Color.parseColor("#FFFFFF"));
        }
        else{
            cityName.setTextColor(Color.parseColor("#000000"));
            today.setTextColor(Color.parseColor("#000000"));
            temperature.setTextColor(Color.parseColor("#000000"));
            condition.setTextColor(Color.parseColor("#000000"));
            feelsLike.setTextColor(Color.parseColor("#000000"));
            day.setTextColor(Color.parseColor("#000000"));
            date.setTextColor(Color.parseColor("#000000"));
        }
    }
}