package com.konst.module;

import java.io.IOException;

/**
 * Класс для самопрограммирования весового модуля
 *
 * @author Kostya
 */
public class BootModule extends Module {
    String version = "";

    /**
     * Конструктор модуля бутлодера.
     *
     * @param version Верситя бутлодера.
     */
    public BootModule(String version, OnEventConnectResult event)throws Exception{
        super(event);
        this.version = version;
    }

    /**
     * Инициализация bluetooth адаптера и модуля.
     * Перед инициализациеи надо создать класс com.kostya.module.BootModule
     * Для соединения {@link BootModule#attach()}
     * @param address     адресс bluetooth модуля весов.
     * @throws Exception Ошибка инициализации.
     * @see Module#init
     */
    public void init( String address) throws Exception {
        init(address);
        //attach();
    }

    public void attach() /*throws Throwable*/ {
        onEventConnectResult.handleResultConnect(ResultConnect.STATUS_ATTACH_START);
        new Thread(runnableBootConnect).start();
    }

    /**
     * Разьеденится с загрузчиком.
     * Вызывать этот метод при закрытии программы.
     */
    public void dettach() {
        removeCallbacksAndMessages(null);
        disconnect();
    }

    /**
     * Обработчик для процесса соединения
     */
    private final Runnable runnableBootConnect = new Runnable() {
        @Override
        public void run() {
            try {
                connect();
                onEventConnectResult.handleResultConnect(ResultConnect.STATUS_LOAD_OK);
            } catch (IOException e) {
                onEventConnectResult.handleConnectError(ResultError.CONNECT_ERROR, e.getMessage());
            }
            onEventConnectResult.handleResultConnect(ResultConnect.STATUS_ATTACH_FINISH);
        }
    };

    /**
     * Комманда старт программирования.
     *
     * @return true - Запущено программирование.
     */
    public boolean start() {
        return "STR".equals(cmd("STR"));
    }

    /**
     * Получить код микросхемы.
     *
     * @return Код в текстовом виде.
     */
    public String getPartCode() {
        return cmd("PRC");
    }

    /**
     * Получить версию железа.
     *
     * @return Версия в текстовом виде.
     */
    public String getHardware() {
        return cmd("HRW");
    }

    /**
     * Получить версию загрузчика.
     *
     * @return Номер версии.
     */
    public int getBootVersion() {
        String vrs = cmd("VRS");
        if (vrs.startsWith(version)) {
            try {
                return Integer.valueOf(vrs.replace(version, ""));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

}
