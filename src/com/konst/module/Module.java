package com.konst.module;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Весовой модуль
 *
 * @author Kostya
 */
public abstract class Module implements InterfaceVersions {
    Module module;
    /**
     * Bluetooth устройство модуля весов.
     */
    protected BluetoothDevice device;
    /**
     * Bluetooth адаптер терминала.
     */
    protected final BluetoothAdapter bluetoothAdapter;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected BluetoothSocket socket;
    protected BufferedReader bufferedReader;
    //protected OutputStreamWriter outputStreamWriter;
    protected BufferedWriter bufferedWriter;
    OnEventConnectResult onEventConnectResult;

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
    public abstract void attach() throws InterruptedException;

    /** Получаем соединение с bluetooth весовым модулем.
     * @throws IOException
     * @throws NullPointerException
     */
    public abstract void connect() throws IOException, NullPointerException;

    /** Получаем разьединение с bluetooth весовым модулем. */
    public abstract void disconnect();

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
        while (!bluetoothAdapter.isEnabled() && !flagTimeout) ; /* ждем включения bluetooth */
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
    public synchronized String command(Commands cmd) {
        try {
            sendCommand(cmd.toString());
            String substring = "";
            for (int i = 0; i < cmd.getTimeOut(); ++i) {
                Thread.sleep(1L);

                if (bufferedReader.ready()) {
                    substring = bufferedReader.readLine();
                    if(substring == null)
                        continue;
                    if (substring.startsWith(cmd.getName())){
                        substring = substring.replace(cmd.getName(),"");
                        return substring.isEmpty() ? cmd.getName() : substring;
                    }else
                        return "";
                }
            }
        } catch (Exception ioe) {
            try {
                connect();
            } catch (Exception e) {
                try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e1) { }
            }
        }
        return "";
    }

    /** Отправить команду.
     * @param cmd Команда.
     * @throws IOException
     */
    private synchronized void sendCommand(String cmd) throws IOException {

        bufferedWriter.write(cmd.toString());
        bufferedWriter.write("\r");
        bufferedWriter.newLine();
        bufferedWriter.flush();
        /*outputStreamWriter.write(cmd);
        outputStreamWriter.write("\r");
        outputStreamWriter.write("\n");
        outputStreamWriter.flush();*///что этот метод делает?
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
}
