package cn.edots.nfc;

import android.annotation.TargetApi;
import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * NFC读取回调
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class ReaderCallback extends Handler implements NfcAdapter.ReaderCallback {

    byte[] SELECTHEAD = new byte[]{
            (byte) 0x00, // CLA Class
            (byte) 0xA4, // INS Instruction
            (byte) 0x04, // P1  Parameter 1
            (byte) 0x00, // P2  Parameter 2
            (byte) 0x07, // Length
    };

    private String[] aids;
    private boolean readFinished;
    private final static int NFC_QUERY = 0;
    private final static int NFC_QUERY_ERROR = 1;
    private final static int NFC_INIT_ERROR = 2;
    private final static int NFC_READ_ERROR = 3;
    private final static int NFC_TOAST_MESSAGE = 4;
    private final static int NFC_READ_SUCCESS = 5;
    private final static int NFC_ALERT_MESSAGE = 6;
    private final static int NFC_SHOW_LOADING = 7;
    private final static int NFC_DISMISS_LOADING = 8;

    public void setReadFinished(boolean readFinished) {
        this.readFinished = readFinished;
    }

    public boolean isReadFinished() {
        return readFinished;
    }

    public ReaderCallback(String[] aids) {
        this.aids = aids;
    }

    @Override
    public void handleMessage(Message msg) {
        // TODO you codes
        switch (msg.what) {
            case NFC_QUERY:
                break;
            case NFC_QUERY_ERROR:
                break;
            case NFC_INIT_ERROR:
                break;
            case NFC_READ_ERROR:
                break;
            case NFC_TOAST_MESSAGE:
                break;
            case NFC_READ_SUCCESS:
                break;
            case NFC_ALERT_MESSAGE:
                break;
            case NFC_SHOW_LOADING:
                break;
            case NFC_DISMISS_LOADING:
                break;
        }
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        readFinished = false;
        Observable.create(new ObservableOnSubscribe<Void>() {
            @Override
            public void subscribe(ObservableEmitter<Void> emitter) {
                readTag(tag);
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe();
    }

    private void readTag(Tag tag) {
        if (!readFinished) {
            Message obtain = Message.obtain();
            obtain.what = NFC_QUERY;//读取成功0
            sendMessage(obtain);
            IsoDep isoDep = IsoDep.get(tag);
            try {
                isoDep.connect();
                isoDep.setTimeout(1200);
                int selectLen = 0;
                for (String aid : aids) {
                    byte[] nfcAidByte = NFCHelper.hexStringToByteArray(aid);
                    byte[] aids = new byte[SELECTHEAD.length + nfcAidByte.length];
                    System.arraycopy(SELECTHEAD, 0, aids, 0, SELECTHEAD.length);
                    System.arraycopy(nfcAidByte, 0, aids, SELECTHEAD.length, nfcAidByte.length);//把aid连接到SELECT后面
                    Param param = new Param();
                    param.aids = aids;
                    param.status = Status.SELECT_TAG;
                    param.res = null;
                    param.isoDep = isoDep;
                    byte[] s = onReading(param);
                    if (param.selected) {
                        selectLen++;
                    }
                    if (s != null) {
                        if (s.length >= param.messageLen) {
                            try {
                                byte[] bytes = new byte[param.messageLen];
                                System.arraycopy(s, 0, bytes, 0, bytes.length);
                                NdefMessage ndefMessage = new NdefMessage(bytes);
                                NdefRecord record = ndefMessage.getRecords()[0];
                                NdefTextRecord textRecord = NdefTextRecord.parse(record);
                                if (textRecord != null) {
                                    String text = textRecord.getText();
                                    if (!TextUtils.isEmpty(text)) {
                                        readFinished = true;
                                        obtain = Message.obtain();
                                        obtain.obj = text;
                                        obtain.what = NFC_READ_SUCCESS;
                                        sendMessage(obtain);
                                    }
                                } else {
                                    Message msg3 = new Message();
                                    msg3.what = NFC_READ_ERROR;//读取失败
                                    sendMessage(msg3);
                                }
                            } catch (FormatException e) {
                                Message msg = new Message();
                                msg.what = NFC_QUERY_ERROR;//读取数据成功，但是解析时抛异常
                                sendMessage(msg);
                            }
                        } else {
                            Message msg = new Message();
                            msg.what = NFC_QUERY_ERROR;//读取数据成功，但是解析时抛异常
                            sendMessage(msg);
                        }
                        break;
                    }
                }
                if (selectLen == 0) {
                    obtain = Message.obtain();
                    obtain.what = NFC_ALERT_MESSAGE;
                    sendMessage(obtain);
                }
                isoDep.close();
            } catch (IOException e) {
                if (e instanceof TagLostException) {
                    obtain = Message.obtain();
                    obtain.what = NFC_QUERY_ERROR;
                } else {
                    obtain.what = NFC_ALERT_MESSAGE;
                }
                sendMessage(obtain);
            }
            obtain = Message.obtain();
            obtain.what = NFC_DISMISS_LOADING;
            sendMessage(obtain);
        }
    }

    public byte[] onReading(Param param) throws IOException {
        if (param.res != null && NFCHelper.bytesToHexString(param.res).equalsIgnoreCase("6A82")) { // 6A82 => File or application not found
            return null;
        }
        byte[] args = param.status.getArgs();

        switch (param.status) {
            case SELECT_TAG:
                param.res = param.isoDep.transceive(param.aids);
                if (param.res.length > 1 && param.res[0] == (byte) 0x90) {
                    param.status = Status.SELECT_CC;
                    param.selected = true;
                    return onReading(param);
                } else {
                    param.selected = false;
                    param.status = Status.SELECT_TAG;
                    return onReading(param);
                }
            case SELECT_CC:
                param.res = param.isoDep.transceive(param.status.getArgs());
                if (param.res.length > 1 && param.res[0] == (byte) 0x90) {
                    param.status = Status.READ_CC;
                    return onReading(param);
                } else {
                    param.status = Status.SELECT_TAG;
                    return onReading(param);
                }
            case READ_CC:
                param.res = param.isoDep.transceive(param.status.getArgs());// res data like => 00 0F 20 00 FF 00 34 04 06 E1 04 00 32 00 FF 90 00
                if (param.res.length == 17 && param.res[1] == (byte) 0x0F) {
                    param.maxReadLen = NFCHelper.byteArrayToInt(new byte[]{0x00, 0x00, param.res[3], param.res[4]}, 0);
                    param.status = Status.SELECT_NDEF;
                    return onReading(param);
                } else {
                    param.status = Status.SELECT_TAG;
                    return onReading(param);
                }
            case SELECT_NDEF:
                args[5] = param.res[9];
                args[6] = param.res[10];
                param.res = param.isoDep.transceive(args);
                if (param.res.length > 1 && param.res[0] == (byte) 0x90) {
                    param.status = Status.READ_MESSAGE_LEN;
                    return onReading(param);
                } else {
                    param.status = Status.SELECT_TAG;
                    return onReading(param);
                }
            case READ_MESSAGE_LEN:
                param.res = param.isoDep.transceive(param.status.getArgs());
                if (param.res.length > 2 && param.res[param.res.length - 2] == (byte) 0x90) {
                    param.messageLen = NFCHelper.byteArrayToInt(new byte[]{0x00, 0x00, param.res[0], param.res[1]}, 0);
                    if (param.messageLen > param.maxReadLen)
                        param.readLen = param.maxReadLen;
                    else param.readLen = param.messageLen;
                    param.status = Status.READ_NDEF;
                    return onReading(param);
                } else {
                    param.status = Status.SELECT_TAG;
                    return onReading(param);
                }
            case READ_NDEF:
                byte[] offsets = NFCHelper.intToBytes(param.readOffset);
                args[2] = offsets[2];
                args[3] = offsets[3];
                byte[] readLens = NFCHelper.intToBytes(param.readLen);
                args[4] = readLens[3];
                param.res = param.isoDep.transceive(args);
                if (param.res.length > 2 && param.res[param.res.length - 2] == (byte) 0x90) {
                    byte[] res = new byte[param.res.length - 2];
                    System.arraycopy(param.res, 0, res, 0, res.length);
                    param.ret = NFCHelper.addBytes(param.ret, res);
                    if (param.ret.length >= param.messageLen) {
                        return param.ret;
                    } else {
                        param.readOffset += param.readLen;
                        if ((param.readOffset + param.readLen) > param.messageLen)
                            param.readLen = param.messageLen - param.readOffset;
                        param.status = Status.READ_NDEF;
                        return onReading(param);
                    }
                } else {
                    param.status = Status.SELECT_TAG;
                    return onReading(param);
                }
            default:
                return null;
        }
    }

    public static class Param {
        public IsoDep isoDep;
        public byte[] aids;
        public Status status;
        public byte[] res;
        public int maxReadLen;
        public int messageLen;
        public int readLen;
        public int readOffset = 2;
        public byte[] ret = new byte[0];
        public boolean selected;
    }
}