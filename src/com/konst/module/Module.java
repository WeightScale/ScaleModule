package com.konst.module;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/** ������� ������
 * @author Kostya
 */
public abstract class Module extends Handler {
    private static BluetoothDevice device;
    private static final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothSocket socket;
    private static OutputStream os;
    private static InputStream is;
    /** ��������� ����� �������� ��� ��������� ����� */
    static final int TIMEOUT_GET_BYTE = 2000;
    /** ��������� ��������� ���������� */
    public enum ResultConnect {
        /**���������� � �������� ������ �� �������� ������ �������*/
        STATUS_LOAD_OK,
        /** ���������� ������ �������� ������ */
        STATUS_SCALE_UNKNOWN,
        /** ����� ������ ������������� (����� ������������ ��� �������� �������� �������) */
        STATUS_ATTACH_FINISH,
        /** ������ ������ ������������� (����� ������������ ��� �������� �������� �������) */
        STATUS_ATTACH_START
    }
    /** ��������� ������ ���������� */
    public enum ResultError{
        /** ������ �������� ��������� */
        TERMINAL_ERROR,
        /** ������ �������� �������� ������ */
        MODULE_ERROR,
        /** ������ ���������� � ������� */
        CONNECT_ERROR
    }

    /** ��������� � ���������� ����������.
     * ������������ ����� ������ ������ init()
     * @param what ��������� ���������� ��������� ResultConnect*/
    public abstract void handleResultConnect(ResultConnect what);
    /** ��������� �� ������� ����������. ������������ ����� ����� ������ init()
     * @param what ��������� ����� ������� ��������� Error
     * @param error �������� ������*/
    public abstract void handleConnectError(ResultError what, String error);

    /** ������������� � ���������� � ������� �������.
     * @param device bluetooth ����������*/
    protected void init( BluetoothDevice device){
        this.device = device;
    }

    protected void init(String address) throws Exception {
        device = bluetoothAdapter.getRemoteDevice(address);
    }

    protected static synchronized String cmd(String cmd) { //������� ������� � �������� �����
        try {
            synchronized (ScaleModule.class) {
                int t = is.available();
                if (t > 0) {
                    is.read(new byte[t]);
                }

                sendCommand(cmd);
                StringBuilder response = new StringBuilder();

                for (int i = 0; i < 400 && response.length() < 129; ++i) {
                    Thread.sleep(1L);
                    if (is.available() > 0) {
                        i = 0;
                        char ch = (char) is.read();
                        if (ch == '\uffff') {
                            connect();
                            break;
                        }
                        if (ch == '\r')
                            continue;
                        if (ch == '\n')
                            if (response.toString().startsWith(cmd.substring(0, 3)))
                                return response.replace(0, 3, "").toString().isEmpty() ? cmd.substring(0, 3) : response.toString();
                            else
                                return "";

                        response.append(ch);
                    }
                }
            }

        } catch (IOException | InterruptedException ioe) {
        }

        try {
            connect();
        } catch (IOException e) {
        }
        return "";
    }

    private static synchronized void sendCommand(String cmd) throws IOException {
        os.write(cmd.getBytes());
        os.write((byte) 0x0D);
        os.write((byte) 0x0A);
        os.flush(); //��� ���� ����� ������?
    }

    public static synchronized boolean sendByte(byte ch) {
        try {
            int t = is.available();
            if (t > 0) {
                is.read(new byte[t]);
            }
            os.write(ch);
            os.flush(); //��� ���� ����� ������?
            return true;
        } catch (IOException ioe) {
        }
        try {
            connect();
        } catch (IOException e) {
        }
        return false;
    }

    public static synchronized int getByte() {

        try {
            for (int i = 0; i < TIMEOUT_GET_BYTE; i++) {
                if (is.available() > 0) {
                    return is.read(); //��������� ������ (����)
                }
                Thread.sleep(1);
            }
            return 0;
        } catch (IOException | InterruptedException ioe) {
        }

        try {
            connect();
        } catch (IOException e) {
        }
        return 0;
    }

    protected static synchronized void connect() throws IOException { //����������� � ������
        disconnect();
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        is = socket.getInputStream();
        os = socket.getOutputStream();
    }

    protected static void disconnect() { //��������������
        try {
            if (socket != null)
                socket.close();
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        } catch (IOException ioe) {
            socket = null;
            //return;
        }
        is = null;
        os = null;
        socket = null;
    }

    public static BluetoothDevice getDevice(){ return device; }

    public BluetoothAdapter getAdapter(){ return bluetoothAdapter; }

    /** �������� ������ ����� �� �������� ������
     * @return ������ �������� ������ � ��������� ����*/
    public static String getModuleVersion() {
        return cmd(InterfaceVersions.CMD_VERSION);
    }


}
