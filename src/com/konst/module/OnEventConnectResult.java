package com.konst.module;

/**
 * @author Kostya
 */
public interface OnEventConnectResult {

    /**Сообщения о результате соединения.
     * Используется при инициализации метода init().
     * @param what Результат соединения константа ResultConnect
     * @see Module.ResultConnect
     */
    void handleResultConnect(Module.ResultConnect what);

    /**Сообщения об ошибках соединения.
     * Используется при инициализаци метода init().
     * @param what  Результат какая ошибака константа Error
     * @param error описание ошибки
     * @see Module.ResultError
     */
    void handleConnectError(Module.ResultError what, String error);
}
