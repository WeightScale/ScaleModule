/**
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
    private ScaleVersion version;
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
    private static final int DIVIDER_AUTO_NULL = 3;
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
    public final void attach() /*throws InterruptedException*/ {
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

    /**
     * Создание соединения с bluetooth.
     *
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
    private ScaleVersion fetchVersion(int version) throws Exception {
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
    public ScaleVersion getVersion() {
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

    /** Получить номер версии программы.
     * @return Номер версии.  */
    public int getNumVersion() { return numVersion; }

    /** Получить имя акаунта google.
     * @return Имя акаунта. */
    public String getUserName() { return username; }

    /** Получить максиматьное значение датчика.
     * @return Значение датчика. */
    public int getLimitTenzo(){ return limitTenzo; }

    /** Установить максимальное значение датчика.
     * @param limitTenzo Значение датчика.     */
    public void setLimitTenzo(int limitTenzo) {
        this.limitTenzo = limitTenzo;
    }

    /** Получить температуру модуля.
     * @return Значение температуры. */
    public int getTemperature() {return temperature; }

    /** Получить сохраненое значение фильтраАЦП.
     * @return Значение фильтра от 1 до 15.   */
    public int getFilterADC() {
        return filterADC;
    }

    /** Установить значение фильтра АЦП.
     * @param filterADC Значение АЦП.*/
    public void setFilterADC(int filterADC) {
        this.filterADC = filterADC;
    }

    /** Получить коэффициент каллибровки.
     * @return Значение коэффициента. */
    public float getCoefficientA() { return coefficientA; }

    /** Усттановить коэффициент каллибровки (только локально не в модуле).
     * @param coefficientA Значение коэффициента.     */
    public void setCoefficientA(float coefficientA) {
        this.coefficientA = coefficientA;
    }

    /** Получить коэффициент смещения.
     * @return Значение коэффициента.  */
    public float getCoefficientB() {
        return coefficientB;
    }

    /** Усттановить коэффициент смещения (только локально не в модуле).
     * @param coefficientB Значение коэффициента.     */
    public void setCoefficientB(float coefficientB) {
        this.coefficientB = coefficientB;
    }

    /** Получить пароль акаута google.
     * @return Пароль.   */
    public String getPassword() {
        return password;
    }

    /** Получить номер телефона.
     * @return Номер телефона.   */
    public String getPhone() {
        return phone;
    }


    /** Получить время работы при бездействии модуля.
     * @return Время в минутах.  */
    public int getTimeOff() {
        return timeOff;
    }

    /** Установить время бездействия модуля.
     * @param timeOff Время в минутах.
     */
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

    public String getAddressBluetoothDevice() { return getDevice().getAddress(); }

    public int getMarginTenzo() {
        return version.getMarginTenzo(); }

    public int getWeightMax(){
        return version.getWeightMax(); }

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
            return (int) ((double) (float) ((Integer.valueOf(Commands.CMD_DATA_TEMP.getParam()) - 0x800000) / 7169) / 0.81) - 273;
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

    /**
     * Установить обнуление.
     *
     * @return true - Обнуление установлено.
     */
    public synchronized boolean setOffsetScale() {
        return version.setOffsetScale();
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

    public void setWeightMax(int weightMax) {
        version.setWeightMax(weightMax);
    }

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
        return version.writeData();
    }

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
                        version.load();
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
        private static final int PERIOD_UPDATE = 2000;

        ThreadBatteryTemperature(BatteryTemperatureCallback batteryTemperatureCallback){
            resultMeasuring = batteryTemperatureCallback;
        }

        @Override
        public void run() {
            running = true;
            while (running){
                try {
                    resultMeasuring.batteryTemperature(getModuleBatteryCharge(), getModuleTemperature());
                    if (enableAutoNull){
                        if (version.getWeight() != Integer.MIN_VALUE && Math.abs(version.getWeight()) < weightError) { //автоноль
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
                try { TimeUnit.MILLISECONDS.sleep((long) PERIOD_UPDATE); } catch (InterruptedException e) {}
            }
        }

        public void cancel() throws InterruptedException{
            join((long) PERIOD_UPDATE);
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
        private static final int PERIOD_UPDATE = 20;

        ThreadWeight(WeightCallback callback){
            resultMeasuring = callback;
        }

        @Override
        public void run() {
            running = true;
            while (running){
                try{
                    int weight = version.updateWeight();
                    ResultWeight msg;
                    if (weight == Integer.MIN_VALUE) {
                        msg = ResultWeight.WEIGHT_ERROR;
                    } else {
                        if (version.isLimit())
                            msg = version.isMargin() ? ResultWeight.WEIGHT_MARGIN : ResultWeight.WEIGHT_LIMIT;
                        else {
                            msg = ResultWeight.WEIGHT_NORMAL;
                        }
                    }
                    resultMeasuring.weight(msg, weight, version.getSensor());
                }catch (Exception e){}

                try { TimeUnit.MILLISECONDS.sleep(PERIOD_UPDATE); } catch (InterruptedException e) {}
            }
            running = false;
        }

        public void cancel() throws InterruptedException{
            join((long) PERIOD_UPDATE);
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

}
