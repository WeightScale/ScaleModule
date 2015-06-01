package com.konst.module; /**
 * Copyright (c) 2015.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Главный класс для работы с весовым модулем. Инициализируем в теле программы. В абстрактных методах используем
 * возвращеные результаты после запуска метода init()
 * Пример:com.kostya.module.ScaleModule scaleModule = new com.kostya.module.ScaleModule();
 * scaleModule.init("version", "bluetooth");
 * @author Kostya
 */
//@TargetApi(Build.VERSION_CODES.ECLAIR)
public abstract class ScaleModule extends Handler {

    protected static BluetoothDevice device;
    protected static final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    protected static BluetoothSocket socket;
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
    public enum Error{
        /** Ошибка настриек терминала */
        TERMINAL_ERROR,
        /** Ошибка настроек весового модуля */
        MODULE_ERROR,
        /** Ошибка соединения с модулем */
        CONNECT_ERROR
    }
    /** Константы результата взвешивания */
    public enum ResultWeight {
        /** Значение веса неправильное */
        WEIGHT_ERROR,
        /** Значение веса в диапазоне весового модуля */
        WEIGHT_NORMAL,
        /** Значение веса в диапазоне лилима взвешивания*/
        WEIGHT_LIMIT,
        /** Значение веса в диапазоне перегрузки */
        WEIGHT_MARGIN
    }

    private static String version;
    private static int numVersion;

    /** Получаем класс загруженой версии весового модуля
     * @return класс версии весового модуля
     */
    public static Versions getVersion() { return Version; }
    protected static Versions Version;

    /** Получаем заряд батареи раннее загруженый в процентах
     * @return заряд батареи в процентах
     */
    public static int getBattery(){return battery;}
    /** Меняем ранне полученое значение заряда батареи весового модуля
     * @param battery Заряд батареи в процентах*/
    public static void setBattery(int battery) { ScaleModule.battery = battery; }
    /** Процент батареи (0-100%) */
    protected static int battery;

    /** Получаем значение веса погрешности для расчета атоноль
     * @return возвращяет значение веса
     */
    public static int getWeightError() { return weightError; }
    /** Сохраняем значение веса погрешности для расчета автоноль
     * @param weightError Значение погрешности в килограмах */
    public static void setWeightError(int weightError) { ScaleModule.weightError = weightError;  }
    /** Погрешность веса автоноль */
    protected static int weightError;

    /** Время для срабатывания автоноль
     * @return возвращяем время после которого установливается автоноль
     */
    public static int getTimerNull() { return timerNull; }
    /** Устонавливаем значение времени после которого срабатывает автоноль
     * @param timerNull Значение времени в секундах*/
    public static void setTimerNull(int timerNull) { ScaleModule.timerNull = timerNull; }
    /** Время срабатывания авто нуля */
    protected static int timerNull;


    /** Сообщения о результате соединения.
     * Используется после вызова метода init()
     * @param what Результат соединения константа ResultConnect*/
    public abstract void handleResultConnect(ResultConnect what);
    /** Сообщения об ошибках соединения. Используется после вызоа метода init()
     * @param what Результат какая ошибака константа Error
     * @param error описание ошибки*/
    public abstract void handleConnectError(Error what, String error);
    /** Инициализация и соединение с весовым модулем. Перед инициализациеи надо создать
     *  класс com.kostya.module.ScaleModule
     * @param moduleVersion Версия модуля для соединения
     * @param device bluetooth устройство*/
    public void init(String moduleVersion, BluetoothDevice device){
        version = moduleVersion;
        this.device = device;
        attach();
    }
    /** Инициализация и соединение с весовым модулем.
     * Перед инициализациеи надо создать класс com.kostya.module.ScaleModule
     * @param moduleVersion Версия модуля для соединения
     * @param address адресс bluetooth устройства*/
    public void init(String moduleVersion, String address) throws Exception {
        version = moduleVersion;
        device = bluetoothAdapter.getRemoteDevice(address);
        attach();
    }
    /** Отсоединение сесового модуля.
     * Необходимо использовать перед закрытием программы чтобы остановить работающие процессы */
    public void dettach() {
        if (isAttach()) {
            try {
                if (HandlerBatteryTemperature.measureBatteryTemperature != null) {
                    HandlerBatteryTemperature.measureBatteryTemperature.execute(false);
                    while (HandlerBatteryTemperature.measureBatteryTemperature.isStart()) ;
                }
                if (HandlerWeight.measureWeight != null) {
                    HandlerWeight.measureWeight.execute(false);
                    while (HandlerWeight.measureWeight.isStart()) ;
                }
            } catch (Exception e) {
            }
        }

        removeCallbacksAndMessages(null);
        disconnect();
    }

