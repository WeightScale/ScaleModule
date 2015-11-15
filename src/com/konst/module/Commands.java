package com.konst.module;

/**
 * @author Kostya
 */
public enum Commands {
    /** Старт программирования. */
    CMD_START_PROGRAM("STR", 200),
    /** Получить код микроконтроллера. */
    CMD_PART_CODE("PRC", 200),
    /** получить версию весов. */
    CMD_VERSION("VRS", 200),
    /** получить/установить АЦП-фильтр. */
    CMD_FILTER("FAD", 200),
    /** получить/установить таймер выключения весов. */
    CMD_TIMER("TOF", 200),
    /** получить/установить скорость передачи данных. */
    CMD_SPEED("BST", 7000),
    /** получить offset. */
    CMD_GET_OFFSET("GCO", 500),
    /** установить offset. */
    CMD_SET_OFFSET("SCO", 500),
    /** получить передать заряд батареи. */
    CMD_BATTERY("GBT", 200),
    /** считать/записать данные температуры. */
    CMD_DATA_TEMP("DTM", 500),
    /** получить версию hardware. */
    CMD_HARDWARE("HRW", 200),
    /** установить имя весов. */
    CMD_NAME("SNA", 200),
    /** каллибровать процент батареи. */
    CMD_CALL_BATTERY("CBT", 400),
    /** Считать/записать данные весов. */
    CMD_DATA("DAT", 200),
    /** получить показание датчика веса. */
    CMD_SENSOR("DCH", 500),
    /** считать/записать имя таблици созданой в google disc. */
    CMD_SPREADSHEET("SGD", 200),
    /** считать/записать account google disc. */
    CMD_G_USER("UGD", 200),
    /** считать/записать password google disc. */
    CMD_G_PASS("PGD", 200),
    /** считать/записать phone for sms boss. */
    CMD_PHONE("PHN", 200),
    /** получить показание датчика веса минус офсет. */
    CMD_SENSOR_OFFSET("DCO", 500),
    /** Выключить питание модуля. */
    CMD_POWER_OFF("POF", 200),
    /** Значение сервис кода. */
    CMD_SERVICE_COD("SRC", 200);

    private final String name;
    private final int time;
    private String cmd;
    static InterfaceVersions interfaceVersions;

    Commands(String n, int t){
        name = n;
        time = t;
    }

    public String toString() { return cmd; }
    public int getTimeOut(){ return time;}

    public String getName(){return name;}
    public String getParam(){
        cmd = name;
        return interfaceVersions.command(this);
    }

    public boolean setParam(String param){
        cmd = name + param;
        return interfaceVersions.command(this).equals(name);
    }

    public boolean setParam(int param){
        cmd = name + param;
        return interfaceVersions.command(this).equals(name);
    }

    public static void setInterfaceCommand(InterfaceVersions i){
        interfaceVersions = i;
    }
}
