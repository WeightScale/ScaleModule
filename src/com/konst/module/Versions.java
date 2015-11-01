package com.konst.module;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Общий класс для всех версий весовых модулей
 *
 * @author Kostya
 */
abstract class Versions  {
    final ScaleModule module;
    /**
     * Время выключения весов.
     */
    public int timeOff;
    /**
     * Калибровочный коэффициент a.
     */
    public float coefficientA;
    /**
     * Калибровочный коэффициент b.
     */
    public float coefficientB;
    /**
     * Максимальный вес для весов.
     */
    public int weightMax;
    /**
     * АЦП-фильтр (0-15).
     */
    public int filterADC;
    /**
     * Текущий вес.
     */
    public int weight;
    /**
     * Предельный вес взвешивания.
     */
    public int weightMargin;
    /**
     * Максимальное показание датчика.
     */
    public int limitTenzo;
    /**
     * Предельное показани датчика.
     */
    int marginTenzo;
    private int speedPort;
    /**
     * Текущее показание датчика веса.
     */
    public int sensorTenzo;
    /**
     * Разница знечений между значение ноля до и после.
     */
    protected int offset;
    /**
     * Калибровочный коэффициент температуры.
     */
    protected float coefficientTemp;
    /**
     * Показание датчика веса с учетом offset.
     */
    protected int sensorTenzoOffset;
    /**
     * Имя таблици google spreadsheet.
     */
    public String spreadsheet = "";
    /**
     * Имя акаунта google.
     */
    public String username = "";
    /**
     * Пароль акаунта google.
     */
    public String password = "";
    /**
     * Номер телефона админа в формате +38хххххххххх.
     */
    public String phone = "";

    protected Versions(ScaleModule module) {
        this.module = module;
    }

    /**Загружаем значения из весового модуля.
     * @throws Exception Ошибки при загрузке настроек из весового модуля.
     */
    protected abstract void load() throws Exception;

    /**Установить ноль.
     * @return true Установлен offset в весовом модуле.
     */
    protected abstract boolean setOffsetScale();

    /**Определяем значения лимита измерения датчика веса.
     * @return true если значения равно лимиту  датчика
     */
    protected abstract boolean isLimit();

    /**Определяем значения перезруза датчика веса.
     * @return true если значение равно перегрузу
     */
    protected abstract boolean isMargin();

    /**Получаем новое значение датчика и преобразуем в вес.
     * @return значение веса.
     */
    protected abstract int updateWeight();

    /**Устанавливаем весы в ноль.
     * @return true Установлен ноль в весовом модуле.
     */
    protected abstract boolean setScaleNull();

    /**Записывем данные в весовой модуль.
     * @return true значения записаны.
     */
    protected abstract boolean writeData();

    protected abstract int getSensorTenzo();

    protected abstract boolean setSpreadsheet(String sheet);

    protected abstract boolean setUsername(String username);

    protected abstract boolean setPassword(String password);

    protected abstract boolean setPhone(String phone);

    /**Получаем значение установленого фильтра в АЦП.
     * @return значение от 1 до 15
     */
    private String getFilterADC() {
        return Commands.CMD_FILTER.getParam();
    }

    protected void loadFilterADC() throws Exception {
        filterADC = Integer.valueOf(getFilterADC());
        if (filterADC < 0 || filterADC > InterfaceVersions.MAX_ADC_FILTER) {
            if (!module.setModuleFilterADC(InterfaceVersions.DEFAULT_ADC_FILTER))
                throw new ErrorModuleException("Фильтер АЦП не установлен в настройках");
            filterADC = InterfaceVersions.DEFAULT_ADC_FILTER;
        }
    }

    protected void loadTimeOff() throws Exception {
        timeOff = Integer.valueOf(module.getModuleTimeOff());
        if (timeOff < InterfaceVersions.MIN_TIME_OFF || timeOff > InterfaceVersions.MAX_TIME_OFF) {
            if (!module.setModuleTimeOff(InterfaceVersions.MIN_TIME_OFF))
                throw new ErrorModuleException("Таймер выключения не установлен в настройках");
            timeOff = InterfaceVersions.MIN_TIME_OFF;
        }
    }

    protected void loadSpeedModule() throws Exception {
        setSpeedPort(Integer.valueOf(module.getModuleSpeedPort()));
        if (getSpeedPort() < 1 || getSpeedPort() > 5) {
            if (!module.setModuleSpeedPort(5))
                throw new ErrorModuleException("Скорость передачи не установлена в настройках");
            setSpeedPort(5);
        }
    }

    protected int getMarginTenzo() {
        return marginTenzo;
    }

    /** Выключить питание модуля.
     * @return true - питание модкля выключено.
     */
    protected boolean powerOff() {
        return Commands.CMD_POWER_OFF.getParam().equals(Commands.CMD_POWER_OFF.getName());
    }

    /**
     * Скорость передачи данных ком порта модуля bluetooth.
     */
    public int getSpeedPort() {
        return speedPort;
    }

    public void setSpeedPort(int speedPort) {
        this.speedPort = speedPort;
    }

    protected static class ErrorTerminalException extends Exception {

        private static final long serialVersionUID = -5686828330979812580L;

        public ErrorTerminalException(String s) {
            super(s);
        }

        @Override
        public String getMessage() {
            return super.getMessage();
        }
    }

    protected static class ErrorModuleException extends Exception {

        private static final long serialVersionUID = -5592575601519548599L;

        public ErrorModuleException(String s) {
            super(s);
        }

        @Override
        public String getMessage() {
            return super.getMessage();
        }
    }

    /**A simple class that provides utilities to ease command line parsing.
     */
    static class SimpleCommandLineParser {

        private final Map<String, String> argMap;

        public SimpleCommandLineParser(String[] arg, String predict) {
            argMap = new HashMap<String, String>();
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
