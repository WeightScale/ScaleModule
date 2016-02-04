/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.konst.module.scale;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import com.konst.module.*;

import java.io.*;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Главный класс для работы с весовым модулем. Инициализируем в теле программы. В абстрактных методах используем
 * возвращеные результаты после запуска метода {@link ScaleModule#init(String)}.
 * Пример:
 * com.kostya.module.ScaleModule scaleModule = new com.kostya.module.ScaleModule("version module");
 * scaleModule.init("bluetooth device");
 * @author Kostya
 */
public class ScaleModule extends Module {
    private InterfaceScaleVersion version;
    private ThreadWeight threadWeight;
    private ThreadBatteryTemperature threadBatteryTemperature;
    /** Процент заряда батареи (0-100%). */
    private int battery;
    /** Температура в целсиях. */
    private int temperature;
    /** Погрешность веса автоноль. */
    protected int weightError;
    /** Счётчик автообнуления. */
    private int autoNull;
    /** Время срабатывания авто ноля. */
    protected int timerNull;
    /** Номер версии программы. */
    private int numVersion;
    /** Имя версии программы */
    private final String versionName;
    /** АЦП-фильтр (0-15). */
    private int filterADC;
    /** Время выключения весов. */
    private int timeOff;
    /** Скорость порта. */
    private int speedPort;
    /** Имя таблици google disk. */
    protected String spreadsheet;
    /** Имя акаунта google.*/
    protected String username;
    /** Пароль акаунта google.*/
    protected String password;
    /** Номер телефона. */
    protected String phone;
    /** Калибровочный коэффициент a. */
    private float coefficientA;
    /** Калибровочный коэффициент b. */
    private float coefficientB;
    /** Текущее показание датчика веса. */
    private int sensorTenzo;
    /** Максимальное показание датчика. */
    private int limitTenzo;
    /** Предельный вес взвешивания. */
    private int weightMargin;
    /** Делитель для авто ноль. */
    private final int DIVIDER_AUTO_NULL = 3;
    /** Флаг использования авто обнуленияю. */
    private boolean enableAutoNull = true;
    /** Константы результата взвешивания. */
    public enum ResultWeight {
        /** Значение веса неправильное. */
        WEIGHT_ERROR,
        /** Значение веса в диапазоне весового модуля. */
        WEIGHT_NORMAL,
        /** Значение веса в диапазоне лилита взвешивания. */
        WEIGHT_LIMIT,
        /** Значение веса в диапазоне перегрузки. */
        WEIGHT_MARGIN
    }

    /** Обратный вызов результата измерения веса. */
    public interface WeightCallback{
        /** Результат веса.
         * @param what Статус веса {@link ResultWeight}
         * @param weight Значение веса в килограммах.
         * @param sensor Значение датчика веса.
         */
        void weight(ResultWeight what, int weight, int sensor);
    }

    /** Обратный вызов результат измерения батареи и температуры модуля. */
    public interface BatteryTemperatureCallback{
        /** Результат измерения.
         * @param battery Значение заряда батареи модуля в процентах.
         * @param temperature Значение температуры модуля в градусах целсия.
         */
        void batteryTemperature(int battery, int temperature);
    }

    /** Конструктор класса весового модуля.
     * @param moduleVersion Имя и номер версии в формате [[Имя][Номер]].
     * @throws Exception Ошибка при создании модуля.
     */
    public ScaleModule(String moduleVersion, BluetoothDevice device, ConnectResultCallback event) throws Exception {
        super(device, event);
        versionName = moduleVersion;
        attach();
    }

    /** Конструктор весового модуля.
     * @param moduleVersion Имя и номер версии в формате [[Имя][Номер]].
     * @param event Обратный вызов результата соединения с весовым модулем {@link ConnectResultCallback}.
     * @throws Exception Ошибка при создании модуля.
     */
    public ScaleModule(String moduleVersion, String bluetoothDevice, ConnectResultCallback event) throws Exception {
        super(bluetoothDevice, event);
        versionName = moduleVersion;
        attach();
    }

    /** Соединится с модулем. */
    @Override
    public void attach() /*throws InterruptedException*/ {
        new RunnableScaleAttach();
    }

    /** Определяем после соединения это весовой модуль и какой версии.
     * Проверяем версию указаной при инициализации класса com.kostya.module.ScaleModule.
     * @return true - Версия правильная.
     */
    @Override
    public boolean isVersion() {
        String vrs = getModuleVersion(); //Получаем версию весов
        if (vrs.startsWith(versionName)) {
            try {
                numVersion = Integer.valueOf(vrs.replace(versionName, ""));
                //setVersion(fetchVersion(numVersion));
                version = fetchVersion(numVersion);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /** Отсоединение от весового модуля.
     * Необходимо использовать перед закрытием программы чтобы остановить работающие процессы
     */
    @Override
    public void dettach() {
        stopMeasuringWeight();
        stopMeasuringBatteryTemperature();
        disconnect();
        //version = null;
    }

    /** Создание соединения с bluetooth.
     * @throws IOException
     * @throws NullPointerException
     */
    @Override
    public synchronized void connect() throws IOException, NullPointerException {
        disconnect();
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
            socket = device.createInsecureRfcommSocketToServiceRecord(getUuid());
        else
            socket = device.createRfcommSocketToServiceRecord(getUuid());
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        //outputStreamWriter = new OutputStreamWriter(socket.getOutputStream()/*, "UTF-8"*/);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
    }

    /** Разсоединение с bluetooth. */
    @Override
    public void disconnect() {
        try {
            if (socket != null)
                socket.close();
            if(bufferedReader != null)
                bufferedReader.close();
            if(bufferedWriter != null)
                bufferedWriter.close();
        } catch (IOException ioe) {
            socket = null;
        }
        bufferedReader =  null;
        bufferedWriter = null;
        socket = null;
    }

    /** Определяем версию весов.
     * @param version Имя версии.
     * @return Экземпляр версии.
     * @throws Exception
     */
    private InterfaceScaleVersion fetchVersion(int version) throws Exception {
        switch (version) {
            case 1:
                return new ScaleVersion1(this);
            case 4:
                return new ScaleVersion4(this);
            default:
                throw new Exception("illegal version");
        }
    }

    /** Получаем класс загруженой версии весового модуля.
     * @return класс версии весового модуля.
     */
    public InterfaceScaleVersion getVersion() {
        return version;
    }

    /** Получаем значение веса погрешности для расчета атоноль.
     * @return возвращяет значение веса.
     */
    public int getWeightError() {
        return weightError;
    }

    /** Сохраняем значение веса погрешности для расчета автоноль.
     * @param weightError Значение погрешности в килограмах.
     */
    public void setWeightError(int weightError) {
        this.weightError = weightError;
    }

    /** Время для срабатывания автоноль.
     * @return возвращяем время после которого установливается автоноль.
     */
    public int getTimerNull() {
        return timerNull;
    }

    /** Устонавливаем значение времени после которого срабатывает автоноль.
     * @param timerNull Значение времени в секундах.
     */
    public void setTimerNull(int timerNull) {
        this.timerNull = timerNull;
    }


    //==================================================================================================================

    /** Установливаем сервис код.
     *
     * @param cod Код.
     * @return true Значение установлено.
     * @see Commands#CMD_SERVICE_COD
     */
    public boolean setModuleServiceCod(String cod) {
        return Commands.CMD_SERVICE_COD.setParam(cod);
    }

    /** Получаем сервис код.
     * @return код
     * @see Commands#CMD_SERVICE_COD
     */
    public String getModuleServiceCod() {
        return Commands.CMD_SERVICE_COD.getParam();
        //return cmd(InterfaceVersions.CMD_SERVICE_COD);
    }

    public int getNumVersion() { return numVersion; }

    public String getAddressBluetoothDevice() { return getDevice().getAddress(); }

    public String getUserName() { return username; }

    public int getMarginTenzo() { return getVersion().getMarginTenzo(); }

    public int getLimitTenzo(){ return limitTenzo; }

    public int getWeightMax(){ return getVersion().getWeightMax(); }

    public int getTemperature() {return temperature; }

    /** Установливаем новое значение АЦП в весовом модуле. Знчение от1 до 15.
     * @param filterADC Значение АЦП от 1 до 15.
     * @return true Значение установлено.
     * @see Commands#CMD_FILTER
     */
    public boolean setModuleFilterADC(int filterADC) {
        if(Commands.CMD_FILTER.setParam(filterADC)){
            this.filterADC = filterADC;
            return true;
        }
        return false;
    }

    /** Получаем из весового модуля время выключения при бездействии устройства.
     * @return время в минутах.
     * @see Commands#CMD_TIMER
     */
    /*public String getModuleTimeOff() {
        return Commands.CMD_TIMER.getParam();
    }*/

    /** Записываем в весовой модуль время бездействия устройства.
     * По истечению времени модуль выключается.
     * @param timeOff Время в минутах.
     * @return true Значение установлено.
     * @see Commands#CMD_TIMER
     */
    public boolean setModuleTimeOff(int timeOff) {
        if(Commands.CMD_TIMER.setParam(timeOff)){
            this.timeOff = timeOff;
            return true;
        }
        return false;
    }

    /** Получаем значение скорости порта bluetooth модуля обмена данными.
     * Значение от 1 до 5.
     * 1 - 9600bps.
     * 2 - 19200bps.
     * 3 - 38400bps.
     * 4 - 57600bps.
     * 5 - 115200bps.
     *
     * @return Значение от 1 до 5.
     * @see Commands#CMD_SPEED
     */
    public String getModuleSpeedPort() {
        return Commands.CMD_SPEED.getParam();
    }

    /** Устанавливаем скорость порта обмена данными bluetooth модуля.
     * Значение от 1 до 5.
     * 1 - 9600bps.
     * 2 - 19200bps.
     * 3 - 38400bps.
     * 4 - 57600bps.
     * 5 - 115200bps.
     *
     * @param speed Значение скорости.
     * @return true - Значение записано.
     * @see Commands#CMD_SPEED
     */
    public boolean setModuleSpeedPort(int speed) {
        return Commands.CMD_SPEED.setParam(speed);
    }

    /** Получить офсет датчика веса.
     * @return Значение офсет.
     * @see Commands#CMD_GET_OFFSET
     */
    public String getModuleOffsetSensor() {
        return Commands.CMD_GET_OFFSET.getParam();
    }

    /** Получить значение датчика веса.
     * @return Значение датчика.
     * @see Commands#CMD_SENSOR
     */
    public String feelWeightSensor() {
        return Commands.CMD_SENSOR.getParam();
    }

    /** Получаем значение заряда батерии.
     * @return Заряд батареи в процентах.
     * @see Commands#CMD_BATTERY
     */
    private int getModuleBatteryCharge() {
        try {
            battery = Integer.valueOf(Commands.CMD_BATTERY.getParam());
        } catch (Exception e) {
            battery = -0;
        }
        return battery;
    }

    /** Устанавливаем заряд батареи.
     * Используется для калибровки заряда батареи.
     * @param charge Заряд батереи в процентах.
     * @return true - Заряд установлен.
     * @see Commands#CMD_CALL_BATTERY
     */
    public boolean setModuleBatteryCharge(int charge) {
        if(Commands.CMD_CALL_BATTERY.setParam(charge)){
            battery = charge;
            return true;
        }
        return false;
    }

    /** Получаем значение температуры весового модуля.
     * @return Температура в градусах.
     * @see Commands#CMD_DATA_TEMP
     */
    private int getModuleTemperature() {
        try {
            return (int) ((float) ((Integer.valueOf(Commands.CMD_DATA_TEMP.getParam()) - 0x800000) / 7169) / 0.81) - 273;
        } catch (Exception e) {
            return -273;
        }
    }

    /** Устанавливаем имя весового модуля.
     * @param name Имя весового модуля.
     * @return true - Имя записано в модуль.
     * @see Commands#CMD_NAME
     */
    public boolean setModuleName(String name) { return Commands.CMD_NAME.setParam(name);}

    /** Устанавливаем калибровку батареи.
     * @param percent Значение калибровки в процентах.
     * @return true - Калибровка прошла успешно.
     * @see Commands#CMD_CALL_BATTERY
     */
    public boolean setModuleCalibrateBattery(int percent) {
        return Commands.CMD_CALL_BATTERY.setParam(percent);
    }

    /**Установить обнуление.
     * @return true - Обнуление установлено.
     */
    public synchronized boolean setOffsetScale() { //обнуление
        return Commands.CMD_SET_OFFSET.getParam().equals(Commands.CMD_SET_OFFSET.getName());
    }

    /** Устанавливаем имя spreadsheet google drive в модуле.
     * @param sheet Имя таблици.
     * @return true - Имя записано успешно.
     */
    public boolean setModuleSpreadsheet(String sheet) {
        if (Commands.CMD_SPREADSHEET.setParam(sheet)){
            spreadsheet = sheet;
            return true;
        }
        return false;
    }

    /** Устанавливаем имя аккаунта google в модуле.
     * @param username Имя аккаунта.
     * @return true - Имя записано успешно.
     */
    public boolean setModuleUserName(String username) {
        if (Commands.CMD_G_USER.setParam(username)){
            this.username = username;
            return true;
        }
        return false;
    }

    /** Устанавливаем пароль в google.
     * @param password Пароль аккаунта.
     * @return true - Пароль записано успешно.
     */
    public boolean setModulePassword(String password) {
        if (Commands.CMD_G_PASS.setParam(password)){
            this.password = password;
            return true;
        }
        return false;
    }

    /** Устанавливаем номер телефона. Формат "+38хххххххххх".
     * @param phone Номер телефона.
     * @return true - телефон записано успешно.
     */
    public boolean setModulePhone(String phone) {
        if(Commands.CMD_PHONE.setParam(phone)){
            this.phone = phone;
            return true;
        }
        return false;
    }

    /** Выключить питание модуля.
     * @return true - питание модкля выключено.
     */
    public boolean powerOff() {
        return Commands.CMD_POWER_OFF.getParam().equals(Commands.CMD_POWER_OFF.getName());
    }

    /** Получить сохраненое значение фильтраАЦП.
     * @return Значение фильтра от 1 до 15.
     */
    public int getFilterADC() {
        return filterADC;
    }

    /** Установить значение фильтра АЦП.
     * @param filterADC Значение АЦП.*/
    public void setFilterADC(int filterADC) {
        this.filterADC = filterADC;
    }

    /*public int getWeightMax() {
        return version.weightMax;
    }*/

    public void setWeightMax(int weightMax) {
        getVersion().setWeightMax(weightMax);
    }

    public float getCoefficientA() { return coefficientA; }

    public void setCoefficientA(float coefficientA) {
        this.coefficientA = coefficientA;
    }

    public float getCoefficientB() {
        return coefficientB;
    }

    public void setCoefficientB(float coefficientB) {
        this.coefficientB = coefficientB;
    }

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }

    /*public int getLimitTenzo() {
        return version.limitTenzo;
    }*/

    public void setLimitTenzo(int limitTenzo) {
        this.limitTenzo = limitTenzo;
    }

    public int getTimeOff() {
        return timeOff;
    }

    public void setTimeOff(int timeOff) {
        this.timeOff = timeOff;
    }

    public String getSpreadSheet() {
        return spreadsheet;
    }

    public void setSensorTenzo(int sensorTenzo) {
        this.sensorTenzo = sensorTenzo;
    }

    public int getSensorTenzo() {
        return sensorTenzo;
    }

    public int getWeightMargin() {
        return weightMargin;
    }

    public void setWeightMargin(int weightMargin) {
        this.weightMargin = weightMargin;
    }

    public int getSpeedPort() {
        return speedPort;
    }

    public void setSpeedPort(int speedPort) {
        this.speedPort = speedPort;
    }

    /*public void setPhone(String phone) {
        version.phone = phone;
    }



    public void setSpreadSheet(String spreadSheet) {
        version.spreadsheet = spreadSheet;
    }



    public void setUserName(String userName) {
        version.username = userName;
    }



    public void setPassword(String password) {
        version.password = password;
    }









    public void setNumVersion(int version) {
        numVersion = version;
    }





    public int getMarginTenzo() {
        return version.getMarginTenzo();
    }

    public boolean isEnableAutoNull() {
        return enableAutoNull;
    }*/

    public void setEnableAutoNull(boolean enableAutoNull) {this.enableAutoNull = enableAutoNull;}

    public void startMeasuringWeight(WeightCallback weightCallback){
        if(threadWeight != null)
            if(threadWeight.isAlive()){
                threadWeight.setResultMeasuring(weightCallback);
                return;
            }

        threadWeight = new ThreadWeight(weightCallback);
        //threadWeight.setPriority(Thread.MAX_PRIORITY);
        //threadAutoWeight.setDaemon(true);
        threadWeight.start();
    }

    public void stopMeasuringWeight(){
        if(threadWeight != null){
            threadWeight.setRunning(false);
            boolean retry = true;
            while(retry){
                try {
                    threadWeight.cancel();
                } catch (InterruptedException | NullPointerException e) {}
                retry = false;
            }
        }
    }

    public void startMeasuringBatteryTemperature(BatteryTemperatureCallback callback){
        if(threadBatteryTemperature != null)
            if(threadBatteryTemperature.isAlive()){
                threadBatteryTemperature.setResultMeasuring(callback);
                return;
            }
        threadBatteryTemperature = new ThreadBatteryTemperature(callback);
        //threadAutoWeight.setDaemon(true);
        threadBatteryTemperature.start();
    }

    public void stopMeasuringBatteryTemperature(){
        if(threadBatteryTemperature != null){
            threadBatteryTemperature.setRunning(false);
            boolean retry = true;
            while(retry){
                try {
                    threadBatteryTemperature.cancel();
                } catch (InterruptedException | NullPointerException e) {}
                retry = false;
            }
        }
    }

    public boolean writeData() {
        return getVersion().writeData();
    }

    /*public void load() throws Exception {
        version.load();
    }*/

    /*public boolean setOffsetScale() {
        return version.setOffsetScale();
    }

    public boolean isLimit() {
        return version.isLimit();
    }

    public boolean isMargin() {
        return version.isMargin();
    }

    public int updateWeight() {
        try {
            return version.updateWeight();
        }catch (Exception e){}
        return 0;
    }

    public boolean setScaleNull() {
        return version.setScaleNull();
    }





    /*public void setWeightCallback(WeightCallback weightCallback) {
        processWeight.setResultMeasuring(weightCallback);
    }*/

    /*public void setBatteryTemperatureCallback(BatteryTemperatureCallback batteryTemperatureCallback) {
        //processBatteryTemperature.setResultMeasuring(batteryTemperatureCallback);
        timerProcessBatteryTemperatureTask.setResultMeasuring(batteryTemperatureCallback);
    }*/

    public void resetAutoNull(){ autoNull = 0; }

    private class RunnableScaleAttach implements Runnable{
        final Thread thread;

        RunnableScaleAttach(){
            connectResultCallback.resultConnect(ResultConnect.STATUS_ATTACH_START, getNameBluetoothDevice().toString());
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {
            try {
                connect();
                if (isVersion()) {
                    try {
                        getVersion().load();
                        connectResultCallback.resultConnect(ResultConnect.STATUS_LOAD_OK, "");
                    } catch (ErrorModuleException e) {
                        connectResultCallback.connectError(ResultError.MODULE_ERROR, e.getMessage());
                    } catch (ErrorTerminalException e) {
                        connectResultCallback.connectError(ResultError.TERMINAL_ERROR, e.getMessage());
                    } catch (Exception e) {
                        connectResultCallback.connectError(ResultError.MODULE_ERROR, e.getMessage());
                    }
                } else {
                    disconnect();
                    connectResultCallback.resultConnect(ResultConnect.STATUS_VERSION_UNKNOWN, "");
                }
            } catch (IOException e) {
                connectResultCallback.connectError(ResultError.CONNECT_ERROR, e.getMessage());
            }
            connectResultCallback.resultConnect(ResultConnect.STATUS_ATTACH_FINISH, "");
        }
    }

    private class ThreadBatteryTemperature extends Thread{
        protected BatteryTemperatureCallback resultMeasuring;
        private boolean running;
        private final int PERIOD_UPDATE = 2000;

        ThreadBatteryTemperature(BatteryTemperatureCallback batteryTemperatureCallback){
            resultMeasuring = batteryTemperatureCallback;
        }

        @Override
        public void run() {
            setRunning(true);
            while (isRunning()){
                try {
                    resultMeasuring.batteryTemperature(getModuleBatteryCharge(), getModuleTemperature());
                    if (enableAutoNull){
                        if (getVersion().getWeight() != Integer.MIN_VALUE && Math.abs(getVersion().getWeight()) < weightError) { //автоноль
                            autoNull += 1;
                            if (autoNull > timerNull / DIVIDER_AUTO_NULL) {
                                setOffsetScale();
                                autoNull = 0;
                            }
                        } else {
                            autoNull = 0;
                        }
                    }

                }catch (Exception e){}
                try { TimeUnit.MILLISECONDS.sleep(PERIOD_UPDATE); } catch (InterruptedException e) {}
            }
        }

        public void cancel() throws InterruptedException{
            join(PERIOD_UPDATE);
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void setResultMeasuring(BatteryTemperatureCallback resultMeasuring) {this.resultMeasuring = resultMeasuring;}
    }

    private class ThreadWeight extends Thread{
        protected WeightCallback resultMeasuring;
        private boolean running;
        private final int PERIOD_UPDATE = 20;

        ThreadWeight(WeightCallback callback){
            resultMeasuring = callback;
        }

        @Override
        public void run() {
            setRunning(true);
            while (isRunning()){
                try{
                    int weight = getVersion().updateWeight();
                    setSensorTenzo(Integer.valueOf(Commands.CMD_SENSOR.getParam()));
                    ResultWeight msg;
                    if (weight == Integer.MIN_VALUE) {
                        msg = ResultWeight.WEIGHT_ERROR;
                    } else {
                        if (getVersion().isLimit())
                            msg = getVersion().isMargin() ? ResultWeight.WEIGHT_MARGIN : ResultWeight.WEIGHT_LIMIT;
                        else {
                            msg = ResultWeight.WEIGHT_NORMAL;
                        }
                    }
                    resultMeasuring.weight(msg, weight, getSensorTenzo());
                }catch (Exception e){}

                try { TimeUnit.MILLISECONDS.sleep(20); } catch (InterruptedException e) {}
            }
            running = false;
        }

        public void cancel() throws InterruptedException{
            join(PERIOD_UPDATE);
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void setResultMeasuring(WeightCallback resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }
    }

    /** Класс для обработки показаний батареи и температуры. */
    /*private abstract class ProcessBatteryTemperature implements Runnable{
        private Thread thread;
        protected BatteryTemperatureCallback resultMeasuring;
        private volatile boolean cancelled;
        *//** счётчик автообнуления *//*
        private int autoNull;
        *//** Время обновления в милисекундах. *//*
        private int timeUpdate = 1000;

        *//** Метод посылает значения веса и датчика.
         * @param battery     результат заряд батареи в процентах.
         * @param temperature результат температуры в градусах.
         * @return возвращяет время для обновления показаний в секундах.
         *//*
        public abstract int onEvent(int battery, int temperature);

        public void setResultMeasuring(BatteryTemperatureCallback resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }

        @Override
        public void run() {
            cancelled = false;
            while (!cancelled) {
                try {
                    timeUpdate = onEvent(getModuleBatteryCharge(), getModuleTemperature());
                    try { Thread.sleep(timeUpdate); } catch (InterruptedException ignored) {}
                    if (cancelled)
                        break;
                    if (version.weight != Integer.MIN_VALUE && Math.abs(version.weight) < weightError) { //автоноль
                        autoNull += 1;
                        if (autoNull > timerNull / InterfaceVersions.DIVIDER_AUTO_NULL) {
                            setOffsetScale();
                            autoNull = 0;
                        }
                    } else {
                        autoNull = 0;
                    }
                }catch (NullPointerException e){}
            }
        }

        private synchronized void cancel() {  cancelled = true;  }

        *//** Сброс счетчика авто ноль. *//*
        private void resetNull() { autoNull = 0; }

        *//** Запускаем измерение.  *//*
        public void start(){
            if(thread == null){
                thread = new Thread(this);
                //this.thread.setDaemon(true);
                thread.start();
            }
        }

        *//** Останавливаем измерение.
         * @param flag true - ждем остановки измерения.
         *//*
        public void stop(boolean flag){
            cancelled = true;
            boolean retry = true;
            while(retry){
                try {
                    if(thread.isAlive()){
                        if(flag)
                            thread.join(timeUpdate * 2);
                        else
                            thread.interrupt();
                    }
                    retry = false;
                } catch (NullPointerException | InterruptedException e) {
                    retry = false;
                }
            }
            thread = null;
        }
    }*/

    /*private class TimerProcessBatteryTemperatureTask extends TimerTask{
        protected BatteryTemperatureCallback resultMeasuring;

        TimerProcessBatteryTemperatureTask(BatteryTemperatureCallback batteryTemperatureCallback){
            resultMeasuring = batteryTemperatureCallback;
        }

        public void setResultMeasuring(BatteryTemperatureCallback resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }

        @Override
        public void run() {
            try {
                resultMeasuring.batteryTemperature(getModuleBatteryCharge(), getModuleTemperature());
                if (enableAutoNull){
                    if (version.weight != Integer.MIN_VALUE && Math.abs(version.weight) < weightError) { //автоноль
                        autoNull += 1;
                        if (autoNull > timerNull / InterfaceVersions.DIVIDER_AUTO_NULL) {
                            setOffsetScale();
                            autoNull = 0;
                        }
                    } else {
                        autoNull = 0;
                    }
                }

            }catch (NullPointerException e){}
        }
    }*/

    /** Класс обработки показаний веса и значения датчика. */
    /*private abstract class ProcessWeight implements Runnable{
        private Thread thread;
        protected WeightCallback resultMeasuring;
        private volatile boolean cancelled;
        *//** Время обновления в милисекундах. *//*
        private int timeUpdate = 50;

        *//** Метод возвращяет значения веса и датчика.
         * @param what   результат статуса измерения enum ResultWeight.
         * @param weight результат веса.
         * @param sensor результат показаний датчика веса.
         * @return возвращяет время для обновления показаний в милисикундах.
         *//*
        public abstract int onEvent(ResultWeight what, int weight, int sensor);

        public void setResultMeasuring(WeightCallback resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }

        @Override
        public void run() {
            cancelled = false;
            while (!cancelled) {
                try{
                    updateWeight();
                    ResultWeight msg;
                    if (version.weight == Integer.MIN_VALUE) {
                        msg = ResultWeight.WEIGHT_ERROR;
                    } else {
                        if (isLimit())
                            msg = isMargin() ? ResultWeight.WEIGHT_MARGIN : ResultWeight.WEIGHT_LIMIT;
                        else {
                            msg = ResultWeight.WEIGHT_NORMAL;
                        }
                    }
                    timeUpdate = onEvent(msg, version.weight, getSensorTenzo());
                }catch (NullPointerException e){}
                try { Thread.sleep(timeUpdate); } catch ( InterruptedException ignored) {}
            }
        }

        private synchronized void cancel() {
            cancelled = true;
        }

        *//** Запускаем измерение. *//*
        public void start(){
            if(thread == null){
                thread = new Thread(this);
                //this.thread.setDaemon(true);
                thread.start();
            }
        }

        *//** Останавливаем измерение.
         * @param flag true - ждем остановки измерения.
         *//*
        public void stop(boolean flag){
            cancelled = true;
            boolean retry = true;
            while(retry){
                try {
                    if(thread.isAlive()){
                        if(flag)
                            thread.join(timeUpdate * 2);
                        else
                            thread.interrupt();
                    }
                    retry = false;
                } catch (NullPointerException | InterruptedException e) {
                    retry = false;
                }
            }
            thread = null;
        }
    }*/

    /** Класс задачи для таймера обрвботки показаний веса и значения датчика. */
    /*private class TimerProcessWeightTask extends TimerTask {
        protected WeightCallback resultMeasuring;

        TimerProcessWeightTask(WeightCallback weightCallback){
            resultMeasuring = weightCallback;
        }

        public void setResultMeasuring(WeightCallback resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }

        @Override
        public void run() {
            try{
                updateWeight();
                ResultWeight msg;
                if (version.weight == Integer.MIN_VALUE) {
                    msg = ResultWeight.WEIGHT_ERROR;
                } else {
                    if (isLimit())
                        msg = isMargin() ? ResultWeight.WEIGHT_MARGIN : ResultWeight.WEIGHT_LIMIT;
                    else {
                        msg = ResultWeight.WEIGHT_NORMAL;
                    }
                }
                resultMeasuring.weight(msg, version.weight, getSensorTenzo());
            }catch (NullPointerException e){}
        }
    }*/
}
