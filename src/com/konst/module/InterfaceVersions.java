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
     * коэфициэнт А.
     * Расчитывается при каллибровки весов. Используется для расчета веса.
     * Используется как параметр комманды {@link Commands#CMD_DATA}
     *
     * @see Commands#CMD_DATA
     */
    String CMD_DATA_CFA = "cfa";
    /**
     * коэфициэнт Б
     * Используется как параметр комманды {@link Commands#CMD_DATA}
     *
     * @see Commands#CMD_DATA
     */
    String CMD_DATA_CFB = "cfb";
    /**
     * Значение максимального веса для тензодатчика.
     * Используется как параметр комманды {@link Commands#CMD_DATA}
     *
     * @see Commands#CMD_DATA
     */
    String CMD_DATA_WGM = "wgm";
    /**
     * Значение максимального значения для тензодатчика.
     * Используется как параметр комманды {@link Commands#CMD_DATA}
     *
     * @see Commands#CMD_DATA
     */
    String CMD_DATA_LMT = "lmt";

    String cmd(Commands cmd);

    String command(Commands cmd);

}
