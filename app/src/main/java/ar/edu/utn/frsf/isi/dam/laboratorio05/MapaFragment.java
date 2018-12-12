package ar.edu.utn.frsf.isi.dam.laboratorio05;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.MyDatabase;
import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.Reclamo;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapaFragment extends SupportMapFragment implements OnMapReadyCallback {
    private GoogleMap miMapa;
    private OnAbrirMapaListener listener;
    private List<Reclamo> listaReclamos = new ArrayList<Reclamo>();
    private ArrayList<Marker> marcadores = new ArrayList<Marker>();
    private int binary; //Variable que determina si se debe mostrar los marcadores o si solo se muestra el mapa para obtener coordenadas
    public MapaFragment() { }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        int tipoMapa =0;
        binary=0;
        Bundle argumentos = getArguments();
        if(argumentos !=null) {
            tipoMapa = argumentos .getInt("tipo_mapa",0);
        }
        getMapAsync(this);

        if(tipoMapa==2){
            getReclamos();
            binary=1;
        }
        return rootView;
    }
    @Override
    public void onMapReady(GoogleMap map) {
        miMapa = map;
        //Solicitamos el permiso de ubicación ni bien entramos a la sección "Ver en mapa".
        updateMap();
        if(binary==1){
            //Por cada reclamo añadimos el marcador
            for (Reclamo r: listaReclamos
                    ) {
                addMarcador(r);
            }

            LatLngBounds bounds = extremos();
            int padding = 50; //Relleno
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            miMapa.animateCamera(cu);
        }else{
            //IMPLEMENTACIÓN SETONMAPLONGCLICKLISTENER
            miMapa.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng){
                    listener.coordenadasSeleccionadas(latLng);
                }
            });
        }
    }

    private LatLngBounds extremos (){
        //Calculamos latitud-longitud extremos
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : marcadores) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        return bounds;
    }
    private void updateMap(){
        if (ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //Si no se tiene el permiso garantizado se solicita el mismo.
            ActivityCompat.requestPermissions((MainActivity)listener,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    123);
            return;
        }
        //Habilitamos ubicación actual.
        miMapa.setMyLocationEnabled(true);
    }
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //Si se obtuvo el permiso adecuado, se permite acceder a la ubicación actual.
        if(requestCode==123){
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   updateMap();
                } else {
                    Toast.makeText(getContext(), "ERROR.", Toast.LENGTH_SHORT).show();
                }
        }
    }

    public void getReclamos(){
        Runnable runn = new Runnable() {
            @Override
            public void run() {
                //Obtenemos la lista de reclamos de la BDD
                MapaFragment.this.listaReclamos = MyDatabase.getInstance(getActivity()).getReclamoDao().getAll();
            }
        };
        Thread hilo = new Thread(runn);
        hilo.start();
    }

    public void addMarcador(Reclamo reclamo){
        LatLng latitudLongitud = new LatLng(reclamo.getLatitud(), reclamo.getLongitud());
        marcadores.add(miMapa.addMarker(
                new MarkerOptions()
                .position(latitudLongitud)
                .title(reclamo.getReclamo())
        ));
    }

    public void setListener(OnAbrirMapaListener listener) {
        this.listener = listener;
    }
    public interface OnAbrirMapaListener {
        public void obtenerCoordenadas();
        public void coordenadasSeleccionadas(LatLng c);
    }
}
