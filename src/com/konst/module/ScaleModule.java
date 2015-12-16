package com.konst.module; /**
 * Copyright (c) 2015.
 */

import android.os.Build;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Главный класс для работы с весовым модулем. Инициализируем в теле программы. В абстрактных методах используем
 * возвращеные результаты после запуска метода {@link ScaleModule#init(String)}.
 * Пример:com.kostya.module.ScaleModule scaleModule = new com.kostya.module.ScaleModule();
 * scaleModule.init("version", "bluetooth");
 *
 * @author Kostya
 */
public class ScaleModule extends Module {
    protected Versions version;
    /**
     * Процент батареи (0-100%)
     */
    protected int battery;
    /**
     * Погрешность веса автоноль
     */
    protected int weightError;
    /**
     * Время срабатывания авто ноля
     */
    protected int timerNull;
    private int numVersion;
    private final String versionName;

    /**
     * Константы результата взвешивания
     */
    public enum ResultWeight {
        /**
         * Значение веса неправильное
         */
        WEIGHT_ERROR,
        /**
         * Значение веса в диапазоне весового модуля
         */
        WEIGHT_NORMAL,
        /**
         * Значение веса в диапазоне лилита взвешивания
         */
        WEIGHT_LIMIT,
        /**
         * Значение веса в диапазоне перегрузки
         */
        WEIGHT_MARGIN
    }

    /**
     * Интерфейс события результат вес.
     */
    public interface WeightCallback{
        int weight(ResultWeight what, int weight, int sensor);
    }

    /**
     * Интерфейс события результат батарея и температура.
     */
    public interface BatteryTemperatureCallback{
        int batteryTemperature(int battery, int temperature);
    }

    public ScaleModule(String moduleVersion) throws Exception {
        versionName = moduleVersion;
    }

    public ScaleModule(String moduleVersion, ConnectResultCallback event) throws Exception {
        super(event);
        versionName = moduleVersion;
    }

    /** Соединится с модулем. */
    @Override
    public void attach() /*throws InterruptedException*/ {
        new RunnableScaleAttach();
    }

    /**
     * Отсоединение весового модуля.
     * Необходимо использовать перед закрытием программы чтобы остановить работающие процессы
     */
    @Override
    public void dettach() {
        stopMeasuringWeight(true);
        stopMeasuringBatteryTemperature(true);
        disconnect();
        version = null;
    }

    @Override
    public synchronized void connect() throws IOException, NullPointerException {
        disconnect();
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
        else
            socket = device.createRfcommSocketToServiceRecord(uuid);
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
    }

    @Override
    public void disconnect() {
        try {
            if(bufferedReader != null)
                bufferedReader.close();
            if(bufferedWriter != null)
                bufferedWriter.close();
            if (socket != null)
                socket.close();
        } catch (IOException ioe) {
            socket = null;
        }
        bufferedReader =  null;
        bufferedWriter = null;
        socket = null;
    }

    final ProcessWeight processWeight = new ProcessWeight() {
        @Override
        public int onEvent(ResultWeight what, int weight, int sensor) {
            return resultMeasuring.weight(what,weight,sensor);
        }
    };

    final ProcessBatteryTemperature processBatteryTemperature = new ProcessBatteryTemperature() {
        @Override
        public int onEvent(int battery, int temperature) {
            return resultMeasuring.batteryTemperature(battery, temperature);
        }
    };

    /**
     * Получаем класс загруженой версии весового модуля
     *
     * @return класс версии весового модуля
     */
    public Versions getVersion() {
        return version;
    }

    /**
     * Получаем заряд батареи раннее загруженый в процентах
     *
     * @return заряд батареи в процентах
     */
    public int getBattery() {
        return battery;
    }

    /**
     * Меняем ранне полученое значение заряда батареи весового модуля
     *
     * @param battery Заряд батареи в процентах
     */
    public void setBattery(int battery) {
        this.battery = battery;
    }

    /**
     * Получаем значение веса погрешности для расчета атоноль
     *
     * @return возвращяет значение веса
     */
    public int getWeightError() {
        return weightError;
    }

    /**
     * Сохраняем значение веса погрешности для расчета автоноль
     *
     * @param weightError Значение погрешности в килограмах
     */
    public void setWeightError(int weightError) {
        this.weightError = weightError;
    }

    /**
     * Время для срабатывания автоноль
     *
     * @return возвращяем время после которого установливается автоноль
     */
    public int getTimerNull() {
        return timerNull;
    }

    /**
     * Устонавливаем значение времени после которого срабатывает автоноль
     *
     * @param timerNull Значение времени в секундах
     */
    public void setTimerNull(int timerNull) {
        this.timerNull = timerNull;
    }

    /**
     * Прверяем если весовой модуль присоеденен.
     *
     * @return true если было присоединение и загрузка версии весового модуля
     */
    public boolean isAttach() {
        return version != null;
    }

    /** Определяем после соединения это весовой модуль и какой версии.
     * Проверяем версию указаной при инициализации класса com.kostya.module.ScaleModule.
     *
     * @return true версия правильная.
     */
    public boolean isScales() {
        String vrs = getModuleVersion(); //Получаем версию весов
        if (vrs.startsWith(versionName)) {
            try {
                numVersion = Integer.valueOf(vrs.replace(versionName, ""));
                version = fetchVersion(numVersion);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /** Определяем версию весов.
     * @param version Имя версии.
     * @return Экземпляр версии.
     * @throws Exception
     */
    private Versions fetchVersion(int version) throws Exception {
        switch (version) {
            case 1:
                return new V1(this);
            case 4:
                return new V4(this);
            default:
                throw new Exception("illegal version");
        }
    }

    //==================================================================================================================

    /**
     * Установливаем сервис код.
     *
     * @param cod Код
     * @return true Значение установлено
     * @see Commands#CMD_SERVICE_COD
     */
    public boolean setModuleServiceCod(String cod) {
        return Commands.CMD_SERVICE_COD.setParam(cod);
    }

    /**
     * Получаем сервис код.
     *
     * @return код
     * @see Commands#CMD_SERVICE_COD
     */
    public String getModuleServiceCod() {
        return Commands.CMD_SERVICE_COD.getParam();
        //return cmd(InterfaceVersions.CMD_SERVICE_COD);
    }

    /**
     * Установливаем новое значение АЦП в весовом модуле. Знчение от1 до 15
     *
     * @param filterADC Значение АЦП от 1 до 15
     * @return true Значение установлено
     * @see Commands#CMD_FILTER
     */
    public boolean setModuleFilterADC(int filterADC) {
        return Commands.CMD_FILTER.setParam(filterADC);
        //return cmd(InterfaceVersions.CMD_FILTER + filterADC).equals(InterfaceVersions.CMD_FILTER);
    }

    /**
     * Получаем из весового модуля время выключения при бездействии устройства
     *
     * @return время в минутах
     * @see Commands#CMD_TIMER
     */
    public String getModuleTimeOff() {
        return Commands.CMD_TIMER.getParam();
    }

    /**
     * записываем в весовой модуль время выключения при бездействии устройства
     *
     * @param timeOff Время в минутах
     * @return true Значение установлено
     * @see Commands#CMD_TIMER
     */
    public boolean setModuleTimeOff(int timeOff) {
        return Commands.CMD_TIMER.setParam(timeOff);
    }

    /**
     * Получаем значение скорости порта bluetooth модуля обмена данными.
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

    /**
     * Устанавливаем скорость порта обмена данными bluetooth модуля.
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

    /**
     * Получить офсет датчика веса.
     *
     * @return Значение офсет.
     * @see Commands#CMD_GET_OFFSET
     */
    public String getModuleOffsetSensor() {
        return Commands.CMD_GET_OFFSET.getParam();
    }

    /**
     * Получить значение датчика веса.
     *
     * @return Значение датчика.
     * @see Commands#CMD_SENSOR
     */
    public String feelWeightSensor() {
        return Commands.CMD_SENSOR.getParam();
    }

    /**
     * Получаем значение заряда батерии.
     *
     * @return Заряд батареи в процентах.
     * @see Commands#CMD_BATTERY
     */
    public int getModuleBatteryCharge() {
        try {
            battery = Integer.valueOf(Commands.CMD_BATTERY.getParam());
        } catch (Exception e) {
            battery = -0;
        }
        return battery;
    }

    /**
     * Устанавливаем заряд батареи.
     * Используется для калибровки заряда батареи.
     *
     * @param charge Заряд батереи в процентах.
     * @return true - Заряд установлен.
     * @see Commands#CMD_CALL_BATTERY
     */
    public boolean setModuleBatteryCharge(int charge) {
        return Commands.CMD_CALL_BATTERY.setParam(charge);
    }

    /**
     * Получаем значение температуры весового модуля.
     *
     * @return Температура в градусах.
     * @see Commands#CMD_DATA_TEMP
     */
    public int getModuleTemperature() {
        try {
            return (int) ((float) ((Integer.valueOf(Commands.CMD_DATA_TEMP.getParam()) - 0x800000) / 7169) / 0.81) - 273;
        } catch (Exception e) {
            return -273;
        }
    }

    /**
     * Устанавливаем имя весового модуля.
     *
     * @param name Имя весового модуля.
     * @return true - Имя записано в модуль.
     * @see Commands#CMD_NAME
     */
    public boolean setModuleName(String name) {
        return Commands.CMD_NAME.setParam(name);
    }

    /**
     * Устанавливаем калибровку батареи.
     *
     * @param percent Значение калибровки в процентах.
     * @return true - Калибровка прошла успешно.
     * @see Commands#CMD_CALL_BATTERY
     */
    public boolean setModuleCalibrateBattery(int percent) {
        return Commands.CMD_CALL_BATTERY.setParam(percent);
    }

    /**
     * Устанавливаем имя spreadsheet в google drive.
     *
     * @param sheet Имя таблици.
     * @return true - Имя записано успешно.
     * @see Versions#setSpreadsheet(String)
     */
    public boolean setModuleSpreadsheet(String sheet) {
        return version.setSpreadsheet(sheet);
    }

    /**
     * Устанавливаем имя аккаунта в google.
     *
     * @param username Имя аккаунта.
     * @return true - Имя записано успешно.
     * @see Versions#setUsername(String)
     */
    public boolean setModuleUserName(String username) {
        return version.setUsername(username);
    }

    /**
     * Устанавливаем пароль в google.
     *
     * @param password Пароль аккаунта.
     * @return true - Пароль записано успешно.
     * @see Versions#setPassword(String)
     */
    public boolean setModulePassword(String password) {
        return version.setPassword(password);
    }

    /**
     * Устанавливаем номер телефона. Формат "+38хххххххххх"
     *
     * @param phone Пароль аккаунта.
     * @return true - телефон записано успешно.
     * @see Versions#setPhone(String)
     */
    public boolean setModulePhone(String phone) {
        return version.setPhone(phone);
    }

    /**
     * Выключить питание модуля.
     *
     * @return true - питание выключено.
     */
    public boolean setModulePowerOff() {
        return version.powerOff();
    }

    /**
     * Получить сохраненое значение фильтраАЦП.
     *
     * @return Значение фильтра от 1 до 15.
     * @see Versions#filterADC
     */
    public int getFilterADC() {
        return version.filterADC;
    }

    /**
     * Установить значение фильтра АЦП.
     *
     * @param filterADC Значение АЦП.
     * @see Versions#filterADC
     */
    public void setFilterADC(int filterADC) {
        version.filterADC = filterADC;
    }

    public int getWeightMax() {
        return version.weightMax;
    }

    public void setWeightMax(int weightMax) {
        version.weightMax = weightMax;
    }

    public int getLimitTenzo() {
        return version.limitTenzo;
    }

    public void setLimitTenzo(int limitTenzo) {
        version.limitTenzo = limitTenzo;
    }

    public String getPhone() {
        return version.phone;
    }

    public void setPhone(String phone) {
        version.phone = phone;
    }

    public int getTimeOff() {
        return version.timeOff;
    }

    public void setTimeOff(int timeOff) {
        version.timeOff = timeOff;
    }

    public float getCoefficientA() {
        return version.coefficientA;
    }

    public void setCoefficientA(float coefficientA) {
        version.coefficientA = coefficientA;
    }

    public float getCoefficientB() {
        return version.coefficientB;
    }

    public void setCoefficientB(float coefficientB) {
        version.coefficientB = coefficientB;
    }

    public String getSpreadSheet() {
        return version.spreadsheet;
    }

    public void setSpreadSheet(String spreadSheet) {
        version.spreadsheet = spreadSheet;
    }

    public String getUserName() {
        return version.username;
    }

    public void setUserName(String userName) {
        version.username = userName;
    }

    public String getPassword() {
        return version.password;
    }

    public void setPassword(String password) {
        version.password = password;
    }

    public int getSensorTenzo() {
        return version.getSensorTenzo();
    }

    public void setSensorTenzo(int sensorTenzo) {
        version.sensorTenzo = sensorTenzo;
    }

    public int getWeightMargin() {
        return version.weightMargin;
    }

    public void setWeightMargin(int weightMargin) {
        version.weightMargin = weightMargin;
    }

    public int getNumVersion() {
        return numVersion;
    }

    public void setNumVersion(int version) {
        numVersion = version;
    }

    public int getSpeedPort() {
        return version.getSpeedPort();
    }

    public String getAddressBluetoothDevice() {
        return getDevice().getAddress();
    }

    public int getMarginTenzo() {
        return version.getMarginTenzo();
    }

    public void load() throws Exception {
        version.load();
    }

    public boolean setOffsetScale() {
        return version.setOffsetScale();
    }

    public boolean isLimit() {
        return version.isLimit();
    }

    public boolean isMargin() {
        return version.isMargin();
    }

    public int updateWeight() {
        return version.updateWeight();
    }

    public boolean setScaleNull() {
        return version.setScaleNull();
    }

    public boolean writeData() {
        return version.writeData();
    }

    public void startMeasuringWeight(){
        processWeight.start();
    }
    public void stopMeasuringWeight(boolean flag){
        processWeight.stop(flag);
    }

    public void startMeasuringBatteryTemperature(){
        processBatteryTemperature.start();
    }

    public void stopMeasuringBatteryTemperature(boolean flag){
        processBatteryTemperature.stop(flag);
    }

    public void setWeightCallback(WeightCallback weightCallback) {
        processWeight.setResultMeasuring(weightCallback);
    }

    public void setBatteryTemperatureCallback(BatteryTemperatureCallback batteryTemperatureCallback) {
        processBatteryTemperature.setResultMeasuring(batteryTemperatureCallback);
    }

    public void resetAutoNull(){
        //measuringBatteryTemperature.resetAutoNull();
        processBatteryTemperature.resetNull();
    }

    class RunnableScaleAttach implements Runnable{
        Thread thread;

        RunnableScaleAttach(){
            connectResultCallback.resultConnect(ResultConnect.STATUS_ATTACH_START);
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {
            try {
                connect();
                if (isScales()) {
                    try {
                        load();
                        connectResultCallback.resultConnect(ResultConnect.STATUS_LOAD_OK);
                    } catch (Versions.ErrorModuleException e) {
                        connectResultCallback.connectError(ResultError.MODULE_ERROR, e.getMessage());
                    } catch (Versions.ErrorTerminalException e) {
                        connectResultCallback.connectError(ResultError.TERMINAL_ERROR, e.getMessage());
                    } catch (Exception e) {
                        connectResultCallback.connectError(ResultError.MODULE_ERROR, e.getMessage());
                    }
                } else {
                    disconnect();
                    connectResultCallback.resultConnect(ResultConnect.STATUS_VERSION_UNKNOWN);
                }
            } catch (IOException e) {
                connectResultCallback.connectError(ResultError.CONNECT_ERROR, e.getMessage());
            }
            connectResultCallback.resultConnect(ResultConnect.STATUS_ATTACH_FINISH);
        }
    }

    /** Класс для обработки показаний батареи и температуры. */
    private abstract class ProcessBatteryTemperature implements Runnable{
        private Thread thread;
        protected BatteryTemperatureCallback resultMeasuring;
        private volatile boolean cancelled;
        /** счётчик автообнуления */
        private int autoNull;
        /** Время обновления в милисекундах. */
        private int timeUpdate = 1000;

        /** Метод посылает значения веса и датчика.
         * @param battery     результат заряд батареи в процентах.
         * @param temperature результат температуры в градусах.
         * @return возвращяет время для обновления показаний в секундах.
         */
        public abstract int onEvent(int battery, int temperature);

        public void setResultMeasuring(BatteryTemperatureCallback resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }

        @Override
        public void run() {
            cancelled = false;
            while (!cancelled) {
                timeUpdate = onEvent(getModuleBatteryCharge(), getModuleTemperature());
                try { Thread.sleep(timeUpdate); } catch (InterruptedException ignored) {}
                if (cancelled)
                    break;
                try {
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

        /** Сброс счетчика авто ноль. */
        private void resetNull() { autoNull = 0; }

        /** Запускаем измерение.  */
        public void start(){
            if(this.thread == null){
                this.thread = new Thread(this);
                //this.thread.setDaemon(true);
                this.thread.start();
            }
        }

        /** Останавливаем измерение.
         * @param flag true - ждем остановки измерения.
         */
        public void stop(boolean flag){
            cancelled = true;
            boolean retry = true;
            while(retry){
                try {
                    if(this.thread.isAlive()){
                        if(flag)
                            this.thread.join(timeUpdate * 2);
                        else
                            this.thread.interrupt();
                    }
                    retry = false;
                } catch (NullPointerException | InterruptedException e) {
                    retry = false;
                }
            }
            this.thread = null;
        }
    }

    /** Класс обработки показаний веса и значения датчика. */
    private abstract class ProcessWeight implements Runnable{
        private Thread thread;
        protected WeightCallback resultMeasuring;
        private volatile boolean cancelled;
        /** Время обновления в милисекундах. */
        private int timeUpdate = 50;

        /** Метод возвращяет значения веса и датчика.
         * @param what   результат статуса измерения enum ResultWeight.
         * @param weight результат веса.
         * @param sensor результат показаний датчика веса.
         * @return возвращяет время для обновления показаний в милисикундах.
         */
        public abstract int onEvent(ResultWeight what, int weight, int sensor);

        public void setResultMeasuring(WeightCallback resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }

        @Override
        public void run() {
            cancelled = false;
            while (!cancelled) {
                updateWeight();
                ResultWeight msg;
                try{
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

        /** Запускаем измерение. */
        public void start(){
            if(this.thread == null){
                this.thread = new Thread(this);
                //this.thread.setDaemon(true);
                this.thread.start();
            }
        }

        /** Останавливаем измерение.
         * @param flag true - ждем остановки измерения.
         */
        public void stop(boolean flag){
            cancelled = true;
            boolean retry = true;
            while(retry){
                try {
                    if(this.thread.isAlive()){
                        if(flag)
                            this.thread.join(timeUpdate * 2);
                        else
                            this.thread.interrupt();
                    }
                    retry = false;
                } catch (NullPointerException | InterruptedException e) {
                    retry = false;
                }
            }
            this.thread = null;
        }
    }
}
