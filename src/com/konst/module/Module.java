package com.konst.module;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Весовой модуль
 *
 * @author Kostya
 */
public abstract class Module extends Handler {
    /**
     * Bluetooth устройство модуля весов.
     */
    private static BluetoothDevice device;
    /**
     * Bluetooth адаптер терминала.
     */
    private static BluetoothAdapter bluetoothAdapter = null;
    private static BluetoothSocket socket;
    private static OutputStream os;
    private static InputStream is;
    OnEventConnectResult onEventConnectResult;
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
        STATUS_SCALE_UNKNOWN,
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
    Handler handler = new Handler();

    Module(OnEventConnectResult event) throws Exception{
        onEventConnectResult = event;
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
    protected static synchronized String cmd(String cmd) {
        try {
            int t = is.available();
            if (t > 0) {
                is.read(new byte[t]);
            }

            sendCommand(cmd);
            StringBuilder response = new StringBuilder();

            for (int i = 0; i < 400 && response.length() < 129; ++i) {
                Thread.sleep(1L);
                if (is.available() > 0) {
                    i = 0;
                    char ch = (char) is.read();
                    if (ch == '\uffff') {
                        connect();
                        break;
                    }
                    if (ch == '\r')
                        continue;
                    if (ch == '\n')
                        if (response.toString().startsWith(cmd.substring(0, 3)))
                            return response.replace(0, 3, "").toString().isEmpty() ? cmd.substring(0, 3) : response.toString();
                        else
                            return "";

                    response.append(ch);
                }
            }

        } catch (Exception ioe) {
            try {
                connect();
            } catch (IOException e) {
                try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e1) { }
            }
        }
        return "";
    }

    private static synchronized void sendCommand(String cmd) throws IOException {
        os.write(cmd.getBytes());
        os.write((byte) 0x0D);
        os.write((byte) 0x0A);
        os.flush(); //что этот метод делает?
    }

    /**
     * Послать байт.
     *
     * @param ch Байт для отсылки.
     * @return true - байт отослан без ошибки.
     */
    public static synchronized boolean sendByte(byte ch) {
        try {
            int t = is.available();
            if (t > 0) {
                is.read(new byte[t]);
            }
            os.write(ch);
            os.flush(); //что этот метод делает?
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
    public static synchronized int getByte() {

        try {
            for (int i = 0; i < TIMEOUT_GET_BYTE; i++) {
                if (is.available() > 0) {
                    return is.read(); //временный символ (байт)
                }
                Thread.sleep(1);
            }
            return 0;
        } catch (IOException | InterruptedException ioe) {
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
    protected static synchronized void connect() throws IOException {
        disconnect();
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        is = socket.getInputStream();
        os = socket.getOutputStream();
    }

    /**
     * Получаем разьединение с bluetooth весовым модулем
     */
    protected static void disconnect() { //рассоединиться
        try {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
            if (socket != null)
                socket.close();
        } catch (IOException ioe) {
            socket = null;
            //return;
        }
        is = null;
        os = null;
        socket = null;
    }

    /**
     * Получить bluetooth устройство модуля.
     *
     * @return bluetooth устройство.
     */
    public static BluetoothDevice getDevice() {
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
     * @see InterfaceVersions#CMD_VERSION
     */
    public static String getModuleVersion() {
        return cmd(InterfaceVersions.CMD_VERSION);
    }

}
