/*
 * Copyright (c) 2010 Jacek Fedorynski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is derived from:
 * 
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChatService.html
 * 
 * Copyright (c) 2009 The Android Open Source Project
 */

package abaqueda.cl.cim_nxt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class NXTTalker {

    public static final int STATE_NONE = 0; //Variable constante que indica ningún estado
    public static final int STATE_CONNECTING = 1; //Variable constante que indica estado conectando
    public static final int STATE_CONNECTED = 2;  //Variable constante que indica el estado de ya conectado
    public static  int distancia=0;

    
    public static int mState; //Variable para el estado del movil

    // La clase android.os.Handler  es el puente que hay entre un hilo secundario (thread)
    // y el hilo principal (aplicación)  ya que el hilo no puede modificar ni insertar datos
    // en el hilo principal (aplicación) esto causaría error.
    public Handler mHandler; //Variable para el manejo o puente(handler) del movil
    public BluetoothAdapter mAdapter; //Variale para el adaptador del movil
    
    public  static ConnectThread mConnectThread;  //Variable para el hilo(thread) de conexión
    public static ConnectedThread mConnectedThread;//Variable para el hilo(thread) conectado
    private byte[] dato= {0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                          0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                          0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,


    };

    // variable de tipo byte para 12 bytes para almacenar los datos obtenidos del robot como por ejemplo los sensores
    public byte[] buffer = new byte[] {(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0};

    BluetoothSocket mmSocket; // Variable para el socket de bluetooth(conexion) del movil
    InputStream mmInStream;//Variable para la entrada de datos
    OutputStream mmOutStream; //Variable para la salida de datos
    DataInputStream mmInStream2;
    DataOutputStream mmOutStream2;

    //Método que obtiene al adaptador de bluetooth por defecto y no el estado por defecto es nulo
    public NXTTalker(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        setState(STATE_NONE);
    }


    private synchronized void setState(int state) {
        mState = state;
        //Si el manejador(handler) del movil no es nulo, entonces  se cambia su estado
        if (mHandler != null) {
            mHandler.obtainMessage(NXT.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        } else {
            //XXX
        }
    }

    //Método que obtiene el estado
    public synchronized int getState() {
        return mState;
    }

    //Método que cambia el puente(handler)
    public synchronized void setHandler(Handler handler) {
        mHandler = handler;
    }
    
     //Método para notificar al usuario
    private void toast(String text) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(NXT.MESSAGE_TOAST);

            // El objeto de tipo Bundle sirve para pasar datos entre varias actividades de android
            Bundle bundle = new Bundle();

            //A través de bundle se coloca el texto toast y se envía a través del handler
            bundle.putString(NXT.TOAST, text);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        } else {
            //XXX
        }
    }

    //Método para conectarse con dispositivo con bluetooth
    public synchronized void connect(BluetoothDevice device) {
        //Log.i("NXT", "NXTTalker.connect()");

        //Si el estado es conectando("connecting") y si el hilo de conectar("connect") no es nulo, este último se
        //cancela y la variable se vuelve nulo
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        //Si el hilo conectado("connected") no es nulo, éste se cancela y la variable se vuelve nulo
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Se crea el objeto del hilo conectar pasandose el dispositivo y se ejecuta("start") y se cambia su estado a concectando("connecting")
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        //Si el hilo de conectar("connect") no es nulo, este último se cancela y la variable se vuelve nulo
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        //Si el hilo de conectado("connected") no es nulo, este último se cancela y la variable se vuelve nulo
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Se crea un nuevo objeto junto pasandose el socket y éste objeto se ejecuta("start")
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        // Se ejecuta el método toast y se envía la notificación al usuario, ademas el estado cambia a conectado("connected")
        toast("Connected to " + device.getName());
        setState(STATE_CONNECTED);
    }

    // Método para parar la conexion con otro dispositivo
    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }
    
    //Método para la conexión fallida en el que cambia el estado a ninguno("none") y el toast es cambiado hacia el usuario
    private void connectionFailed() {
        setState(STATE_NONE);
        toast("Connection failed");
    }
    
    //Método para la conexión perdida en el que cambia el estado a ninguno("none") y el toast es cambiado hacia el usuario
    private void connectionLost() {
        setState(STATE_NONE);
        toast("Connection lost");
    }
    
    public void motors3(byte l, byte r, byte action, boolean speedReg, boolean motorSync) {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        
        //Log.i("NXT", "motors3: " + Byte.toString(l) + ", " + Byte.toString(r) + ", " + Byte.toString(action));
        data[5] = l;
        data[19] = r;
        data[33] = action;
        if (speedReg) {
            data[7] |= 0x01;
            data[21] |= 0x01;
        }
        if (motorSync) {
            data[7] |= 0x02;
            data[21] |= 0x02;
        }
        write(data);
    }


    public synchronized void reaccion_10cm() {
        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                      // 0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,

        };
        data[19] = (byte) 0;
        data[5] = (byte) 70;
        data[10] = (byte) 180;
        data[24] = (byte)0;
        for (int i = 1; i <= 4; i++) {
            //por cada  write(data) solo hace rotacion de 180°
            write(data);
        }
    }

    public synchronized void reaccion_15cm() {
        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };

        data[19] = (byte) 70;
        data[5] = (byte) 0;
        data[10] = (byte) 0;
        data[24] = (byte)180;
        for (int i = 1; i <= 4; i++) {
            //por cada  write(data) solo hace rotacion de 180°
            write(data);
        }
    }

    public synchronized void reaccion_20cm() {
        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[19] = (byte) 0;
        data[5] = (byte) 40;
        data[10] = (byte) 0;
        data[24] = (byte)180;
        for (int i = 1; i <= 4; i++) {
            //por cada  write(data) solo hace rotacion de 180°
            write(data);
        }
    }

    public synchronized void reaccion_25cm() {
        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[19] = (byte) 40;
        data[5] = (byte) 0;
        data[10] = (byte) 0;
        data[24] = (byte)180;

        for (int i = 1; i <= 4; i++) {
            //por cada  write(data) solo hace rotacion de 180°
            write(data);
        }
    }

    public synchronized  void rotacion_adelante() {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[5] = (byte) 12;
        data[19] = (byte) 12;
        data[10] = (byte) 180;
        data[24] = (byte) 180;
        for (int i = 1; i <= 6; i++) {
            //por cada  write(data) solo hace rotacion de 180°
               write(data);
        }
    }


    public void rotacion_atras() {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };

        //Grados de rotación del motor hasta 180 grados
        data[10]=(byte)180;
        data[24]=(byte)180;
        data[5] = (byte)-30;
        data[19] = (byte)-30;
        for(int i=1;i<=6;i++){
            //por cada  write(data) solo hace rotacion de 180°
            write(data);
        }
    }

    public void solo_andar() {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[5] = (byte)20;
        data[19] = (byte)20;
        write(data);
    }

    public void solo_andar_luz() {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[5] = (byte)12;
        data[19] = (byte)12;
        write(data);
    }

    public void luz_izquierda() {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[5] = (byte) 15;
        data[19] = (byte) 0;
        data[10] = (byte) 60;
       // data[24] = (byte) 180;
       // for (int i = 1; i <= 2; i++) {
            //por cada  write(data) solo hace rotacion de 180°
            write(data);
        //}
    }

    public void luz_derecha() {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[5] = (byte) 0;
        data[19] = (byte) 15;
        //data[10] = (byte) 180;
        data[24] = (byte) 60;
        //for (int i = 1; i <= 2; i++) {
            //por cada  write(data) solo hace rotacion de 180°
            write(data);
        //}
    }



    public void deshabilitar() {
        /*byte[] data = {0x0c, 0x00, (byte) 0x80, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x05, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x05, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        };*/

        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x05, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x05, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        };

        write(data);

    }

    public void tiempo_adelante_empezar(){
        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[5] = (byte)30;
        data[19] = (byte)30;
        write(data);
    }

    public void tiempo_parar(){
        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                       0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[5] = (byte)0;
        data[19] = (byte)0;
        write(data);
    }

    public void tiempo_atras_empezar(){
        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x00, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
        };
        data[5] = (byte)(-30);
        data[19] = (byte)(-30);
        write(data);
    }



    public void habilitar(){
        byte[] data = {0x0c, 0x00, (byte) 0x80, 0x05, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,//para sensor de color
                       0x0c, 0x00, (byte) 0x80, 0x05, 0x01, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,//sensor de tacto
                       0x0c, 0x00, (byte) 0x80, 0x05, 0x03, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,//sensor de luz
        };
        write(data);
    }

    public int opcion(int posicion){
        int hj[];
        if(posicion==0){
            return 5;
        }else{
        if(posicion==1){
            return 15;
        }else{
        if(posicion==2){
            return 25;
        }else{
        if(posicion==3){
            return 35;
        }else{
        if(posicion==4){
            return 45;
        }else{
        if(posicion==5){
            return 55;
        }else{
        if(posicion==6){
            return 65;
        }else{
        if(posicion==7){
            return 75;
        }else{
        if(posicion==8){
            return 85;
        }else{
        if(posicion==9){
            return 95;
        }}}}}}}}}}
        return 0;
    }



