package com.konst.module;

/**
 * Created by Kostya on 07.07.2015.
 */
public interface OnEventConnectResult {
    /**
     * ��������� � ���������� ����������.
     * ������������ ��� ������������� ������ init().
     * @param what ��������� ���������� ��������� ResultConnect
     * @see Module.ResultConnect
     */
    void handleResultConnect(Module.ResultConnect what);
    /**
     * ��������� �� ������� ����������.
     * ������������ ��� ������������ ������ init().
     * @param what  ��������� ����� ������� ��������� Error
     * @param error �������� ������
     * @see Module.ResultError
     */
    void handleConnectError(Module.ResultError what, String error);
}
