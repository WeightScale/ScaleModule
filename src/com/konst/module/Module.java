package com.konst.module;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import com.konst.module.scale.ScaleModule;

import java.io.BufferedWriter;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Весовой модуль
 *
 * @author Kostya
 */
public abstract class Module implements InterfaceModule {
    /** Bluetooth устройство модуля весов. */
    protected BluetoothDevice device;
    /** Bluetooth адаптер терминала. */
    protected final BluetoothAdapter bluetoothAdapter;
    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected BluetoothSocket socket;
    protected BufferedReader bufferedReader;
    private Timer commandTimeout;
    protected BufferedWriter bufferedWriter;
    protected ConnectResultCallback connectResultCallback;

    /** Константы результат соединения.  */
    public enum ResultConnect {
        /** Соединение и загрузка данных из весового модуля успешно. */
        STATUS_LOAD_OK,
        /** Неизвесная вервия весового модуля. */
        STATUS_VERSION_UNKNOWN,
        /** Конец стадии присоединения (можно использовать для закрытия прогресс диалога). */
        STATUS_ATTACH_FINISH,
        /** Начало стадии присоединения (можно использовать для открытия прогресс диалога). */
        STATUS_ATTACH_START
    }

    /** Константы ошибок соединения. */
    public enum ResultError {
        /** Ошибка настриек терминала. */
        TERMINAL_ERROR,
        /** Ошибка настроек весового модуля. */
        MODULE_ERROR,
        /** Ошибка соединения с модулем. */
        CONNECT_ERROR
    }

    public abstract void dettach();
    public abstract void attach() throws InterruptedException;
    public abstract boolean isVersion();

    /** Получаем соединение с bluetooth весовым модулем.
     * @throws IOException
     * @throws NullPointerException
     */
    public abstract void connect() throws IOException, NullPointerException;

    /** Получаем разьединение с bluetooth весовым модулем. */
    public abstract void disconnect();

    private boolean flagTimeout;
    private final Handler handler = new Handler();

    /*protected Module() throws Exception{
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
        while (!bluetoothAdapter.isEnabled() && !flagTimeout) ; *//* ждем включения bluetooth *//*
        if(flagTimeout)
            throw new Exception("Timeout enabled bluetooth");
        Commands.setInterfaceCommand(this);
    }*/

    protected Module(BluetoothDevice device, ConnectResultCallback event)throws Exception{
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
        init(device);
        connectResultCallback = event;
        Commands.setInterfaceCommand(this);
    }

    protected Module(String device, ConnectResultCallback event) throws Exception{
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
        init(device);
        connectResultCallback = event;
        Commands.setInterfaceCommand(this);
    }

    public void setConnectResultCallback(ConnectResultCallback connectResultCallback) {
        this.connectResultCallback = connectResultCallback;
    }

    /** Инициализация bluetooth адаптера и модуля.
     * Перед инициализациеи надо создать класс com.kostya.module.ScaleModule
     * Для соединения {@link ScaleModule#attach()}
     * @param device bluetooth устройство.
     */
    private void init( BluetoothDevice device) throws Exception{
        if(device == null)
            throw new Exception("Bluetooth device is null ");
        this.device = device;
    }

    /** Инициализация и соединение с весовым модулем.
     *
     * @param address Адресс bluetooth.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    private void init(String address) throws Exception{
        device = bluetoothAdapter.getRemoteDevice(address);
    }

    /** Послать команду к модулю и получить ответ.
     *
     * @param commands Команда в текстовом виде. Формат [команда][параметр] параметр может быть пустым.
     *            Если есть парамет то обрабатывается параметр, иначе команда возвращяет параметр.
     * @return Имя команды или параметр. Если вернулась имя команды то посланый параметр обработан удачно. while((line = br.readLine()) != null)
     * Если вернулась пустая строка то команда не выполнена.
     * @see InterfaceModule
     */
    @Override
    public synchronized String command(Commands commands) {
        //Timer timer = new Timer();
        try {
            //timer.schedule(new TimerProcessCommandTimeout(), commands.getTimeOut(), commands.getTimeOut());
            startCommandTimeout(commands.getTimeOut());
            sendCommand(commands.toString());
            for (int i = 0; i < commands.getTimeOut(); ++i) {
            //while (true) {
                //condition.await();
                Thread.sleep(1L);
                if (bufferedReader.ready()) {
                    String substring = bufferedReader.readLine();
                    if(substring == null)
                        continue;
                    stopCommandTimeout();
                    if (substring.startsWith(commands.getName())){
                        substring = substring.replace(commands.getName(),"");
                        return substring.isEmpty() ? commands.getName() : substring;
                    }else
                        return "";
                }
            }
        } catch (Exception ioe) {
            try {
                stopCommandTimeout();
                connect();
            } catch (Exception e) {
                try { TimeUnit.SECONDS.sleep(2); } catch (InterruptedException e1) { }
            }
        }
        stopCommandTimeout();
        return "";
    }

    private void startCommandTimeout(int time){
        stopCommandTimeout();
        commandTimeout = new Timer();
        commandTimeout.schedule(new TimerProcessCommandTimeout(), time+500);
    }

    private void stopCommandTimeout(){
        if(commandTimeout != null){
            commandTimeout.cancel();
            commandTimeout.purge();
        }
    }

    private class TimerProcessCommandTimeout extends TimerTask {
        @Override
        public void run() {
            try {
                disconnect();
            } catch (Exception e) {
                try { TimeUnit.SECONDS.sleep(2); } catch (InterruptedException e1) { }
            }
        }
    }

    /** Отправить команду.
     * @param cmd Команда.
     * @throws IOException
     */
    private synchronized void sendCommand(String cmd) throws IOException {
        bufferedWriter.write(cmd);
        bufferedWriter.write("\r");
        //bufferedWriter.write("\n");
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    /** Получить bluetooth устройство модуля.
     *
     * @return bluetooth устройство.
     */
    public BluetoothDevice getDevice() {
        return device;
    }

    /** Получить bluetooth адаптер терминала.
     *
     * @return bluetooth адаптер.
     */
    public BluetoothAdapter getAdapter() {
        return bluetoothAdapter;
    }

    /** Получаем версию программы из весового модуля
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

    /** Получаем версию hardware весового модуля.
     *
     * @return Hardware версия весового модуля.
     * @see Commands#CMD_HARDWARE
     */
    public String getModuleHardware() {
        return Commands.CMD_HARDWARE.getParam();
    }

    /** Установить мощьность передатчика.
     * @param power
     * @return
     */
    public boolean setModulePower(int power) {
        return Commands.CMD_POWER.setParam(power);
    }

    public UUID getUuid() { return uuid; }
}
