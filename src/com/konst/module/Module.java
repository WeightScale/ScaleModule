package com.konst.module;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Весовой модуль
 *
 * @author Kostya
 */
public abstract class Module extends Thread implements InterfaceVersions {
    Module module;
    /**
     * Bluetooth устройство модуля весов.
     */
    private BluetoothDevice device;
    /**
     * Bluetooth адаптер терминала.
     */
    private final BluetoothAdapter bluetoothAdapter;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket;
    private OutputStreamWriter mmOutputStreamWriter;
    private InputStream is;
    OnEventConnectResult onEventConnectResult;
    volatile StringBuilder response = new StringBuilder();
    /**
     * Константа время задержки для получения байта.
     */
    private static final int TIMEOUT_GET_BYTE = 2000;

    /**
     * Константы результат соединения
     */
    public enum ResultConnect {
        /**
         * Соединение и загрузка данных из весового модуля успешно
         */
        STATUS_LOAD_OK,
        /**
         * Неизвесная вервия весового модуля
         */
        STATUS_VERSION_UNKNOWN,
        /**
         * Конец стадии присоединения (можно использовать для закрытия прогресс диалога)
         */
        STATUS_ATTACH_FINISH,
        /**
         * Начало стадии присоединения (можно использовать для открытия прогресс диалога)
         */
        STATUS_ATTACH_START
    }

    /**
     * Константы ошибок соединения
     */
    public enum ResultError {
        /**
         * Ошибка настриек терминала
         */
        TERMINAL_ERROR,
        /**
         * Ошибка настроек весового модуля
         */
        MODULE_ERROR,
        /**
         * Ошибка соединения с модулем
         */
        CONNECT_ERROR
    }

    public abstract void dettach();
    public abstract void attach();

    boolean flagTimeout;
    final Handler handler = new Handler();

