package abaqueda.cl.cim_nxt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

/*
Codigo modificado de projectos de ejemplos de google:
http://developer.android.com/samples/BluetoothChat/src/com.example.android.bluetoothchat/DeviceListActivity.html
Esta clase busca los dispositivos bluetooth pareados para poder seleccionar alguno y conectarse a el
*/

public class SearchDeviceActivity extends Activity {

    public static String EXTRA_DEVICE_ADDRESS = "device_address"; //variable de tipo string que contiene la Dirección del dispositivo

    private ArrayAdapter<String> mPairedDevicesArrayAdapter; //Variable array de tipo string para dsipositivos pareados
    private ArrayAdapter<String> mNewDevicesArrayAdapter;//Variable array de tipo string para nuevos dispositivos
    private BluetoothAdapter mBtAdapter; //Variable de adaptador bluetooth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //configuración de la ventana
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        //Resultado si el usuario cancela
        setResult(Activity.RESULT_CANCELED);
        //Inicializa el botón para realizar la detección de dispositivos
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        //Inicializa los dipositivos pareados en un array y una variable para dispositivos nuevos
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        //Busca y encuentra la lista para los dispositivos pareados
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        //Encuentra y configura la lista de los nuevos dispositivos
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Registro para los dispositivos que han sido encontrado
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Registro para los dispositivos que han sido descubierto ha finalizado
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        //Se obtiene el adaptador de Bluetooth Local
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        //Se obtiene un conjunto de dispositivos pareados en la actualidad
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        //Si hay dispositivos pareados, se agrega cada uno al ArrayAdapter de dispositivos pareados
        boolean empty = true;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if ((device.getBluetoothClass() != null) && (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.TOY_ROBOT)) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    empty = false;
                }
            }
        }
        if (!empty) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            findViewById(R.id.no_devices).setVisibility(View.GONE);
        }
    }

    //Método para eliminar la exploración de nuevos dispositivos
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        this.unregisterReceiver(mReceiver);
    }

    // Método que comienza el descubrimiento de dispositivos con el adaptador bluetooth
    private void doDiscovery() {

       //Indica el título y la visibilidad de "Scanning"
        setProgressBarIndeterminateVisibility(true);
        setTitle("Scanning...");

        //findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

       // Si ya se esta descubriendo anteriormente se para
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Solicitud al adaptador bluetooth para descubrir dispositivos
        mBtAdapter.startDiscovery();
        // Se vacía el array de los nuevos dispositivos
        mNewDevicesArrayAdapter.clear();
        findViewById(R.id.title_new_devices).setVisibility(View.GONE);

        // Si el contador de los dispositivos pareados es 0, se, se muestra que no hay dispositivos
        if (mPairedDevicesArrayAdapter.getCount() == 0) {
            findViewById(R.id.no_devices).setVisibility(View.VISIBLE);
        }
    }

    // Listener que espera con un click para todos los dispositivos en la lista
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            //Se cancela el descubrimiento ya que puede ser muy costoso y se está a punto de conectarse
            mBtAdapter.cancelDiscovery();

            //Se obtiene la dirección MAC del dispositivo, el cual son los últimos caracteres en la vista(view)
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Se crea el resultado en Intent y se incluye la dirección MAC
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Se tiene y modifica el resultado y se finaliza esta actividad
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    //El BroadcastReceiver  escucha(o espera) por dispositivos descubiertos
    // y cambia el título cuando el descubrimiento es finalizado
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //Cuando el desubrimiento encuentra un dispositivo
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Obtiene el objeto de dispositivo bluetooth desde el indent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Si ya está pareado, lo omite, porque ya ha sido escuchado(listened)
                if ((device.getBondState() != BluetoothDevice.BOND_BONDED) && (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.TOY_ROBOT)) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
                    findViewById(R.id.no_devices).setVisibility(View.GONE);
                }

                //Cuando el descubrimiento está finalizado, cambia el título de la actividad
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("Select device");
                findViewById(R.id.button_scan).setVisibility(View.VISIBLE);
            }
        }
    };
}