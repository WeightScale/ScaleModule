package com.konst.module;

import java.io.IOException;

/**
 * Класс для самопрограммирования весового модуля
 * @author Kostya
 */
public abstract class BootModule extends Module {
    String version = "";

    /** Конструктор модуля бутлодера.
     * @param version Верситя бутлодера.
     */
    public BootModule(String version){
        this.version = version;
    }

    /** Инициализация соединения с будлодером.
     * Перед инициализациеи надо создать класс com.kostya.module.BootModule
     * @param bootVersion Имя будлодера для синхронизации с весовым модулем.
     * @param address адресс bluetooth модуля весов.*/
    public void init(String bootVersion, String address) throws Exception {
        init(address);
        version = bootVersion;
        attach();
    }

    private void attach() /*throws Throwable*/ {
        handleResultConnect(ResultConnect.STATUS_ATTACH_START);
        new Thread(runnableBootConnect).start();
    }

    public void dettach(){
        removeCallbacksAndMessages(null);
        disconnect();
    }

    Runnable runnableBootConnect = new Runnable() {
        @Override
        public void run() {
            try {
                connect();
                handleResultConnect(ResultConnect.STATUS_LOAD_OK);
            } catch (IOException e) {
                handleConnectError(ResultError.CONNECT_ERROR, e.getMessage());
            }
            handleResultConnect(ResultConnect.STATUS_ATTACH_FINISH);
        }
    };

    /** Комманда старт программирования.
     * @return true - Запущено программирование.
     */
    public boolean start(){   return cmd("STR").equals("STR"); }

    /** Получить код микросхемы.
     * @return Код в текстовом виде.
     */
    public String getPartCode(){  return cmd("PRC"); }

    /** Получить версию железа.
     * @return Версия в текстовом виде.
     */
    public String getHardware(){  return cmd("HRW");  }

    /** Получить версию загрузчика.
     * @return Номер версии.
     */
    public int getBootVersion(){
        String vrs = cmd("VRS");
        if (vrs.startsWith(this.version)) {
            try {
                return Integer.valueOf(vrs.replace(this.version, ""));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

}
