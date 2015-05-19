package com.konst.module;

/**
 * Константы общии для всех версий модулей.
 * @author Kostya
 */
public interface InterfaceVersions {

    int MAX_ADC_FILTER = 15;                //максимальное значение фильтра ацп
    int DEFAULT_ADC_FILTER = 8;             //максимальное значение фильтра ацп
    int MAX_TIME_OFF = 60;                  //максимальное время бездействия весов в минутах
    int MIN_TIME_OFF = 10;                  //минимальное время бездействия весов в минутах

    int DIVIDER_AUTO_NULL = 3;                          //делитель для авто ноль

    String CMD_VERSION = "VRS";                         //получить версию весов
    String CMD_FILTER = "FAD";                          //получить/установить АЦП-фильтр
    String CMD_TIMER = "TOF";                           //получить/установить таймер выключения весов
    String CMD_SPEED = "BST";                           //получить/установить скорость передачи данных
    String CMD_GET_OFFSET = "GCO";
    String CMD_SET_OFFSET = "SCO";                      //установить offset
    String CMD_BATTERY = "GBT";                         //получить передать заряд батареи
    String CMD_DATA_TEMP = "DTM";                       //считать/записать данные температуры
    String CMD_HARDWARE = "HRW";                        //получить версию hardware
    String CMD_NAME = "SNA";                            //установить имя весов
    String CMD_CALL_BATTERY = "CBT";                    //каллибровать процент батареи

    String CMD_DATA = "DAT";                //считать/записать данные весов
    String CMD_SENSOR = "DCH";              //получить показание датчика веса
    String CMD_SPREADSHEET = "SGD";         //считать/записать имя таблици созданой в google disc
    String CMD_G_USER = "UGD";              //считать/записать account google disc
    String CMD_G_PASS = "PGD";              //считать/записать password google disc
    String CMD_PHONE = "PHN";               //считать/записать phone for sms boss
    String CMD_SENSOR_OFFSET = "DCO";       //получить показание датчика веса минус офсет

    String CMD_DATA_CFA = "cfa";            //коэфициэнт А
    String CMD_DATA_CFB = "cfb";            //коэфициэнт Б
    String CMD_DATA_WGM = "wgm";            //вес максимальный
    String CMD_DATA_LMT = "lmt";            //лимит тензодатчика

}
