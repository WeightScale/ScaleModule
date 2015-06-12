package com.konst.module;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/** Весовой модуль
 * @author Kostya
 */
public abstract class Module extends Handler {
    private static BluetoothDevice device;
    private static final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothSocket socket;
    private static OutputStream os;
    private static InputStream is;
    /** Константа время задержки для получения байта */
    static final int TIMEOUT_GET_BYTE = 2000;
    /** Константы результат соединения */
    public enum ResultConnect {
        /**Соединение и загрузка данных из весового модуля успешно*/
        STATUS_LOAD_OK,
        /** Неизвесная вервия весового модуля */
        STATUS_SCALE_UNKNOWN,
        /** Конец стадии присоединения (можно использовать для закрытия прогресс диалога) */
        STATUS_ATTACH_FINISH,
        /** Начало стадии присоединения (можно использовать для открытия прогресс диалога) */
        STATUS_ATTACH_START
    }
    /** Константы ошибок соединения */
    public enum ResultError{
        /** Ошибка настриек терминала */
        TERMINAL_ERROR,
        /** Ошибка настроек весового модуля */
        MODULE_ERROR,
        /** Ошибка соединения с модулем */
        CONNECT_ERROR
    }

    /** Сообщения о результате соединения.
     * Используется после вызова метода init()
     * @param what Результат соединения константа ResultConnect*/
    public abstract void handleResultConnect(ResultConnect what);
    /** Сообщения об ошибках соединения. Используется после вызоа метода init()
     * @param what Результат какая ошибака константа Error
     * @param error описание ошибки*/
    public abstract void handleConnectError(ResultError what, String error);

    /** Инициализация и соединение с весовым модулем.
     * @param device bluetooth устройство*/
    protected void init( BluetoothDevice device){
        this.device = device;
    }

    protected void init(String address) throws Exception {
        device = bluetoothAdapter.getRemoteDevice(address);
    }

    protected static synchronized String cmd(String cmd) { //послать команду и получить ответ
        try {
            synchronized (ScaleModule.class) {
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
            }

        } catch (IOException | InterruptedException ioe) {
        }

        try {
            connect();
        } catch (IOException e) {
        }
        return "";
    }

    private static synchronized void sendCommand(String cmd) throws IOException {
        os.write(cmd.getBytes());
        os.write((byte) 0x0D);
        os.write((byte) 0x0A);
        os.flush(); //что этот метод делает?
    }

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

    protected static synchronized void connect() throws IOException { //соединиться с весами
        disconnect();
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        is = socket.getInputStream();
        os = socket.getOutputStream();
    }

    protected static void disconnect() { //рассоединиться
        try {
            if (socket != null)
                socket.close();
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        } catch (IOException ioe) {
            socket = null;
            //return;
        }
        is = null;
        os = null;
        socket = null;
    }

    public static BluetoothDevice getDevice(){ return device; }

    public BluetoothAdapter getAdapter(){ return bluetoothAdapter; }

    /** Получаем версию весов из весового модуля
     * @return Версию весового модуля в текстовом виде*/
    public static String getModuleVersion() {
        return cmd(InterfaceVersions.CMD_VERSION);
    }


}
