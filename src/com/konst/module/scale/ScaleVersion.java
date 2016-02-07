/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.konst.module.scale;

import com.konst.module.Commands;

/**
 * @author Kostya
 */
abstract class ScaleVersion {
    /** максимальное значение фильтра ацп. */
    protected static final int MAX_ADC_FILTER = 15;
    /** Значение фильтра ацп по умоляанию. */
    protected static final int DEFAULT_ADC_FILTER = 8;
    /** Максимальное время бездействия весов в минутах. */
    protected static final int MAX_TIME_OFF = 60;
    /** Минимальное время бездействия весов в минутах.  */
    protected static final int MIN_TIME_OFF = 10;

    /** Коэфициэнт А.
     * Расчитывается при каллибровки весов. Используется для расчета веса.
     * Используется как параметр комманды {@link Commands#CMD_DATA}
     * @see Commands#CMD_DATA  */
    public static final String CMD_DATA_CFA = "cfa";
    /** Коэфициэнт Б.
     * Используется как параметр комманды {@link Commands#CMD_DATA}
     *
     * @see Commands#CMD_DATA
     */
    public static final String CMD_DATA_CFB = "cfb";
    /** Значение максимального веса для тензодатчика.
     * Используется как параметр комманды {@link Commands#CMD_DATA}
     *
     * @see Commands#CMD_DATA
     */
    public static final String CMD_DATA_WGM = "wgm";
    /** Значение максимального значения для тензодатчика.
     * Используется как параметр комманды {@link Commands#CMD_DATA}
     *
     * @see Commands#CMD_DATA
     */
    public static final String CMD_DATA_LMT = "lmt";
    /**Загрузить сохраненные параметры из модуля.
     * @throws Exception Ошибка загрузки параметров.
     */
    abstract void load() throws Exception;
    /** Обновить значения веса.
     * Получаем показания сенсора и переводим в занчение веса.
     * @return Значение веса.
     */
    abstract int updateWeight();
    abstract boolean writeData();
    abstract int getWeight();
    abstract int getSensor();
    abstract int getMarginTenzo();
    abstract int getWeightMax();
    abstract void setWeightMax(int weightMax);
    abstract boolean isLimit();
    abstract boolean isMargin();
    abstract boolean setOffsetScale();

}
