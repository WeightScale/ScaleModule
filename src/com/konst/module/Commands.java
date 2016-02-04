package com.konst.module;

/**
 * @author Kostya
 */
public enum Commands {
    /** Старт программирования. */
    CMD_START_PROGRAM("STR", 300),
    /** Получить код микроконтроллера. */
    CMD_PART_CODE("PRC", 300),
    /** получить версию весов. */
    CMD_VERSION("VRS", 300),
    /** получить/установить АЦП-фильтр. */
    CMD_FILTER("FAD", 300),
    /** получить/установить таймер выключения весов. */
    CMD_TIMER("TOF", 300),
    /** получить/установить скорость передачи данных. */
    CMD_SPEED("BST", 7000),
    /** получить offset. */
    CMD_GET_OFFSET("GCO", 1000),
    /** установить offset. */
    CMD_SET_OFFSET("SCO", 1000),
    /** получить передать заряд батареи. */
    CMD_BATTERY("GBT", 300),
    /** считать/записать данные температуры. */
    CMD_DATA_TEMP("DTM", 1000),
    /** получить версию hardware. */
    CMD_HARDWARE("HRW", 300),
    /** установить имя весов. */
    CMD_NAME("SNA", 7000),
    /** каллибровать процент батареи. */
    CMD_CALL_BATTERY("CBT", 400),
    /** Считать/записать данные весов. */
    CMD_DATA("DAT", 300),
    /** получить показание датчика веса. */
    CMD_SENSOR("DCH", 1000),
    /** считать/записать имя таблици созданой в google disc. */
    CMD_SPREADSHEET("SGD", 300),
    /** считать/записать account google disc. */
    CMD_G_USER("UGD", 300),
    /** считать/записать password google disc. */
    CMD_G_PASS("PGD", 300),
    /** считать/записать phone for sms boss. */
    CMD_PHONE("PHN", 300),
    /** получить показание датчика веса минус офсет. */
    CMD_SENSOR_OFFSET("DCO", 1000),
    /** Выключить питание модуля. */
    CMD_POWER_OFF("POF", 300),
    /** Установка мощности модуля. */
    CMD_POWER("MTP", 7000),
    /** Значение сервис кода. */
    CMD_SERVICE_COD("SRC", 300);

    private final String name;
    private final int time;
    private String cmd;
    private static InterfaceVersions interfaceVersions;

    Commands(String n, int t){
        name = n;
        time = t;
    }

    public String toString() { return cmd; }

    /** Получит время timeout комманды.
     * @return Время в милисекундах.
     */
    public int getTimeOut(){ return time;}

    public String getName(){return name;}

    /** Выполнить комманду получить данные.
     * @return Данные выполненой комманды.
     */
    public String getParam(){
        cmd = name;
        return interfaceVersions.command(this);
    }

    /** Выполнить комманду установить данные.
     * @param param Данные для установки.
     * @return true - комманда выполнена.
     */
    public boolean setParam(String param){
        cmd = name + param;
        return interfaceVersions.command(this).equals(name);
    }

    /** Выполнить комманду установить данные.
     * @param param Данные для установки.
     * @return true - комманда выполнена.
     */
    public boolean setParam(int param){
        cmd = name + param;
        return interfaceVersions.command(this).equals(name);
    }

    public static void setInterfaceCommand(InterfaceVersions i){
        interfaceVersions = i;
    }
}