    /** Прверяем если весовой модуль присоеденен.
     * @return true если было присоединение и загрузка версии весового модуля
     */
    public static boolean isAttach() {
        return Version != null;
    }
    /** Определяем после соединения это весовой модуль и какой версии
     * указаной при инициализации класса com.kostya.module.ScaleModule.
     * @return true версия правильная
     * */
    public static boolean isScales() {
        String vrs = cmd(InterfaceVersions.CMD_VERSION); //Получаем версию весов
        if (vrs.startsWith(version)) {
            try {
                numVersion = Integer.valueOf(vrs.replace(version, ""));
                Version = selectVersion(numVersion);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    private void attach() {
        //final Throwable[] initException = {null};
        handleResultConnect(ResultConnect.STATUS_ATTACH_START);
        new ConnectThread().start();
        /*t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                //initException[0] = ex.getCause();
                handleModuleConnectError(HandlerScaleConnect.Result.STATUS_CONNECT_ERROR, ex.getCause().getMessage());
                handleConnectFinish();
            }
        });*/

        //obtainMessage(Result.STATUS_ATTACH_START.ordinal(), device.getName()).sendToTarget();
        //t.join();
        //if (initException[0] != null)
        //    throw initException[0];

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

    private static Versions selectVersion(int version) throws Exception {
        switch (version) {
            case 1:
                return new V1();
            case 4:
                return new V4();
            default:
                throw new Exception("illegal version");
        }
    }

    //==================================================================================================================
    /** Получаем версию весов из весового модуля
     * @return Версию весового модуля в текстовом виде*/
    public static String getModuleVersion() {
        return cmd(InterfaceVersions.CMD_VERSION);
    }
    /** Установливаем новое значение АЦП в весовом модуле. Знчение от1 до 15
     * @param  filterADC Значение АЦП от 1 до 15
     * @return true Значение установлено*/
    public static boolean setModuleFilterADC(int filterADC) {
        return cmd(InterfaceVersions.CMD_FILTER + filterADC).equals(InterfaceVersions.CMD_FILTER);
    }
    /** Получаем из весового модуля время выключения при бездействии устройства
     * @return время в минутах*/
    public static String getModuleTimeOff() {
        return cmd(InterfaceVersions.CMD_TIMER);
    }
    /** записываем в весовой модуль время выключения при бездействии устройства
     * @param timeOff Время в минутах
     * @return true Значение установлено*/
    public static boolean setModuleTimeOff(int timeOff) {
        return cmd(InterfaceVersions.CMD_TIMER + timeOff).equals(InterfaceVersions.CMD_TIMER);
    }

    /** Получаем значение скорости порта bluetooth модуля обмена данными.
     *  Значение от 1 до 5.
     *  1 - 9600bps.
     *  2 - 19200bps.
     *  3 - 38400bps.
     *  4 - 57600bps.
     *  5 - 115200bps.
     * @return Значение от 1 до 5.
     */
    public static String getModuleSpeedPort() {
        return cmd(InterfaceVersions.CMD_SPEED);
    }

    /** Устанавливаем скорость порта обмена данными bluetooth модуля.
     *  Значение от 1 до 5.
     *  1 - 9600bps.
     *  2 - 19200bps.
     *  3 - 38400bps.
     *  4 - 57600bps.
     *  5 - 115200bps.
     * @param speed Значение скорости.
     * @return true - Значение записано.
     */
    public static boolean setModuleSpeedPort(int speed) {
        return cmd(InterfaceVersions.CMD_SPEED + speed).equals(InterfaceVersions.CMD_SPEED);
    }

    public static String getModuleOffsetSensor() {
        return cmd(InterfaceVersions.CMD_GET_OFFSET);
    }

    public static boolean setModuleOffsetSensor() {
        return cmd(InterfaceVersions.CMD_SET_OFFSET).equals(InterfaceVersions.CMD_SET_OFFSET);
    }

    public static String getModuleSensor() {
        return cmd(InterfaceVersions.CMD_SENSOR);
    }

    /** Получаем значение заряда батерии.
     * @return Заряд батареи в процентах.
     */
    public static int getModuleBatteryCharge() {
        try {
            battery = Integer.valueOf(cmd(InterfaceVersions.CMD_BATTERY));
        } catch (Exception e) {
            battery = -0;
        }
        return battery;
    }

    /** Устанавливаем заряд батареи.
     *  Используется для калибровки заряда батареи.
     * @param charge Заряд батереи в процентах.
     * @return true - Заряд установлен.
     */
    public static boolean setModuleBatteryCharge(int charge) {
        return cmd(InterfaceVersions.CMD_CALL_BATTERY + charge).equals(InterfaceVersions.CMD_CALL_BATTERY);
    }
    /** Получаем значение температуры весового модуля.
     * @return Температура в градусах.
     */
    public static int getModuleTemperature() {
        try {
            return (int) ((float) ((Integer.valueOf(cmd(InterfaceVersions.CMD_DATA_TEMP)) - 0x800000) / 7169) / 0.81) - 273;
        } catch (Exception e) {
            return -273;
        }
    }
    /** Получаем версию hardware весового модуля.
     * @return Hardware версия весового модуля. */
    public static String getModuleHardware() {
        return cmd(InterfaceVersions.CMD_HARDWARE);
    }
    /** Устанавливаем имя весового модуля.
     * @param name Имя весового модуля.
     * @return true - Имя записано в модуль.*/
    public static boolean setModuleName(String name) {
        return cmd(InterfaceVersions.CMD_NAME + name).equals(InterfaceVersions.CMD_NAME);
    }
    /** Устанавливаем калибровку батареи.
     * @param percent Значение калибровки в процентах.
     * @return true - Калибровка прошла успешно.*/
    public static boolean setModuleCalibrateBattery(int percent) {
        return cmd(InterfaceVersions.CMD_CALL_BATTERY + percent).equals(InterfaceVersions.CMD_CALL_BATTERY);
    }
    /** Устанавливаем имя spreadsheet в google drive.
     * @param sheet Имя таблици.
     * @return true - Имя записано успешно.*/
    public static boolean setModuleSpreadsheet(String sheet) {
        return Version.setSpreadsheet(sheet);
    }
    /** Устанавливаем имя аккаунта в google.
     * @param username Имя аккаунта.
     * @return true - Имя записано успешно.*/
    public static boolean setModuleUserName(String username) { return Version.setUsername(username); }
    /** Устанавливаем пароль в google.
     * @param password Пароль аккаунта.
     * @return true - Пароль записано успешно.*/
    public static boolean setModulePassword(String password) {
        return Version.setPassword(password);
    }
    /** Устанавливаем номер телефона. Формат "+38хххххххххх"
     * @param phone Пароль аккаунта.
     * @return true - телефон записано успешно.*/
    public static boolean setModulePhone(String phone) {
        return Version.setPhone(phone);
    }


    /** Получить сохраненое значение фильтраАЦП.
     * @return Значение фильтра от 1 до 15.
     */
    public static int getFilterADC(){return Versions.filterADC;}
    public static void setFilterADC(int filterADC){ Versions.filterADC = filterADC; }

    public static int getWeightMax() { return Versions.weightMax; }
    public static void setWeightMax(int weightMax) { Versions.weightMax = weightMax; }

    public static int getLimitTenzo() { return Versions.limitTenzo; }
    public static void setLimitTenzo(int limitTenzo) { Versions.limitTenzo = limitTenzo; }

    public static String getPhone() { return Versions.phone; }
    public static void setPhone(String phone){ Versions.phone = phone; }

    public static int getTimeOff(){return Versions.timeOff;}
    public static void setTimeOff(int timeOff) { Versions.timeOff = timeOff; }


    public static float getCoefficientA() { return Versions.coefficientA; }
    public static void setCoefficientA(float coefficientA) { Versions.coefficientA = coefficientA; }

    public static float getCoefficientB() {   return Versions.coefficientB;  }
    public static void setCoefficientB(float coefficientB) {  Versions.coefficientB = coefficientB;  }

    public static String getSpreadSheet(){return Versions.spreadsheet;}
    public static void setSpreadSheet(String spreadSheet){ Versions.spreadsheet = spreadSheet; }

    public static String getUserName(){return Versions.username;}
    public static void setUserName(String userName){Versions.username = userName;}

    public static String getPassword(){return Versions.password;}
    public static void setPassword(String password) { Versions.password = password; }

    public static int getSensorTenzo() {return Version.getSensorTenzo(); }
    public static void setSensorTenzo(int sensorTenzo) { Versions.sensorTenzo = sensorTenzo; }

    public static int getWeightMargin() {  return Versions.weightMargin;  }
    public static void setWeightMargin(int weightMargin) { Versions.weightMargin = weightMargin;  }

    public static int getNumVersion() {
        return numVersion;
    }

    public static void setNumVersion(int version) {
        numVersion = version;
    }

    public static String getName() {
        return device.getName();
    }

    public static String getAddress() {
        return device.getAddress();
    }

    public static int getMarginTenzo() {
        return Versions.getMarginTenzo();
    }

    public static void load() throws Exception {
        Version.load();
    }

    public static boolean setOffsetScale() {
        return Version.setOffsetScale();
    }

    public static boolean isLimit() {
        return Version.isLimit();
    }

    public static boolean isMargin() {
        return Version.isMargin();
    }

    public static int updateWeight() {
        return Version.updateWeight();
    }

    public static boolean setScaleNull() {
        return Version.setScaleNull();
    }

    public static boolean writeData() {
        return Version.writeData();
    }

    private class ConnectThread extends Thread {

        @Override
        public void run() {
            try {
                connect();
                if (isScales()) {
                    try {
                        load();
                        handleResultConnect(ResultConnect.STATUS_LOAD_OK);
                    } catch (Versions.ErrorModuleException e) {
                        handleConnectError(Error.MODULE_ERROR, e.getMessage());
                    }catch (Versions.ErrorTerminalException e){
                        handleConnectError(Error.TERMINAL_ERROR, e.getMessage());
                    }catch (Exception e){
                        handleConnectError(Error.MODULE_ERROR, e.getMessage());
                    }
                } else {
                    disconnect();
                    handleResultConnect(ResultConnect.STATUS_SCALE_UNKNOWN);
                }
            } catch (IOException e) {
                handleConnectError(Error.CONNECT_ERROR, e.getMessage());
            }
            handleResultConnect(ResultConnect.STATUS_ATTACH_FINISH);
        }
    }

    /**
     * Класс для показаний батареи и температуры надо использевать после
     * создания класса com.kostya.module.ScaleModule и инициализации метода init().
     */
    public abstract static class HandlerBatteryTemperature {
        static MeasureBatteryTemperature measureBatteryTemperature;

        /** Метод посылает значения веса и датчика
         * @param battery результат заряд батареи в процентах
         * @param temperature результат температуры в градусах
         * @return возвращяет время для обновления показаний в секундах*/
        public abstract int handlerBatteryTemperature(int battery, int temperature);

        private class MeasureBatteryTemperature extends Thread {
            private boolean start;
            private boolean cancelled;
            /** счётчик автообнуления */
            private int autoNull;
            /** Время обновления в секундах*/
            public int timeUpdate = 1;

            @Override
            public synchronized void start() {
                //setPriority(Thread.MIN_PRIORITY);
                super.start();
                start = true;
            }

            @Override
            public void run() {
                while (!cancelled) {
                    timeUpdate = handlerBatteryTemperature(getModuleBatteryCharge(), getModuleTemperature());
                    try { TimeUnit.SECONDS.sleep(timeUpdate); } catch (InterruptedException ignored) { cancelled = true;  }
                    if (Versions.weight != Integer.MIN_VALUE && Math.abs(Versions.weight) < weightError) { //автоноль
                        autoNull += 1;
                        if (autoNull > timerNull / InterfaceVersions.DIVIDER_AUTO_NULL) {
                            setOffsetScale();
                            autoNull = 0;
                        }
                    } else {
                        autoNull = 0;
                    }
                }
                start = false;
            }

            private void cancel() {
                cancelled = true;
            }

            private void execute(boolean exe) {
                if (exe) {
                    if (!start)
                        start();
                } else
                    cancel();
            }

            private boolean isStart() {
                return start;
            }

            private void resetNull(){
                autoNull = 0;
            }

        }
        /** Метод запускает или останавливает процесс измерения
         * @param process true запускаем процесс false останавливаем
         */
        public void process(final boolean process) {
            try {
                if (isAttach()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (measureBatteryTemperature != null) {
                                measureBatteryTemperature.execute(false);
                                while (measureBatteryTemperature.isStart()) ;
                            }
                            measureBatteryTemperature = new MeasureBatteryTemperature();
                            measureBatteryTemperature.execute(process);
                        }
                    }).start();
                }

            } catch (Exception e) {
            }
        }

        public void resetAutoNull(){
            measureBatteryTemperature.resetNull();
        }
    }

    /**
     * Класс выводит показаний веса и значения датчика. Надо использевать после
     * создания класса com.kostya.module.ScaleModule и инициализации метода init()
     * абстрактный метод handlerWeight возвращяе показания.
     */
    public abstract static class HandlerWeight {
        static MeasureWeight measureWeight;
        /** Метод возвращяет значения веса и датчика
         * @param what результат статуса измерения enum ResultWeight
         * @param weight результат веса
         * @param sensor результат показаний датчика веса
         * @return возвращяет время для обновления показаний в милисикундах*/
        public abstract int handlerWeight(ResultWeight what, int weight, int sensor);

        private class MeasureWeight extends Thread { //поток получения батареи
            //final HandlerWeightUpdate h;
            private boolean start;
            private boolean cancelled;
            public int timeUpdate = 50;

            @Override
            public synchronized void start() {
                //setPriority(Thread.MIN_PRIORITY);
                super.start();
                start = true;
            }

            @Override
            public void run() {
                while (!cancelled) {
                    updateWeight();
                    ResultWeight msg;
                    if (Versions.weight == Integer.MIN_VALUE) {
                        msg = ResultWeight.WEIGHT_ERROR;
                    } else {
                        if (isLimit())
                            msg = isMargin() ? ResultWeight.WEIGHT_MARGIN : ResultWeight.WEIGHT_LIMIT;
                        else {
                            msg = ResultWeight.WEIGHT_NORMAL;
                        }
                    }
                    timeUpdate = handlerWeight(msg, Versions.weight, getSensorTenzo());
                    try { Thread.sleep(timeUpdate); } catch (InterruptedException ignored) { cancelled = true; }
                }
                start = false;
            }

            private void cancel() {
                cancelled = true;
            }

            private void execute(boolean exe) {
                if (exe) {
                    if (!start)
                        start();
                } else
                    cancel();
            }

            private boolean isStart() {
                return start;
            }
        }
        /** Метод запускает или останавливает процесс измерения
         * @param process true запускаем процесс false останавливаем
         */
        public void process(final boolean process) {
            try {
                if (isAttach()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (measureWeight != null) {
                                measureWeight.execute(false);
                                while (measureWeight.isStart()){}
                            }
                            measureWeight = new MeasureWeight();
                            measureWeight.execute(process);
                        }
                    }).start();
                }

            } catch (Exception e) {
            }
        }

    }
}
