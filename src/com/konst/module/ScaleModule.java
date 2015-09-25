package com.konst.module; /**
 * Copyright (c) 2015.
 */

import android.bluetooth.BluetoothDevice;

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
    private String versionName;
    OnEventResultMeasuring onEventResultMeasuring;
    RunnableScaleConnect runnableScaleConnect;

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

    public ScaleModule(String moduleVersion) throws Exception {
        runnableScaleConnect = new RunnableScaleConnect();
        versionName = moduleVersion;
    }

    public ScaleModule(String moduleVersion, OnEventConnectResult event) throws Exception {
        super(event);
        runnableScaleConnect = new RunnableScaleConnect();
        versionName = moduleVersion;
    }

    HandlerWeight handlerWeight = new HandlerWeight() {
        @Override
        public int onEvent(ResultWeight what, int weight, int sensor) {
            return resultMeasuring.resultWeight(what,weight,sensor);
        }
    };

    HandlerBatteryTemperature handlerBatteryTemperature = new HandlerBatteryTemperature() {
        @Override
        public int onEvent(int battery, int temperature) {
            return 5;
        }
    };

    public interface OnEventResultMeasuring{
        int resultWeight(ResultWeight what, int weight, int sensor);
        int resultBatteryTemperature(int battery, int temperature);
    }

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
     * Отсоединение весового модуля.
     * Необходимо использовать перед закрытием программы чтобы остановить работающие процессы
     */
    @Override
    public void dettach() {
        removeCallbacksAndMessages(null);
        disconnect();
        version = null;
    }

    /**
     * Прверяем если весовой модуль присоеденен.
     *
     * @return true если было присоединение и загрузка версии весового модуля
     */
    public boolean isAttach() {
        return version != null;
    }

    /**
     * Определяем после соединения это весовой модуль и какой версии
     * указаной при инициализации класса com.kostya.module.ScaleModule.
     *
     * @return true версия правильная
     */
    public boolean isScales() {
        String vrs = cmd(InterfaceVersions.CMD_VERSION); //Получаем версию весов
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

    /**
     * Соединится с модулем.
     */
    @Override
    public void attach() {
        onEventConnectResult.handleResultConnect(ResultConnect.STATUS_ATTACH_START);
        new Thread(runnableScaleConnect).start();
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
     * @see InterfaceVersions#CMD_SERVICE_COD
     */
    public boolean setModuleServiceCod(String cod) {
        return cmd(InterfaceVersions.CMD_SERVICE_COD + cod).equals(InterfaceVersions.CMD_SERVICE_COD);
    }

    /**
     * Получаем сервис код.
     *
     * @return код
     * @see InterfaceVersions#CMD_SERVICE_COD
     */
    public String getModuleServiceCod() {
        return cmd(InterfaceVersions.CMD_SERVICE_COD);
    }

    /**
     * Установливаем новое значение АЦП в весовом модуле. Знчение от1 до 15
     *
     * @param filterADC Значение АЦП от 1 до 15
     * @return true Значение установлено
     * @see InterfaceVersions#CMD_FILTER
     */
    public boolean setModuleFilterADC(int filterADC) {
        return cmd(InterfaceVersions.CMD_FILTER + filterADC).equals(InterfaceVersions.CMD_FILTER);
    }

    /**
     * Получаем из весового модуля время выключения при бездействии устройства
     *
     * @return время в минутах
     * @see InterfaceVersions#CMD_TIMER
     */
    public String getModuleTimeOff() {
        return cmd(InterfaceVersions.CMD_TIMER);
    }

    /**
     * записываем в весовой модуль время выключения при бездействии устройства
     *
     * @param timeOff Время в минутах
     * @return true Значение установлено
     * @see InterfaceVersions#CMD_TIMER
     */
    public boolean setModuleTimeOff(int timeOff) {
        return cmd(InterfaceVersions.CMD_TIMER + timeOff).equals(InterfaceVersions.CMD_TIMER);
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
     * @see InterfaceVersions#CMD_SPEED
     */
    public String getModuleSpeedPort() {
        return cmd(InterfaceVersions.CMD_SPEED);
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
     * @see InterfaceVersions#CMD_SPEED
     */
    public boolean setModuleSpeedPort(int speed) {
        return cmd(InterfaceVersions.CMD_SPEED + speed).equals(InterfaceVersions.CMD_SPEED);
    }

    /**
     * Получить офсет датчика веса.
     *
     * @return Значение офсет.
     * @see InterfaceVersions#CMD_GET_OFFSET
     */
    public String getModuleOffsetSensor() {
        return cmd(InterfaceVersions.CMD_GET_OFFSET);
    }

    /**
     * Получить значение датчика веса.
     *
     * @return Значение датчика.
     * @see InterfaceVersions#CMD_SENSOR
     */
    public String feelWeightSensor() {
        return cmd(InterfaceVersions.CMD_SENSOR);
    }

    /**
     * Получаем значение заряда батерии.
     *
     * @return Заряд батареи в процентах.
     * @see InterfaceVersions#CMD_BATTERY
     */
    public int getModuleBatteryCharge() {
        try {
            battery = Integer.valueOf(cmd(InterfaceVersions.CMD_BATTERY));
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
     * @see InterfaceVersions#CMD_CALL_BATTERY
     */
    public boolean setModuleBatteryCharge(int charge) {
        return cmd(InterfaceVersions.CMD_CALL_BATTERY + charge).equals(InterfaceVersions.CMD_CALL_BATTERY);
    }

    /**
     * Получаем значение температуры весового модуля.
     *
     * @return Температура в градусах.
     * @see InterfaceVersions#CMD_DATA_TEMP
     */
    public int getModuleTemperature() {
        try {
            return (int) ((float) ((Integer.valueOf(cmd(InterfaceVersions.CMD_DATA_TEMP)) - 0x800000) / 7169) / 0.81) - 273;
        } catch (Exception e) {
            return -273;
        }
    }

    /**
     * Получаем версию hardware весового модуля.
     *
     * @return Hardware версия весового модуля.
     * @see InterfaceVersions#CMD_HARDWARE
     */
    public String getModuleHardware() {
        return cmd(InterfaceVersions.CMD_HARDWARE);
    }

    /**
     * Устанавливаем имя весового модуля.
     *
     * @param name Имя весового модуля.
     * @return true - Имя записано в модуль.
     * @see InterfaceVersions#CMD_NAME
     */
    public boolean setModuleName(String name) {
        return cmd(InterfaceVersions.CMD_NAME + name).equals(InterfaceVersions.CMD_NAME);
    }

    /**
     * Устанавливаем калибровку батареи.
     *
     * @param percent Значение калибровки в процентах.
     * @return true - Калибровка прошла успешно.
     * @see InterfaceVersions#CMD_CALL_BATTERY
     */
    public boolean setModuleCalibrateBattery(int percent) {
        return cmd(InterfaceVersions.CMD_CALL_BATTERY + percent).equals(InterfaceVersions.CMD_CALL_BATTERY);
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

    public String getNameBluetoothDevice() {
        return getDevice().getName();
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
        handlerWeight.start();
    }
    public void stopMeasuringWeight(boolean flag){
        handlerWeight.stop(flag);
    }

    public void startMeasuringBatteryTemperature(){
        handlerBatteryTemperature.start();
    }
    public void stopMeasuringBatteryTemperature(boolean flag){
        handlerBatteryTemperature.stop(flag);
    }

    public void setOnEventResultMeasuring(OnEventResultMeasuring onEventResultMeasuring) {
        //this.onEventResultMeasuring = onEventResultMeasuring;
        handlerWeight.setResultMeasuring(onEventResultMeasuring);
        //handlerBatteryTemperature.setResultMeasuring(onEventResultMeasuring);
    }

    public void resetAutoNull(){
        handlerBatteryTemperature.resetAutoNull();
    }

    class RunnableScaleConnect implements Runnable{

        @Override
        public void run() {
            try {
                connect();
                if (isScales()) {
                    try {
                        load();
                        onEventConnectResult.handleResultConnect(ResultConnect.STATUS_LOAD_OK);
                    } catch (Versions.ErrorModuleException e) {
                        onEventConnectResult.handleConnectError(ResultError.MODULE_ERROR, e.getMessage());
                    } catch (Versions.ErrorTerminalException e) {
                        onEventConnectResult.handleConnectError(ResultError.TERMINAL_ERROR, e.getMessage());
                    } catch (Exception e) {
                        onEventConnectResult.handleConnectError(ResultError.MODULE_ERROR, e.getMessage());
                    }
                } else {
                    disconnect();
                    onEventConnectResult.handleResultConnect(ResultConnect.STATUS_SCALE_UNKNOWN);
                }
            } catch (IOException e) {
                onEventConnectResult.handleConnectError(ResultError.CONNECT_ERROR, e.getMessage());
            }
            onEventConnectResult.handleResultConnect(ResultConnect.STATUS_ATTACH_FINISH);
        }
    }

    /**
     * Класс для обработки показаний батареи и температуры надо использевать после
     * создания класса com.kostya.module.ScaleModule и инициализации метода init().
     */
    public abstract class HandlerBatteryTemperature {
        RunnableBatteryTemperature runnableBatteryTemperature;
        //OnEventResultMeasuring resultMeasuring;

        /** Метод посылает значения веса и датчика.
         //* @param battery     результат заряд батареи в процентах.
         //* @param temperature результат температуры в градусах.
         * @return возвращяет время для обновления показаний в секундах.
         */
        public abstract int onEvent(int battery, int temperature);

        public HandlerBatteryTemperature(){
            runnableBatteryTemperature = new RunnableBatteryTemperature();
        }

        /*public void setResultMeasuring(OnEventResultMeasuring resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }*/

        /** Метод запускает или останавливает процесс измерения.
         * @param process true запускаем процесс false останавливаем.
         */
        private void process(final boolean process, boolean wait) {
            try {
                if (isAttach()) {
                    if(process){
                        if (!runnableBatteryTemperature.isStart()) {
                            new Thread(runnableBatteryTemperature).start();
                        }
                    }else{
                        runnableBatteryTemperature.cancel();
                    }

                    if (!process){
                        if(wait){
                            while (runnableBatteryTemperature.isStart()) {}
                        }
                    }
                }

            } catch (Exception e) {
            }
        }

        public void resetAutoNull() {
            runnableBatteryTemperature.resetNull();
        }

        private class RunnableBatteryTemperature implements Runnable{
            private boolean start;
            private volatile boolean cancelled;
            /** счётчик автообнуления */
            private int autoNull;
            /**
             * Время обновления в секундах
             */
            public int timeUpdate = 1;

            @Override
            public void run() {
                start = true;
                cancelled = false;

                while (!cancelled) {
                    timeUpdate = onEvent(getModuleBatteryCharge(), getModuleTemperature());
                    try { TimeUnit.SECONDS.sleep(timeUpdate); } catch (InterruptedException ignored) {}
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
                start = false;
            }

            private boolean isStart() {return start;}

            private synchronized void cancel() {  cancelled = true;  }

            private void resetNull() { autoNull = 0; }
        }

        /** Запускаем измерение.  */
        public void start(){
            process(true, false);
        }

        /** Останавливаем измерение.
         * @param flag true - ждем остановки измерения.
         */
        public void stop(boolean flag){
            process(false, flag);
        }
    }

    /** Класс обработки показаний веса и значения датчика.
     * Надо использевать после создания класса com.kostya.module.ScaleModule
     * и инициализации метода init().
     */
    public abstract class HandlerWeight {
        RunnableWeight runnableWeight;
        OnEventResultMeasuring resultMeasuring;

        public HandlerWeight(){
            runnableWeight = new RunnableWeight();
            //resultMeasuring = onEventResultMeasuring;
        }

        public void setResultMeasuring(OnEventResultMeasuring resultMeasuring) {
            this.resultMeasuring = resultMeasuring;
        }

        /**Метод возвращяет значения веса и датчика.
         * @param what   результат статуса измерения enum ResultWeight.
         * @param weight результат веса.
         * @param sensor результат показаний датчика веса.
         * @return возвращяет время для обновления показаний в милисикундах.
         */
        public abstract int onEvent(ResultWeight what, int weight, int sensor);

        /**Метод запускает или останавливает процесс измерения.
         * @param process true запускаем процесс false останавливаем.
         */
        private void process(final boolean process, boolean wait) {
            try {
                if (isAttach()) {
                    if(process){
                        if (!runnableWeight.isStart()) {
                            new Thread(runnableWeight).start();
                        }
                    }else{
                        runnableWeight.cancel();
                    }

                    if (!process){
                        if(wait){
                            while (runnableWeight.isStart()) {}
                        }
                    }
                }
            } catch (Exception e) { }
        }

        private class RunnableWeight implements Runnable{
            private volatile boolean cancelled;
            private boolean start;
            public int timeUpdate = 50;

            @Override
            public void run() {
                start = true;
                cancelled = false;
                while (!cancelled) {
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
                    try { Thread.sleep(timeUpdate); } catch ( InterruptedException ignored) {}
                }
                start = false;
            }

            private boolean isStart() {return start;}

            private synchronized void cancel() {
                cancelled = true;
            }
        }

        /** Запускаем измерение.         *
         */
        public void start(){
            process(true, false);
        }

        /** Останавливаем измерение.
         * @param flag true - ждем остановки измерения.
         */
        public void stop(boolean flag){
            process(false, flag);
        }
    }
}
