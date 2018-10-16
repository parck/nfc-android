package cn.edots.nfc;

public enum Status {
        SELECT_TAG(new byte[]{}),
        SELECT_CC(new byte[]{
                (byte) 0x00,
                (byte) 0xA4,
                (byte) 0x00,
                (byte) 0x0C,
                (byte) 0x02,
                (byte) 0xE1,
                (byte) 0x03
        }),
        READ_CC(new byte[]{
                (byte) 0x00,
                (byte) 0xB0,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x0F
        }),
        SELECT_NDEF(new byte[]{
                (byte) 0x00,
                (byte) 0xA4,
                (byte) 0x00,
                (byte) 0x0C,
                (byte) 0x02,
                (byte) 0x00,
                (byte) 0x00
        }),
        READ_MESSAGE_LEN(new byte[]{
                (byte) 0x00,
                (byte) 0xB0,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x02
        }),
        READ_NDEF(new byte[]{
                (byte) 0x00,
                (byte) 0xB0,
                (byte) 0x00,
                (byte) 0x02,
                (byte) 0x30
        });

        private byte[] args;

        Status(byte[] args) {
            this.args = args;
        }

        public byte[] getArgs() {
            return args;
        }
    }