    protected Module() throws Exception{
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
            throw new Exception("Bluetooth adapter missing");
        bluetoothAdapter.enable();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!bluetoothAdapter.isEnabled())
                    flagTimeout = true;
            }
        }, 5000);
        while (!bluetoothAdapter.isEnabled() && !flagTimeout) ;//ждем включения bluetooth
        if(flagTimeout)
            throw new Exception("Timeout enabled bluetooth");
        Commands.setInterfaceCommand(this);
        module = this;
    }

    protected Module(OnEventConnectResult event) throws Exception{
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
            throw new Exception("Bluetooth adapter missing");
        bluetoothAdapter.enable();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!bluetoothAdapter.isEnabled())
                    flagTimeout = true;
            }
        }, 5000);
        while (!bluetoothAdapter.isEnabled() && !flagTimeout) ;//ждем включения bluetooth
        if(flagTimeout)
            throw new Exception("Timeout enabled bluetooth");
        onEventConnectResult = event;
        Commands.setInterfaceCommand(this);
        module = this;
    }

    public void setOnEventConnectResult(OnEventConnectResult onEventConnectResult) {
        this.onEventConnectResult = onEventConnectResult;
    }

    /** Инициализация bluetooth адаптера и модуля.
     * Перед инициализациеи надо создать класс com.kostya.module.ScaleModule
     * Для соединения {@link ScaleModule#attach()}
     * @param device bluetooth устройство.
     */
    public void init( BluetoothDevice device) throws Exception{
        if(device == null)
            throw new Exception("Bluetooth device is null ");
        this.device = device;
    }

    /**
     * Инициализация и соединение с весовым модулем.
     *
     * @param address Адресс bluetooth.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public void init(String address) throws Exception{
        device = bluetoothAdapter.getRemoteDevice(address);
    }

    /**
     * Послать команду к модулю и получить ответ
     *
     * @param cmd Команда в текстовом виде. Формат [команда][параметр] параметр может быть пустым.
     *            Если есть парамет то обрабатывается параметр, иначе команда возвращяет параметр.
     * @return Имя команды или параметр. Если вернулась имя команды то посланый параметр обработан удачно.
     * Если вернулась пустая строка то команда не выполнена.
     * @see InterfaceVersions
     */
    @Override
    public synchronized String cmd(Commands cmd) {
        try {

            sendCommand(cmd.toString());

        } catch (Exception ioe) {
            try {
                //connect();
            } catch (Exception e) {
                try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e1) { }
            }
        }
        return "";
    }

    @Override
    public String command(Commands cmd) {
        return null;
    }


    /**
     * Послать команду к модулю и получить ответ
     *
     * @param cmd Команда в текстовом виде. Формат [команда][параметр] параметр может быть пустым.
     *            Если есть парамет то обрабатывается параметр, иначе команда возвращяет параметр.
     * @return Имя команды или параметр. Если вернулась имя команды то посланый параметр обработан удачно.
     * Если вернулась пустая строка то команда не выполнена.
     * @see InterfaceVersions
     */
    /*@Override
    public synchronized String cmd(final Commands cmd) {
        final StringBuilder response = new StringBuilder();

        try {
            os = socket.getOutputStream();
            is = socket.getInputStream();
            sendCommand(cmd.toString());

            final Thread readThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] bytes = new byte[1024];
                    int numRead = 0;
                    try {
                        while ((numRead = is.read(bytes)) >= 0) {
                            response.append(new String(bytes, 0, numRead));
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }catch (StringIndexOutOfBoundsException e){
                        e.printStackTrace();
                    }

                }
            });

            synchronized (readThread){
                readThread.start();
                for (int i = 0; i < 10000; ++i) {
                    readThread.wait(1);
                    if(response.toString().indexOf('\n')!=-1){
                        break;
                    }
                }
                if(readThread.isAlive()) {
                    os.close();
                    is.close();
                    readThread.interrupt();
                }
            }
            return response.replace(0, 3, "").toString().isEmpty() ? cmd.getName() : response.toString();
        } catch (Exception ioe) {
            try {
                connect();
            } catch (Exception e) {
                try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e1) { }
            }
        }
        return "";
    }*/


    /** Отправить команду.
     * @param cmd Команда.
     * @throws IOException
     */
    private synchronized void sendCommand(String cmd) throws IOException {

        mmOutputStreamWriter.write(cmd);
        mmOutputStreamWriter.write("\r");
        mmOutputStreamWriter.write("\n");
        mmOutputStreamWriter.flush();//что этот метод делает?
        /*os.write(cmd.getBytes());
        os.write((byte) 0x0D);
        os.write((byte) 0x0A);
        os.flush(); */
    }

    /**
     * Послать байт.
     *
     * @param ch Байт для отсылки.
     * @return true - байт отослан без ошибки.
     */
    public synchronized boolean sendByte(byte ch) {
        try {
            int t = is.available();
            if (t > 0) {
                is.read(new byte[t]);
            }
            mmOutputStreamWriter.write(ch);
            mmOutputStreamWriter.flush();
            //os.write(ch);
            //os.flush(); //что этот метод делает?
            return true;
        } catch (IOException ioe) {
        }
        try {
            connect();
        } catch (IOException e) {
        }
        return false;
    }

    /**
     * Получить байт.
     *
     * @return Принятый байт.
     */
    public synchronized int getByte() {

        try {
            for (int i = 0; i < TIMEOUT_GET_BYTE; i++) {
                if (is.available() > 0) {
                    return is.read(); //временный символ (байт)
                }
                Thread.sleep(1);
            }
            return 0;
        } catch (Exception ioe) {
        }

        try {
            connect();
        } catch (IOException e) {
        }
        return 0;
    }

    /**
     * Получаем соединение с bluetooth весовым модулем.
     *
     * @throws IOException Ошибка соединения.
     */
    protected synchronized void connect() throws IOException, NullPointerException {
        disconnect();
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB)
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                try {
                    Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                    socket = (BluetoothSocket) m.invoke(device, 1);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            else
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);

        else
            socket = device.createRfcommSocketToServiceRecord(uuid);


        //socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        is = socket.getInputStream();
        //os = socket.getOutputStream();
        mmOutputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
    }

    /**
     * Получаем разьединение с bluetooth весовым модулем
     */
    protected void disconnect() { //рассоединиться
        try {
            if (is != null)
                is.close();
            if (mmOutputStreamWriter != null)
                mmOutputStreamWriter.close();
            /*if (os != null)
                os.close();*/
            if (socket != null)
                socket.close();
        } catch (IOException ioe) {
            socket = null;
            //return;
        }
        is = null;
        //os = null;
        mmOutputStreamWriter = null;
        socket = null;
    }

    /**
     * Получить bluetooth устройство модуля.
     *
     * @return bluetooth устройство.
     */
    public BluetoothDevice getDevice() {
        return device;
    }

    /**
     * Получить bluetooth адаптер терминала.
     *
     * @return bluetooth адаптер.
     */
    public BluetoothAdapter getAdapter() {
        return bluetoothAdapter;
    }

    /**
     * Получаем версию программы из весового модуля
     *
     * @return Версия весового модуля в текстовом виде.
     * @see Commands#CMD_VERSION
     */
    public String getModuleVersion() {
        return Commands.CMD_VERSION.getParam();
    }

    /** Возвращяем имя bluetooth утройства.
     * @return Имя bluetooth.
     */
    public CharSequence getNameBluetoothDevice() {
        return device.getName();
    }

    /**
     * Получаем версию hardware весового модуля.
     *
     * @return Hardware версия весового модуля.
     * @see Commands#CMD_HARDWARE
     */
    public String getModuleHardware() {
        return Commands.CMD_HARDWARE.getParam();
    }

    protected class ConnectClientThread extends Thread {
        private final BluetoothSocket mmSocket;
        //private final BluetoothDevice mmDevice;

        public ConnectClientThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            //mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
            socket = mmSocket;
            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
            module.start();
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}

    @Override
    public synchronized void start() {
        InputStream tmpIn = null;
        OutputStreamWriter tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = new OutputStreamWriter(socket.getOutputStream());
        } catch (IOException e) { }

        is = tmpIn;
        mmOutputStreamWriter = tmpOut;
        super.start();
    }

    @Override
    public void run() {
        byte[] bytes = new byte[1024];
        int numRead = 0;

        while (true) {
            try {
                while ((numRead = is.read(bytes)) >= 0) {
                    response.append(new String(bytes, 0, numRead));
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    /* Вызываем для передачи данных */
    protected void write(String bytes) {
        try {
            mmOutputStreamWriter.write(bytes);
        } catch (IOException e) { }
    }

    /* Вызываем для завершения соединения */
    protected void cancel() {
        try {
            socket.close();
        } catch (IOException e) { }
    }
}
