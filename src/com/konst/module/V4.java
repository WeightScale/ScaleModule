package com.konst.module;

import java.util.Iterator;

/**
 *  Класс весового модуля
 *  @version 4
 */
class V4 extends Versions {

    @Override
    protected void load() throws Exception { //загрузить данные
        //======================================================================
        loadFilterADC();
        //======================================================================
        loadTimeOff();
        //======================================================================
        try {
            offset = Integer.valueOf(ScaleModule.getModuleOffsetSensor());
        } catch (Exception e) {
            throw new ErrorModuleException("Сделать обнуление в настройках");
        }
        //======================================================================
        spreadsheet = Module.cmd(InterfaceVersions.CMD_SPREADSHEET);
        username = Module.cmd(InterfaceVersions.CMD_G_USER);
        password = Module.cmd(InterfaceVersions.CMD_G_PASS);
        phone = Module.cmd(InterfaceVersions.CMD_PHONE);
        //======================================================================

        isDataValid(Module.cmd(InterfaceVersions.CMD_DATA));

        weightMargin = (int) (weightMax * 1.2);
        marginTenzo = (int) ((weightMax / coefficientA) * 1.2);
    }

    @Override
    protected synchronized int updateWeight() {
        try {
            sensorTenzoOffset = Integer.valueOf(Module.cmd(InterfaceVersions.CMD_SENSOR_OFFSET));
            return weight = (int) (coefficientA * sensorTenzoOffset);
        } catch (Exception e) {
            return sensorTenzoOffset = weight = Integer.MIN_VALUE;
        }
    }

    @Override
    protected boolean isLimit() {
        return Math.abs(sensorTenzoOffset + offset) > limitTenzo;
    }

    @Override
    protected synchronized boolean setOffsetScale() { //обнуление
        return Module.cmd(InterfaceVersions.CMD_SET_OFFSET).equals(InterfaceVersions.CMD_SET_OFFSET);
    }

    @Override
    protected boolean writeData() {
        return Module.cmd(InterfaceVersions.CMD_DATA +
                InterfaceVersions.CMD_DATA_CFA + '=' + coefficientA + ' ' +
                InterfaceVersions.CMD_DATA_WGM + '=' + weightMax + ' ' +
                InterfaceVersions.CMD_DATA_LMT + '=' + limitTenzo).equals(InterfaceVersions.CMD_DATA);
    }

    @Override
    protected int getSensorTenzo() {
        return sensorTenzoOffset + offset;
    }

    @Override
    protected boolean isMargin() {
        return Math.abs(sensorTenzoOffset + offset) > marginTenzo;
    }

    @Override
    protected boolean setScaleNull() {
        return setOffsetScale();
    }

    protected void isDataValid(String d) throws Exception{
        String[] parts = d.split(" ", 0);
        SimpleCommandLineParser data = new SimpleCommandLineParser(parts, "=");
        Iterator<String> iteratorData = data.getKeyIterator();
        synchronized (this){
            while (iteratorData.hasNext()) {
                switch (iteratorData.next()) {
                    case InterfaceVersions.CMD_DATA_CFA:
                        coefficientA = Float.valueOf(data.getValue(InterfaceVersions.CMD_DATA_CFA));//получаем коэфициент
                        if (coefficientA == 0.0f)
                            throw new ErrorModuleException("Коэффициент А=" + coefficientA);
                        break;
                    case InterfaceVersions.CMD_DATA_CFB:
                        coefficientB = Float.valueOf(data.getValue(InterfaceVersions.CMD_DATA_CFB));//получить offset
                        break;
                    case InterfaceVersions.CMD_DATA_WGM:
                        weightMax = Integer.parseInt(data.getValue(InterfaceVersions.CMD_DATA_WGM));//получаем макимальнай вес
                        if (weightMax <= 0)
                            throw new ErrorModuleException("Предельный вес =" + weightMax);
                        break;
                    case InterfaceVersions.CMD_DATA_LMT:
                        limitTenzo = Integer.parseInt(data.getValue(InterfaceVersions.CMD_DATA_LMT));//получаем макимальнай показание перегруза
                        break;
                    default:
                }
            }
        }
    }

    @Override
    protected boolean setSpreadsheet(String sheet) {
        return Module.cmd(InterfaceVersions.CMD_SPREADSHEET + sheet).equals(InterfaceVersions.CMD_SPREADSHEET);
    }

    @Override
    protected boolean setUsername(String username) {
        return Module.cmd(InterfaceVersions.CMD_G_USER + username).equals(InterfaceVersions.CMD_G_USER);
    }

    @Override
    protected boolean setPassword(String password) {
        return Module.cmd(InterfaceVersions.CMD_G_PASS + password).equals(InterfaceVersions.CMD_G_PASS);
    }

    @Override
    protected boolean setPhone(String phone) {
        return Module.cmd(InterfaceVersions.CMD_PHONE + phone).equals(InterfaceVersions.CMD_PHONE);
    }

}