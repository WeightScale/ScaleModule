package com.konst.module;

import java.io.IOException;

/** Класс для самопрограммирования весового модуля.
 * @author Kostya
 */
public class BootModule extends Module {
    public RunnableBootConnect runnableBootConnect;
    String versionName = "";

    /** Конструктор модуля бутлодера.
     * @param version Верситя бутлодера.
     */
    public BootModule(String version, OnEventConnectResult event)throws Exception{
        super(event);
        runnableBootConnect = new RunnableBootConnect();
        versionName = version;

    }

    @Override
    public void attach(){
        onEventConnectResult.handleResultConnect(ResultConnect.STATUS_ATTACH_START);
        new Thread(runnableBootConnect).start();
    }

    /**
     * Разьеденится с загрузчиком.
     * Вызывать этот метод при закрытии программы.
     */
    @Override
    public void dettach(){
        //removeCallbacksAndMessages(null);todo проверка без handel
        disconnect();
    }

    /** Обработчик для процесса соединения
     */
    private class RunnableBootConnect implements Runnable{

        @Override
        public void run() {
            try {
                connect();
                if(isBootloader()){
                    onEventConnectResult.handleResultConnect(ResultConnect.STATUS_LOAD_OK);
                }else {
                    disconnect();
                    onEventConnectResult.handleResultConnect(ResultConnect.STATUS_VERSION_UNKNOWN);
                }

            } catch (IOException e) {
                onEventConnectResult.handleConnectError(ResultError.CONNECT_ERROR, e.getMessage());
            }
            onEventConnectResult.handleResultConnect(ResultConnect.STATUS_ATTACH_FINISH);
        }
    }

    /**
     * Комманда старт программирования.
     * Версия 2 и выше.
     * @return true - Запущено программирование.
     */
    public boolean startProgramming() {
        return Commands.CMD_START_PROGRAM.getParam().equals(Commands.CMD_START_PROGRAM.getName());
    }

    /**
     * Получить код микроконтролера.
     * Версия 2 и выше.
     * @return Код в текстовом виде.
     */
    public String getPartCode() {
        return Commands.CMD_PART_CODE.getParam();
    }

    /**
     * Получить версию загрузчика.
     *
     * @return Номер версии.
     */
    public int getBootVersion() {
        String vrs = getModuleVersion();
        if (vrs.startsWith(versionName)) {
            try {
                return Integer.valueOf(vrs.replace(versionName, ""));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Определяем имя после соединения это бутлоадер модуль.
     * Указывается имя при инициализации класса com.kostya.module.BootModule.
     *
     * @return true Имя совпадает.
     */
    public boolean isBootloader() {
        String vrs = getModuleVersion(); //Получаем версию весов
        return vrs.startsWith(versionName);
    }

}
