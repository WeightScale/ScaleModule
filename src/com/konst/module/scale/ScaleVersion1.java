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
import com.konst.module.ErrorModuleException;

/**
 * @author Kostya
 */
public class ScaleVersion1 implements InterfaceScaleVersion {
    private ScaleModule scaleModule;
    /** Текущий вес.  */
    private int weight;
    /** Максимальный вес для весов. */
    private int weightMax;

    ScaleVersion1(ScaleModule module){
        scaleModule = module;
    }

    @Override
    public void load() throws Exception {
        scaleModule.setFilterADC(Integer.valueOf(Commands.CMD_FILTER.getParam()));
        if (scaleModule.getFilterADC() < 0 || scaleModule.getFilterADC() > MAX_ADC_FILTER) {
            if (!scaleModule.setModuleFilterADC(DEFAULT_ADC_FILTER))
                throw new ErrorModuleException("Фильтер АЦП не установлен в настройках");
        }
        //==============================================================================================================
        scaleModule.setTimeOff(Integer.valueOf(Commands.CMD_TIMER.getParam()));
        if (scaleModule.getTimeOff() < MIN_TIME_OFF || scaleModule.getTimeOff() > MAX_TIME_OFF) {
            if (!scaleModule.setModuleTimeOff(MIN_TIME_OFF))
                throw new ErrorModuleException("Таймер выключения не установлен в настройках");
        }
        //==============================================================================================================
        int speed = Integer.valueOf(Commands.CMD_SPEED.getParam());
        if (speed < 1 || speed > 5) {
            if (!Commands.CMD_SPEED.setParam(5))
                throw new ErrorModuleException("Скорость передачи не установлена в настройках");
        }
        //==============================================================================================================
        parserData(Commands.CMD_DATA.getParam());

        scaleModule.setWeightMargin((int) (weightMax * 1.2));
    }

    @Override
    public boolean isLimit() {
        return Math.abs(weight) > weightMax;
    }

    @Override
    public boolean isMargin() {
        return Math.abs(weight) < scaleModule.getWeightMargin();
    }

   /* @Override
    public int getSensor() {
        return sensorTenzo;
    }*/

    @Override
    public synchronized int updateWeight() {
        try {
            scaleModule.setSensorTenzo(Integer.valueOf(Commands.CMD_SENSOR.getParam()));
            //return weight = (int) (coefficientA/sensorTenzo  + coefficientB);
            return weight = (int) (scaleModule.getSensorTenzo() * scaleModule.getCoefficientA()   + scaleModule.getCoefficientB());
        } catch (Exception e) {
            scaleModule.setSensorTenzo(Integer.MIN_VALUE);
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public boolean writeData() {
        return Commands.CMD_DATA.setParam("S" + scaleModule.getCoefficientA() + ' ' + scaleModule.getCoefficientB() + ' ' + weightMax);
    }

    @Override
    public int getWeight() { return weight; }

    @Override
    public int getMarginTenzo() { return 0; }

    @Override
    public int getWeightMax() { return weightMax; }

    @Override
    public void setWeightMax(int weightMax) { this.weightMax = weightMax; }

    /*@Override
    public void setSensor(int sensor) {
        sensorTenzo = sensor;
    }*/

    protected void parserData(String d) throws Exception {
        StringBuilder dataBuffer = new StringBuilder(d);
        //synchronized (this) {
        dataBuffer.deleteCharAt(0);
        String str = dataBuffer.substring(0, dataBuffer.indexOf(" "));
        scaleModule.setCoefficientA(Float.valueOf(str));
        if (scaleModule.getCoefficientA() == 0.0f)
            throw new ErrorModuleException("Коэффициент А=" + scaleModule.getCoefficientA());
        dataBuffer.delete(0, dataBuffer.indexOf(" ") + 1);
        str = dataBuffer.substring(0, dataBuffer.indexOf(" "));
        scaleModule.setCoefficientB(Float.valueOf(str));
        dataBuffer.delete(0, dataBuffer.indexOf(" ") + 1);
        weightMax = Integer.valueOf(dataBuffer.toString());
        if (weightMax <= 0)
            throw new ErrorModuleException("Предельный вес =" + weightMax);
        if (weightMax <= 0) {
            weightMax = 1000;
        }
        //}
    }



}
