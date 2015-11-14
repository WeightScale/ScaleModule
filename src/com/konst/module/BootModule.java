package com.konst.module;


import java.io.*;

/**
 * Класс для самопрограммирования весового модуля.
 * @author Kostya
 */
public class BootModule extends Module {
    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private OutputStream outputStream;
    public RunnableBootConnect runnableBootConnect;
    String versionName = "";
    /**
     * Константа время задержки для получения байта.
     */
    private static final int TIMEOUT_GET_BYTE = 2000;

    /** Конструктор модуля бутлодера.
     * @param version Верситя бутлодера.
     */
    public BootModule(String version, OnEventConnectResult event)throws Exception{
        super(event);
        runnableBootConnect = new RunnableBootConnect();
        versionName = version;

    }

    @Override
    public void attach(){
        onEventConnectResult.handleResultConnect(ResultConnect.STATUS_ATTACH_START);
        new Thread(runnableBootConnect).start();
    }

    /**
     * Разьеденится с загрузчиком.
     * Вызывать этот метод при закрытии программы.
     */
    @Override
    public void dettach(){
        //removeCallbacksAndMessages(null);todo проверка без handel
        disconnect();
    }

    /**
     * Получаем соединение с bluetooth весовым модулем.
     * @throws IOException Ошибка соединения.
     */
    @Override
    public synchronized void connect() throws IOException, NullPointerException {
        disconnect();
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB)
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
        else
            socket = device.createRfcommSocketToServiceRecord(uuid);
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        inputStream = socket.getInputStream();
        inputStreamReader = new InputStreamReader(inputStream);
        bufferedReader = new BufferedReader(inputStreamReader);
        outputStream = socket.getOutputStream();
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
    }

    /**
     * Получаем разьединение с bluetooth весовым модулем
     */
    @Override
    public void disconnect() {
        try {
            if(inputStreamReader != null)
                inputStreamReader.close();
            if(inputStream != null)
                inputStream.close();
            if (bufferedWriter != null)
                bufferedWriter.close();
            if (outputStream != null)
                outputStream.close();
            if (socket != null)
                socket.close();
        } catch (IOException ioe) {
            socket = null;
        }
        inputStream = null;
        inputStreamReader =  null;
        outputStream = null;
        bufferedWriter = null;
        socket = null;
    }

    /**
     * Послать байт.
     *
     * @param ch Байт для отсылки.
     * @return true - байт отослан без ошибки.
     */
    public synchronized boolean sendByte(byte ch) {
        try {
            while (inputStreamReader.ready()){
                inputStreamReader.read();
            }
            outputStream.write(ch);
            outputStream.flush();
            //os.write(ch);
            //os.flush(); //что этот метод делает?
            return true;
        } catch (IOException ioe) {}
        try {
            connect();
        } catch (IOException e) {}
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
                if(inputStreamReader.ready()){
                    return inputStream.read();
                }
                Thread.sleep(1);
            }
            return 0;
        } catch (Exception ioe) {}

        try {
            connect();
        } catch (IOException e) {}
        return 0;
    }

    /** Обработчик для процесса соединения
     */
    private class RunnableBootConnect implements Runnable{

        @Override
        public void run() {
            try {
                connect();
                if(isBootloader()){
                    onEventConnectResult.handleResultConnect(ResultConnect.STATUS_LOAD_OK);
                }else {
                    disconnect();
                    onEventConnectResult.handleResultConnect(ResultConnect.STATUS_VERSION_UNKNOWN);
                }

            } catch (IOException e) {
                onEventConnectResult.handleConnectError(ResultError.CONNECT_ERROR, e.getMessage());
            }
            onEventConnectResult.handleResultConnect(ResultConnect.STATUS_ATTACH_FINISH);
        }
    }

    /**
     * Комманда старт программирования.
     * Версия 2 и выше.
     * @return true - Запущено программирование.
     */
    public boolean startProgramming() {
        return Commands.CMD_START_PROGRAM.getParam().equals(Commands.CMD_START_PROGRAM.getName());
    }

    /**
     * Получить код микроконтролера.
     * Версия 2 и выше.
     * @return Код в текстовом виде.
     */
    public String getPartCode() {
        return Commands.CMD_PART_CODE.getParam();
    }

    /**
     * Получить версию загрузчика.
     *
     * @return Номер версии.
     */
    public int getBootVersion() {
        String vrs = getModuleVersion();
        if (vrs.startsWith(versionName)) {
            try {
                return Integer.valueOf(vrs.replace(versionName, ""));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Определяем имя после соединения это бутлоадер модуль.
     * Указывается имя при инициализации класса com.kostya.module.BootModule.
     *
     * @return true Имя совпадает.
     */
    public boolean isBootloader() {
        String vrs = getModuleVersion(); //Получаем версию весов
        return vrs.startsWith(versionName);
    }

}
