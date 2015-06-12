package com.konst.module;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Общий класс для всех версий весовых модулей
 * @author Kostya
 */
abstract class Versions implements InterfaceVersions{

    /**время выключения весов*/
    public static int timeOff;
    //protected int sensor;                      //показание датчика веса
    /** калибровочный коэффициент a */
    public static float coefficientA;
    /**калибровочный коэффициент b*/
    public static float coefficientB;
    /** максимальный вес для весов */
    public static int weightMax;
    /** АЦП-фильтр (0-15) */
    public static int filterADC;
    /** текущий вес */
    public static int weight;
    /** предельный вес взвешивания */
    public static int weightMargin;
    /** максимальное показание датчика */
    public static int limitTenzo;
    /** предельное показани датчика */
    static int marginTenzo;
    /** скорость передачи данных ком порта модуля bluetooth */
    protected int speed;
    /** текущее показание датчика веса */
    public static int sensorTenzo;
    /** разница знечений между значение ноля до и после */
    protected static int offset;
    /** калибровочный коэффициент температуры */
    protected float coefficientTemp;
    /** показание датчика веса с учетом offset */
    protected static int sensorTenzoOffset;
    /** имя таблици google spreadsheet */
    public static String spreadsheet = "";
    /** имя акаунта google */
    public static String username = "";
    /** пароль акаунта google */
    public static String password = "";
    /** номер телефона админа в формате +38хххххххххх*/
    public static String phone = "";

    /** Загружаем значения из весового модуля
     * @throws Exception Ошибки при загрузке настроек из весового модуля*/
    protected abstract void load() throws Exception;
    /** Установить ноль
     * @return true Установлен offset в весовом модуле*/
    protected abstract boolean setOffsetScale();
    /** Определяем значения лимита измерения датчика веса
     * @return true если значения равно лимиту  датчика */
    protected abstract boolean isLimit();
    /** Определяем значения перезруза датчика веса
     * @return true если значение равно перегрузу */
    protected abstract boolean isMargin();
    /** Получаем новое значение датчика и преобразуем в вес
     * @return значение веса */
    protected abstract int updateWeight();
    /** устанавливаем весы в ноль
     * @return true Установлен ноль в весовом модуле */
    protected abstract boolean setScaleNull();
    /** Записывем данные в весовой модуль
     * @return true значения записаны */
    protected abstract boolean writeData();

    protected abstract int getSensorTenzo();

    protected abstract boolean setSpreadsheet(String sheet);

    protected abstract boolean setUsername(String username);

    protected abstract boolean setPassword(String password);

    protected abstract boolean setPhone(String phone);

    /** Получаем значение установленого фильтра в АЦП
     * @return значение от 1 до 15 */
    private static String getFilterADC() { return ScaleModule.cmd(CMD_FILTER); }

    protected void loadFilterADC() throws Exception {
        filterADC = Integer.valueOf(getFilterADC());
        if (filterADC < 0 || filterADC > MAX_ADC_FILTER) {
            if (!ScaleModule.setModuleFilterADC(DEFAULT_ADC_FILTER))
                throw new ErrorModuleException("Фильтер АЦП не установлен в настройках");
            filterADC = InterfaceVersions.DEFAULT_ADC_FILTER;
        }
    }

    protected void loadTimeOff() throws Exception {
        timeOff = Integer.valueOf(ScaleModule.getModuleTimeOff());
        if (timeOff < InterfaceVersions.MIN_TIME_OFF || timeOff > MAX_TIME_OFF) {
            if (!ScaleModule.setModuleTimeOff(MIN_TIME_OFF))
                throw new ErrorModuleException("Таймер выключения не установлен в настройках");
            timeOff = InterfaceVersions.MIN_TIME_OFF;
        }
    }

    protected void loadSpeedModule() throws Exception {
        speed = Integer.valueOf(ScaleModule.getModuleSpeedPort());
        if (speed < 1 || speed > 5) {
            if (!ScaleModule.setModuleSpeedPort(5))
                throw new ErrorModuleException("Скорость передачи не установлена в настройках");
            speed = 5;
        }
    }

    protected static int getMarginTenzo() { return marginTenzo; }

    protected static class ErrorTerminalException extends Exception{

        private static final long serialVersionUID = -5686828330979812580L;

        public ErrorTerminalException(String s) { super(s); }
        @Override
        public String getMessage(){ return super.getMessage(); }
    }

    protected static class ErrorModuleException extends Exception{

        private static final long serialVersionUID = -5592575601519548599L;

        public ErrorModuleException(String s) { super(s); }
        @Override
        public String getMessage() { return super.getMessage();  }
    }

    /**
     * A simple class that provides utilities to ease command line parsing.
     */
    class SimpleCommandLineParser {

        private final Map<String, String> argMap;

        public SimpleCommandLineParser(String[] arg, String predict) {
            argMap = new HashMap<>();
            for (String anArg : arg) {
                String[] str = anArg.split(predict, 2);
                if (str.length > 1) {
                    argMap.put(str[0], str[1]);
                }
            }
        }

        public String getValue(String... keys) {
            for (String key : keys) {
                if (argMap.get(key) != null) {
                    return argMap.get(key);
                }
            }
            return null;
        }

        public Iterator<String> getKeyIterator() {
            Set<String> keySet = argMap.keySet();
            if (!keySet.isEmpty()) {
                return keySet.iterator();
            }
            return null;
        }
    }

}
