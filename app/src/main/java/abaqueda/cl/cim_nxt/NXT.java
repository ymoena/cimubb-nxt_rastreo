package abaqueda.cl.cim_nxt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

//////////////////////////////////////////////////////////////
///     Aplicacion modificada por Yerko Moena 2017-2018    ///
///        en base a avances realizados anteriormente      ///
///            por otros compañeros en CIMUBB              ///
//////////////////////////////////////////////////////////////
public class NXT extends Activity {

    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_STATE_CHANGE = 2;
    public static final String TOAST = "toast"; // Variable que se ocupa para la notificación al usuario sin que se pare el proceso
    //firebase auth object
    private static final String TAGLOG = "firebase-db";
    private static final int REQUEST_ENABLE_BT = 1;//Variable de solicitud de habilitar bluetooth
    private static final int REQUEST_CONNECT_DEVICE = 2; //Variable para conectar el dispositivo
    private static final int REQUEST_SETTINGS = 3; //Variable de ajustes
    public static boolean deshabilitar;
    public static int estado;
    public static byte[] sensores = new byte[1024]; //Variable de tipo byte que almacenaran los sensores
    static NXTTalker mNXTTalker;
    public byte[] salida = new byte[]{(byte) 0, (byte) 0, (byte) 0};
    int i = 0;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    //DatabaseReference root = database.getReference().getRoot().child("Coordenadas");
    DatabaseReference historial = database.getReference().getRoot().child("Historial_Rastreo");
    DatabaseReference nuevafecha;
    //Variable que contiene el daptador por defecto del dispositivo
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    int contador = 1;
    int distancia = 0;

    String[] dato_rojo = new String[5];
    String[] dato_azul = new String[5];
    String[] dato_verde = new String[5];
    String[] dato_negro = new String[2];
    String temp_x, temp_y, temp_px, temp_py, temp_nx, temp_ny;


