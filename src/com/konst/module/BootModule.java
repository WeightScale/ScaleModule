package com.konst.module;

import java.io.IOException;

/**
 * Класс для самопрограммирования весового модуля
 * @author Kostya
 */
public abstract class BootModule extends ScaleModule {
    String version;

    /** Инициализация соединения с будлодером.
     * Перед инициализациеи надо создать класс com.kostya.module.BootModule
     * @param bootVersion Имя будлодера для синхронизации с весовым модулем.
     * @param address адресс bluetooth модуля весов.
     * @throws Exception неправильный формат адреса bluetooth*/
    @Override
    public void init(String bootVersion, String address) throws Exception {
        version = bootVersion;
        device = bluetoothAdapter.getRemoteDevice(address);
        attachBoot();
    }

    private void attachBoot() /*throws Throwable*/ {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connect();
                    handleResultConnect(ResultConnect.STATUS_LOAD_OK);
                } catch (IOException e) {
                    handleConnectError(Error.CONNECT_ERROR, e.getMessage());
                }
                handleResultConnect(ResultConnect.STATUS_ATTACH_FINISH);
            }
        });

        handleResultConnect(ResultConnect.STATUS_ATTACH_START);
        t.start();
    }



}
