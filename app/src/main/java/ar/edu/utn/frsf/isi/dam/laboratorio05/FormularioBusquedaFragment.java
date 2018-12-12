package ar.edu.utn.frsf.isi.dam.laboratorio05;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.Reclamo;

public class FormularioBusquedaFragment extends Fragment {

    private NuevoReclamoFragment.OnNuevoLugarListener listener;
    private Button btnBuscar;
    private Spinner spinnerTipoReclamo;
    private ArrayAdapter<Reclamo.TipoReclamo> adapterTipoReclamo;


    public void setListener(NuevoReclamoFragment.OnNuevoLugarListener listener) {
        this.listener = listener;
    }

    public FormularioBusquedaFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_busqueda_reclamo, container, false);
        spinnerTipoReclamo = v.findViewById(R.id.spinner_tipo_recamo);
        btnBuscar = v.findViewById(R.id.btn_buscar);

        adapterTipoReclamo = new ArrayAdapter<>(getActivity(),android.R.layout.simple_spinner_item,Reclamo.TipoReclamo.values());
        adapterTipoReclamo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoReclamo.setAdapter(adapterTipoReclamo);

        btnBuscar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                listener.crearMapaPorTipoReclamo(spinnerTipoReclamo.getSelectedItemPosition());
            }
        });

    return v;
    }

}