    //Variable booleana que contiene al adaptador bluetooth si no es nulo(true)
    boolean hasBluetooth = !(mBluetoothAdapter == null);
    boolean rrojo = false;
    boolean razul = false;
    boolean rverde = false;
    boolean rastreo = false;
    boolean esAzul = false;
    boolean esRojo = false;
    boolean esVerde = false;
    private Intent intent;
    private int mState = NXTTalker.STATE_NONE; //Variable para indicar el estado del robot
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_STATE_CHANGE:
                    mState = msg.arg1;
                    break;
            }
        }
    };
    private int MotorSeleccionado = 1;
    private byte Power = 100; //Variable para la intensidad del motor del robot
    private int mSavedState = NXTTalker.STATE_NONE; //Variable inicializada en estado ninguno
    private boolean mNewLaunch = true; //Variable para nuevo lanzamiento
    private String mDeviceAddress = null; //Variable de la dirección del dispositivo inicializado en nulo

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        Log.i("cem Info: ", "Paso 1");
        //  Variable que contiene el menu y se castea con linearlayout, el cual alinea las vistas
        final LinearLayout MenuLayout = (LinearLayout) findViewById(R.id.menuLayout); //Menu superior

        // Variables de tipo final que se castean con relativelayout el cual es el por defecto en android
        final RelativeLayout LinkLayout = (RelativeLayout) findViewById(R.id.linkLayout); //Panel link para iniciar la conexión
        final RelativeLayout MotorLayout = (RelativeLayout) findViewById(R.id.motorLayout); //Panel motores
        final RelativeLayout SensorLayout = (RelativeLayout) findViewById(R.id.sensorLayout); //Panel sensores
        final RelativeLayout EjecucionLayout = (RelativeLayout) findViewById(R.id.ejecucionLayout); //Panel Ejecucion
        final RelativeLayout ConfigureLayout = (RelativeLayout) findViewById(R.id.configureLayout); //Panel Ejecucion

        //Indica que menu y link se vean de forma predeterminada al entrar a la aplicación con VISIBLE y motor y sensor no ocurre eso
        MenuLayout.setVisibility(View.VISIBLE);
        LinkLayout.setVisibility(View.VISIBLE);
        MotorLayout.setVisibility(View.INVISIBLE);
        SensorLayout.setVisibility(View.INVISIBLE);
        EjecucionLayout.setVisibility(View.INVISIBLE);
        ConfigureLayout.setVisibility(View.INVISIBLE);

        //Panel link (forma grafica)
        final ToggleButton Bluetooth = (ToggleButton) findViewById(R.id.bluetooth);
        final ToggleButton Habilitar = (ToggleButton) findViewById(R.id.habilitar);
        final Button Tiempo_adelante = (Button) findViewById(R.id.tiempo_adelante);
        final Button Rotacion_adelante = (Button) findViewById(R.id.rotacion_adelante);
        final Button Tiempo_atras = (Button) findViewById(R.id.tiempo_atras);
        final Button Rotacion_atras = (Button) findViewById(R.id.rotacion_atras);
        final Button Reaccion_10cm = (Button) findViewById(R.id.reaccion_10cm);
        final Button Reaccion_15cm = (Button) findViewById(R.id.reaccion_15cm);
        final Button Reaccion_20cm = (Button) findViewById(R.id.reaccion_20cm);
        final Button Reaccion_25cm = (Button) findViewById(R.id.reaccion_25cm);
        final Button Capturar_linea = (Button) findViewById(R.id.capturar_linea);
        final Button GiroIzquierda = (Button) findViewById(R.id.izquierda);
        final Button Capturar_fondo = (Button) findViewById(R.id.capturar_fondo);
        final Button GiroDerecha = (Button) findViewById(R.id.derecha);
        final TextView Text_linea = (TextView) findViewById(R.id.text_linea);
        final TextView Text_fondo = (TextView) findViewById(R.id.text_fondo);
        final Button Line_Follower = (Button) findViewById(R.id.line_follower);
        final Button Configure = (Button) findViewById(R.id.configure);
        final TextView etiqueta = (TextView) findViewById(R.id.etiquetacolor);
        final TextView movimiento = (TextView) findViewById(R.id.movimiento);
        final Button Prueba = (Button) findViewById(R.id.prueba);
        final Button Rastrear = (Button) findViewById(R.id.rastrear);
        final ToggleButton ModoAutomatico = (ToggleButton) findViewById(R.id.iniciarcolor);
        ModoAutomatico.setVisibility(View.INVISIBLE);
        final Button Detener = (Button) findViewById(R.id.detener);
        Rastrear.setVisibility(View.INVISIBLE);


        // Recibe los datos para la conexión con CIM-NXT
        intent = getIntent();
        Bundle bundle = getIntent().getExtras();
        final int[] resultado = bundle.getIntArray("resultado");

        Log.i("cem Info: ", "Paso 2");

        if (resultado[0] == 1) {
            etiqueta.setTextColor(Color.RED);
            etiqueta.setText("Robot Rojo");
            esRojo = true;
        } else if (resultado[1] == 1) {
            etiqueta.setTextColor(Color.BLUE);
            etiqueta.setText("Robot Azul");
            esAzul = true;
        } else if (resultado[2] == 1) {
            etiqueta.setTextColor(Color.GREEN);
            etiqueta.setText("Robot Verde");
            esVerde = true;
        } else {
            etiqueta.setText("Error");
        }

        Text_linea.setText("0");
        Text_fondo.setText("0");


        //Seguimiento colores


        Log.i("cem Info: ", "Paso 3");


        final TextView rojox = (TextView) findViewById(R.id.datoColorx);
        final TextView rojoy = (TextView) findViewById(R.id.datoColory);
        final TextView azulx = (TextView) findViewById(R.id.azulx);
        final TextView azuly = (TextView) findViewById(R.id.azuly);
        final TextView verdex = (TextView) findViewById(R.id.verdex);
        final TextView verdey = (TextView) findViewById(R.id.verdey);
        final TextView datos = (TextView) findViewById(R.id.datos);
        datos.setText("");
        final TextView colorObjetivox = (TextView) findViewById(R.id.datoObjetivox);
        final TextView colorObjetivoy = (TextView) findViewById(R.id.datoObjetivoy);

        final TextView colorRobot = (TextView) findViewById(R.id.colorRobot);


        Log.i("cem Info: ", "Paso 4");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference datonx = database.getReference().getRoot().child("Punto").child("Negro").child("RobotNx");
        DatabaseReference datony = database.getReference().getRoot().child("Punto").child("Negro").child("RobotNy");
        //resetNegro();

        final DatabaseReference rojo = database.getReference().getRoot().child("Punto").child("Rojo");
        //resetRojo();

        final DatabaseReference azul = database.getReference().getRoot().child("Punto").child("Azul");
        //resetAzul();

        final DatabaseReference verde = database.getReference().getRoot().child("Punto").child("Verde");
        //resetVerde();


        Log.i("cem Info: ", "Paso 5");
        rojo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Rastrear.setVisibility(View.VISIBLE);
                ModoAutomatico.setVisibility(View.VISIBLE);
                if (rrojo == true || resultado[0] == 1) {
                    //Log.i("Info - Puntos:",datox+" "+datoy+" "+legx+" "+legy+" "+objetivox+" "+objetivoy);

                    colorRobot.setText("Rojo");
                    colorRobot.setTextColor(Color.RED);

                    verdex.setText("");
                    verdey.setText("");
                    azulx.setText("");
                    azuly.setText("");
                    //moverRobot_v2(datox,datoy,legx,legy,objetivox,objetivoy);
                    String valorx = dataSnapshot.child("RobotRx").getValue().toString();
                    rojox.setTextColor(Color.BLACK);
                    rojox.setText("x:" + valorx);


                    String valory = dataSnapshot.child("RobotRy").getValue().toString();
                    rojoy.setTextColor(Color.BLACK);
                    rojoy.setText("x:" + valory);
                    //moverRobot_v3(dataSnapshot, rojo);

                    String valordx = dataSnapshot.child("RobotRdx").getValue().toString();
                    String valordy = dataSnapshot.child("RobotRdy").getValue().toString();


                    dato_rojo[0] = valorx;
                    dato_rojo[1] = valory;
                    dato_rojo[2] = valordx;
                    dato_rojo[3] = valordy;


                    if (valorx.equals("0") && valory.equals("0")) {
                        Log.i("cim Info", "es igual a 0");
                        datos.setText("No hay datos para rastrear");
                        datos.setBackgroundColor(Color.rgb(88, 160, 46));
                        datos.setTextColor(Color.WHITE);
                        Rastrear.setEnabled(false);
                        Rastrear.setBackgroundColor(Color.GRAY);
                        ModoAutomatico.setEnabled(false);
                        ModoAutomatico.setBackgroundColor(Color.GRAY);
                        ModoAutomatico.setTextColor(Color.WHITE);
                    } else {
                        datos.setText("Existen datos para rastrear");
                        datos.setBackgroundColor(Color.rgb(88, 160, 46));
                        datos.setTextColor(Color.WHITE);
                        Rastrear.setEnabled(true);
                        ModoAutomatico.setEnabled(true);
                        Rastrear.setBackgroundColor(Color.rgb(88, 160, 46));
                        ModoAutomatico.setBackgroundColor(Color.rgb(88, 160, 46));
                        ModoAutomatico.setTextColor(Color.WHITE);
                    }
                }
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAGLOG, "Error!", databaseError.toException());
            }
        });
        azul.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Rastrear.setVisibility(View.VISIBLE);
                ModoAutomatico.setVisibility(View.VISIBLE);

                if (razul == true || resultado[1] == 1) {
                    //Log.i("Info - Puntos:",datox+" "+datoy+" "+legx+" "+legy+" "+objetivox+" "+objetivoy);

                    colorRobot.setText("Azul");
                    colorRobot.setTextColor(Color.BLUE);

                    verdex.setText("");
                    verdey.setText("");
                    rojox.setText("");
                    rojoy.setText("");
                    //moverRobot_v2(datox,datoy,legx,legy,objetivox,objetivoy);
                    String valorx = dataSnapshot.child("RobotAx").getValue().toString();
                    azulx.setTextColor(Color.BLACK);
                    azulx.setText("x:" + valorx);

                    String valory = dataSnapshot.child("RobotAy").getValue().toString();
                    azuly.setTextColor(Color.BLACK);
                    azuly.setText("x:" + valory);
                    //moverRobot_v3(dataSnapshot, rojo);

                    String valordx = dataSnapshot.child("RobotAdx").getValue().toString();
                    String valordy = dataSnapshot.child("RobotAdy").getValue().toString();


                    dato_azul[0] = valorx;
                    dato_azul[1] = valory;
                    dato_azul[2] = valordx;
                    dato_azul[3] = valordy;


                    if (valorx.equals("0") && valory.equals("0")) {
                        Log.i("cim Info", "es igual a 0");
                        datos.setText("No hay datos para rastrear");
                        datos.setBackgroundColor(Color.rgb(88, 160, 46));
                        datos.setTextColor(Color.WHITE);
                        Rastrear.setEnabled(false);
                        Rastrear.setBackgroundColor(Color.GRAY);
                        ModoAutomatico.setEnabled(false);
                        ModoAutomatico.setBackgroundColor(Color.GRAY);
                        ModoAutomatico.setTextColor(Color.WHITE);
                    } else {
                        datos.setText("Existen datos para rastrear");
                        datos.setBackgroundColor(Color.rgb(88, 160, 46));
                        datos.setTextColor(Color.WHITE);
                        Rastrear.setEnabled(true);
                        ModoAutomatico.setEnabled(true);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAGLOG, "Error!", databaseError.toException());
            }
        });
        verde.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Rastrear.setVisibility(View.VISIBLE);
                ModoAutomatico.setVisibility(View.VISIBLE);
                if (rverde == true || resultado[2] == 1) {
                    //Log.i("Info - Puntos:",datox+" "+datoy+" "+legx+" "+legy+" "+objetivox+" "+objetivoy);

                    colorRobot.setText("Verde");
                    colorRobot.setTextColor(Color.GREEN);

                    rojox.setText("");
                    rojoy.setText("");
                    azulx.setText("");
                    azuly.setText("");
                    //moverRobot_v2(datox,datoy,legx,legy,objetivox,objetivoy);
                    String valorx = dataSnapshot.child("RobotVx").getValue().toString();
                    verdex.setTextColor(Color.BLACK);
                    verdex.setText("x:" + valorx);

                    String valory = dataSnapshot.child("RobotVy").getValue().toString();
                    verdey.setTextColor(Color.BLACK);
                    verdey.setText("x:" + valory);
                    //moverRobot_v3(dataSnapshot, rojo);

                    String valordx = dataSnapshot.child("RobotVdx").getValue().toString();
                    String valordy = dataSnapshot.child("RobotVdy").getValue().toString();


                    dato_verde[0] = valorx;
                    dato_verde[1] = valory;
                    dato_verde[2] = valordx;
                    dato_verde[3] = valordy;


                    if (valorx.equals("0") && valory.equals("0")) {
                        Log.i("cim Info", "es igual a 0");
                        datos.setText("No hay datos para rastrear");
                        datos.setBackgroundColor(Color.rgb(88, 160, 46));
                        datos.setTextColor(Color.WHITE);
                        Rastrear.setEnabled(false);
                        Rastrear.setBackgroundColor(Color.GRAY);
                        ModoAutomatico.setEnabled(false);
                        ModoAutomatico.setBackgroundColor(Color.GRAY);
                        ModoAutomatico.setTextColor(Color.WHITE);
                    } else {
                        datos.setText("Existen datos para rastrear");
                        datos.setBackgroundColor(Color.rgb(88, 160, 46));
                        datos.setTextColor(Color.WHITE);
                        Rastrear.setEnabled(true);
                        ModoAutomatico.setEnabled(true);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAGLOG, "Error!", databaseError.toException());
            }
        });
        //Negro
        datonx.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String valor = dataSnapshot.getValue().toString();
                colorObjetivox.setTextColor(Color.BLACK);
                colorObjetivox.setText("x:" + valor);

                dato_negro[0] = valor;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAGLOG, "Error!", databaseError.toException());
            }
        });
        datony.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String valor = dataSnapshot.getValue().toString();
                colorObjetivoy.setTextColor(Color.BLACK);
                colorObjetivoy.setText("y:" + valor);

                dato_negro[1] = valor;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAGLOG, "Error!", databaseError.toException());
            }
        });


        Log.i("cem Info: ", "Paso 6");

        //  Log.i("cim Datos en Firebase"," Rx:" +dato_rojo[0]+" Ry:"+dato_rojo[1]+" Ax:" +dato_azul[0]+" Ay:"+dato_azul[1]+" Vx:" +dato_verde[0]+" Vy:"+dato_verde[1]+" Nx:" +dato_negro[0]+" Ny:"+dato_negro[1]);


        Log.i("cem Info: ", "Paso 7");
        ModoAutomatico.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    //resetRojo();
                    //resetNegro();
                    //resetAzul();
                    //resetVerde();


                    if (resultado[0] == 1) {
                        rrojo = true;
                    } else if (resultado[1] == 1) {
                        razul = true;
                    } else if (resultado[0] == 2) {
                        rverde = true;
                    }
                } else {
                    contador = 1;

                    if (resultado[0] == 1) {
                        rrojo = false;
                    } else if (resultado[1] == 1) {
                        razul = false;
                    } else if (resultado[2] == 1) {
                        rverde = false;
                    }
                }
            }
        });

        Log.i("cem Info: ", "Paso 8");
        Rastrear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Date date = new Date();
                DateFormat fechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                final String fecha = fechaHora.format(date);
                nuevafecha = historial.child(fecha);

                movimiento.setText("");
                datos.setText("Rastreando...");
                datos.setBackgroundColor(Color.BLUE);
                Rastrear.setVisibility(View.INVISIBLE);
                ModoAutomatico.setVisibility(View.INVISIBLE);
                Detener.setVisibility(View.VISIBLE);

                if (resultado[0] == 1) {
                    Log.i("cim Info: ", "Datos Rojo enviados");
                    //RastrearRobot(dato_rojo[0],dato_rojo[1],dato_rojo[2],dato_rojo[3], dato_negro[0], dato_negro[1] );
                    runThread_rojo(dato_rojo, dato_negro);
                }
                if (resultado[1] == 1) {
                    Log.i("cim Info: ", "Datos Azul enviados");
                    runThread_azul(dato_azul, dato_negro);
                }
                if (resultado[2] == 1) {
                    Log.i("cim Info: ", "Datos Verde enviados");
                    runThread_verde(dato_verde, dato_negro);
                }


                //resetAzul();
                //resetRojo();
                //resetVerde();
                //resetNegro();
            }
        });

        Detener.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rastreo = false;
                dato_rojo[0] = temp_x;
                dato_rojo[1] = temp_y;
                dato_rojo[2] = temp_px;
                dato_rojo[3] = temp_px;
                dato_negro[0] = temp_nx;
                dato_negro[1] = temp_ny;
                Detener.setVisibility(View.INVISIBLE);
                Rastrear.setVisibility(View.VISIBLE);
                ModoAutomatico.setVisibility(View.VISIBLE);
                movimiento.setText("");
                datos.setText("Existen datos para rastrear");
                datos.setBackgroundColor(Color.rgb(88, 160, 46));
                datos.setTextColor(Color.WHITE);

                contador = 1;
                Log.i("cem Info: ", "Detenido");
            }
        });

        Log.i("cem Info: ", "Paso 9");
        Bluetooth.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
            public void onClick(View v) {
                //Si hasbluetooth es falso, corresponde que no pudo encontrar el bluetooth por defecto
                if (!hasBluetooth)
                    Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_LONG).show();
                if (Bluetooth.isChecked()) {
                    // Si el bluetooth existe pero el adaptador no esta habilitado, se le pide al usuario que lo habilite
                    if (hasBluetooth && !mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_LONG).show();
                    } else {
                        if (hasBluetooth && mBluetoothAdapter.isEnabled()) {
                            if (mSavedState == NXTTalker.STATE_CONNECTED) {
                                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                                mNXTTalker.connect(device);
                            } else {
                                if (mNewLaunch) {
                                    mNewLaunch = false;
                                    startConnection();
                                }
                            }
                            Toast.makeText(getApplicationContext(), "Bluetooth turned off", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                mNXTTalker.stop();
            }
        });

        Log.i("cem Info: ", "Paso 10");
        Capturar_linea.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sensores = mNXTTalker.buffer;
                int valor = Integer.parseInt(Byte.toString(sensores[9]));
                Text_linea.setText(String.valueOf(valor));

            }
        });

        Capturar_fondo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sensores = mNXTTalker.buffer;
                int valor = Integer.parseInt(Byte.toString(sensores[10]));
                Text_fondo.setText(String.valueOf(valor));

            }
        });

        GiroDerecha.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                girarDer();

            }
        });

        GiroIzquierda.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                girarIzq();

            }
        });

        Log.i("cem Info: ", "Paso 11");
        Habilitar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Habilitar.isChecked()) {
                    // Si el bluetooth existe pero el adaptador no esta habilitado, se le pide al usuario que lo habilite
                    if (hasBluetooth && !mBluetoothAdapter.isEnabled()) {
                        Toast.makeText(getApplicationContext(), "Intente conectarse primero", Toast.LENGTH_LONG).show();
                    } else {
                        if (hasBluetooth && mBluetoothAdapter.isEnabled() && (NXTTalker.mState == NXTTalker.STATE_CONNECTED)) {
                            //mNXTTalker.motors3();
                            // mNXTTalker.write2(bufferSalida);
                            estado = 0;
                            salida[0] = 100;
                            mNXTTalker.deshabilitar();
                            deshabilitar = true;
                        }
                    }
                }
                if (!Habilitar.isChecked()) {
                    // Si el bluetooth existe pero el adaptador no esta habilitado, se le pide al usuario que lo habilite
                    if (hasBluetooth && !mBluetoothAdapter.isEnabled()) {
                        Toast.makeText(getApplicationContext(), "Intente conectarse primero", Toast.LENGTH_LONG).show();
                    } else {
                        if (hasBluetooth && mBluetoothAdapter.isEnabled() && (NXTTalker.mState == NXTTalker.STATE_CONNECTED)) {
                            estado = 1;
                            deshabilitar = false;
                            mNXTTalker.habilitar();
                        }
                    }
                }
            }
        });
        Log.i("cem Info: ", "Paso 12");
        Tiempo_adelante.setOnClickListener(new View.OnClickListener() {

                                               public void onClick(View v) {
                                                   mNXTTalker.tiempo_adelante_empezar();
                                                   try {
                                                       Thread.sleep(4000);
                                                   } catch (InterruptedException e) {
                                                       e.printStackTrace();
                                                   }
                                                   mNXTTalker.tiempo_parar();
                                               }
                                           }
        );

        Rotacion_adelante.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                mNXTTalker.rotacion_adelante();
            }
        });

        Rotacion_atras.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                mNXTTalker.rotacion_atras();
            }
        });

        Tiempo_atras.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                mNXTTalker.tiempo_atras_empezar();
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mNXTTalker.tiempo_parar();
            }
        });


        Log.i("cem Info: ", "Paso 13");
        Prueba.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                girarDer();
                hacia_adelante();
            }
        });


        Reaccion_10cm.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                boolean dale = true;
                int distancia, estados;

                mNXTTalker.solo_andar();
                while (dale) {
                    sensores = mNXTTalker.buffer;
                    if (sensores[7] == -1) {
                    } else {
                        distancia = Integer.parseInt(Byte.toString(sensores[7]));
                        if (distancia > 0 && distancia <= 10) {
                            mNXTTalker.tiempo_parar();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.reaccion_10cm();
                            try {
                                Thread.sleep(2500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.rotacion_adelante();
                            dale = false;
                        }
                    }
                }
            }
        });

        Log.i("cem Info: ", "Paso 14");
        Reaccion_15cm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean dale = true;
                int distancia, estados;

                mNXTTalker.solo_andar();
                while (dale) {
                    sensores = mNXTTalker.buffer;
                    if (sensores[7] == -1) {
                    } else {
                        distancia = Integer.parseInt(Byte.toString(sensores[7]));
                        if (distancia > 10 && distancia <= 15) {
                            mNXTTalker.tiempo_parar();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.rotacion_atras();
                            try {
                                Thread.sleep(5500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.tiempo_parar();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            mNXTTalker.reaccion_15cm();
                            dale = false;
                        }
                    }
                }
            }
        });


        Reaccion_20cm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean dale = true;
                int distancia, estados;
                mNXTTalker.solo_andar();
                while (dale) {
                    sensores = mNXTTalker.buffer;
                    if (sensores[7] == -1) {
                    } else {
                        distancia = Integer.parseInt(Byte.toString(sensores[7]));
                        if (distancia > 15 && distancia <= 20) {
                            mNXTTalker.tiempo_parar();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.reaccion_20cm();
                            try {
                                Thread.sleep(8500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.tiempo_parar();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.rotacion_atras();

                            dale = false;
                        }
                    }
                }
            }
        });

        Reaccion_25cm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean dale = true;
                int distancia, estados;
                mNXTTalker.solo_andar();
                while (dale) {
                    sensores = mNXTTalker.buffer;
                    if (sensores[7] == -1) {
                    } else {
                        distancia = Integer.parseInt(Byte.toString(sensores[7]));
                        if (distancia > 20 && distancia <= 25) {
                            mNXTTalker.tiempo_parar();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.rotacion_adelante();
                            try {
                                Thread.sleep(5500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mNXTTalker.tiempo_parar();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            mNXTTalker.reaccion_25cm();
                            dale = false;
                        }
                    }
                }
            }
        });
        Log.i("cem Info: ", "Paso 15");

        Line_Follower.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                int posicion_linea = Integer.parseInt(Text_linea.getText().toString());
                int posicion_fondo = Integer.parseInt(Text_fondo.getText().toString());
                int luz1, luz2, luz3;
                mNXTTalker.solo_andar_luz();


                while (true) {
                    //
                    //sensores = mNXTTalker.buffer;
                    luz1 = 0;
                    luz2 = 0;
                    luz3 = 0;
                    luz1 = Integer.parseInt(Byte.toString(sensores[8]));//Luz derecha
                    luz2 = Integer.parseInt(Byte.toString(sensores[9]));//Luz Central
                    luz3 = Integer.parseInt(Byte.toString(sensores[10]));//Luz izquierda

                    if ((luz2 > (posicion_linea - 5) && luz2 < (posicion_linea + 5)) && (luz1 > (posicion_linea - 5) && luz1 < (posicion_linea + 5)) && (luz3 > (posicion_linea - 5) && luz3 < (posicion_linea + 5))) {
                        mNXTTalker.tiempo_parar();
                        break;
                    } else {

                        //if ((luz2> (posicion_linea-5) && luz2<(posicion_linea+5))&& (luz1>(posicion_fondo-5) && luz1<(posicion_fondo+5)) && (luz3>(posicion_fondo-5) && luz3 <(posicion_fondo+5))) {
                        if ((luz2 > (posicion_linea - 5) && luz2 < (posicion_linea + 5))) {
                            //solo andar
                            mNXTTalker.solo_andar_luz();

                        } else {
                            if ((luz1 > (posicion_linea - 3) && luz1 < (posicion_linea + 5)) && (luz2 > (posicion_fondo - 3) && luz2 < (posicion_fondo + 3))) {// luz derecha es luz1

                                while ((Integer.parseInt(Byte.toString(sensores[9])) > posicion_linea + 5) || (Integer.parseInt(Byte.toString(sensores[9])) < posicion_linea - 3)) {
                                    mNXTTalker.tiempo_parar();

                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mNXTTalker.luz_izquierda();
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                mNXTTalker.luz_derecha();
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mNXTTalker.tiempo_parar();

                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                if ((luz3 > (posicion_linea - 3) && luz3 < (posicion_linea + 5)) && (luz2 > (posicion_fondo - 3) && luz2 < (posicion_fondo + 3))) {//luz izquierda es luz3

                                    while ((Integer.parseInt(Byte.toString(sensores[9])) > (posicion_linea + 5)) || (Integer.parseInt(Byte.toString(sensores[9])) < (posicion_linea - 3))) {
                                        mNXTTalker.tiempo_parar();
                                        //luz2 = Integer.parseInt(Byte.toString(sensores[9]));//Luz Central
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }


                                        mNXTTalker.luz_derecha();
                                        try {
                                            Thread.sleep(200);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    mNXTTalker.luz_izquierda();
                                    try {
                                        Thread.sleep(150);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mNXTTalker.tiempo_parar();

                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                }/*else{
                                    if((luz2 >(posicion_fondo-5) && luz2 <(posicion_fondo+5)) && (luz1 >(posicion_fondo-5) && luz1 <(posicion_fondo+5)) && (luz3 >(posicion_fondo-5) && luz3 <(posicion_fondo+5))){

                                        while ((Integer.parseInt(Byte.toString(sensores[9]))>(posicion_linea+3)) || ((Integer.parseInt(Byte.toString(sensores[9])))<(posicion_linea-3))) {
                                            mNXTTalker.tiempo_parar();
                                            //luz2 = Integer.parseInt(Byte.toString(sensores[9]));//Luz Central
                                            try {
                                                Thread.sleep(500);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }


                                            mNXTTalker.luz_izquierda();
                                            mNXTTalker.luz_derecha();
                                            try {
                                                Thread.sleep(200);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }*/

                            }
                        }
                    }

                }

            }


        });

        Log.i("cem Info: ", "Paso 16");
        //Panel  de los motores
        final Button MotorR = (Button) findViewById(R.id.motorR); //Avanzar motor seleccionado R
        final Button MotorL = (Button) findViewById(R.id.motorL); //Retroceder motor seleccinado L
        final Button Motor1c = (Button) findViewById(R.id.motor1); //Cambiar al motor 1
        final Button Motor2c = (Button) findViewById(R.id.motor2); //Cambiar al motor 2
        final Button Motor3c = (Button) findViewById(R.id.motor3); //Cambiar al motor 3

        Motor1c.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MotorSeleccionado = 1;
            }
        });
        Motor2c.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MotorSeleccionado = 2;
            }
        });
        Motor3c.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MotorSeleccionado = 3;
            }
        });

        MotorR.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        switch (MotorSeleccionado) {
                            case 1:
                                moverMotores((byte) 1, (byte) 0, (byte) 0);
                                break;
                            case 2:
                                moverMotores((byte) 0, (byte) 1, (byte) 0);
                                //  mNXTTalker.limite((byte) 0, (byte) 50, (byte) 0,false,false);
                                break;
                            case 3:
                                moverMotores((byte) 0, (byte) 0, (byte) 1);
                                break;
                        }

                        return true;
                    case MotionEvent.ACTION_UP:
                        moverMotores((byte) 0, (byte) 0, (byte) 0);
                        return true;
                }
                return false;
            }
        });
        MotorL.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        switch (MotorSeleccionado) {
                            case 1:

                                moverMotores((byte) -1, (byte) 0, (byte) 0);
                                break;
                            case 2:
                                moverMotores((byte) 0, (byte) -1, (byte) 0);
                                // mNXTTalker.limite((byte) 0, (byte) 30, (byte) 0,false,false);
                                break;
                            case 3:
                                moverMotores((byte) 0, (byte) 0, (byte) -1);
                                break;
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        moverMotores((byte) 0, (byte) 0, (byte) 0);
                        return true;
                }
                return false;
            }
        });

        //Barra de intensidad del motor del robot
        SeekBar powerSeekBar = (SeekBar) findViewById(R.id.power);
        powerSeekBar.setProgress(Power);
        powerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                Power = (byte) progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Log.i("cem Info: ", "Paso 17");
        //Panel Sensores

        final TextView datosSensor1 = (TextView) findViewById(R.id.sensor1); //Datos del sensor 1: lightSensor1
        final TextView datosSensor2 = (TextView) findViewById(R.id.sensor2); //Datos del sensor 2: lightSensor2
        final TextView datosSensor3 = (TextView) findViewById(R.id.sensor3); //Datos del sensor 3: ultrasonido
        final TextView datosSensor4 = (TextView) findViewById(R.id.sensor4); //Datos del sensor 4: lightSensor3

        final TextView instrucciones = (TextView) findViewById(R.id.instrucciones); //Sugiere abrir el programa del robot

        if (savedInstanceState != null) {
            mNewLaunch = false;
            mDeviceAddress = savedInstanceState.getString("device_address");
            if (mDeviceAddress != null) {
                mSavedState = NXTTalker.STATE_CONNECTED;
            }
        }

        //Menu superior

        final Button Link = (Button) findViewById(R.id.sLink);
        final Button Motores = (Button) findViewById(R.id.sMotores);
        final Button Sensores = (Button) findViewById(R.id.sSensores);
        final Button Ejecucion = (Button) findViewById(R.id.Ejecucion);

        Link.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MenuLayout.setVisibility(View.VISIBLE);
                LinkLayout.setVisibility(View.VISIBLE);
                MotorLayout.setVisibility(View.INVISIBLE);
                SensorLayout.setVisibility(View.INVISIBLE);
                EjecucionLayout.setVisibility(View.INVISIBLE);
                ConfigureLayout.setVisibility(View.INVISIBLE);
            }
        });
        Motores.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MenuLayout.setVisibility(View.VISIBLE);
                LinkLayout.setVisibility(View.INVISIBLE);
                MotorLayout.setVisibility(View.VISIBLE);
                SensorLayout.setVisibility(View.INVISIBLE);
                EjecucionLayout.setVisibility(View.INVISIBLE);
                ConfigureLayout.setVisibility(View.INVISIBLE);
            }
        });

        Ejecucion.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MenuLayout.setVisibility(View.VISIBLE);
                LinkLayout.setVisibility(View.INVISIBLE);
                MotorLayout.setVisibility(View.INVISIBLE);
                SensorLayout.setVisibility(View.INVISIBLE);
                EjecucionLayout.setVisibility(View.VISIBLE);
                ConfigureLayout.setVisibility(View.INVISIBLE);

            }
        });

        Configure.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                MenuLayout.setVisibility(View.VISIBLE);
                LinkLayout.setVisibility(View.INVISIBLE);
                MotorLayout.setVisibility(View.INVISIBLE);
                SensorLayout.setVisibility(View.INVISIBLE);
                EjecucionLayout.setVisibility(View.INVISIBLE);
                ConfigureLayout.setVisibility(View.VISIBLE);

            }
        });

        Sensores.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                MenuLayout.setVisibility(View.VISIBLE);
                LinkLayout.setVisibility(View.INVISIBLE);
                MotorLayout.setVisibility(View.INVISIBLE);
                SensorLayout.setVisibility(View.VISIBLE);
                EjecucionLayout.setVisibility(View.INVISIBLE);
                ConfigureLayout.setVisibility(View.INVISIBLE);


                datosSensor1.setTextColor(0xff0000cc);
                datosSensor2.setTextColor(0xff0000cc);
                datosSensor3.setTextColor(0xff0000cc);
                datosSensor4.setTextColor(0xff0000cc);

                // Hace que los datos recibidos se muestren de manera constante(loop)
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // upadte textView here
                        sensores = mNXTTalker.buffer;

                        if (sensores[0] != 0) {
                            instrucciones.setVisibility(View.INVISIBLE);
                        } else {
                            instrucciones.setVisibility(View.VISIBLE);
                        }
                        /*
                        if ((sensores[6]) == -1) {
                            datosSensor2.setTextColor(0xffff0000);
                            datosSensor2.setText("Dato incorreto");
                        } else {
                            datosSensor1.setText(Byte.toString(sensores[6]));
                        }*/
                        if (deshabilitar) {
                            //sensores[7]=0;
                            datosSensor1.setText("0 cm");
                        } else {
                            if ((sensores[7]) == -1) {
                                datosSensor1.setTextColor(0xffff0000);
                                datosSensor1.setText("Dato incorreto");
                            } else {
                                int distancia;
                                distancia = Integer.parseInt(Byte.toString(sensores[7]));
                                //Byte.toString(sensores[7])
                                datosSensor1.setText(distancia + " cm");
                            }
                        }
                        //datosSensor3.setText("R: " + Byte.toString(sensores[8]) + " |   G: " + Byte.toString(sensores[9]) + " |   B: " + Byte.toString(sensores[10]));
                        if (deshabilitar) {
                            datosSensor2.setText("0 lux");
                            datosSensor3.setText("0 lux");
                            datosSensor4.setText("0 lux");
                        } else {
                            datosSensor2.setText(Byte.toString(sensores[8]) + " lux");
                            datosSensor3.setText(Byte.toString(sensores[9]) + " lux");
                            datosSensor4.setText(Byte.toString(sensores[10]) + " lux");
                        }

                        handler.postDelayed(this, 200); // set time here to refresh textView
                    }

                });


            }


        });

        mNXTTalker = new NXTTalker(mHandler);
        Log.i("cem Info: ", "Paso 18");
    }

    //Método que intenta la conexion con un dispositivo
    private void startConnection() {
        Intent intent = new Intent(this, SearchDeviceActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }

    public void resetRojo() {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference rojo_color = database.getReference().getRoot().child("Punto").child("Rojo");
        HashMap<String, Integer> rojo = new HashMap<>();


        rojo.put("RobotRx", 0);
        rojo.put("RobotRy", 0);
        rojo.put("RobotRdx", 0);
        rojo.put("RobotRdy", 0);
        rojo_color.setValue(rojo);


    }

    public void resetAzul() {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference azul_color = database.getReference().getRoot().child("Punto").child("Azul");
        HashMap<String, Integer> azul = new HashMap<>();


        azul.put("RobotAx", 0);
        azul.put("RobotAy", 0);
        azul.put("RobotAdx", 0);
        azul.put("RobotAdy", 0);
        azul_color.setValue(azul);


    }

    public void resetVerde() {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference verde_color = database.getReference().getRoot().child("Punto").child("Verde");
        HashMap<String, Integer> verde = new HashMap<>();


        verde.put("RobotVx", 0);
        verde.put("RobotVy", 0);
        verde.put("RobotVdx", 0);
        verde.put("RobotVdy", 0);
        verde_color.setValue(verde);


    }

    public void resetNegro() {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference negro_color = database.getReference().getRoot().child("Punto").child("Negro");
        HashMap<String, Integer> negro = new HashMap<>();


        negro.put("RobotNx", 0);
        negro.put("RobotNy", 0);
        negro_color.setValue(negro);

    }

    //Metodo para mover los motores dependiendo de la intensidad("power") que tenga
    public void moverMotores(byte a, byte b, byte c) {
        a = (byte) (Power * a);
        b = (byte) (Power * b);
        c = (byte) (Power * c);
        mNXTTalker.motors3(a, b, c, false, false);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:

                if (resultCode == Activity.RESULT_OK) {
                    startConnection();
                } else {
                    Toast.makeText(this, "Bluetooth not enabled, exiting.", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(SearchDeviceActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mDeviceAddress = address;
                    mNXTTalker.connect(device);
                }
                break;
            case REQUEST_SETTINGS:
                //XXX?
                break;
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mState == NXTTalker.STATE_CONNECTED) {
            outState.putString("device_address", mDeviceAddress);
        }
    }

    //Los runthreads son threads(hilos) que sirven para que el robot se mueva cada dos segundos
    //Ejecutando la funcion RastrearRobot_version3
    public void runThread_rojo(final String[] dato_rojo, final String[] dato_negro) {

        temp_x = dato_rojo[0];
        temp_y = dato_rojo[1];
        temp_px = dato_rojo[2];
        temp_py = dato_rojo[3];
        temp_nx = dato_negro[0];
        temp_ny = dato_negro[1];

        rastreo = true;
        new Thread() {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    RastrearRobot_version3(dato_rojo[0], dato_rojo[1], dato_rojo[2], dato_rojo[3], dato_negro[0], dato_negro[1]);
                    //RastrearRobot_90gradosgiros(dato_rojo[0], dato_rojo[1], dato_rojo[2], dato_rojo[3], dato_negro[0], dato_negro[1]);
                }
            };

            public void run() {
                i = 0;
                while (i++ < 1000) {//Mapea hasta 1000 veces
                    try {
                        runOnUiThread(runnable);
                        Thread.sleep(2000);//Mapea cada 2 segundo

                        if (i == 1001) {
                            break;
                        }

                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                    }
                }

            }
        }.start();

    }
    public void runThread_azul(final String[] dato_azul, final String[] dato_negro) {

        temp_x = dato_azul[0];
        temp_y = dato_azul[1];
        temp_px = dato_azul[2];
        temp_py = dato_azul[3];
        temp_nx = dato_negro[0];
        temp_ny = dato_negro[1];

        new Thread() {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    RastrearRobot_version3(dato_azul[0], dato_azul[1], dato_azul[2], dato_azul[3], dato_negro[0], dato_negro[1]);
                }
            };

            public void run() {
                i = 0;
                while (i++ < 1000) {//Mapea hasta 1000 veces
                    try {

                        runOnUiThread(runnable);
                        Thread.sleep(2000);//Mapea cada 2 segundo

                        if (i == 1001) {
                            break;
                        }

                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                    }
                }

            }
        }.start();

    }
    public void runThread_verde(final String[] dato_verde, final String[] dato_negro) {

        temp_x = dato_verde[0];
        temp_y = dato_verde[1];
        temp_px = dato_verde[2];
        temp_py = dato_verde[3];
        temp_nx = dato_negro[0];
        temp_ny = dato_negro[1];

        new Thread() {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    RastrearRobot_version3(dato_verde[0], dato_verde[1], dato_verde[2], dato_verde[3], dato_negro[0], dato_negro[1]);
                }
            };

            public void run() {
                i = 0;
                while (i++ < 1000) {//Mapea hasta 1000 veces
                    try {

                        runOnUiThread(runnable);
                        Thread.sleep(2000);//Mapea cada 2 segundo

                        if (i == 1001) {
                            break;
                        }

                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                    }
                }

            }
        }.start();

    }

    //giros en 45º
    public void girarDer() {

        switch (MotorSeleccionado) {
            case 1:
                moverMotores((byte) 1, (byte) 0, (byte) 0);
                break;
            case 2:
                moverMotores((byte) 0, (byte) 1, (byte) 0);
                //  mNXTTalker.limite((byte) 0, (byte) 50, (byte) 0,false,false);
                break;
            case 3:
                moverMotores((byte) 0, (byte) 0, (byte) 1);
                break;
        }
        try {
            Thread.sleep(390);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mNXTTalker.tiempo_parar();


    }

    public void girarIzq() {

        switch (MotorSeleccionado) {
            case 1:
                moverMotores((byte) -1, (byte) 0, (byte) 0);
                break;
            case 2:
                moverMotores((byte) 0, (byte) -1, (byte) 0);
                //  mNXTTalker.limite((byte) 0, (byte) 50, (byte) 0,false,false);
                break;
            case 3:
                moverMotores((byte) 0, (byte) 0, (byte) -1);
                break;
        }
        try {
            Thread.sleep(220);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mNXTTalker.tiempo_parar();


    }

    public void hacia_adelante() {

        mNXTTalker.tiempo_adelante_empezar();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mNXTTalker.tiempo_parar();
    }

    //inInside indica si el robot se encuentra dentro de un radio establecido(rad) del objetivo retornando true o false
    public boolean isInside(int objetivo_x, int objetivo_y, int rad, int color_x, int color_y) {
        // Compare radius of circle with distance of its center from
        // given point

        int x = color_x;
        int y = color_y;
        int circle_x = objetivo_x;
        int circle_y = objetivo_y;
        int DistPD = (x - circle_x) * (x - circle_x) +
                (y - circle_y) * (y - circle_y);
        int radio = rad * rad;
//        Log.i("cim Info:","RadioPD: "+DistPD+" radio: "+radio);

        return (x - circle_x) * (x - circle_x) +
                (y - circle_y) * (y - circle_y) <= rad * rad;
    }

    //Funcion que indica al robot como moverse
    public void RastrearRobot_version3(String datox, String datoy, String legx, String legy, String objetivox, String objetivoy) {

        int Rx = Integer.parseInt(datox);
        int Ry = Integer.parseInt(datoy);
        int Lx = Integer.parseInt(legx);
        int Ly = Integer.parseInt(legy);
        int Ox = Integer.parseInt(objetivox);
        int Oy = Integer.parseInt(objetivoy);


        final HashMap<String, String> actual = new HashMap<>();
        final HashMap<String, Integer> dato = new HashMap<>();


        Date date = new Date();
        DateFormat hora = new SimpleDateFormat("HH:mm:ss");

        final String houra = hora.format(date);
        actual.put("Hora", houra);


        //distancia = distancia(Lx,Ly,Rx,Ry);

        //Se podia dar el caso de que el robot al moverse pasara del cuadrante positivo donde idealmente se debe mover, a uno negativo
        //esto generaria un problema con los numeros negativos, por esto sume 500 a cada x e y de las coordenadas para alejarlo de los bordes
        //y evitar esto
        if (contador == 1) {
            //calculamos distancia entre la parte forntal del robot y el punto medio ede los circulos que estan en las ruedas,
            //con esto determinamos cuanto se movera hacia adelante el robot, moviendose a la mitad de esta distancia
            distancia = distancia(Lx, Ly, Rx, Ry);
            Lx = Lx + 500;
            Ly = Ly + 500;
            Rx = Rx + 500;
            Ry = Ry + 500;
            Ox = Ox + 500;
            Oy = Oy + 500;
            dato_negro[0] = String.valueOf(Ox);
            dato_negro[1] = String.valueOf(Oy);

            if (esRojo == true) {
                dato.put("RobotRx", Rx);
                dato.put("RobotRy", Ry);
                dato.put("RobotRdx", Lx);
                dato.put("RobotRdy", Ly);

                dato.put("RobotNx", Ox);
                dato.put("RobotNy", Oy);

                nuevafecha.child(String.valueOf(houra)).setValue(actual);
                nuevafecha.child(String.valueOf(houra)).setValue(dato);

            } else if (esAzul == true) {
                dato.put("RobotAx", Rx);
                dato.put("RobotAy", Ry);
                dato.put("RobotAdx", Lx);
                dato.put("RobotAdy", Ly);

                dato.put("RobotNx", Ox);
                dato.put("RobotNy", Oy);

                nuevafecha.child(String.valueOf(houra)).setValue(dato);

            } else if (esVerde == true) {
                dato.put("RobotVx", Rx);
                dato.put("RobotVy", Ry);
                dato.put("RobotVdx", Lx);
                dato.put("RobotVdy", Ly);

                dato.put("RobotNx", Ox);
                dato.put("RobotNy", Oy);

                nuevafecha.child(String.valueOf(houra)).setValue(dato);

            }

        }

        // Log.i("Info:","Distancia entre punto medio y parte delantera: " + distancia);
        String mensaje = "Rx: " + Rx + " Ry: " + Ry + " Lx: " + Lx + " Ly: " + Ly + " Ox: " + Ox + " Oy: " + Oy + " Dist: " + distancia + " Cont: " + contador;

        Log.i("Info:", "Datos en int: " + mensaje);

        boolean estaDentro = false;
        final TextView datos = (TextView) findViewById(R.id.datos);
        final TextView movimiento = (TextView) findViewById(R.id.movimiento);
        final Button Rastrear = (Button) findViewById(R.id.rastrear);
        final ToggleButton ModoAutomatico = (ToggleButton) findViewById(R.id.iniciarcolor);
        final Button Detener = (Button) findViewById(R.id.detener);
        try {

            //Pregunto si la parte delantera del robot se encuentra en el radio del objetivo, de ser asi finaliza la funcion
            //si no, sigue moviendose el robot
            estaDentro = isInside(Ox, Oy, 170, Rx, Ry);

            if (rastreo == true) {

                datos.setText("Rastreando... " + contador);
                datos.setBackgroundColor(Color.BLUE);

                if (estaDentro == true) {
                    //Objetivo encontrado
                    Toast.makeText(getApplicationContext(), "Objetivo encontrado", Toast.LENGTH_SHORT).show();
                    Log.i("Info:", "Objetivo encontrado");
                    Detener.setVisibility(View.INVISIBLE);
                    Rastrear.setVisibility(View.VISIBLE);
                    ModoAutomatico.setVisibility(View.VISIBLE);
                    datos.setText("Existen datos para rastrear");
                    datos.setBackgroundColor(Color.rgb(88, 160, 46));
                    datos.setTextColor(Color.WHITE);
                    movimiento.setText("Objetivo encontrado");
                    contador = 1;
                    i = 1001;
                    rastreo = false;

                    nuevafecha.child(String.valueOf(houra)).setValue(actual);

                    //Esto era para el boton ModoAutomatico que finalmente no implemente
                    if (esRojo = true) {
                        dato_rojo[0] = temp_x;
                        dato_rojo[1] = temp_y;
                        dato_rojo[2] = temp_px;
                        dato_rojo[3] = temp_py;
                        //resetRojo();
                    } else if (esAzul) {
                        dato_azul[0] = temp_x;
                        dato_azul[1] = temp_y;
                        dato_azul[2] = temp_px;
                        dato_azul[3] = temp_py;
                        //resetAzul();
                    } else if (esVerde) {
                        dato_verde[0] = temp_x;
                        dato_verde[1] = temp_y;
                        dato_verde[2] = temp_px;
                        dato_verde[3] = temp_py;
                        //resetVerde();
                    }

                    return;
                }

                if (Rx == 0 || Ry == 0 || Ox == 0 || Oy == 0 || Lx == 0 || Ly == 0) {
                    Log.i("Info:", "Alguna coordenada es 0, hacer nada");
                    //do nada
                }

                //A partir de aqui voy preguntando si la parte fo¡rontal del robot se encuentra en el mismo cuadrante(area) con el
                //objetivo en base al punto medio de las ruedas traseras que serviran como punto de origen para realizar las consultas
                if (Rx >= Lx && Ry >= Ly) {
                    //Robot Punta area 4
                    if (Ox > Lx && Oy < Ly) {
                        Log.i("Info:", "Girar Izquierda - Area 4");
                        movimiento.setText("Giro a la Izquierda - Area 4");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[1] = String.valueOf(Ry + ((Ry - Ly) / 2));
                        //Los giros se realizan en 45 grados
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) + (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) (((Lx - Rx) * Math.sin(45) - (Ly - Ry) * Math.cos(45) - Ry)) * -1);
                        girarIzq();
                    } else if (Ox < Lx && Oy > Ly) {
                        Log.i("Info:", "Girar Derecha - Area 4");
                        movimiento.setText("Giro a la Derecha - Area 4");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) - (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) ((Lx - Rx) * Math.sin(45) + (Ly - Ry) * Math.cos(45) + Ry));
                        girarDer();
                    } else if (Ox < Lx && Oy < Ly) {
                        Log.i("Info:", "Girar Izquierda o Derecha - Area 4");
                        movimiento.setText("Giro a la Izquierda *  - Area 4");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) + (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) (((Lx - Rx) * Math.sin(45) - (Ly - Ry) * Math.cos(45) - Ry)) * -1);
                        girarIzq();
                    } else if (Ox >= Lx && Oy >= Ly) {
                        Log.i("Info:", "Hacia adelante - Area 4");
                        movimiento.setText("Hacia adelante - Area 4");
                        dato_rojo[0] = String.valueOf(Rx + ((Rx - Lx) / 2));
                        dato_rojo[1] = String.valueOf(Ry + ((Ry - Ly) / 2));
                        dato_rojo[2] = String.valueOf(Lx + ((Rx - Lx) / 2));
                        dato_rojo[3] = String.valueOf(Ly + ((Ry - Ly) / 2));
                        hacia_adelante();
                    } else {

                        Log.i("Info:", "Hacia adelante - Area 4 *");
                        movimiento.setText("Hacia adelante - Area 4 *");
                        dato_rojo[0] = String.valueOf(Rx + 1);
                        dato_rojo[1] = String.valueOf(Ry + 1);
                        dato_rojo[2] = String.valueOf(Lx + 1);
                        dato_rojo[3] = String.valueOf(Ly + 1);
                        //hacia_adelante();
                    }
                } else if (Rx <= Lx && Ry >= Ly) {
                    //Robot Punta area 3

                    if (Ox > Lx && Oy > Ly) {
                        Log.i("Info:", "Girar Izquierda - Area 3");
                        movimiento.setText("Giro a la Izquierda - Area 3");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) + (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) (((Lx - Rx) * Math.sin(45) - (Ly - Ry) * Math.cos(45) - Ry)) * -1);
                        girarIzq();
                    } else if (Ox < Lx && Oy < Ly) {
                        Log.i("Info:", "Girar Derecha - Area 3");
                        movimiento.setText("Giro a la Derecha - Area 3");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) - (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) ((Lx - Rx) * Math.sin(45) + (Ly - Ry) * Math.cos(45) + Ry));
                        girarDer();
                    } else if (Ox > Lx && Oy < Ly) {
                        Log.i("Info:", "Girar Izquierda o Derecha - Area 3");
                        movimiento.setText("Giro a la Izquierda * - Area 3");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) + (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) (((Lx - Rx) * Math.sin(45) - (Ly - Ry) * Math.cos(45) - Ry)) * -1);
                        girarIzq();
                    } else if (Ox <= Lx && Oy >= Ly) {
                        Log.i("Info:", "Hacia adelante - Area 3");
                        movimiento.setText("Hacia adelante - Area 3");
                        dato_rojo[0] = String.valueOf(Rx - ((Lx - Rx) / 2));
                        dato_rojo[1] = String.valueOf(Ry + ((Ry - Ly) / 2));
                        dato_rojo[2] = String.valueOf(Lx - ((Lx - Rx) / 2));
                        dato_rojo[3] = String.valueOf(Ly + ((Ry - Ly) / 2));
                        hacia_adelante();
                    } else {
                        Log.i("Info:", "Uno - Area 3 *");
                        movimiento.setText("Uno - Area 3 *");
                        dato_rojo[0] = String.valueOf(Rx + 1);
                        dato_rojo[1] = String.valueOf(Ry + 1);
                        dato_rojo[2] = String.valueOf(Lx + 1);
                        dato_rojo[3] = String.valueOf(Ly + 1);
                        //hacia_adelante();
                    }
                } else if (Rx <= Lx && Ry <= Ly) {
                    //Robot Punta area 2
                    if (Ox < Lx && Oy > Ly) {
                        Log.i("Info:", "Girar Izquierda - Area 2");
                        movimiento.setText("Giro a la Izquierda - Area 2");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) + (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) (((Lx - Rx) * Math.sin(45) - (Ly - Ry) * Math.cos(45) - Ry)) * -1);
                        girarIzq();
                    } else if (Ox > Lx && Oy < Ly) {
                        Log.i("Info:", "Girar Derecha - Area 2");
                        movimiento.setText("Giro a la Derecha - Area 2");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) - (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) ((Lx - Rx) * Math.sin(45) + (Ly - Ry) * Math.cos(45) + Ry));
                        girarDer();
                    } else if (Ox > Lx && Oy > Ly) {
                        Log.i("Info:", "Girar Izquierda o Derecha * - Area 2");
                        movimiento.setText("Giro a la Izquierda * - Area 2");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) + (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) (((Lx - Rx) * Math.sin(45) - (Ly - Ry) * Math.cos(45) - Ry)) * -1);
                        girarIzq();
                    } else if (Ox <= Lx && Oy <= Ly) {
                        Log.i("Info:", "Hacia adelante - Area 2");
                        movimiento.setText("Hacia adelante - Area 2");
                        dato_rojo[0] = String.valueOf(Rx - ((Lx - Rx) / 2));
                        dato_rojo[1] = String.valueOf(Ry - ((Ly - Ry) / 2));
                        dato_rojo[2] = String.valueOf(Lx - ((Lx - Rx) / 2));
                        dato_rojo[3] = String.valueOf(Ly - ((Ly - Ry) / 2));
                        hacia_adelante();
                    } else {

                        Log.i("Info:", "Hacia adelante - Area 2 *");
                        movimiento.setText("Hacia adelante - Area 2 *");
                        dato_rojo[0] = String.valueOf(Rx + 1);
                        dato_rojo[1] = String.valueOf(Ry + 1);
                        dato_rojo[2] = String.valueOf(Lx + 1);
                        dato_rojo[3] = String.valueOf(Ly + 1);
                        //hacia_adelante();
                    }
                } else if (Rx >= Lx && Ry <= Ly) {
                    //Robot Punta area 1
                    if (Ox < Lx && Oy < Ly) {
                        Log.i("Info:", "Girar Izquierda - Area 1");
                        movimiento.setText("Giro a la Izquierda - Area 1");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) + (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) (((Lx - Rx) * Math.sin(45) - (Ly - Ry) * Math.cos(45) - Ry)) * -1);
                        Log.i("Info:", Rx + "," + Ry + "," + dato_rojo[0] + "," + dato_rojo[1]);
                        girarIzq();
                    } else if (Ox > Lx && Oy > Ly) {
                        Log.i("Info:", "Girar Derecha - Area 1");
                        movimiento.setText("Giro a la Derecha - Area 1");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) - (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) ((Lx - Rx) * Math.sin(45) + (Ly - Ry) * Math.cos(45) + Ry));
                        girarDer();
                    } else if (Ox < Lx && Oy > Ly) {
                        Log.i("Info:", "Girar Izquierda o Derecha - Area 1");
                        movimiento.setText("Giro a la Izquierda * - Area 1");
                        dato_rojo[0] = String.valueOf(Rx);
                        dato_rojo[1] = String.valueOf(Ry);
                        dato_rojo[2] = String.valueOf((int) ((Lx - Rx) * Math.cos(45) + (Ly - Ry) * Math.sin(45) + Rx));
                        dato_rojo[3] = String.valueOf((int) (((Lx - Rx) * Math.sin(45) - (Ly - Ry) * Math.cos(45) - Ry)) * -1);
                        girarIzq();
                    } else if (Ox >= Lx && Oy <= Ly) {
                        Log.i("Info:", "Hacia adelante - Area 1");
                        movimiento.setText("Hacia adelante - Area 1");
                        dato_rojo[0] = String.valueOf(Rx + ((Rx - Lx) / 2));
                        dato_rojo[1] = String.valueOf(Ry - ((Ly - Ry) / 2));
                        dato_rojo[2] = String.valueOf(Lx + ((Rx - Lx) / 2));
                        dato_rojo[3] = String.valueOf(Ly - ((Ly - Ry) / 2));
                        hacia_adelante();
                    } else {

                        Log.i("Info:", "Hacia adelante - Area 1 *");
                        movimiento.setText("Hacia adelante - Area 1 *");
                        dato_rojo[0] = String.valueOf(Rx + 1);
                        dato_rojo[1] = String.valueOf(Ry + 1);
                        dato_rojo[2] = String.valueOf(Lx + 1);
                        dato_rojo[3] = String.valueOf(Ly + 1);
                        //hacia_adelante();
                    }


                }
                contador++;


                if (esRojo == true) {
                    dato.put("RobotRx", Rx);
                    dato.put("RobotRy", Ry);
                    dato.put("RobotRdx", Lx);
                    dato.put("RobotRdy", Ly);

                    dato.put("RobotNx", Ox);
                    dato.put("RobotNy", Oy);

                    nuevafecha.child(String.valueOf(houra)).setValue(actual);
                    nuevafecha.child(String.valueOf(houra)).setValue(dato);

                } else if (esAzul == true) {
                    dato.put("RobotAx", Rx);
                    dato.put("RobotAy", Ry);
                    dato.put("RobotAdx", Lx);
                    dato.put("RobotAdy", Ly);

                    dato.put("RobotNx", Ox);
                    dato.put("RobotNy", Oy);

                    nuevafecha.child(String.valueOf(houra)).setValue(dato);

                } else if (esVerde == true) {
                    dato.put("RobotVx", Rx);
                    dato.put("RobotVy", Ry);
                    dato.put("RobotVdx", Lx);
                    dato.put("RobotVdy", Ly);

                    dato.put("RobotNx", Ox);
                    dato.put("RobotNy", Oy);

                    nuevafecha.child(String.valueOf(houra)).setValue(dato);

                }


            } else {
                i = 1001;
                return;
            }
        } catch (Exception ex) {
            Log.e("ERROR", ex.getMessage());
        }
    }

    //calcular distancia entre el punto frontal del robot y las ruedas traseras
    public int distancia(int x1, int y1, int x2, int y2) {
        int distance;
        return distance = (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

    }
}

