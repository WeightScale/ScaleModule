/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.konst.module.scale;

import android.bluetooth.BluetoothDevice;
import com.konst.module.Commands;
import com.konst.module.ConnectResultCallback;
import com.konst.module.ErrorModuleException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Kostya
 */
public class ScaleVersion4 implements InterfaceScaleVersion {
    private final ScaleModule scaleModule;
    /** Показание датчика веса с учетом offset.  */
    private int sensorTenzoOffset;
    /** Разница знечений между значение ноля до и после. */
    private int offset;
    /** Текущий вес.  */
    private int weight;
    /** Максимальный вес для весов. */
    private int weightMax;
    /** Предельное показани датчика. */
    protected int marginTenzo;


    ScaleVersion4(ScaleModule module){
        scaleModule = module;
    }

    @Override
    public void load() throws Exception {
        //======================================================================
        scaleModule.setFilterADC(Integer.valueOf(Commands.CMD_FILTER.getParam()));
        if (scaleModule.getFilterADC() < 0 || scaleModule.getFilterADC() > MAX_ADC_FILTER) {
            if (!scaleModule.setModuleFilterADC(DEFAULT_ADC_FILTER))
                throw new ErrorModuleException("Фильтер АЦП не установлен в настройках");
        }
        //======================================================================
        scaleModule.setTimeOff(Integer.valueOf(Commands.CMD_TIMER.getParam()));
        if (scaleModule.getTimeOff() < MIN_TIME_OFF || scaleModule.getTimeOff() > MAX_TIME_OFF) {
            if (!scaleModule.setModuleTimeOff(MIN_TIME_OFF))
                throw new ErrorModuleException("Таймер выключения не установлен в настройках");
        }
        //======================================================================
        int speed = Integer.valueOf(Commands.CMD_SPEED.getParam());
        if (speed < 1 || speed > 5) {
            if (!Commands.CMD_SPEED.setParam(5))
                throw new ErrorModuleException("Скорость передачи не установлена в настройках");
        }
        //======================================================================
        try {
            offset = Integer.valueOf(Commands.CMD_GET_OFFSET.getParam());
        } catch (Exception e) {
            throw new ErrorModuleException("Сделать обнуление в настройках");
        }
        //======================================================================
        scaleModule.spreadsheet = Commands.CMD_SPREADSHEET.getParam();
        scaleModule.username = Commands.CMD_G_USER.getParam();
        scaleModule.password = Commands.CMD_G_PASS.getParam();
        scaleModule.phone = Commands.CMD_PHONE.getParam();
        //======================================================================

        parserData(Commands.CMD_DATA.getParam());

        scaleModule.setWeightMargin((int) (weightMax * 1.2));
        marginTenzo = (int) ((weightMax / scaleModule.getCoefficientA()) * 1.2);
    }

    /** Проверка лимита нагрузки сенсора.
     * @return true - Лимит нагрузки сенсора превышен.
     */
    @Override
    public boolean isLimit() {
        //return Math.abs(sensorTenzoOffset + offset) > scaleModule.getLimitTenzo();
        return Math.abs(scaleModule.getSensorTenzo()) > scaleModule.getLimitTenzo();
    }

    @Override
    public boolean isMargin() {
        //return Math.abs(sensorTenzoOffset + offset) > marginTenzo;
        return Math.abs(scaleModule.getSensorTenzo()) > marginTenzo;
    }

    /*@Override
    public int getSensor() {
        return Integer.valueOf(Commands.CMD_SENSOR.getParam());
    }*/

    /** Обновить значения веса.
     * Получаем показания сенсора и переводим в занчение веса.
     * @return Значение веса.
     */
    @Override
    public synchronized int updateWeight() {
        try {
            sensorTenzoOffset = Integer.valueOf(Commands.CMD_SENSOR_OFFSET.getParam());
            return weight = (int) (sensorTenzoOffset * scaleModule.getCoefficientA());
        } catch (Exception e) {
            return sensorTenzoOffset= weight = Integer.MIN_VALUE;
        }
    }

    /**Записать данные параметров в модуль.
     * @return true - Данные записаны.
     * @see Commands#CMD_DATA
     */
    @Override
    public boolean writeData() {
        return Commands.CMD_DATA.setParam(CMD_DATA_CFA + '=' + scaleModule.getCoefficientA() + ' ' +
                        CMD_DATA_WGM + '=' + weightMax + ' ' +
                        CMD_DATA_LMT + '=' + scaleModule.getLimitTenzo());
    }

    @Override
    public int getWeight() { return weight; }

    @Override
    public int getMarginTenzo() { return marginTenzo; }

    @Override
    public int getWeightMax() { return weightMax; }

    @Override
    public void setWeightMax(int weightMax) { this.weightMax = weightMax;}

    /*@Override
    public void setSensor(int sensor) {
        sensorTenzoOffset = sensor;
    }*/

    /**Проверка данных полученых от модуля.
     * Формат параметра данных: [[{@link InterfaceScaleVersion#CMD_DATA_CFA}=[значение]] [{@link InterfaceScaleVersion#CMD_DATA_WGM}=[значение]] [{@link InterfaceScaleVersion#CMD_DATA_LMT}=[значение]]]
     * @param d Данные
     * @throws Exception Данные не правельные.
     * @see ScaleVersion4#load()
     * @see Commands#CMD_DATA
     */
    private void parserData(String d) throws Exception {
        String[] parts = d.split(" ", 0);
        SimpleCommandLineParser data = new SimpleCommandLineParser(parts, "=");
        Iterator<String> iteratorData = data.getKeyIterator();
        //synchronized (this) {
        while (iteratorData.hasNext()) {
            switch (iteratorData.next()) {
                case CMD_DATA_CFA:
                    scaleModule.setCoefficientA(Float.valueOf(data.getValue(CMD_DATA_CFA)));//получаем коэфициент
                    if (scaleModule.getCoefficientA() == 0.0f)
                        throw new ErrorModuleException("Коэффициент А=" + scaleModule.getCoefficientA());
                    break;
                case CMD_DATA_CFB:
                    scaleModule.setCoefficientB(Float.valueOf(data.getValue(CMD_DATA_CFB)));//получить offset
                    break;
                case CMD_DATA_WGM:
                    weightMax = Integer.parseInt(data.getValue(CMD_DATA_WGM));//получаем макимальнай вес
                    if (weightMax <= 0)
                        throw new ErrorModuleException("Предельный вес =" + weightMax);
                    break;
                case CMD_DATA_LMT:
                    scaleModule.setLimitTenzo(Integer.parseInt(data.getValue(CMD_DATA_LMT))); //получаем макимальнай показание перегруза
                    break;
                default:
            }
        }
        //}
    }

    /** Парсер комманды. */
    static class SimpleCommandLineParser {

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
