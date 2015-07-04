package com.konst.module;

/**
 * Константы общии для всех версий модулей.
 *
 * @author Kostya
 */
public interface InterfaceVersions {
    /**
     * максимальное значение фильтра ацп
     */
    int MAX_ADC_FILTER = 15;
    /**
     * максимальное значение фильтра ацп
     */
    int DEFAULT_ADC_FILTER = 8;
    /**
     * максимальное время бездействия весов в минутах
     */
    int MAX_TIME_OFF = 60;
    /**
     * минимальное время бездействия весов в минутах
     */
    int MIN_TIME_OFF = 10;

    /**
     * делитель для авто ноль
     */
    int DIVIDER_AUTO_NULL = 3;
    /**
     * получить версию весов
     */
    String CMD_VERSION = "VRS";
    /**
     * получить/установить АЦП-фильтр
     */
    String CMD_FILTER = "FAD";
    /**
     * получить/установить таймер выключения весов
     */
    String CMD_TIMER = "TOF";
    /**
     * получить/установить скорость передачи данных
     */
    String CMD_SPEED = "BST";
    /**
     * получить offset
     */
    String CMD_GET_OFFSET = "GCO";
    /**
     * установить offset
     */
    String CMD_SET_OFFSET = "SCO";
    /**
     * получить передать заряд батареи
     */
    String CMD_BATTERY = "GBT";
    /**
     * считать/записать данные температуры
     */
    String CMD_DATA_TEMP = "DTM";
    /**
     * получить версию hardware
     */
    String CMD_HARDWARE = "HRW";
    /**
     * установить имя весов
     */
    String CMD_NAME = "SNA";
    /**
     * каллибровать процент батареи
     */
    String CMD_CALL_BATTERY = "CBT";
    /**
     * Считать/записать данные весов
     */
    String CMD_DATA = "DAT";
    /**
     * получить показание датчика веса
     */
    String CMD_SENSOR = "DCH";
    /**
     * считать/записать имя таблици созданой в google disc
     */
    String CMD_SPREADSHEET = "SGD";
    /**
     * считать/записать account google disc
     */
    String CMD_G_USER = "UGD";
    /**
     * считать/записать password google disc
     */
    String CMD_G_PASS = "PGD";
    /**
     * считать/записать phone for sms boss
     */
    String CMD_PHONE = "PHN";
    /**
     * получить показание датчика веса минус офсет
     */
    String CMD_SENSOR_OFFSET = "DCO";
    /**
     * Выключить питание модуля
     */
    String CMD_POWER_OFF = "POF";

    /**
     * коэфициэнт А.
     * Расчитывается при каллибровки весов. Используется для расчета веса.
     * Используется как параметр комманды {@link InterfaceVersions#CMD_DATA}
     *
     * @see InterfaceVersions#CMD_DATA
     */
    String CMD_DATA_CFA = "cfa";
    /**
     * коэфициэнт Б
     * Используется как параметр комманды {@link InterfaceVersions#CMD_DATA}
     *
     * @see InterfaceVersions#CMD_DATA
     */
    String CMD_DATA_CFB = "cfb";
    /**
     * Значение максимального веса для тензодатчика.
     * Используется как параметр комманды {@link InterfaceVersions#CMD_DATA}
     *
     * @see InterfaceVersions#CMD_DATA
     */
    String CMD_DATA_WGM = "wgm";
    /**
     * Значение максимального значения для тензодатчика.
     * Используется как параметр комманды {@link InterfaceVersions#CMD_DATA}
     *
     * @see InterfaceVersions#CMD_DATA
     */
    String CMD_DATA_LMT = "lmt";

}
