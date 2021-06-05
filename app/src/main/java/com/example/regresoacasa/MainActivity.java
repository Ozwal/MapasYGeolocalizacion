package com.example.regresoacasa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    SupportMapFragment mapFragment;
    GoogleMap mapa;

    EditText lat, lng;
    Button calcular;

    LatLng coorCasa, coorAct;
    private Marker marcAct, marcCasa;
    RequestQueue requestQueue;
    FusedLocationProviderClient mFusedLocationClient;

    String latAct, lngAct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lat = (EditText) findViewById(R.id.txtLat);
        lng = (EditText) findViewById(R.id.txtLng);
        calcular = (Button) findViewById(R.id.btnCalcular);
        calcularRuta();

        requestQueue = Volley.newRequestQueue(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void calcularRuta() {
        calcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng home = new LatLng(Double.parseDouble(lat.getText().toString()), Double.parseDouble(lng.getText().toString()));
                coorCasa = home;
                mapa.clear();
                if (marcCasa != null) marcCasa.remove();
                marcCasa = mapa.addMarker(new MarkerOptions()
                        .position(coorAct).title("Ubicación casa"));
                marcAct = mapa.addMarker(new MarkerOptions()
                        .position(coorCasa).title("Ubicación actual"));
//                Toast.makeText(getApplication(), "CALCULANDO RUTA ...", Toast.LENGTH_SHORT).show();
                System.out.println(direccion(coorAct, coorCasa));
                peticion(direccion(coorAct, coorCasa));
            }
        });
    }

    public String direccion(LatLng ori, LatLng dest) {
        String outputFormat = "json";
        String origin = "origin=" + ori.latitude + "," + ori.longitude;
        String destination = "destination=" + dest.latitude + "," + dest.longitude;
        String parameters = origin + "&" + destination;
        String url = "https://maps.googleapis.com/maps/api/directions/" + outputFormat + "?" + parameters + "&key=AIzaSyBHtYD_i3eqYqdCroUTQDwzb5FtqD323oc";
        return url;
//&key=AIzaSyBHtYD_i3eqYqdCroUTQDwzb5FtqD323oc
    }

    List<String> LAT = new ArrayList<>();
    List<String> LNG = new ArrayList<>();

    public void peticion(String url) {
        JsonRequest jsonRequest = new JsonObjectRequest(
                url, null, response -> {
            try {
                JSONArray item = response.getJSONArray("routes");
                for (int i = 0; i < item.length(); i++) {
                    JSONArray legs = ((JSONObject) item.get(i)).getJSONArray("legs");
                    for (int j = 0; j < legs.length(); j++) {
                        JSONArray steps = ((JSONObject) legs.get(j)).getJSONArray("steps");
                        for (int k = 0; k < steps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) steps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);
                            Polyline polyline1 = mapa.addPolyline(new PolylineOptions()
                                    .clickable(true)
                                    .addAll(list)
                                    .width(5)
                                    .color(Color.RED));
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        },
                error -> {
                    Log.d("VOLL", "Error \n" + error.toString());
                });
        requestQueue.add(jsonRequest);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;

            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {

            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng  myUbication = new LatLng(location.getLatitude(), location.getLongitude());
                    coorAct = myUbication;
                    CameraUpdate cm = CameraUpdateFactory.newLatLngZoom(myUbication, 16);
                    mapa.addMarker(new MarkerOptions()
                            .position(myUbication)
                            .title("Ubicación actual"));
                    mapa.animateCamera(cm);
                    coorAct = myUbication;
                    latAct = "(Lat):"+coorAct.latitude;
                    lngAct = "(Lng):"+coorAct.longitude;
                    Toast.makeText(MainActivity.this,"Ubicando ...", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,"No se puede obtener localización", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}