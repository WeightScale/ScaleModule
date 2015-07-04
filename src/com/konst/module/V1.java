package com.konst.module;

/**
 * Класс весового модуля
 *
 * @version 1
 */
class V1 extends Versions {

    @Override
    protected void load() throws Exception { //загрузить данные
        loadFilterADC();
        //==============================================================================================================
        loadTimeOff();
        //==============================================================================================================
        isDataValid(Module.cmd(InterfaceVersions.CMD_DATA));

        weightMargin = (int) (weightMax * 1.2);
    }

    @Override
    protected synchronized int updateWeight() {
        try {
            sensorTenzo = Integer.valueOf(Module.cmd(InterfaceVersions.CMD_SENSOR));
            return weight = (int) (coefficientA * sensorTenzo + coefficientB);
        } catch (Exception e) {
            return sensorTenzo = weight = Integer.MIN_VALUE;
        }
    }

    @Override
    protected boolean isLimit() {
        return Math.abs(weight) > weightMax;
    }

    @Override
    protected synchronized boolean setOffsetScale() { //обнуление
        try {
            coefficientB = -coefficientA * Integer.parseInt(Module.cmd(InterfaceVersions.CMD_SENSOR));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean writeData() {
        return Module.cmd(InterfaceVersions.CMD_DATA + 'S' + coefficientA + ' ' + coefficientB + ' ' + weightMax).equals(InterfaceVersions.CMD_DATA);
    }

    @Override
    protected int getSensorTenzo() {
        return sensorTenzoOffset;
    }

    @Override
    protected boolean setSpreadsheet(String sheet) {
        return false;
    }

    @Override
    protected boolean setUsername(String username) {
        return false;
    }

    @Override
    protected boolean setPassword(String password) {
        return false;
    }

    @Override
    protected boolean setPhone(String phone) {
        return false;
    }

    @Override
    protected boolean isMargin() {
        return Math.abs(weight) < weightMargin;
    }

    @Override
    protected boolean setScaleNull() {
        String str = Module.cmd(InterfaceVersions.CMD_SENSOR);
        if (str.isEmpty()) {
            return false;
        }

        if (setOffsetScale()) {
            if (writeData()) {
                sensorTenzo = Integer.valueOf(str);
                return true;
            }
        }
        return false;
    }

    protected void isDataValid(String d) throws Exception {
        StringBuilder dataBuffer = new StringBuilder(d);
        synchronized (this) {
            dataBuffer.deleteCharAt(0);
            String str = dataBuffer.substring(0, dataBuffer.indexOf(" "));
            coefficientA = Float.valueOf(str);
            if (coefficientA == 0.0f)
                throw new ErrorModuleException("Коэффициент А=" + coefficientA);
            dataBuffer.delete(0, dataBuffer.indexOf(" ") + 1);
            str = dataBuffer.substring(0, dataBuffer.indexOf(" "));
            coefficientB = Float.valueOf(str);
            dataBuffer.delete(0, dataBuffer.indexOf(" ") + 1);
            weightMax = Integer.valueOf(dataBuffer.toString());
            if (weightMax <= 0)
                throw new ErrorModuleException("Предельный вес =" + weightMax);
            if (weightMax <= 0) {
                weightMax = 1000;
            }
        }
    }

    /*@Override
    public boolean backupPreference() {
        //SharedPreferences.Editor editor = context.getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE).edit();

            *//*Main.preferencesUpdate.write(CMD_FILTER, String.valueOf(filter));
            Main.preferencesUpdate.write(CMD_TIMER, String.valueOf(timer));
            Main.preferencesUpdate.write(CMD_BATTERY, String.valueOf(battery));
            Main.preferencesUpdate.write(CMD_DATA_CFA, String.valueOf(coefficientA));
            Main.preferencesUpdate.write(CMD_DATA_CFB, String.valueOf(coefficientB));
            Main.preferencesUpdate.write(CMD_DATA_WGM, String.valueOf(weightMax));*//*

        //editor.apply();
        return true;
    }*/

}