//Método que envia los comandos al robot
    private  void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        r.write(out);
    }

    private synchronized void write2(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }

        r.write2 (out);
    }

    public void write2(byte[] out, int off, int len) {
        ConnectedThread r;
        synchronized (this) {

            r = mConnectedThread;
        }
       // r.write2(out);
    }


    public byte[] read() {
        ConnectedThread r;

        synchronized (this) {
            if (mState == STATE_CONNECTED) {
                r = mConnectedThread;
                buffer = r.read();
            }
        }
        return buffer;
    }

    // Clase que  ejecuta el hilo para conectar("connect")
    public class ConnectThread extends Thread {
        // Se crea una variable socket para la comunicación entre los dispositivos y una variable de dispositivo bluetooth
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        // Constructor de la clase
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }

        //Método que se ejecuta de forma concurrente para conectarse entre dispositivos
        public void run() {
            setName("ConnectThread");
            mAdapter.cancelDiscovery();
            
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mmSocket.connect();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    connectionFailed();
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    return;
                }
            }
            
            synchronized (NXTTalker.this) {
                mConnectThread = null;
            }
            
            connected(mmSocket, mmDevice);
        }
        
        public void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //Método que se ejecuta de forma concurrente para estar ya conectado entre dispositivos
    public class ConnectedThread extends Thread {
        
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            DataInputStream tmpIn2 = null;
            DataOutputStream tmpOut2 = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mmInStream2 = tmpIn2;
            mmOutStream2 = tmpOut2;
        }
        
        public void run() {
            int bytes;
            
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }
        
        private  void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();

            } catch (IOException e) {
                e.printStackTrace();
                // XXX?
            }
        }

        private synchronized void write2(byte[] buffer) {
            try {
                mmOutStream2.write(buffer);
                mmOutStream2.flush();

            } catch (IOException e) {
                e.printStackTrace();
                // XXX?
            }
        }

        public byte[] read() {
            int bytes = 0;
            try {
                bytes = mmInStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                connectionLost();
            }
            return buffer;
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
