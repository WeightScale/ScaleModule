package com.konst.module;

/** Обратный вызов при соеденении с модулем.
 * @author Kostya
 */
public interface ConnectResultCallback {

    /**Сообщения о результате соединения.
     * Используется при инициализации метода init().
     * @param what Результат соединения константа ResultConnect
     * @see Module.ResultConnect
     */
    void resultConnect(Module.ResultConnect what, String arg);

    /**Сообщения об ошибках соединения.
     * Используется при инициализаци метода init().
     * @param what  Результат какая ошибака константа Error
     * @param error описание ошибки
     * @see Module.ResultError
     */
    void connectError(Module.ResultError what, String error);
}
