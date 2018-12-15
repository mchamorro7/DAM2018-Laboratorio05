package ar.edu.utn.frsf.isi.dam.laboratorio05;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
import android.util.Log;
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

    //DECLARACIONES PARA GRABAR AUDIO
    private Button btnGrabarAudio;
    private Button btnPararAudio;
    private Button btnReproducirAudio;
    private static final String LOG_TAG = "AudioRecordTest";
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private Boolean reproduciendo = false;
    private String audioAnterior = null;

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
        btnGrabarAudio = (Button) v.findViewById(R.id.btnGrabarAudio);
        btnPararAudio = (Button) v.findViewById(R.id.btnPararAudio);
        btnReproducirAudio = (Button) v.findViewById(R.id.btnReproducirAudio);
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
        btnGrabarAudio.setEnabled(edicionActivada);
        btnTomarFoto.setEnabled(edicionActivada);
        btnReproducirAudio.setEnabled(false);
        btnPararAudio.setEnabled(false);


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
                btnReproducirAudio.setEnabled(false);
            }
        });

        //BOTON GRABAR AUDIO
        btnGrabarAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Chequeamos permisos
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 1234);
                } else {
                    grabar();
                }
            }
        });

        //BOTON PARAR AUDIO

        btnPararAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pararGrabar();
            }
        });

        //BOTON REPRODUCIR AUDIO

        btnReproducirAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reproducirAudio();
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

    //CREAMOS EL FILE DEL AUDIO

    private File createAudioFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        String audioFileName = "3GP_" + timeStamp + "_";
        File dir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File audio = File.createTempFile(
                audioFileName, /* prefix */
                ".3gp", /* suffix */
                dir /* directory */
        );
        reclamoActual.setPathAudio(audio.getAbsolutePath());
        return audio;
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

    //METODO GRABAR AUDIO

    private void grabar(){
        btnPararAudio.setEnabled(true);
        btnGrabarAudio.setEnabled(false);
        btnReproducirAudio.setEnabled(false);
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        try {
            createAudioFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.setOutputFile(reclamoActual.getPathAudio());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        mRecorder.start();
        System.out.println("Se comienza a grabar.");

    }

    //METODO TERMINAR DE GRABAR AUDIO

    private void pararGrabar() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        btnGrabarAudio.setEnabled(true);
        btnPararAudio.setEnabled(false);
        btnReproducirAudio.setEnabled(true);
        System.out.println("Se para de grabar.");
    }

    //METODO REPRODUCIR AUDIO

    private void reproducirAudio(){
        mPlayer = new MediaPlayer();
        btnPararAudio.setEnabled(false);
        btnGrabarAudio.setEnabled(false);
        btnReproducirAudio.setEnabled(true);
        buscarCoord.setEnabled(false);
        if(reproduciendo){
            mPlayer.release();
            mPlayer = null;
            buscarCoord.setEnabled(true);
            btnGrabarAudio.setEnabled(true);
            reproduciendo=false;
        }else{
            try {
                System.out.println("Se comienza a reproducir.");
                reproduciendo=true;
                mPlayer.setDataSource(reclamoActual.getPathAudio());
                mPlayer.prepare();
                mPlayer.start();
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mPlayer.release();
                        mPlayer = null;
                        buscarCoord.setEnabled(true);
                        btnGrabarAudio.setEnabled(true);
                        System.out.println("Se termina de reproducir.");
                    }
                });
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }

        }

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
                            btnPararAudio.setEnabled(false);
                            if(reclamoActual.getPathFoto()!=null){
                                onActivityResult(REQUEST_IMAGE_SAVE, Activity.RESULT_OK, null);
                            }
                            if(reclamoActual.getPathAudio()!=null){
                                btnReproducirAudio.setEnabled(true);
                            }
                            fotoAnterior = reclamoActual.getPathFoto();
                            audioAnterior = reclamoActual.getPathAudio();
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
        btnGrabarAudio.setEnabled(true);
        if(reclamoActual.getPathAudio()!=null){
            btnReproducirAudio.setEnabled(true);
        }
        btnPararAudio.setEnabled(false);
        Runnable hiloActualizacion = new Runnable() {
            @Override
            public void run() {
                if(fotoAnterior!=reclamoActual.getPathFoto() && fotoAnterior!=null){
                    System.out.println("1");
                    File f = new File(fotoAnterior);
                    f.delete();
                    System.out.println("Se borro la foto anterior.");
                }
                if(audioAnterior!=reclamoActual.getPathAudio() && audioAnterior!=null){
                    System.out.println("2");
                    File f = new File(audioAnterior);
                    f.delete();
                    System.out.println("Se borro el audio anterior.");
                }
                if(reclamoActual.getId()>0){
                    System.out.println("3");
                    reclamoDao.update(reclamoActual);
                }
                else{
                    System.out.println("4");
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
