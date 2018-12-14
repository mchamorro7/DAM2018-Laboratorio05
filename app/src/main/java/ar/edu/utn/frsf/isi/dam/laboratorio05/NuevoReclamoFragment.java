package ar.edu.utn.frsf.isi.dam.laboratorio05;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.MyDatabase;
import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.Reclamo;
import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.ReclamoDao;

import static android.app.Activity.RESULT_OK;

public class NuevoReclamoFragment extends Fragment {

    public interface OnNuevoLugarListener {
        public void obtenerCoordenadas();
        public void crearMapaPorTipoReclamo(int posReclamoSpinner);
    }

    public void setListener(OnNuevoLugarListener listener) {
        this.listener = listener;
    }

    private Reclamo reclamoActual;
    private ReclamoDao reclamoDao;

    private EditText reclamoDesc;
    private EditText mail;
    private Spinner tipoReclamo;
    private TextView tvCoord;
    private Button buscarCoord;
    private Button btnGuardar;
    private OnNuevoLugarListener listener;

    private Button btnTomarFoto;
    private ImageView foteli;

    //DECLARACIONES PARA USO DE CÁMARA
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_SAVE = 2;

    private String fotoAnterior = null;

    private ArrayAdapter<Reclamo.TipoReclamo> tipoReclamoAdapter;
    public NuevoReclamoFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        reclamoDao = MyDatabase.getInstance(this.getActivity()).getReclamoDao();

        View v = inflater.inflate(R.layout.fragment_nuevo_reclamo, container, false);

        reclamoDesc = (EditText) v.findViewById(R.id.reclamo_desc);
        mail= (EditText) v.findViewById(R.id.reclamo_mail);
        tipoReclamo= (Spinner) v.findViewById(R.id.reclamo_tipo);
        tvCoord= (TextView) v.findViewById(R.id.reclamo_coord);
        buscarCoord= (Button) v.findViewById(R.id.btnBuscarCoordenadas);
        btnGuardar= (Button) v.findViewById(R.id.btnGuardar);
        btnTomarFoto = (Button) v.findViewById(R.id.btnTomarFoto);
        foteli = (ImageView) v.findViewById(R.id.foteli);

        tipoReclamoAdapter = new ArrayAdapter<Reclamo.TipoReclamo>(getActivity(),android.R.layout.simple_spinner_item,Reclamo.TipoReclamo.values());

        tipoReclamoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoReclamo.setAdapter(tipoReclamoAdapter);



        int idReclamo =0;
        if(getArguments()!=null)  {
            idReclamo = getArguments().getInt("idReclamo",0);
        }

        cargarReclamo(idReclamo);


        boolean edicionActivada = !tvCoord.getText().toString().equals("0;0");
        /*reclamoDesc.setEnabled(edicionActivada );
        mail.setEnabled(edicionActivada );*/
        tipoReclamo.setEnabled(edicionActivada);
        btnGuardar.setEnabled(edicionActivada);
        btnTomarFoto.setEnabled(edicionActivada);

        buscarCoord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.obtenerCoordenadas();

            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveOrUpdateReclamo();

            }
        });


        //BOTON TOMAR FOTO
        btnTomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, 123);
                    } else {
                        sacarGuardarFoto();
                    }
                }

                /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }*/
            }
        });
        return v;
    }

    //CREAMOS EL FILE DE LA IMAGEN
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
                File dir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File image = File.createTempFile( imageFileName,
                        /* prefix */ ".jpg",
                        /* suffix */ dir
                        /* directory */ );
                reclamoActual.setPathFoto(image.getAbsolutePath());
                return image;

    }

    //METODO SACARFOTO
    private void sacarGuardarFoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {

            File photoFile = null;

            try {
                photoFile = createImageFile();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this.getContext(), "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_SAVE);
            }

        }
    }

    //RECIBIMOS EL PATH DE LA IMAGEN
    @Override
    public void onActivityResult(int reqCode,int resCode, Intent data) {
        if (reqCode == REQUEST_IMAGE_CAPTURE && resCode == Activity.RESULT_OK) {
            //System.out.println("ENTRO AL CAPTURE");
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            foteli.setImageBitmap(imageBitmap);
        }
        if (reqCode == REQUEST_IMAGE_SAVE && resCode == Activity.RESULT_OK) {
            //System.out.println("ENTRO AL SAVE");
            File file = new File(reclamoActual.getPathFoto());
            Bitmap imageBitmap = null;
            try {
                imageBitmap = MediaStore.Images.Media .getBitmap(getActivity().getContentResolver(), Uri.fromFile(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (imageBitmap != null) {
                foteli.setImageBitmap(imageBitmap);
            }

        }
        //System.out.println("SALIO DEL SAVE");
    }



    private void cargarReclamo(final int id){
        if( id >0){
            Runnable hiloCargaDatos = new Runnable() {
                @Override
                public void run() {
                    reclamoActual = reclamoDao.getById(id);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Obtenemos la foto que habiamos guardado.
                            if(reclamoActual.getPathFoto()!=null){
                                onActivityResult(REQUEST_IMAGE_SAVE, Activity.RESULT_OK, null);
                            }
                            fotoAnterior = reclamoActual.getPathFoto();

                            mail.setText(reclamoActual.getEmail());
                            tvCoord.setText(reclamoActual.getLatitud()+";"+reclamoActual.getLongitud());
                            reclamoDesc.setText(reclamoActual.getReclamo());
                            Reclamo.TipoReclamo[] tipos= Reclamo.TipoReclamo.values();
                            for(int i=0;i<tipos.length;i++) {
                                if(tipos[i].equals(reclamoActual.getTipo())) {
                                    tipoReclamo.setSelection(i);
                                    break;
                                }
                            }
                        }
                    });
                }
            };
            Thread t1 = new Thread(hiloCargaDatos);
            t1.start();
        }else{
        String coordenadas = "0;0";
        if(getArguments()!=null) coordenadas = getArguments().getString("latLng","0;0");
        tvCoord.setText(coordenadas);
        reclamoActual = new Reclamo();
    }

    }



    private void saveOrUpdateReclamo(){
        reclamoActual.setEmail(mail.getText().toString());
        reclamoActual.setReclamo(reclamoDesc.getText().toString());
        reclamoActual.setTipo(tipoReclamoAdapter.getItem(tipoReclamo.getSelectedItemPosition()));
        if(tvCoord.getText().toString().length()>0 && tvCoord.getText().toString().contains(";")) {
            String[] coordenadas = tvCoord.getText().toString().split(";");
            reclamoActual.setLatitud(Double.valueOf(coordenadas[0]));
            reclamoActual.setLongitud(Double.valueOf(coordenadas[1]));
        }
        Runnable hiloActualizacion = new Runnable() {
            @Override
            public void run() {
                if(fotoAnterior!=reclamoActual.getPathFoto() && fotoAnterior!=null){
                    System.out.println("1");
                    File f = new File(fotoAnterior);
                    f.delete();
                    System.out.println("Se borro la foto anterior.");
                }

                if(reclamoActual.getId()>0){
                    System.out.println("2");
                    reclamoDao.update(reclamoActual);
                }
                else{
                    System.out.println("3");
                    reclamoDao.insert(reclamoActual);
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // limpiar vista
                        mail.setText(R.string.texto_vacio);
                        tvCoord.setText(R.string.texto_vacio);
                        reclamoDesc.setText(R.string.texto_vacio);
                        foteli.setImageResource(android.R.color.darker_gray);
                        getActivity().getFragmentManager().popBackStack();
                    }
                });
            }
        };
        Thread t1 = new Thread(hiloActualizacion);
        t1.start();
    }


}
