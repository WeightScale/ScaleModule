package com.konst.module;

import java.util.Iterator;

/** Класс весового модуля.
 * @version 4
 */
class V4 extends Versions {

    public V4(ScaleModule module) {
        super(module);
    }

    /**Загрузить сохраненные параметры из модуля.
     * @throws Exception Ошибка загрузки параметров.
     */
    @Override
    protected void load() throws Exception {
        //======================================================================
        loadFilterADC();
        //======================================================================
        loadTimeOff();
        //======================================================================
        loadSpeedModule();
        //======================================================================
        try {
            offset = Integer.valueOf(module.getModuleOffsetSensor());
        } catch (Exception e) {
            throw new ErrorModuleException("Сделать обнуление в настройках");
        }
        //======================================================================
        spreadsheet = Commands.CMD_SPREADSHEET.getParam();
        username = Commands.CMD_G_USER.getParam();
        password = Commands.CMD_G_PASS.getParam();
        phone = Commands.CMD_PHONE.getParam();
        //======================================================================

        parserData(Commands.CMD_DATA.getParam());

        weightMargin = (int) (weightMax * 1.2);
        //marginTenzo = (int) ((weightMax * coefficientA) * 1.2);
        marginTenzo = (int) ((weightMax / coefficientA) * 1.2);
    }

    /** Обновить значения веса.
     * Получаем показания сенсора и переводим в занчение веса.
     * @return Значение веса.
     */
    @Override
    protected synchronized int updateWeight() {
        try {
            sensorTenzoOffset = Integer.valueOf(Commands.CMD_SENSOR_OFFSET.getParam());
            //return weight = (int) (sensorTenzoOffset / coefficientA );
            return weight = (int) (sensorTenzoOffset * coefficientA );
        } catch (Exception e) {
            return sensorTenzoOffset = weight = Integer.MIN_VALUE;
        }
    }

    /** Проверка лимита нагрузки сенсора.
     * @return true - Лимит нагрузки сенсора превышен.
     */
    @Override
    protected boolean isLimit() {
        return Math.abs(sensorTenzoOffset + offset) > limitTenzo;
    }

    /**Установить обнуление.
     * @return true - Обнуление установлено.
     */
    @Override
    protected synchronized boolean setOffsetScale() { //обнуление
        return Commands.CMD_SET_OFFSET.getParam().equals(Commands.CMD_SET_OFFSET.getName());
    }

    /**Записать данные параметров в модуль.
     * @return true - Данные записаны.
     * @see Commands#CMD_DATA
     */
    @Override
    protected boolean writeData() {
        return Commands.CMD_DATA.setParam(
                InterfaceVersions.CMD_DATA_CFA + '=' + coefficientA + ' ' +
                        InterfaceVersions.CMD_DATA_WGM + '=' + weightMax + ' ' +
                        InterfaceVersions.CMD_DATA_LMT + '=' + limitTenzo);
    }

    /**Получить показания сенсора с учетом обнуления.
     * @return Значение сенсора.
     * @see V4#setOffsetScale()
     * @see V4#sensorTenzoOffset
     */
    @Override
    protected int getSensorTenzo() {
        return sensorTenzoOffset + offset;
    }

    @Override
    protected boolean isMargin() {
        return Math.abs(sensorTenzoOffset + offset) > marginTenzo;
    }

    /**Установить ноль.
     * @return true - Ноль установлен.
     */
    @Override
    protected boolean setScaleNull() {
        return setOffsetScale();
    }

    /**Проверка данных полученых от модуля.
     * Формат параметра данных: [[{@link InterfaceVersions#CMD_DATA_CFA}=[значение]] [{@link InterfaceVersions#CMD_DATA_WGM}=[значение]] [{@link InterfaceVersions#CMD_DATA_LMT}=[значение]]]
     * @param d Данные
     * @throws Exception Данные не правельные.
     * @see V4#load()
     * @see Commands#CMD_DATA
     */
    protected void parserData(String d) throws Exception {
        String[] parts = d.split(" ", 0);
        SimpleCommandLineParser data = new SimpleCommandLineParser(parts, "=");
        Iterator<String> iteratorData = data.getKeyIterator();
        //synchronized (this) {
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
        //}
    }

    @Override
    protected boolean setSpreadsheet(String sheet) {
        return Commands.CMD_SPREADSHEET.setParam(sheet);
    }

    @Override
    protected boolean setUsername(String username) {
        return Commands.CMD_G_USER.setParam(username);
    }

    @Override
    protected boolean setPassword(String password) {
        return Commands.CMD_G_PASS.setParam(password);
    }

    @Override
    protected boolean setPhone(String phone) {
        return Commands.CMD_PHONE.setParam(phone);
    }

}